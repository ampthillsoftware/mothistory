package com.ampsoft.MOTHistory.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;

public final class DateFormatter {

    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK);

    private DateFormatter() {
    }

    public static String asDisplayDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.trim().isEmpty()) {
            return "";
        }
        try {
            OffsetDateTime parsed = parseIsoOffsetDateTime(isoDateTime);
            if (parsed == null) {
                throw new DateTimeParseException("Unable to parse date", isoDateTime, 0);
            }
            return parsed.getDayOfMonth() + ordinalSuffix(parsed.getDayOfMonth()) + " "
                    + parsed.format(MONTH_YEAR_FORMAT);
        } catch (DateTimeParseException e) {
            try {
                LocalDate parsed = LocalDate.parse(isoDateTime, LOCAL_DATE_FORMAT);
                return parsed.getDayOfMonth() + ordinalSuffix(parsed.getDayOfMonth()) + " "
                        + parsed.format(MONTH_YEAR_FORMAT);
            } catch (DateTimeParseException ignored) {
                return isoDateTime;
            }
        }
    }

    public static OffsetDateTime parseIsoOffsetDateTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(isoDateTime, INPUT_FORMAT);
        } catch (DateTimeParseException e) {
            try {
                LocalDate parsed = LocalDate.parse(isoDateTime, LOCAL_DATE_FORMAT);
                return parsed.atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    public static LocalDate parseToLocalDate(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDateTime, LOCAL_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            OffsetDateTime parsed = parseIsoOffsetDateTime(isoDateTime);
            return parsed != null ? parsed.toLocalDate() : null;
        }
    }

    public static String extractLatestExpiryDate(Vehicle vehicle) {
        if (vehicle == null || vehicle.getMotTests() == null || vehicle.getMotTests().isEmpty()) {
            return null;
        }
        MotTest latest = null;
        OffsetDateTime latestCompleted = null;
        for (MotTest motTest : vehicle.getMotTests()) {
            if (motTest == null) {
                continue;
            }
            OffsetDateTime completed = parseIsoOffsetDateTime(motTest.getCompletedDate());
            if (completed == null) {
                continue;
            }
            if (latestCompleted == null || completed.isAfter(latestCompleted)) {
                latestCompleted = completed;
                latest = motTest;
            }
        }
        return latest != null ? latest.getExpiryDate() : null;
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
