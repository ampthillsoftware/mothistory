package com.ampsoft.MOTHistory.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ampsoft.MOTHistory.BuildConfig;
import com.ampsoft.MOTHistory.R;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.ArrayList;
import java.util.List;

public class AdsManager {

    private static final String TAG = "AdsManager";

    private static final int FIRST_INTERSTITIAL_AFTER_SUCCESS_COUNT = 3;
    private static final int INTERSTITIAL_FREQUENCY = 5;
    private static final long MIN_INTERSTITIAL_INTERVAL_MS = 3L * 60L * 1000L;

    private static AdsManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Object lock = new Object();
    private final List<Runnable> pendingReadyActions = new ArrayList<>();
    private ConsentInformation consentInformation;
    private boolean mobileAdsInitialized;
    private boolean allowAdsWithoutConsentDueToError;
    private InterstitialAd interstitialAd;
    private boolean interstitialLoading;
    private int successfulLookupCount;
    private long lastInterstitialShownAtMs;

    public interface ContinueAction {
        void run();
    }

    public enum BannerPlacement {
        SEARCH,
        RESULT,
        MILEAGE,
        MOT_HISTORY,
        SAVED_CARS
    }

    public interface BannerAttachedListener {
        void onAttached(AdView adView);
    }

    public static AdsManager getInstance() {
        if (instance == null) {
            instance = new AdsManager();
        }
        return instance;
    }

    private AdsManager() {
    }

    public void initialize(@NonNull Activity activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> initialize(activity));
            return;
        }

        synchronized (lock) {
            if (consentInformation == null) {
                consentInformation = UserMessagingPlatform.getConsentInformation(activity);
            }
        }

        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();
        if (BuildConfig.DEBUG) {
            paramsBuilder.setConsentDebugSettings(
                    new ConsentDebugSettings.Builder(activity).build()
            );
        }
        ConsentRequestParameters params = paramsBuilder.build();

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    mainHandler.post(() -> {
                        Log.d(TAG, "Consent info updated. canRequestAds=" + consentInformation.canRequestAds());
                        if (consentInformation.canRequestAds()) {
                            startMobileAds(activity.getApplicationContext());
                        }
                        loadAndShowConsentFormIfRequired(activity);
                    });
                },
                formError -> {
                    mainHandler.post(() -> {
                        Log.w(TAG, "Consent info update failed: "
                                + (formError != null ? formError.getMessage() : "unknown"));
                        // Continue with non-personalized defaults if consent retrieval fails.
                        synchronized (lock) {
                            allowAdsWithoutConsentDueToError = true;
                        }
                        startMobileAds(activity.getApplicationContext());
                    });
                }
        );
    }

    public boolean canRequestAds() {
        synchronized (lock) {
            return canRequestAdsLocked();
        }
    }

    public boolean isPrivacyOptionsRequired() {
        synchronized (lock) {
            return consentInformation != null
                    && consentInformation.getPrivacyOptionsRequirementStatus()
                    == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        }
    }

    public void showPrivacyOptionsForm(@NonNull Activity activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> showPrivacyOptionsForm(activity));
            return;
        }
        UserMessagingPlatform.showPrivacyOptionsForm(
                activity,
                formError -> {
                    if (formError != null) {
                        logPrivacyOptionsError(formError);
                        Toast.makeText(
                                activity,
                                R.string.settings_privacy_options_unavailable,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    public void maybeShowInterstitialOnLookupSuccess(
            @NonNull Activity activity,
            @NonNull ContinueAction continueAction
    ) {
        InterstitialAd adToShow = null;
        int currentSuccessCount;
        boolean shouldShowNow;
        synchronized (lock) {
            successfulLookupCount++;
            currentSuccessCount = successfulLookupCount;
            shouldShowNow = shouldShowInterstitialLocked();
            if (shouldShowNow) {
                adToShow = interstitialAd;
                interstitialAd = null;
            }
        }
        Log.d(TAG, "Lookup success count=" + currentSuccessCount
                + ", hasInterstitial=" + (adToShow != null)
                + ", eligibleToShow=" + shouldShowNow);

        if (adToShow == null) {
            maybeLoadInterstitial(activity.getApplicationContext());
            continueAction.run();
            return;
        }

        InterstitialAd finalAdToShow = adToShow;
        finalAdToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial showed.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                synchronized (lock) {
                    lastInterstitialShownAtMs = System.currentTimeMillis();
                }
                Log.d(TAG, "Interstitial dismissed.");
                maybeLoadInterstitial(activity.getApplicationContext());
                continueAction.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                Log.w(TAG, "Interstitial failed to show: "
                        + (adError != null ? adError.getMessage() : "unknown"));
                maybeLoadInterstitial(activity.getApplicationContext());
                continueAction.run();
            }
        });
        finalAdToShow.show(activity);
    }

    public AdView attachAnchoredBanner(
            @NonNull Activity activity,
            @NonNull FrameLayout container,
            @NonNull BannerPlacement placement
    ) {
        synchronized (lock) {
            if (!canRequestAdsLocked()) {
                container.removeAllViews();
                return null;
            }
        }

        if (!mobileAdsInitialized) {
            container.removeAllViews();
            return null;
        }

        AdView adView = new AdView(activity);
        adView.setAdUnitId(resolveBannerAdUnitId(placement));
        adView.setAdSize(getAnchoredAdaptiveSize(activity, container));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner loaded for placement=" + placement.name());
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.w(TAG, "Banner failed to load for placement=" + placement.name()
                        + ": code=" + loadAdError.getCode()
                        + ", message=" + loadAdError.getMessage());
            }
        });
        container.removeAllViews();
        container.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
        return adView;
    }

    public void attachAnchoredBannerWhenReady(
            @NonNull Activity activity,
            @NonNull FrameLayout container,
            @NonNull BannerPlacement placement,
            @NonNull BannerAttachedListener listener
    ) {
        runWhenAdsReady(() -> activity.runOnUiThread(() -> {
            AdView adView = attachAnchoredBanner(activity, container, placement);
            listener.onAttached(adView);
        }));
    }

    private String resolveBannerAdUnitId(@NonNull BannerPlacement placement) {
        switch (placement) {
            case SAVED_CARS:
                return BuildConfig.ADMOB_SEARCH_BANNER_AD_UNIT_ID;
            case MILEAGE:
            case MOT_HISTORY:
            case RESULT:
                return BuildConfig.ADMOB_RESULT_BANNER_AD_UNIT_ID;
            case SEARCH:
            default:
                return BuildConfig.ADMOB_SEARCH_BANNER_AD_UNIT_ID;
        }
    }

    private void loadAndShowConsentFormIfRequired(@NonNull Activity activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> loadAndShowConsentFormIfRequired(activity));
            return;
        }
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                activity,
                formError -> mainHandler.post(() -> {
                    if (formError != null) {
                        Log.w(TAG, "Consent form flow error: " + formError.getMessage());
                    } else {
                        Log.d(TAG, "Consent form flow completed.");
                    }
                    if (consentInformation != null && consentInformation.canRequestAds()) {
                        startMobileAds(activity.getApplicationContext());
                    } else {
                        dispatchReadyActionsIfPossible();
                    }
                })
        );
    }

    private void startMobileAds(@NonNull Context context) {
        boolean alreadyInitialized;
        synchronized (lock) {
            alreadyInitialized = mobileAdsInitialized;
            if (!mobileAdsInitialized) {
                mobileAdsInitialized = true;
            }
        }

        if (alreadyInitialized) {
            Log.d(TAG, "Mobile Ads already initialized.");
            maybeLoadInterstitial(context);
            dispatchReadyActionsIfPossible();
            return;
        }

        Log.d(TAG, "Initializing Mobile Ads.");
        MobileAds.initialize(context, initializationStatus -> maybeLoadInterstitial(context));
        dispatchReadyActionsIfPossible();
    }

    private void maybeLoadInterstitial(@NonNull Context context) {
        synchronized (lock) {
            if (!mobileAdsInitialized || !canRequestAds()
                    || interstitialLoading || interstitialAd != null) {
                return;
            }
            interstitialLoading = true;
        }
        Log.d(TAG, "Loading interstitial.");

        InterstitialAd.load(
                context,
                BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd loadedAd) {
                        synchronized (lock) {
                            interstitialLoading = false;
                            interstitialAd = loadedAd;
                        }
                        Log.d(TAG, "Interstitial loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        synchronized (lock) {
                            interstitialLoading = false;
                            interstitialAd = null;
                        }
                        Log.w(TAG, "Interstitial failed to load: "
                                + (loadAdError != null ? loadAdError.getMessage() : "unknown"));
                    }
                }
        );
    }

    private boolean shouldShowInterstitialLocked() {
        if (!mobileAdsInitialized || interstitialAd == null) {
            return false;
        }
        if (successfulLookupCount < FIRST_INTERSTITIAL_AFTER_SUCCESS_COUNT) {
            return false;
        }
        if ((successfulLookupCount - FIRST_INTERSTITIAL_AFTER_SUCCESS_COUNT) % INTERSTITIAL_FREQUENCY != 0) {
            return false;
        }
        return System.currentTimeMillis() - lastInterstitialShownAtMs >= MIN_INTERSTITIAL_INTERVAL_MS;
    }

    private boolean canRequestAdsLocked() {
        if (allowAdsWithoutConsentDueToError) {
            return true;
        }
        return consentInformation != null && consentInformation.canRequestAds();
    }

    private void runWhenAdsReady(@NonNull Runnable action) {
        boolean runImmediately;
        synchronized (lock) {
            runImmediately = mobileAdsInitialized && canRequestAdsLocked();
            if (!runImmediately) {
                pendingReadyActions.add(action);
            }
        }
        if (runImmediately) {
            action.run();
        }
    }

    private void dispatchReadyActionsIfPossible() {
        List<Runnable> actionsToRun = new ArrayList<>();
        synchronized (lock) {
            if (!mobileAdsInitialized || !canRequestAdsLocked() || pendingReadyActions.isEmpty()) {
                return;
            }
            actionsToRun.addAll(pendingReadyActions);
            pendingReadyActions.clear();
        }
        for (Runnable action : actionsToRun) {
            action.run();
        }
    }

    private AdSize getAnchoredAdaptiveSize(@NonNull Activity activity, @NonNull View container) {
        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();
        float density = displayMetrics.density;
        int adWidthPixels = container.getWidth();
        if (adWidthPixels == 0) {
            adWidthPixels = displayMetrics.widthPixels;
        }
        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    private void logPrivacyOptionsError(@NonNull FormError formError) {
        Log.w(TAG, "Privacy options form failed: code="
                + formError.getErrorCode()
                + ", message=" + formError.getMessage());
    }
}
