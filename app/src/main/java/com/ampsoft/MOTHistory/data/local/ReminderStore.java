package com.ampsoft.MOTHistory.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class ReminderStore {

    public static final class ReminderConfig {
        private final String registration;
        private final String vehicleSummary;
        private final String expiryDateIso;
        private final int offsetDays;
        private final long scheduledAtMs;

        public ReminderConfig(
                @NonNull String registration,
                @NonNull String vehicleSummary,
                @NonNull String expiryDateIso,
                int offsetDays,
                long scheduledAtMs
        ) {
            this.registration = registration;
            this.vehicleSummary = vehicleSummary;
            this.expiryDateIso = expiryDateIso;
            this.offsetDays = offsetDays;
            this.scheduledAtMs = scheduledAtMs;
        }

        public String getRegistration() {
            return registration;
        }

        public String getVehicleSummary() {
            return vehicleSummary;
        }

        public String getExpiryDateIso() {
            return expiryDateIso;
        }

        public int getOffsetDays() {
            return offsetDays;
        }

        public long getScheduledAtMs() {
            return scheduledAtMs;
        }
    }

    private static final String PREFS_NAME = "mot_reminder_store";
    private static final String KEY_REMINDERS = "reminders";
    private static final Gson GSON = new Gson();
    private static final Type REMINDER_MAP_TYPE =
            new TypeToken<Map<String, ReminderConfig>>() { }.getType();

    private ReminderStore() {
    }

    public static void saveReminder(@NonNull Context context, @NonNull ReminderConfig config) {
        Map<String, ReminderConfig> reminders = loadAll(context);
        reminders.put(config.getRegistration().toUpperCase(), config);
        saveAll(context, reminders);
    }

    public static void removeReminder(@NonNull Context context, @Nullable String registration) {
        if (registration == null || registration.trim().isEmpty()) {
            return;
        }
        Map<String, ReminderConfig> reminders = loadAll(context);
        reminders.remove(registration.trim().toUpperCase());
        saveAll(context, reminders);
    }

    @Nullable
    public static ReminderConfig getReminder(@NonNull Context context, @Nullable String registration) {
        if (registration == null || registration.trim().isEmpty()) {
            return null;
        }
        return loadAll(context).get(registration.trim().toUpperCase());
    }

    public static boolean hasReminder(@NonNull Context context, @Nullable String registration) {
        return getReminder(context, registration) != null;
    }

    @NonNull
    private static Map<String, ReminderConfig> loadAll(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_REMINDERS, "");
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        Map<String, ReminderConfig> reminders = GSON.fromJson(json, REMINDER_MAP_TYPE);
        return reminders != null ? new HashMap<>(reminders) : new HashMap<>();
    }

    private static void saveAll(@NonNull Context context, @NonNull Map<String, ReminderConfig> reminders) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_REMINDERS, GSON.toJson(reminders)).apply();
    }
}
