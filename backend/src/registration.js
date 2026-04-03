export function normalizeRegistration(rawValue) {
    if (!rawValue) {
        return "";
    }

    return rawValue.replace(/\s+/g, "").trim().toUpperCase();
}

export function isValidRegistration(rawValue) {
    return /^[A-Z0-9]{2,8}$/.test(normalizeRegistration(rawValue));
}
