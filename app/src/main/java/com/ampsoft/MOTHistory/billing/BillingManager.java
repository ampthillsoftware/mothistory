package com.ampsoft.MOTHistory.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class BillingManager implements PurchasesUpdatedListener {

    public static final String REMOVE_ADS_PRODUCT_ID = "mot_history_remove_ads";

    private static final String TAG = "BillingManager";
    private static final String PREFS_NAME = "billing_entitlements";
    private static final String KEY_ADS_REMOVED = "ads_removed";

    public interface Listener {
        void onBillingStateChanged(boolean adsRemoved, @Nullable String formattedPrice);
    }

    private static BillingManager instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private BillingClient billingClient;
    private Context applicationContext;
    private ProductDetails removeAdsProduct;
    private String formattedPrice;
    private boolean initialized;
    private boolean adsRemoved;

    private BillingManager() {
    }

    public static synchronized BillingManager getInstance() {
        if (instance == null) {
            instance = new BillingManager();
        }
        return instance;
    }

    public void initialize(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (this) {
            if (initialized) {
                return;
            }
            initialized = true;
            applicationContext = appContext;
            adsRemoved = getPrefs(appContext).getBoolean(KEY_ADS_REMOVED, false);
            billingClient = BillingClient.newBuilder(appContext)
                    .setListener(this)
                    .enablePendingPurchases(
                            PendingPurchasesParams.newBuilder()
                                    .enableOneTimeProducts()
                                    .build())
                    .enableAutoServiceReconnection()
                    .build();
        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                    return;
                }
                queryOwnedProducts();
                queryRemoveAdsProduct();
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected; automatic reconnection is enabled.");
            }
        });
        notifyListeners();
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
        notifyListener(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public boolean isAdsRemoved() {
        synchronized (this) {
            return adsRemoved;
        }
    }

    @Nullable
    public String getFormattedPrice() {
        synchronized (this) {
            return formattedPrice;
        }
    }

    public boolean isRemoveAdsAvailable() {
        synchronized (this) {
            return removeAdsProduct != null && !adsRemoved;
        }
    }

    public void purchaseRemoveAds(@NonNull Activity activity) {
        ProductDetails product;
        synchronized (this) {
            product = removeAdsProduct;
        }
        if (product == null) {
            Log.w(TAG, "Remove ads product is not available yet.");
            return;
        }
        List<ProductDetails.OneTimePurchaseOfferDetails> offers =
                product.getOneTimePurchaseOfferDetailsList();
        if (offers == null || offers.isEmpty()) {
            Log.w(TAG, "Remove ads product has no eligible purchase offer.");
            return;
        }
        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offers.get(0).getOfferToken())
                        .build();
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productParams))
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "Unable to launch billing flow: " + result.getDebugMessage());
        }
    }

    private void queryRemoveAdsProduct() {
        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(REMOVE_ADS_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || result == null
                    || result.getProductDetailsList().isEmpty()) {
                Log.w(TAG, "Remove ads product unavailable: " + billingResult.getDebugMessage());
                return;
            }
            ProductDetails details = result.getProductDetailsList().get(0);
            String price = null;
            List<ProductDetails.OneTimePurchaseOfferDetails> offers =
                    details.getOneTimePurchaseOfferDetailsList();
            if (offers != null && !offers.isEmpty()) {
                price = offers.get(0).getFormattedPrice();
            }
            synchronized (this) {
                removeAdsProduct = details;
                formattedPrice = price;
            }
            notifyListeners();
        });
    }

    private void queryOwnedProducts() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Unable to query purchases: " + billingResult.getDebugMessage());
                return;
            }
            for (Purchase purchase : purchases) {
                processPurchase(purchase);
            }
            if (purchases.isEmpty()) {
                setAdsRemoved(false);
            }
        });
    }

    @Override
    public void onPurchasesUpdated(
            @NonNull BillingResult billingResult,
            @Nullable List<Purchase> purchases
    ) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                processPurchase(purchase);
            }
        } else if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w(TAG, "Purchase failed: " + billingResult.getDebugMessage());
        }
    }

    private void processPurchase(@NonNull Purchase purchase) {
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED
                || !purchase.getProducts().contains(REMOVE_ADS_PRODUCT_ID)) {
            return;
        }
        setAdsRemoved(true);
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.acknowledgePurchase(params, billingResult -> {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Purchase acknowledgement failed: "
                            + billingResult.getDebugMessage());
                }
            });
        }
    }

    private void setAdsRemoved(boolean removed) {
        synchronized (this) {
            adsRemoved = removed;
        }
        // The entitlement is only a local startup cache; ownership is refreshed from Play.
        Context context;
        synchronized (this) {
            context = applicationContext;
        }
        if (context != null) {
            getPrefs(context).edit().putBoolean(KEY_ADS_REMOVED, removed).apply();
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            notifyListener(listener);
        }
    }

    private void notifyListener(@NonNull Listener listener) {
        boolean removed;
        String price;
        synchronized (this) {
            removed = adsRemoved;
            price = formattedPrice;
        }
        mainHandler.post(() -> listener.onBillingStateChanged(removed, price));
    }

    private SharedPreferences getPrefs(@Nullable Context context) {
        Context appContext = context != null ? context.getApplicationContext() : null;
        if (appContext == null) {
            throw new IllegalStateException("BillingManager has not been initialized");
        }
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
