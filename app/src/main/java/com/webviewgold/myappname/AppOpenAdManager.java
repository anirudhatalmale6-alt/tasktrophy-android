package com.webviewgold.myappname;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

/**
 * Manages App Open ads for TaskTrophy.
 * Shows a full-screen ad when the user brings the app to the foreground.
 *
 * Cooldown: 4 hours between ads.
 * Skips first launch so user sees app content immediately.
 */
public class AppOpenAdManager {

    private static final String TAG = "AppOpenAdManager";
    private static final long COOLDOWN_MS = 4 * 60 * 60 * 1000L; // 4 hours

    private final Activity activity;
    private final String adUnitId;

    private AppOpenAd appOpenAd;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private long lastAdShownTime = 0;
    private boolean firstLaunch = true;

    public AppOpenAdManager(@NonNull Activity activity) {
        this.activity = activity;
        this.adUnitId = activity.getString(R.string.app_open_ad_id);
    }

    /**
     * Load an App Open ad so it's ready to show.
     */
    public void loadAd() {
        if (isLoadingAd || appOpenAd != null) {
            return;
        }

        isLoadingAd = true;
        Log.d(TAG, "Loading App Open ad...");

        AdRequest adRequest = new AdRequest.Builder().build();
        AppOpenAd.load(activity, adUnitId, adRequest, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                Log.d(TAG, "App Open ad loaded successfully");
                appOpenAd = ad;
                isLoadingAd = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "App Open ad failed to load: " + loadAdError.getMessage());
                appOpenAd = null;
                isLoadingAd = false;
            }
        });
    }

    /**
     * Show the App Open ad if one is loaded and cooldown has elapsed.
     * Call this from onStart() or when the app comes to foreground.
     */
    public void showAdIfReady() {
        // Skip on first launch
        if (firstLaunch) {
            firstLaunch = false;
            Log.d(TAG, "Skipping App Open ad on first launch");
            loadAd();
            return;
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastAdShownTime < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - lastAdShownTime)) / 60000;
            Log.d(TAG, "App Open ad cooldown active, " + remaining + " min remaining");
            return;
        }

        // Check if ad is available
        if (appOpenAd == null) {
            Log.d(TAG, "App Open ad not loaded, loading now");
            loadAd();
            return;
        }

        if (isShowingAd) {
            return;
        }

        Log.d(TAG, "Showing App Open ad");
        isShowingAd = true;

        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open ad dismissed");
                appOpenAd = null;
                isShowingAd = false;
                lastAdShownTime = System.currentTimeMillis();
                // Pre-load next ad
                loadAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "App Open ad failed to show: " + adError.getMessage());
                appOpenAd = null;
                isShowingAd = false;
                loadAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open ad is showing");
            }
        });

        appOpenAd.show(activity);
    }

    /**
     * Clean up resources.
     */
    public void destroy() {
        appOpenAd = null;
    }
}
