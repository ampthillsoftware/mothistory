package com.ampsoft.MOTHistory.ui.result;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.model.Defect;
import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.util.DateFormatter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class MotRecordSummary {

    enum Tone {
        CLEAN,
        REVIEW,
        IMPORTANT,
        LIMITED
    }

    private static final int MAX_SIGNALS = 4;

    private MotRecordSummary() {
    }

    @NonNull
    static Summary build(@NonNull Context context, @Nullable Vehicle vehicle) {
        List<MotTest> tests = MotHistoryInsights.getSortedTestsDescending(vehicle);
        if (tests.isEmpty()) {
            return new Summary(
                    Tone.LIMITED,
                    context.getString(R.string.mot_record_summary_limited_title),
                    context.getString(R.string.mot_record_summary_limited_body),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        List<Signal> signals = new ArrayList<>();
        MotTest latestTest = tests.get(0);
        boolean latestFailed = isFail(latestTest);
        int latestAdvisories = countDefectsOfType(latestTest, "ADVISORY");
        int latestFailures = countLatestFailureItems(latestTest);

        if (latestFailed) {
            signals.add(new Signal(
                    Tone.IMPORTANT,
                    context.getString(R.string.mot_record_signal_latest_fail)
            ));
            if (latestFailures > 0) {
                signals.add(new Signal(
                        Tone.IMPORTANT,
                        context.getString(
                                R.string.mot_record_signal_latest_failure_items,
                                latestFailures,
                                pluralize("item", latestFailures)
                        )
                ));
            }
        }

        LocalDate expiryDate = DateFormatter.parseToLocalDate(latestTest.getExpiryDate());
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
            signals.add(new Signal(
                    Tone.IMPORTANT,
                    context.getString(
                            R.string.mot_record_signal_expired,
                            DateFormatter.asDisplayDate(latestTest.getExpiryDate())
                    )
            ));
        }

        if (latestAdvisories > 0) {
            signals.add(new Signal(
                    Tone.REVIEW,
                    context.getString(
                            R.string.mot_record_signal_latest_advisories,
                            latestAdvisories,
                            pluralize("advisory", latestAdvisories)
                    )
            ));
        }

        addRecurringThemeSignals(context, tests, signals);

        MotHistoryInsights.MileageStats mileageStats = MotHistoryInsights.buildMileageStats(vehicle);
        if (mileageStats.hasMileageDrop()) {
            signals.add(new Signal(
                    Tone.IMPORTANT,
                    context.getString(R.string.mot_record_signal_mileage_drop)
            ));
        } else if (mileageStats.getPointsAscending().size() >= 2) {
            signals.add(new Signal(
                    Tone.CLEAN,
                    context.getString(R.string.mot_record_signal_mileage_consistent)
            ));
        }

        Tone tone = chooseTone(signals, latestFailed, latestAdvisories);
        return new Summary(
                tone,
                titleForTone(context, tone),
                bodyForTone(context, tone),
                topSignals(signals),
                buildYearlyChangeSignals(context, tests)
        );
    }

    @NonNull
    private static List<String> buildYearlyChangeSignals(
            @NonNull Context context,
            @NonNull List<MotTest> tests
    ) {
        Map<Integer, List<MotTest>> testsByYear = groupTestsByCompletedYear(tests);
        if (testsByYear.size() < 2) {
            return Collections.singletonList(
                    context.getString(R.string.mot_record_changes_not_enough_history)
            );
        }

        List<Integer> yearsDescending = new ArrayList<>(testsByYear.keySet());
        Collections.sort(yearsDescending, Collections.reverseOrder());

        int currentYear = yearsDescending.get(0);
        int previousYear = yearsDescending.get(1);
        List<MotTest> currentYearTests = testsByYear.get(currentYear);
        List<MotTest> previousYearTests = testsByYear.get(previousYear);
        if (currentYearTests == null || currentYearTests.isEmpty()
                || previousYearTests == null || previousYearTests.isEmpty()) {
            return Collections.singletonList(
                    context.getString(R.string.mot_record_changes_not_enough_history)
            );
        }

        MotTest currentFinal = currentYearTests.get(0);
        MotTest previousFinal = previousYearTests.get(0);
        List<String> changes = new ArrayList<>();
        changes.add(context.getString(
                R.string.mot_record_changes_compare_years,
                currentYear,
                previousYear
        ));

        String currentResult = displayResult(currentFinal);
        String previousResult = displayResult(previousFinal);
        if (!currentResult.equals(previousResult)) {
            changes.add(context.getString(
                    R.string.mot_record_changes_result_changed,
                    previousResult,
                    currentResult
            ));
        } else {
            changes.add(context.getString(
                    R.string.mot_record_changes_result_same,
                    currentResult
            ));
        }

        DefectComparison defectComparison = compareDefects(previousFinal, currentFinal);
        if (defectComparison.getNewCount() > 0) {
            changes.add(context.getString(
                    R.string.mot_record_changes_new_items,
                    defectComparison.getNewCount(),
                    pluralize("item", defectComparison.getNewCount())
            ));
        }
        if (defectComparison.getRemovedCount() > 0) {
            changes.add(context.getString(
                    R.string.mot_record_changes_removed_items,
                    defectComparison.getRemovedCount(),
                    pluralize("item", defectComparison.getRemovedCount())
            ));
        }
        if (defectComparison.getRepeatedCount() > 0) {
            changes.add(context.getString(
                    R.string.mot_record_changes_repeated_items,
                    defectComparison.getRepeatedCount(),
                    pluralize("item", defectComparison.getRepeatedCount())
            ));
        }
        if (hasEarlierSameYearFailure(currentYearTests, currentFinal)) {
            changes.add(context.getString(R.string.mot_record_changes_same_year_fail));
        }

        String mileageDelta = formatMileageDelta(currentFinal, previousFinal);
        if (mileageDelta != null) {
            changes.add(context.getString(
                    R.string.mot_record_changes_mileage_delta,
                    mileageDelta
            ));
        }

        if (changes.size() == 1) {
            changes.add(context.getString(R.string.mot_record_changes_no_specific));
        }
        return changes;
    }

    @NonNull
    private static Map<Integer, List<MotTest>> groupTestsByCompletedYear(@NonNull List<MotTest> tests) {
        Map<Integer, List<MotTest>> testsByYear = new LinkedHashMap<>();
        for (MotTest test : tests) {
            OffsetDateTime completedDate = parseCompletedDate(test != null ? test.getCompletedDate() : null);
            if (completedDate == null) {
                continue;
            }
            int year = completedDate.getYear();
            List<MotTest> yearTests = testsByYear.get(year);
            if (yearTests == null) {
                yearTests = new ArrayList<>();
                testsByYear.put(year, yearTests);
            }
            yearTests.add(test);
        }
        return testsByYear;
    }

    @NonNull
    private static DefectComparison compareDefects(@NonNull MotTest previousFinal, @NonNull MotTest currentFinal) {
        Set<String> previousDefects = comparableDefects(previousFinal);
        Set<String> currentDefects = comparableDefects(currentFinal);
        int newCount = 0;
        int removedCount = 0;
        int repeatedCount = 0;

        for (String currentDefect : currentDefects) {
            if (previousDefects.contains(currentDefect)) {
                repeatedCount++;
            } else {
                newCount++;
            }
        }
        for (String previousDefect : previousDefects) {
            if (!currentDefects.contains(previousDefect)) {
                removedCount++;
            }
        }

        return new DefectComparison(newCount, removedCount, repeatedCount);
    }

    @NonNull
    private static Set<String> comparableDefects(@Nullable MotTest test) {
        Set<String> defects = new HashSet<>();
        if (test == null || test.getDefects() == null) {
            return defects;
        }
        for (Defect defect : test.getDefects()) {
            if (defect == null) {
                continue;
            }
            String type = normalize(defect.getType());
            if (!isComparableDefectType(type, defect.isDangerous())) {
                continue;
            }
            String text = defect.getText() == null ? "" : defect.getText().trim().toLowerCase(Locale.UK);
            if (!text.isEmpty()) {
                defects.add(type + ":" + text.replaceAll("\\s+", " "));
            }
        }
        return defects;
    }

    private static boolean isComparableDefectType(@NonNull String type, boolean dangerous) {
        return dangerous
                || "ADVISORY".equals(type)
                || "MAJOR".equals(type)
                || "DANGEROUS".equals(type)
                || "FAIL".equals(type);
    }

    private static boolean hasEarlierSameYearFailure(
            @NonNull List<MotTest> currentYearTests,
            @NonNull MotTest currentFinal
    ) {
        for (int i = 1; i < currentYearTests.size(); i++) {
            MotTest test = currentYearTests.get(i);
            if (test != currentFinal && isFail(test)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String formatMileageDelta(@NonNull MotTest currentFinal, @NonNull MotTest previousFinal) {
        Long currentMileage = parseMileage(currentFinal.getOdometerValue());
        Long previousMileage = parseMileage(previousFinal.getOdometerValue());
        String currentUnit = currentFinal.getOdometerUnit();
        String previousUnit = previousFinal.getOdometerUnit();
        if (currentMileage == null || previousMileage == null
                || currentUnit == null || previousUnit == null
                || !currentUnit.trim().equalsIgnoreCase(previousUnit.trim())) {
            return null;
        }
        return MotHistoryInsights.formatDelta(currentMileage - previousMileage, currentUnit.trim());
    }

    @Nullable
    private static Long parseMileage(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static OffsetDateTime parseCompletedDate(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @NonNull
    private static String displayResult(@Nullable MotTest test) {
        String result = normalize(test != null ? test.getTestResult() : null);
        if (result.contains("FAIL")) {
            return "fail";
        }
        if (result.contains("PASS") && countDefectsOfType(test, "ADVISORY") > 0) {
            return "pass with advisories";
        }
        if (result.contains("PASS")) {
            return "pass";
        }
        return "not available";
    }

    private static void addRecurringThemeSignals(
            @NonNull Context context,
            @NonNull List<MotTest> tests,
            @NonNull List<Signal> signals
    ) {
        Map<String, Integer> themeCounts = new LinkedHashMap<>();
        Map<String, String> themeLabels = new LinkedHashMap<>();
        themeLabels.put("tyre", context.getString(R.string.mot_record_theme_tyres));
        themeLabels.put("brake", context.getString(R.string.mot_record_theme_brakes));
        themeLabels.put("suspension", context.getString(R.string.mot_record_theme_suspension));
        themeLabels.put("corrosion", context.getString(R.string.mot_record_theme_corrosion));
        themeLabels.put("lamp", context.getString(R.string.mot_record_theme_lights));
        themeLabels.put("exhaust", context.getString(R.string.mot_record_theme_exhaust));
        themeLabels.put("steering", context.getString(R.string.mot_record_theme_steering));

        for (MotTest test : tests) {
            if (test == null || test.getDefects() == null) {
                continue;
            }
            Set<String> themesInTest = new HashSet<>();
            for (Defect defect : test.getDefects()) {
                String text = defect == null || defect.getText() == null
                        ? ""
                        : defect.getText().toLowerCase(Locale.UK);
                for (String theme : themeLabels.keySet()) {
                    if (text.contains(theme) || ("lamp".equals(theme) && text.contains("light"))) {
                        themesInTest.add(theme);
                    }
                }
            }
            for (String theme : themesInTest) {
                Integer count = themeCounts.get(theme);
                themeCounts.put(theme, count == null ? 1 : count + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : themeCounts.entrySet()) {
            int count = entry.getValue();
            if (count < 2 || signals.size() >= MAX_SIGNALS) {
                continue;
            }
            signals.add(new Signal(
                    Tone.REVIEW,
                    context.getString(
                            R.string.mot_record_signal_recurring_theme,
                            themeLabels.get(entry.getKey()),
                            count
                    )
            ));
        }
    }

    private static Tone chooseTone(@NonNull List<Signal> signals, boolean latestFailed, int latestAdvisories) {
        if (latestFailed || hasTone(signals, Tone.IMPORTANT)) {
            return Tone.IMPORTANT;
        }
        if (latestAdvisories > 0 || hasTone(signals, Tone.REVIEW)) {
            return Tone.REVIEW;
        }
        return Tone.CLEAN;
    }

    private static boolean hasTone(@NonNull List<Signal> signals, @NonNull Tone tone) {
        for (Signal signal : signals) {
            if (signal.getTone() == tone) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String titleForTone(@NonNull Context context, @NonNull Tone tone) {
        switch (tone) {
            case IMPORTANT:
                return context.getString(R.string.mot_record_summary_important_title);
            case REVIEW:
                return context.getString(R.string.mot_record_summary_review_title);
            case LIMITED:
                return context.getString(R.string.mot_record_summary_limited_title);
            case CLEAN:
            default:
                return context.getString(R.string.mot_record_summary_clean_title);
        }
    }

    @NonNull
    private static String bodyForTone(@NonNull Context context, @NonNull Tone tone) {
        switch (tone) {
            case IMPORTANT:
                return context.getString(R.string.mot_record_summary_important_body);
            case REVIEW:
                return context.getString(R.string.mot_record_summary_review_body);
            case LIMITED:
                return context.getString(R.string.mot_record_summary_limited_body);
            case CLEAN:
            default:
                return context.getString(R.string.mot_record_summary_clean_body);
        }
    }

    private static List<String> topSignals(@NonNull List<Signal> signals) {
        List<String> topSignals = new ArrayList<>();
        for (Signal signal : signals) {
            if (topSignals.size() >= MAX_SIGNALS) {
                break;
            }
            topSignals.add(signal.getText());
        }
        return topSignals;
    }

    private static int countDefectsOfType(@Nullable MotTest test, @NonNull String type) {
        if (test == null || test.getDefects() == null) {
            return 0;
        }
        int count = 0;
        for (Defect defect : test.getDefects()) {
            if (defect != null && type.equals(normalize(defect.getType()))) {
                count++;
            }
        }
        return count;
    }

    private static int countLatestFailureItems(@Nullable MotTest test) {
        if (test == null || test.getDefects() == null) {
            return 0;
        }
        int count = 0;
        for (Defect defect : test.getDefects()) {
            String type = defect != null ? normalize(defect.getType()) : "";
            if (defect != null && (defect.isDangerous()
                    || "MAJOR".equals(type)
                    || "DANGEROUS".equals(type)
                    || "FAIL".equals(type))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isFail(@Nullable MotTest test) {
        return test != null && normalize(test.getTestResult()).contains("FAIL");
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.UK);
    }

    @NonNull
    private static String pluralize(@NonNull String singular, int count) {
        if (count == 1) {
            return singular;
        }
        if ("advisory".equals(singular)) {
            return "advisories";
        }
        return singular + "s";
    }

    static final class Summary {
        private final Tone tone;
        private final String title;
        private final String body;
        private final List<String> signals;
        private final List<String> changes;

        Summary(
                @NonNull Tone tone,
                @NonNull String title,
                @NonNull String body,
                @NonNull List<String> signals,
                @NonNull List<String> changes
        ) {
            this.tone = tone;
            this.title = title;
            this.body = body;
            this.signals = new ArrayList<>(signals);
            this.changes = new ArrayList<>(changes);
        }

        Tone getTone() {
            return tone;
        }

        String getTitle() {
            return title;
        }

        String getBody() {
            return body;
        }

        List<String> getSignals() {
            return new ArrayList<>(signals);
        }

        List<String> getChanges() {
            return new ArrayList<>(changes);
        }
    }

    private static final class Signal {
        private final Tone tone;
        private final String text;

        Signal(@NonNull Tone tone, @NonNull String text) {
            this.tone = tone;
            this.text = text;
        }

        Tone getTone() {
            return tone;
        }

        String getText() {
            return text;
        }
    }

    private static final class DefectComparison {
        private final int newCount;
        private final int removedCount;
        private final int repeatedCount;

        DefectComparison(int newCount, int removedCount, int repeatedCount) {
            this.newCount = newCount;
            this.removedCount = removedCount;
            this.repeatedCount = repeatedCount;
        }

        int getNewCount() {
            return newCount;
        }

        int getRemovedCount() {
            return removedCount;
        }

        int getRepeatedCount() {
            return repeatedCount;
        }
    }
}
