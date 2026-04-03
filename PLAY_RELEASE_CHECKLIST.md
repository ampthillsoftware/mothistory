# Play Release Checklist

## Privacy And Policy

- [x] Public privacy policy URL created:
  - `https://ampthillsoftware.github.io/mothistory-site/`
- [x] Privacy policy contact email set:
  - `ampthillsoftware@gmail.com`
- [ ] Enter the privacy policy URL in Google Play Console.
- [ ] Complete the Data safety form in Play Console.
- [ ] Complete the Ads declaration in Play Console.
- [ ] Complete App content declarations in Play Console.

## AdMob

- [ ] Resolve `Consent Manager API is not enabled`.
- [ ] Confirm the privacy message is published and active.
- [ ] Re-test live ads on device after AdMob console changes.
- [ ] Confirm Search / Saved Cars / Vehicle details / Full MOT history / Mileage analysis banner behavior.
- [ ] Confirm interstitials are still loading and showing at the intended cadence.

## Store Listing

- [ ] Set app contact details in Play Console:
  - support email: `ampthillsoftware@gmail.com`
- [ ] Write short description.
- [ ] Write full description.
- [x] Prepare screenshots for phone form factors.
- [x] Prepare feature graphic.
- [x] Confirm app icon is final.

## Build And Signing

- [x] Ensure `local.properties` contains:
  - `mot.backendBaseUrl`
  - `mot.backendAppKey`
  - production AdMob IDs
- [x] Create a new Play upload keystore and export its certificate:
  - keystore: `keystores/mothistory-upload.jks`
  - certificate: `keystores/mothistory-upload-cert.pem`
- [x] Build release app bundle:
  - `./gradlew :app:bundleRelease`
- [x] Confirm the generated `.aab` exists under:
  - `app/build/outputs/bundle/release/`
- [x] Verify release signing configuration if not already handled by your environment.
  - `:app:signReleaseBundle` completed successfully during the release build
- [x] Bump the app `versionCode` after rejected uploads consumed earlier codes.
  - current release bundle is now built from `versionCode 3`
- [ ] If the old upload key is lost, request a Play Console upload-key reset
  using `keystores/mothistory-upload-cert.pem`.

## Backend

- [x] Cloud Run proxy deployed.
- [ ] Keep Cloud Run env vars up to date:
  - `DVSA_CLIENT_ID`
  - `DVSA_CLIENT_SECRET`
  - `DVSA_API_KEY`
  - `DVSA_ACCESS_TOKEN_URL`
  - `APP_SHARED_SECRET`
- [ ] Monitor Cloud Run logs during testing and initial rollout.

## Pre-Release QA

- [ ] Verify live MOT lookups succeed through the proxy on a real device.
  - direct Cloud Run probe succeeded for `CE12UGP` on 2026-03-11 with `HTTP/2 200`
- [ ] Verify recent-search swipe removal manually on a real device.
- [ ] Verify saved vehicles still persist correctly.
- [ ] Verify bottom-nav root behavior from detail screens.
- [ ] Verify dark/light/system themes.
- [ ] Verify no obvious crashes on cold start and repeated launches.

## Play Tracks

- [ ] Upload the `.aab` to Internal testing first.
- [ ] Install from Play Internal testing and sanity-check the app.
- [ ] Move to Closed testing if needed.
- [ ] Submit for Production when Play requirements are satisfied.
