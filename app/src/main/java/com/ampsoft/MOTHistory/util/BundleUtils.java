package com.ampsoft.MOTHistory.util;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.io.Serializable;

public final class BundleUtils {

    private BundleUtils() {
    }

    @Nullable
    public static <T extends Serializable> T getSerializable(Bundle bundle, String key, Class<T> clazz) {
        if (bundle == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getSerializable(key, clazz);
        }
        @SuppressWarnings("deprecation")
        Serializable value = bundle.getSerializable(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }
}
