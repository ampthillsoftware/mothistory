package com.ampsoft.MOTHistory.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class DateFormatter {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK);

    private DateFormatter() {
    }

    public static String asDisplayDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.trim().isEmpty()) {
            return "";
        }
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(isoDateTime, INPUT_FORMAT);
            return parsed.getDayOfMonth() + ordinalSuffix(parsed.getDayOfMonth()) + " "
                    + parsed.format(MONTH_YEAR_FORMAT);
        } catch (DateTimeParseException e) {
            return isoDateTime;
        }
    }

    private static String ordinalSuffix(int dayOfMonth) {
        if (dayOfMonth >= 11 && dayOfMonth <= 13) {
            return "th";
        }
        switch (dayOfMonth % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
