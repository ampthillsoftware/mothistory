package com.ampsoft.MOTHistory.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RegistrationValidator {

    private static final Pattern SIMPLE_UK_REG_PATTERN =
            Pattern.compile("^[A-Z0-9]{2,8}$");

    private RegistrationValidator() {
    }

    public static String normalize(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        return rawValue.replace(" ", "").trim().toUpperCase(Locale.UK);
    }

    public static boolean isValid(String rawValue) {
        String normalized = normalize(rawValue);
        return SIMPLE_UK_REG_PATTERN.matcher(normalized).matches();
    }
}
