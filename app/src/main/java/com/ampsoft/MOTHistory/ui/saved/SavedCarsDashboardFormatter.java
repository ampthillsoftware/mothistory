package com.ampsoft.MOTHistory.ui.saved;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.local.ReminderStore;
import com.ampsoft.MOTHistory.data.model.Defect;
import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.reminders.MotReminderScheduler;
import com.ampsoft.MOTHistory.ui.common.VehicleCardAdapter;
import com.ampsoft.MOTHistory.util.DateFormatter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SavedCarsDashboardFormatter {

    static final class DashboardSummary {
        private final int dueSoonCount;
        private final int overdueCount;
        private final int remindersOnCount;
        private final String nextDueSummary;

        DashboardSummary(int dueSoonCount, int overdueCount, int remindersOnCount, String nextDueSummary) {
            this.dueSoonCount = dueSoonCount;
            this.overdueCount = overdueCount;
            this.remindersOnCount = remindersOnCount;
            this.nextDueSummary = nextDueSummary;
        }

        int getDueSoonCount() {
            return dueSoonCount;
        }

        int getOverdueCount() {
            return overdueCount;
        }

        int getRemindersOnCount() {
            return remindersOnCount;
        }

        String getNextDueSummary() {
            return nextDueSummary;
        }
    }

    static final class SavedCarCardData {
        private final Vehicle vehicle;
        private final VehicleCardAdapter.CardMetadata cardMetadata;
        @Nullable
        private final LocalDate expiryDate;

        SavedCarCardData(
                @NonNull Vehicle vehicle,
                @NonNull VehicleCardAdapter.CardMetadata cardMetadata,
                @Nullable LocalDate expiryDate
        ) {
            this.vehicle = vehicle;
            this.cardMetadata = cardMetadata;
            this.expiryDate = expiryDate;
        }

        @NonNull
        Vehicle getVehicle() {
            return vehicle;
        }

        @NonNull
        VehicleCardAdapter.CardMetadata getCardMetadata() {
            return cardMetadata;
        }

        @Nullable
        LocalDate getExpiryDate() {
            return expiryDate;
        }
    }

    private static final int DUE_SOON_THRESHOLD_DAYS = 30;

    private SavedCarsDashboardFormatter() {
    }

    @NonNull
    static List<SavedCarCardData> buildCardDataList(
            @NonNull Context context,
            @NonNull List<Vehicle> vehicles
    ) {
        List<SavedCarCardData> items = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            items.add(buildCardData(context, vehicle));
        }
        items.sort((left, right) -> compareExpiry(left.getExpiryDate(), right.getExpiryDate()));
        return items;
    }

    @NonNull
    static DashboardSummary buildSummary(
            @NonNull Context context,
            @NonNull List<SavedCarCardData> cards
    ) {
        int dueSoonCount = 0;
        int overdueCount = 0;
        int remindersOnCount = 0;
        SavedCarCardData nextDueCard = null;

        LocalDate today = LocalDate.now();
        for (SavedCarCardData card : cards) {
            LocalDate expiryDate = card.getExpiryDate();
            if (ReminderStore.hasReminder(context, card.getVehicle().getRegistration())) {
                remindersOnCount++;
            }
            if (expiryDate == null) {
                continue;
            }
            long daysUntil = ChronoUnit.DAYS.between(today, expiryDate);
            if (daysUntil < 0) {
                overdueCount++;
            } else if (daysUntil <= DUE_SOON_THRESHOLD_DAYS) {
                dueSoonCount++;
            }

            if (nextDueCard == null || expiryDate.isBefore(nextDueCard.getExpiryDate())) {
                nextDueCard = card;
            }
        }

        String nextDueSummary = nextDueCard == null
                ? context.getString(R.string.saved_cars_dashboard_next_due_none)
                : context.getString(
                        R.string.saved_cars_dashboard_next_due_value,
                        fallback(nextDueCard.getVehicle().getRegistration()),
                        describeDueDate(context, nextDueCard.getExpiryDate())
                );

        return new DashboardSummary(dueSoonCount, overdueCount, remindersOnCount, nextDueSummary);
    }

    @NonNull
    private static SavedCarCardData buildCardData(@NonNull Context context, @NonNull Vehicle vehicle) {
        MotTest latestTest = getLatestMotTest(vehicle);
        LocalDate expiryDate = DateFormatter.parseToLocalDate(DateFormatter.extractLatestExpiryDate(vehicle));
        ReminderStore.ReminderConfig reminderConfig =
                ReminderStore.getReminder(context, vehicle.getRegistration());

        VehicleCardAdapter.CardMetadata metadata = new VehicleCardAdapter.CardMetadata(
                buildPrimaryStatusChip(context, expiryDate),
                buildLatestResultChip(context, latestTest),
                buildReminderChip(context, reminderConfig),
                buildSummaryText(context, expiryDate, latestTest, reminderConfig)
        );

        return new SavedCarCardData(vehicle, metadata, expiryDate);
    }

    @NonNull
    private static VehicleCardAdapter.StatusChip buildPrimaryStatusChip(
            @NonNull Context context,
            @Nullable LocalDate expiryDate
    ) {
        if (expiryDate == null) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_status_no_expiry),
                    R.color.mot_blue,
                    R.color.white
            );
        }

        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (daysUntil < 0) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_status_overdue),
                    R.color.status_fail_surface,
                    R.color.status_fail
            );
        }
        if (daysUntil <= DUE_SOON_THRESHOLD_DAYS) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_status_due_soon),
                    R.color.status_warning_surface,
                    R.color.status_warning
            );
        }
        return new VehicleCardAdapter.StatusChip(
                context.getString(R.string.saved_cars_status_valid),
                R.color.status_pass_surface,
                R.color.status_pass
        );
    }

    @Nullable
    private static VehicleCardAdapter.StatusChip buildLatestResultChip(
            @NonNull Context context,
            @Nullable MotTest latestTest
    ) {
        if (latestTest == null) {
            return null;
        }
        int advisoryCount = countAdvisories(latestTest);
        String result = latestTest.getTestResult() == null
                ? ""
                : latestTest.getTestResult().trim().toUpperCase(Locale.UK);

        if (result.contains("FAIL")) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_result_fail),
                    R.color.status_fail_surface,
                    R.color.status_fail
            );
        }
        if (advisoryCount > 0) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_result_advisories, advisoryCount),
                    R.color.status_warning_surface,
                    R.color.status_warning
            );
        }
        if (result.contains("PASS")) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_result_pass),
                    R.color.status_pass_surface,
                    R.color.status_pass
            );
        }
        return null;
    }

    @NonNull
    private static VehicleCardAdapter.StatusChip buildReminderChip(
            @NonNull Context context,
            @Nullable ReminderStore.ReminderConfig config
    ) {
        if (config == null) {
            return new VehicleCardAdapter.StatusChip(
                    context.getString(R.string.saved_cars_reminder_chip_off),
                    R.color.mot_blue,
                    R.color.white
            );
        }
        return new VehicleCardAdapter.StatusChip(
                context.getString(
                        R.string.saved_cars_reminder_chip_on,
                        MotReminderScheduler.describeReminderOffset(context, config.getOffsetDays())
                ),
                R.color.mot_blue,
                R.color.white
        );
    }

    @NonNull
    private static String buildSummaryText(
            @NonNull Context context,
            @Nullable LocalDate expiryDate,
            @Nullable MotTest latestTest,
            @Nullable ReminderStore.ReminderConfig reminderConfig
    ) {
        String dueSummary = expiryDate == null
                ? context.getString(R.string.saved_cars_due_unknown)
                : describeDueDate(context, expiryDate);
        String latestResult = describeLatestResult(context, latestTest);

        if (reminderConfig == null) {
            return context.getString(
                    R.string.saved_cars_summary_without_reminder,
                    dueSummary,
                    latestResult
            );
        }
        return context.getString(
                R.string.saved_cars_summary_with_reminder,
                dueSummary,
                latestResult,
                MotReminderScheduler.describeReminderOffset(context, reminderConfig.getOffsetDays())
        );
    }

    @NonNull
    private static String describeDueDate(@NonNull Context context, @NonNull LocalDate expiryDate) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        String displayDate = DateFormatter.asDisplayDate(expiryDate.toString());
        if (daysUntil < 0) {
            return context.getString(
                    R.string.saved_cars_due_overdue,
                    Math.abs(daysUntil),
                    displayDate
            );
        }
        if (daysUntil == 0) {
            return context.getString(R.string.saved_cars_due_today, displayDate);
        }
        return context.getString(R.string.saved_cars_due_future, daysUntil, displayDate);
    }

    @NonNull
    private static String describeLatestResult(@NonNull Context context, @Nullable MotTest latestTest) {
        if (latestTest == null || latestTest.getTestResult() == null || latestTest.getTestResult().trim().isEmpty()) {
            return context.getString(R.string.saved_cars_latest_result_unknown);
        }

        int advisoryCount = countAdvisories(latestTest);
        String result = latestTest.getTestResult().trim().toUpperCase(Locale.UK);
        if (result.contains("FAIL")) {
            return context.getString(R.string.saved_cars_latest_result_fail);
        }
        if (result.contains("PASS") && advisoryCount > 0) {
            return context.getString(R.string.saved_cars_latest_result_pass_advisories, advisoryCount);
        }
        if (result.contains("PASS")) {
            return context.getString(R.string.saved_cars_latest_result_pass);
        }
        return latestTest.getTestResult().trim();
    }

    @Nullable
    private static MotTest getLatestMotTest(@Nullable Vehicle vehicle) {
        if (vehicle == null || vehicle.getMotTests() == null || vehicle.getMotTests().isEmpty()) {
            return null;
        }
        MotTest latest = null;
        LocalDate latestDate = null;
        for (MotTest motTest : vehicle.getMotTests()) {
            if (motTest == null) {
                continue;
            }
            LocalDate completedDate = DateFormatter.parseToLocalDate(motTest.getCompletedDate());
            if (completedDate == null) {
                continue;
            }
            if (latestDate == null || completedDate.isAfter(latestDate)) {
                latest = motTest;
                latestDate = completedDate;
            }
        }
        return latest;
    }

    private static int countAdvisories(@Nullable MotTest motTest) {
        if (motTest == null || motTest.getDefects() == null) {
            return 0;
        }
        int count = 0;
        for (Defect defect : motTest.getDefects()) {
            if (defect != null
                    && defect.getType() != null
                    && defect.getType().trim().toUpperCase(Locale.UK).contains("ADVISORY")) {
                count++;
            }
        }
        return count;
    }

    private static int compareExpiry(@Nullable LocalDate left, @Nullable LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    @NonNull
    private static String fallback(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }
}
