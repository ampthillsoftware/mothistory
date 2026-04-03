package com.ampsoft.MOTHistory.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferences {

    private static final String PREFS_NAME = "mot_history_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private ThemePreferences() {
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(getSavedMode(context)));
    }

    public static int getSavedMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public static void saveMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public static int toNightMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case MODE_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            case MODE_SYSTEM:
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }
}
