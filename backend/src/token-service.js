import axios from "axios";

const TOKEN_REFRESH_SKEW_MS = 5 * 60 * 1000;

export class TokenService {
    constructor(config) {
        this.config = config;
        this.cachedToken = null;
        this.expiresAtMs = 0;
        this.inFlightPromise = null;
    }

    async getAccessToken() {
        if (this.hasUsableToken()) {
            return this.cachedToken;
        }

        if (this.inFlightPromise) {
            return this.inFlightPromise;
        }

        this.inFlightPromise = this.fetchAccessToken();
        try {
            return await this.inFlightPromise;
        } finally {
            this.inFlightPromise = null;
        }
    }

    hasUsableToken() {
        return Boolean(this.cachedToken)
                && Date.now() + TOKEN_REFRESH_SKEW_MS < this.expiresAtMs;
    }

    async fetchAccessToken() {
        const params = new URLSearchParams();
        params.set("grant_type", "client_credentials");
        params.set("client_id", this.config.dvsaClientId);
        params.set("client_secret", this.config.dvsaClientSecret);
        params.set("scope", this.config.dvsaScope);

        const response = await axios.post(
                this.config.dvsaAccessTokenUrl,
                params.toString(),
                {
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    timeout: 15000
                }
        );

        const accessToken = response?.data?.access_token;
        const expiresInSeconds = Number.parseInt(response?.data?.expires_in || "3600", 10);
        if (!accessToken) {
            throw new Error("Token endpoint returned no access_token");
        }

        this.cachedToken = accessToken;
        this.expiresAtMs = Date.now() + (expiresInSeconds * 1000);
        return this.cachedToken;
    }
}
