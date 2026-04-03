# DVSA Proxy Backend

Minimal Node.js proxy for moving DVSA credentials out of the Android app.

## Current Deployment

- GCP project ID: `mot-history-proxy`
- Cloud Run region: `europe-west2`
- Cloud Run service URL:
  `https://mot-history-proxy-tpdj4giwiq-nw.a.run.app`

Secrets are not stored in this repo. Use the values already present in
`/home/dan/AndroidStudioProjects/MOTHistory/local.properties` for:

- `mot.backendBaseUrl`
- `mot.backendAppKey`
- `dvsa.clientId`
- `dvsa.clientSecret`
- `dvsa.apiKey`
- `dvsa.tenantId`

If `dvsa.accessTokenUrl` is not present locally, derive the backend
`DVSA_ACCESS_TOKEN_URL` from `dvsa.tenantId` as:

```text
https://login.microsoftonline.com/<tenantId>/oauth2/v2.0/token
```

## Why this exists

The Android app currently embeds DVSA direct-auth credentials. That is suitable
for debug/test validation only, but it is not a safe Play Store release model.
This proxy keeps the DVSA client secret and API key on the server and exposes a
single lookup endpoint for the app.

## API shape

- `GET /health`
- `GET /api/vehicles/:registration`
- `GET /v1/trade/vehicles/registration/:registration`

The lookup route:

- uppercases and strips spaces from the registration
- validates the normalized registration format
- fetches/caches a DVSA OAuth access token in memory
- calls the DVSA registration lookup endpoint
- maps common errors to app-friendly responses

## Environment variables

Copy `.env.example` and set:

- `PORT`
- `DVSA_CLIENT_ID`
- `DVSA_CLIENT_SECRET`
- `DVSA_API_KEY`
- `DVSA_ACCESS_TOKEN_URL`
- `DVSA_SCOPE`
- `DVSA_BASE_URL`
- `APP_SHARED_SECRET`

`APP_SHARED_SECRET` is optional. If set, clients must send the same value in
the `x-app-key` header. This is only light friction, not strong protection.

## Run locally

```bash
cd backend
npm install
export DVSA_CLIENT_ID=...
export DVSA_CLIENT_SECRET=...
export DVSA_API_KEY=...
export DVSA_ACCESS_TOKEN_URL=...
npm run dev
```

Example request:

```bash
curl http://localhost:8080/api/vehicles/AB12CDE
```

With optional app key:

```bash
curl -H "x-app-key: your-shared-secret" http://localhost:8080/api/vehicles/AB12CDE
```

The compatibility route below is what the Android app can use without changing
its existing Retrofit path shape:

```bash
curl http://localhost:8080/v1/trade/vehicles/registration/AB12CDE
```

## Test locally with the Android app

### Emulator

Set these keys in the Android project's `local.properties`:

```properties
mot.backendBaseUrl=http://10.0.2.2:8080/
mot.backendAppKey=your-shared-secret
```

Then rebuild/install the debug app. Debug builds now allow cleartext traffic, so
the emulator can reach the local Node server through `10.0.2.2`.

### Physical Android device

The easiest options are:

- deploy the proxy to Cloud Run first and point `mot.backendBaseUrl` at the
  HTTPS service URL
- or expose the local backend through an HTTPS tunnel such as `ngrok` or
  `cloudflared`, then point `mot.backendBaseUrl` at that public HTTPS URL

Example:

```properties
mot.backendBaseUrl=https://your-tunnel-or-cloud-run-url/
mot.backendAppKey=your-shared-secret
```

## Deploy to Cloud Run

The simplest path is source-based deployment.

One-time setup:

```bash
gcloud auth login
gcloud config set project mot-history-proxy
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com
```

Deploy from the repository root:

```bash
cd /home/dan/AndroidStudioProjects/MOTHistory
```

Minimal first deploy:

```bash
gcloud run deploy mot-history-dvsa-proxy \
  --source backend \
  --region europe-west2 \
  --allow-unauthenticated \
  --set-env-vars DVSA_CLIENT_ID=...,DVSA_CLIENT_SECRET=...,DVSA_API_KEY=...,DVSA_ACCESS_TOKEN_URL=...,APP_SHARED_SECRET=...
```

Recommended production adjustments:

- set `--min-instances 0`
- keep memory small, e.g. `--memory 256Mi`
- set a reasonable concurrency, e.g. `--concurrency 20`
- store secrets in Secret Manager instead of plain env vars
- add Cloud Run request authentication or API Gateway later if needed
- add Play Integrity based attestation later if abuse becomes a concern

Example with lightweight production settings:

```bash
gcloud run deploy mot-history-dvsa-proxy \
  --source backend \
  --region europe-west2 \
  --allow-unauthenticated \
  --memory 256Mi \
  --cpu 1 \
  --concurrency 20 \
  --min-instances 0 \
  --max-instances 10 \
  --set-env-vars DVSA_CLIENT_ID=...,DVSA_CLIENT_SECRET=...,DVSA_API_KEY=...,DVSA_ACCESS_TOKEN_URL=...,APP_SHARED_SECRET=...
```

Values for the `--set-env-vars` arguments should come from the current local
Android config, not from committed files:

- `DVSA_CLIENT_ID` <- `dvsa.clientId`
- `DVSA_CLIENT_SECRET` <- `dvsa.clientSecret`
- `DVSA_API_KEY` <- `dvsa.apiKey`
- `APP_SHARED_SECRET` <- `mot.backendAppKey`
- `DVSA_ACCESS_TOKEN_URL` <- `dvsa.accessTokenUrl`, or derive it from
  `dvsa.tenantId` if that key is the only one present locally

After deploy, Cloud Run returns a service URL like:

```text
https://mot-history-dvsa-proxy-xxxxx-ew.a.run.app
```

Use that as:

```properties
mot.backendBaseUrl=https://mot-history-dvsa-proxy-xxxxx-ew.a.run.app/
mot.backendAppKey=your-shared-secret
```

Then rebuild the app and verify a live lookup.

For this project, the currently deployed URL is:

```properties
mot.backendBaseUrl=https://mot-history-proxy-tpdj4giwiq-nw.a.run.app/
```

## Useful Cloud Run Commands

Read the current deployed URL:

```bash
gcloud run services describe mot-history-dvsa-proxy \
  --region europe-west2 \
  --format='value(status.url)'
```

Tail service logs:

```bash
gcloud run services logs tail mot-history-dvsa-proxy --region europe-west2
```

Alternative Cloud Logging tail:

```bash
gcloud logging tail 'resource.type="cloud_run_revision" AND resource.labels.service_name="mot-history-dvsa-proxy"' --project mot-history-proxy
```

Read recent logs:

```bash
gcloud run services logs read mot-history-dvsa-proxy --region europe-west2 --limit 100
```

## Android changes needed after backend is live

Once this service is deployed:

1. Point the Android Retrofit client at the backend base URL.
2. Send the optional `x-app-key` if `APP_SHARED_SECRET` is enabled on the
   backend.
3. Remove `TokenManager`, `AuthTokenService`, and `AuthInterceptor` from the
   Android request path once the proxy path is fully proven.
4. Keep the existing repository/UI error mapping for `404`, `429`, and generic
   service failures.
5. Remove DVSA secrets from `local.properties` for release builds.
