const requiredEnv = [
    "DVSA_CLIENT_ID",
    "DVSA_CLIENT_SECRET",
    "DVSA_API_KEY",
    "DVSA_ACCESS_TOKEN_URL"
];

function readRequired(name) {
    const value = process.env[name];
    if (!value || !value.trim()) {
        throw new Error(`Missing required environment variable: ${name}`);
    }
    return value.trim();
}

export function loadConfig() {
    for (const name of requiredEnv) {
        readRequired(name);
    }

    return {
        port: Number.parseInt(process.env.PORT || "8080", 10),
        dvsaClientId: readRequired("DVSA_CLIENT_ID"),
        dvsaClientSecret: readRequired("DVSA_CLIENT_SECRET"),
        dvsaApiKey: readRequired("DVSA_API_KEY"),
        dvsaAccessTokenUrl: readRequired("DVSA_ACCESS_TOKEN_URL"),
        dvsaScope: (process.env.DVSA_SCOPE || "https://tapi.dvsa.gov.uk/.default").trim(),
        dvsaBaseUrl: (process.env.DVSA_BASE_URL || "https://history.mot.api.gov.uk/").trim(),
        appSharedSecret: (process.env.APP_SHARED_SECRET || "").trim()
    };
}
