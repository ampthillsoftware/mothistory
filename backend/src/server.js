import express from "express";
import { loadConfig } from "./config.js";
import { DvsaClient } from "./dvsa-client.js";
import { normalizeRegistration, isValidRegistration } from "./registration.js";
import { TokenService } from "./token-service.js";

const config = loadConfig();
const tokenService = new TokenService(config);
const dvsaClient = new DvsaClient(config, tokenService);
const app = express();

app.disable("x-powered-by");

app.get("/health", (_request, response) => {
    response.json({
        status: "ok"
    });
});

async function handleVehicleLookup(request, response) {
    if (!isAuthorized(request)) {
        response.status(401).json({
            message: "Unauthorized."
        });
        return;
    }

    const normalizedRegistration = normalizeRegistration(request.params.registration);
    if (!isValidRegistration(normalizedRegistration)) {
        response.status(400).json({
            message: "Invalid registration format."
        });
        return;
    }

    try {
        const dvsaResponse = await dvsaClient.lookupVehicleByRegistration(normalizedRegistration);
        response.status(dvsaResponse.status).json(dvsaResponse.data);
    } catch (error) {
        const status = error?.response?.status || 502;
        if (status === 404) {
            response.status(404).json({ message: "No vehicle found for that registration." });
            return;
        }
        if (status === 429) {
            response.status(429).json({ message: "Rate limit reached. Please try again later." });
            return;
        }
        if (status === 400) {
            response.status(400).json({ message: "Vehicle lookup request was rejected." });
            return;
        }

        console.error("DVSA proxy lookup failed", {
            status,
            detail: error?.response?.data || error?.message
        });
        response.status(502).json({
            message: "Unable to contact MOT service."
        });
    }
}

app.get("/api/vehicles/:registration", handleVehicleLookup);
app.get("/v1/trade/vehicles/registration/:registration", handleVehicleLookup);

app.listen(config.port, () => {
    console.log(`DVSA proxy listening on :${config.port}`);
});

function isAuthorized(request) {
    if (!config.appSharedSecret) {
        return true;
    }

    return request.get("x-app-key") === config.appSharedSecret;
}
