package com.ampsoft.MOTHistory.ui.result;

import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class MotHistoryInsights {

    private MotHistoryInsights() {
    }

    static List<MotTest> getSortedTestsDescending(Vehicle vehicle) {
        List<MotTest> sorted = new ArrayList<>();
        if (vehicle == null || vehicle.getMotTests() == null) {
            return sorted;
        }
        sorted.addAll(vehicle.getMotTests());
        sorted.sort((left, right) -> compareDates(
                right != null ? right.getCompletedDate() : null,
                left != null ? left.getCompletedDate() : null
        ));
        return sorted;
    }

    static List<MotTest> getRecentTests(Vehicle vehicle, int limit) {
        List<MotTest> sorted = getSortedTestsDescending(vehicle);
        if (sorted.size() <= limit) {
            return sorted;
        }
        return new ArrayList<>(sorted.subList(0, limit));
    }

    static MotTest getLatestTest(Vehicle vehicle) {
        List<MotTest> tests = getSortedTestsDescending(vehicle);
        return tests.isEmpty() ? null : tests.get(0);
    }

    static MileageStats buildMileageStats(Vehicle vehicle) {
        List<MileagePoint> points = new ArrayList<>();
        for (MotTest test : getSortedTestsDescending(vehicle)) {
            if (test == null) {
                continue;
            }
            Long mileageValue = parseMileageValue(test.getOdometerValue());
            OffsetDateTime completedDate = parseDate(test.getCompletedDate());
            String unit = normalizeUnit(test.getOdometerUnit());
            if (mileageValue == null || completedDate == null || unit == null) {
                continue;
            }
            points.add(new MileagePoint(test, mileageValue, unit, completedDate));
        }
        points.sort(Comparator.comparing(MileagePoint::getCompletedDate));
        return new MileageStats(points);
    }

    static ResultBreakdown buildResultBreakdown(Vehicle vehicle) {
        int passes = 0;
        int passesWithAdvisories = 0;
        int failures = 0;

        for (MotTest test : getSortedTestsDescending(vehicle)) {
            if (test == null) {
                continue;
            }
            String result = normalize(test.getTestResult());
            boolean hasAdvisory = hasAdvisory(test);
            if (result.contains("FAIL")) {
                failures++;
            } else if (result.contains("PASS") && hasAdvisory) {
                passesWithAdvisories++;
            } else if (result.contains("PASS")) {
                passes++;
            }
        }

        return new ResultBreakdown(passes, passesWithAdvisories, failures);
    }

    static String formatMileage(Long mileageValue, String unit) {
        if (mileageValue == null || unit == null || unit.trim().isEmpty()) {
            return null;
        }
        return String.format(Locale.UK, "%,d %s", mileageValue, unit.trim());
    }

    static String formatDelta(Long delta, String unit) {
        if (delta == null || unit == null || unit.trim().isEmpty()) {
            return null;
        }
        return String.format(Locale.UK, "%+,d %s", delta, unit.trim());
    }

    static final class MileagePoint {
        private final MotTest motTest;
        private final long mileageValue;
        private final String unit;
        private final OffsetDateTime completedDate;

        MileagePoint(MotTest motTest, long mileageValue, String unit, OffsetDateTime completedDate) {
            this.motTest = motTest;
            this.mileageValue = mileageValue;
            this.unit = unit;
            this.completedDate = completedDate;
        }

        MotTest getMotTest() {
            return motTest;
        }

        long getMileageValue() {
            return mileageValue;
        }

        String getUnit() {
            return unit;
        }

        OffsetDateTime getCompletedDate() {
            return completedDate;
        }
    }

    static final class MileageStats {
        private final List<MileagePoint> points;

        MileageStats(List<MileagePoint> points) {
            this.points = Collections.unmodifiableList(new ArrayList<>(points));
        }

        List<MileagePoint> getPointsAscending() {
            return points;
        }

        MileagePoint getLatestPoint() {
            return points.isEmpty() ? null : points.get(points.size() - 1);
        }

        MileagePoint getPreviousPoint() {
            return points.size() < 2 ? null : points.get(points.size() - 2);
        }

        boolean hasMileageDrop() {
            for (int i = 1; i < points.size(); i++) {
                if (points.get(i).getMileageValue() < points.get(i - 1).getMileageValue()) {
                    return true;
                }
            }
            return false;
        }

        Long getLatestDelta() {
            MileagePoint latest = getLatestPoint();
            MileagePoint previous = getPreviousPoint();
            if (latest == null || previous == null || !latest.getUnit().equals(previous.getUnit())) {
                return null;
            }
            return latest.getMileageValue() - previous.getMileageValue();
        }

        String getUnit() {
            MileagePoint latest = getLatestPoint();
            return latest == null ? null : latest.getUnit();
        }

        String getAverageYearlyMileageText() {
            if (points.size() < 2) {
                return null;
            }
            MileagePoint first = points.get(0);
            MileagePoint last = points.get(points.size() - 1);
            if (!first.getUnit().equals(last.getUnit())) {
                return null;
            }
            double years = Duration.between(first.getCompletedDate(), last.getCompletedDate()).toDays() / 365.25d;
            if (years <= 0d) {
                return null;
            }
            long average = Math.round((last.getMileageValue() - first.getMileageValue()) / years);
            return formatMileage(average, last.getUnit()) + " / year";
        }
    }

    static final class ResultBreakdown {
        private final int passes;
        private final int passesWithAdvisories;
        private final int failures;

        ResultBreakdown(int passes, int passesWithAdvisories, int failures) {
            this.passes = passes;
            this.passesWithAdvisories = passesWithAdvisories;
            this.failures = failures;
        }

        int getPasses() {
            return passes;
        }

        int getPassesWithAdvisories() {
            return passesWithAdvisories;
        }

        int getFailures() {
            return failures;
        }

        int getTotal() {
            return passes + passesWithAdvisories + failures;
        }

        int getPassRatePercent() {
            int total = getTotal();
            if (total == 0) {
                return 0;
            }
            return Math.round(((passes + passesWithAdvisories) * 100f) / total);
        }
    }

    private static int compareDates(String left, String right) {
        OffsetDateTime leftDate = parseDate(left);
        OffsetDateTime rightDate = parseDate(right);
        if (leftDate == null && rightDate == null) {
            return 0;
        }
        if (leftDate == null) {
            return -1;
        }
        if (rightDate == null) {
            return 1;
        }
        return leftDate.compareTo(rightDate);
    }

    private static OffsetDateTime parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Long parseMileageValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.replace(",", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            return null;
        }
        return unit.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static boolean hasAdvisory(MotTest test) {
        if (test == null || test.getDefects() == null) {
            return false;
        }
        for (com.ampsoft.MOTHistory.data.model.Defect defect : test.getDefects()) {
            if (defect != null && "ADVISORY".equals(normalize(defect.getType()))) {
                return true;
            }
        }
        return false;
    }
}
