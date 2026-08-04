package com.ampsoft.MOTHistory.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

public final class AppReviewManager {

    private AppReviewManager() {
    }

    public static void launchInAppReview(@NonNull Activity activity, @NonNull Runnable onComplete) {
        ReviewManager reviewManager = ReviewManagerFactory.create(activity);
        reviewManager.requestReviewFlow().addOnCompleteListener(requestTask -> {
            if (!requestTask.isSuccessful()) {
                onComplete.run();
                return;
            }
            ReviewInfo reviewInfo = requestTask.getResult();
            reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener(flowTask -> onComplete.run());
        });
    }

    public static void openPlayStoreListing(@NonNull Context context) {
        String packageName = context.getPackageName().replace(".debug", "");
        Intent marketIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + packageName)
        );
        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(marketIntent);
        } catch (ActivityNotFoundException ignored) {
            Intent webIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
            );
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(webIntent);
        }
    }
}
