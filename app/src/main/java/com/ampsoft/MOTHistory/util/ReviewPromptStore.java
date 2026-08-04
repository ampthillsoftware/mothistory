package com.ampsoft.MOTHistory.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class ReviewPromptStore {

    private static final String PREFS_NAME = "review_prompt_store";
    private static final String KEY_SUCCESSFUL_LOOKUPS = "successful_lookups";
    private static final String KEY_NEXT_ELIGIBLE_LOOKUP = "next_eligible_lookup";
    private static final String KEY_NEXT_ELIGIBLE_AT_MS = "next_eligible_at_ms";
    private static final String KEY_DECLINED = "declined";
    private static final String KEY_RATE_REQUESTED = "rate_requested";
    private static final String KEY_RATE_REMINDER_SHOWN = "rate_reminder_shown";
    private static final String KEY_REVIEW_CYCLE_COMPLETE = "review_cycle_complete";
    private static final int INITIAL_TRIGGER_LOOKUP_COUNT = 2;
    private static final int SNOOZE_LOOKUP_INCREMENT = 3;
    private static final long SNOOZE_DURATION_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final int RATE_REMINDER_LOOKUP_INCREMENT = 5;
    private static final long RATE_REMINDER_DELAY_MS = 30L * 24L * 60L * 60L * 1000L;

    private ReviewPromptStore() {
    }

    public static boolean recordSuccessfulLookup(Context context) {
        SharedPreferences prefs = getPrefs(context);
        int successfulLookups = prefs.getInt(KEY_SUCCESSFUL_LOOKUPS, 0) + 1;
        prefs.edit().putInt(KEY_SUCCESSFUL_LOOKUPS, successfulLookups).apply();
        return shouldPromptNow(context, successfulLookups, System.currentTimeMillis());
    }

    public static void markRateRequested(Context context) {
        SharedPreferences prefs = getPrefs(context);
        boolean alreadyRequested = prefs.getBoolean(KEY_RATE_REQUESTED, false);
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(KEY_RATE_REQUESTED, true);
        if (alreadyRequested) {
            editor.putBoolean(KEY_REVIEW_CYCLE_COMPLETE, true);
        } else {
            int successfulLookups = prefs.getInt(KEY_SUCCESSFUL_LOOKUPS, 0);
            editor.putInt(KEY_NEXT_ELIGIBLE_LOOKUP,
                            successfulLookups + RATE_REMINDER_LOOKUP_INCREMENT)
                    .putLong(KEY_NEXT_ELIGIBLE_AT_MS,
                            System.currentTimeMillis() + RATE_REMINDER_DELAY_MS);
        }
        editor.apply();
    }

    public static void snooze(Context context) {
        SharedPreferences prefs = getPrefs(context);
        int successfulLookups = prefs.getInt(KEY_SUCCESSFUL_LOOKUPS, 0);
        SharedPreferences.Editor editor = prefs.edit();
        if (prefs.getBoolean(KEY_RATE_REMINDER_SHOWN, false)) {
            editor.putBoolean(KEY_REVIEW_CYCLE_COMPLETE, true);
        } else {
            editor.putInt(KEY_NEXT_ELIGIBLE_LOOKUP, successfulLookups + SNOOZE_LOOKUP_INCREMENT)
                    .putLong(KEY_NEXT_ELIGIBLE_AT_MS,
                            System.currentTimeMillis() + SNOOZE_DURATION_MS);
        }
        editor.apply();
    }

    public static void declinePermanently(Context context) {
        getPrefs(context).edit().putBoolean(KEY_DECLINED, true).apply();
    }

    public static boolean isDeclined(Context context) {
        return getPrefs(context).getBoolean(KEY_DECLINED, false);
    }

    private static boolean shouldPromptNow(Context context, int successfulLookups, long nowMs) {
        SharedPreferences prefs = getPrefs(context);
        if (prefs.getBoolean(KEY_DECLINED, false)
                || prefs.getBoolean(KEY_REVIEW_CYCLE_COMPLETE, false)
                || (prefs.getBoolean(KEY_RATE_REQUESTED, false)
                && prefs.getBoolean(KEY_RATE_REMINDER_SHOWN, false))) {
            return false;
        }
        int nextEligibleLookup = prefs.getInt(KEY_NEXT_ELIGIBLE_LOOKUP, INITIAL_TRIGGER_LOOKUP_COUNT);
        long nextEligibleAtMs = prefs.getLong(KEY_NEXT_ELIGIBLE_AT_MS, 0L);
        boolean eligible = successfulLookups >= nextEligibleLookup && nowMs >= nextEligibleAtMs;
        if (eligible && prefs.getBoolean(KEY_RATE_REQUESTED, false)) {
            prefs.edit().putBoolean(KEY_RATE_REMINDER_SHOWN, true).apply();
        }
        return eligible;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
