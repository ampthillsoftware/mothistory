package com.ampsoft.MOTHistory.reminders;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.local.ReminderStore;
import com.ampsoft.MOTHistory.data.model.Vehicle;
import com.ampsoft.MOTHistory.util.DateFormatter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class MotReminderScheduler {

    public static final int DEFAULT_OFFSET_DAYS = 14;
    public static final int[] SUPPORTED_OFFSETS_DAYS = new int[] {30, 14, 7, 1, 0};

    private static final String CHANNEL_ID = "mot_due_reminders";
    private static final String UNIQUE_WORK_PREFIX = "mot_due_";
    static final String KEY_REGISTRATION = "registration";

    private MotReminderScheduler() {
    }

    public static void ensureNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }

    public static boolean canPostNotifications(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean scheduleReminder(
            @NonNull Context context,
            @NonNull Vehicle vehicle,
            int offsetDays
    ) {
        String expiryDateIso = DateFormatter.extractLatestExpiryDate(vehicle);
        String registration = vehicle.getRegistration();
        if (expiryDateIso == null || registration == null || registration.trim().isEmpty()) {
            cancelReminder(context, registration);
            return false;
        }

        OffsetDateTime triggerAt = DateFormatter.parseIsoOffsetDateTime(expiryDateIso);
        if (triggerAt == null) {
            cancelReminder(context, registration);
            return false;
        }

        OffsetDateTime scheduledTime = triggerAt.minusDays(offsetDays).withHour(9).withMinute(0)
                .withSecond(0).withNano(0);
        long delayMs = Duration.between(OffsetDateTime.now(), scheduledTime).toMillis();
        if (delayMs <= 0L) {
            cancelReminder(context, registration);
            return false;
        }

        ensureNotificationChannel(context);
        ReminderStore.saveReminder(context, new ReminderStore.ReminderConfig(
                registration.trim().toUpperCase(Locale.UK),
                buildVehicleSummary(vehicle),
                expiryDateIso,
                offsetDays,
                scheduledTime.toInstant().toEpochMilli()
        ));

        Data inputData = new Data.Builder()
                .putString(KEY_REGISTRATION, registration)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MotReminderWorker.class)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                buildUniqueWorkName(registration),
                ExistingWorkPolicy.REPLACE,
                request
        );
        return true;
    }

    public static void cancelReminder(@NonNull Context context, String registration) {
        if (registration == null || registration.trim().isEmpty()) {
            return;
        }
        ReminderStore.removeReminder(context, registration);
        WorkManager.getInstance(context).cancelUniqueWork(buildUniqueWorkName(registration));
    }

    public static String describeReminderOffset(@NonNull Context context, int offsetDays) {
        if (offsetDays == 30) {
            return context.getString(R.string.reminder_offset_month);
        }
        if (offsetDays == 14) {
            return context.getString(R.string.reminder_offset_two_weeks);
        }
        if (offsetDays == 7) {
            return context.getString(R.string.reminder_offset_week);
        }
        if (offsetDays == 1) {
            return context.getString(R.string.reminder_offset_day);
        }
        return context.getString(R.string.reminder_offset_same_day);
    }

    public static String buildSavedCarSummary(@NonNull Context context, @NonNull Vehicle vehicle) {
        ReminderStore.ReminderConfig config = ReminderStore.getReminder(context, vehicle.getRegistration());
        if (config == null) {
            return context.getString(R.string.saved_cars_reminder_none);
        }
        return context.getString(
                R.string.saved_cars_reminder_on,
                describeReminderOffset(context, config.getOffsetDays())
        );
    }

    public static String buildStatusText(@NonNull Context context, @NonNull Vehicle vehicle) {
        ReminderStore.ReminderConfig config = ReminderStore.getReminder(context, vehicle.getRegistration());
        String expiryDate = DateFormatter.asDisplayDate(DateFormatter.extractLatestExpiryDate(vehicle));
        if (config == null) {
            return context.getString(R.string.reminder_status_off, expiryDate);
        }
        return context.getString(
                R.string.reminder_status_on,
                describeReminderOffset(context, config.getOffsetDays()),
                expiryDate
        );
    }

    private static String buildUniqueWorkName(String registration) {
        return UNIQUE_WORK_PREFIX + registration.trim().toUpperCase(Locale.UK);
    }

    private static String buildVehicleSummary(@NonNull Vehicle vehicle) {
        String make = vehicle.getMake() == null ? "" : vehicle.getMake().trim();
        String model = vehicle.getModel() == null ? "" : vehicle.getModel().trim();
        String summary = (make + " " + model).trim();
        return summary.isEmpty() ? vehicle.getRegistration() : summary;
    }

    static String getChannelId() {
        return CHANNEL_ID;
    }
}
