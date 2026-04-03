import axios from "axios";

export class DvsaClient {
    constructor(config, tokenService) {
        this.config = config;
        this.tokenService = tokenService;
        this.httpClient = axios.create({
            baseURL: config.dvsaBaseUrl,
            timeout: 15000
        });
    }

    async lookupVehicleByRegistration(normalizedRegistration) {
        const accessToken = await this.tokenService.getAccessToken();
        return this.httpClient.get(
                `v1/trade/vehicles/registration/${encodeURIComponent(normalizedRegistration)}`,
                {
                    headers: {
                        Authorization: `Bearer ${accessToken}`,
                        "X-API-Key": this.config.dvsaApiKey
                    }
                }
        );
    }
}
