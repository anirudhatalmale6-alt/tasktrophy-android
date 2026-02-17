package com.webviewgold.myappname;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * WebAppInterface - Remote Control Ad Architecture
 *
 * This class provides a JavaScript interface for controlling ads from the website.
 * No automatic ad triggers - all ads are controlled via JavaScript commands.
 *
 * JavaScript Functions Available:
 * - Android.loadInterstitial() - Pre-load interstitial ad
 * - Android.showInterstitial() - Show interstitial ad
 * - Android.loadRewardAd() - Pre-load rewarded ad
 * - Android.showRewardAd() - Show rewarded ad
 * - Android.showBanner(position) - Show banner ad (position: "top" or "bottom")
 * - Android.hideBanner() - Hide banner ad
 * - Android.showMREC(position) - Show MREC 300x250 video banner (position: "top", "bottom", "center")
 * - Android.hideMREC() - Hide MREC ad
 *
 * JavaScript Callbacks:
 * - onAdCompleted('REWARD') - Called when reward is earned
 * - onAdClosed() - Called when any ad is closed
 */
public class WebAppInterface {
    private static final String TAG = "WebAppInterface";

    private final Activity activity;
    private final WebView webView;
    private final FrameLayout adContainer;

    // Ad instances
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private AdView bannerAdView;
    private AdView mrecAdView;

    // Ad Unit IDs (from strings.xml)
    private final String interstitialAdUnitId;
    private final String rewardedAdUnitId;
    private final String bannerAdUnitId;

    // Handler for UI operations
    private final Handler mainHandler;

    // Loading states
    private boolean isInterstitialLoading = false;
    private boolean isRewardedLoading = false;

    // Retry counters to prevent infinite retry loops
    private int interstitialRetryCount = 0;
    private int rewardedRetryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;

    public WebAppInterface(Activity activity, WebView webView, FrameLayout adContainer) {
        this.activity = activity;
        this.webView = webView;
        this.adContainer = adContainer;
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Get Ad Unit IDs from strings.xml
        this.interstitialAdUnitId = activity.getString(R.string.interstitial_full_screen);
        this.rewardedAdUnitId = activity.getString(R.string.admob_rewarded_id);
        this.bannerAdUnitId = activity.getString(R.string.banner_footer);

        Log.d(TAG, "WebAppInterface initialized");
    }

    // ==================== INTERSTITIAL ADS ====================

    /**
     * Pre-load interstitial ad so it's ready to show
     */
    @JavascriptInterface
    public void loadInterstitial() {
        Log.d(TAG, "loadInterstitial() called");

        if (isInterstitialLoading) {
            Log.d(TAG, "Interstitial already loading, skipping");
            return;
        }

        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial already loaded, skipping");
            return;
        }

        isInterstitialLoading = true;

        mainHandler.post(() -> {
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(activity, interstitialAdUnitId, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.d(TAG, "Interstitial ad loaded successfully");
                        interstitialAd = ad;
                        isInterstitialLoading = false;
                        interstitialRetryCount = 0; // Reset retry counter on success
                        setupInterstitialCallbacks();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        interstitialAd = null;
                        isInterstitialLoading = false;
                        // Retry with exponential backoff, max 3 retries
                        if (interstitialRetryCount < MAX_RETRY_COUNT) {
                            interstitialRetryCount++;
                            long delay = 30000L * interstitialRetryCount; // 30s, 60s, 90s
                            Log.d(TAG, "Will retry interstitial in " + (delay/1000) + "s (attempt " + interstitialRetryCount + ")");
                            mainHandler.postDelayed(() -> loadInterstitial(), delay);
                        } else {
                            Log.d(TAG, "Max interstitial retries reached, stopping");
                        }
                    }
                });
        });
    }

    /**
     * Show interstitial ad immediately
     */
    @JavascriptInterface
    public void showInterstitial() {
        Log.d(TAG, "showInterstitial() called");

        mainHandler.post(() -> {
            if (interstitialAd != null) {
                interstitialAd.show(activity);
            } else {
                Log.d(TAG, "Interstitial ad not ready");
                // Notify JS that ad wasn't shown
                executeJavaScript("onAdNotReady('interstitial')");
            }
        });
    }

    private void setupInterstitialCallbacks() {
        if (interstitialAd == null) return;

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed");
                interstitialAd = null;
                // Notify website
                executeJavaScript("onAdClosed()");
                // Pre-load next ad
                loadInterstitial();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                interstitialAd = null;
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed");
            }
        });
    }

    // ==================== REWARDED ADS ====================

    /**
     * Pre-load rewarded ad so it's ready to show
     */
    @JavascriptInterface
    public void loadRewardAd() {
        Log.d(TAG, "loadRewardAd() called");

        if (isRewardedLoading) {
            Log.d(TAG, "Rewarded ad already loading, skipping");
            return;
        }

        if (rewardedAd != null) {
            Log.d(TAG, "Rewarded ad already loaded, skipping");
            return;
        }

        isRewardedLoading = true;

        mainHandler.post(() -> {
            AdRequest adRequest = new AdRequest.Builder().build();

            RewardedAd.load(activity, rewardedAdUnitId, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        Log.d(TAG, "Rewarded ad loaded successfully");
                        rewardedAd = ad;
                        isRewardedLoading = false;
                        rewardedRetryCount = 0; // Reset retry counter on success
                        setupRewardedCallbacks();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        rewardedAd = null;
                        isRewardedLoading = false;
                        // Retry with exponential backoff, max 3 retries
                        if (rewardedRetryCount < MAX_RETRY_COUNT) {
                            rewardedRetryCount++;
                            long delay = 30000L * rewardedRetryCount; // 30s, 60s, 90s
                            Log.d(TAG, "Will retry rewarded in " + (delay/1000) + "s (attempt " + rewardedRetryCount + ")");
                            mainHandler.postDelayed(() -> loadRewardAd(), delay);
                        } else {
                            Log.d(TAG, "Max rewarded retries reached, stopping");
                        }
                    }
                });
        });
    }

    /**
     * Show rewarded ad immediately
     */
    @JavascriptInterface
    public void showRewardAd() {
        Log.d(TAG, "showRewardAd() called");

        mainHandler.post(() -> {
            if (rewardedAd != null) {
                rewardedAd.show(activity, rewardItem -> {
                    Log.d(TAG, "User earned reward: " + rewardItem.getAmount() + " " + rewardItem.getType());
                    // Notify website that reward was earned
                    executeJavaScript("onAdCompleted('REWARD')");
                    // Also call legacy callback for backwards compatibility
                    executeJavaScript("adMobRewardGranted()");
                });
            } else {
                Log.d(TAG, "Rewarded ad not ready");
                // Notify JS that ad wasn't shown so game can handle it
                executeJavaScript("onAdNotReady('rewarded')");
            }
        });
    }

    /**
     * Legacy method name for backwards compatibility
     */
    @JavascriptInterface
    public void showRewardedAd() {
        showRewardAd();
    }

    private void setupRewardedCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed");
                rewardedAd = null;
                // Notify website
                executeJavaScript("onAdClosed()");
                // Pre-load next ad
                loadRewardAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                rewardedAd = null;
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed");
            }
        });
    }

    // ==================== BANNER ADS ====================

    /**
     * Show banner ad at specified position
     * @param position "top" or "bottom" (default: bottom)
     */
    @JavascriptInterface
    public void showBanner(String position) {
        Log.d(TAG, "showBanner() called with position: " + position);

        mainHandler.post(() -> {
            if (adContainer == null) {
                Log.e(TAG, "Ad container is null, cannot show banner");
                return;
            }

            // Remove existing banner if any
            if (bannerAdView != null) {
                adContainer.removeView(bannerAdView);
                bannerAdView.destroy();
            }

            // Create new banner
            bannerAdView = new AdView(activity);
            bannerAdView.setAdUnitId(bannerAdUnitId);
            bannerAdView.setAdSize(AdSize.BANNER);

            // Set layout params based on position
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );

            if ("top".equalsIgnoreCase(position)) {
                params.gravity = android.view.Gravity.TOP;
            } else {
                params.gravity = android.view.Gravity.BOTTOM;
            }

            bannerAdView.setLayoutParams(params);

            // Add to container and load ad
            adContainer.addView(bannerAdView);
            adContainer.setVisibility(View.VISIBLE);

            AdRequest adRequest = new AdRequest.Builder().build();
            bannerAdView.loadAd(adRequest);

            Log.d(TAG, "Banner ad loading...");
        });
    }

    /**
     * Hide the banner ad
     */
    @JavascriptInterface
    public void hideBanner() {
        Log.d(TAG, "hideBanner() called");

        mainHandler.post(() -> {
            if (bannerAdView != null) {
                bannerAdView.setVisibility(View.GONE);
            }
            if (adContainer != null && mrecAdView == null) {
                adContainer.setVisibility(View.GONE);
            }
        });
    }

    // ==================== MREC (300x250) VIDEO BANNER ADS ====================

    /**
     * Show MREC (Medium Rectangle 300x250) video banner ad
     * Great for video ads and higher engagement
     * @param position "top", "bottom", or "center" (default: center)
     */
    @JavascriptInterface
    public void showMREC(String position) {
        Log.d(TAG, "showMREC() called with position: " + position);

        mainHandler.post(() -> {
            if (adContainer == null) {
                Log.e(TAG, "Ad container is null, cannot show MREC");
                return;
            }

            // Remove existing MREC if any
            if (mrecAdView != null) {
                adContainer.removeView(mrecAdView);
                mrecAdView.destroy();
            }

            // Create new MREC ad
            mrecAdView = new AdView(activity);
            mrecAdView.setAdUnitId(bannerAdUnitId); // Uses same ad unit, AdMob will serve MREC
            mrecAdView.setAdSize(AdSize.MEDIUM_RECTANGLE); // 300x250

            // Set layout params based on position
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );

            if ("top".equalsIgnoreCase(position)) {
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
            } else if ("bottom".equalsIgnoreCase(position)) {
                params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            } else {
                // Default: center
                params.gravity = android.view.Gravity.CENTER;
            }

            mrecAdView.setLayoutParams(params);

            // Add to container and load ad
            adContainer.addView(mrecAdView);
            adContainer.setVisibility(View.VISIBLE);

            AdRequest adRequest = new AdRequest.Builder().build();
            mrecAdView.loadAd(adRequest);

            Log.d(TAG, "MREC ad loading...");
        });
    }

    /**
     * Hide the MREC ad
     */
    @JavascriptInterface
    public void hideMREC() {
        Log.d(TAG, "hideMREC() called");

        mainHandler.post(() -> {
            if (mrecAdView != null) {
                mrecAdView.setVisibility(View.GONE);
                adContainer.removeView(mrecAdView);
                mrecAdView.destroy();
                mrecAdView = null;
            }
            if (adContainer != null && bannerAdView == null) {
                adContainer.setVisibility(View.GONE);
            }
        });
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Execute JavaScript on the WebView
     */
    private void executeJavaScript(String script) {
        mainHandler.post(() -> {
            if (webView != null) {
                String fullScript = "javascript:" + script;
                Log.d(TAG, "Executing: " + fullScript);
                webView.evaluateJavascript(script, null);
            }
        });
    }

    /**
     * Check if rewarded ad is ready to show
     * @return true if ad is loaded and ready
     */
    @JavascriptInterface
    public boolean isRewardAdReady() {
        return rewardedAd != null;
    }

    /**
     * Check if interstitial ad is ready to show
     * @return true if ad is loaded and ready
     */
    @JavascriptInterface
    public boolean isInterstitialReady() {
        return interstitialAd != null;
    }

    /**
     * Pre-load all ads (call this on app start)
     */
    public void preloadAllAds() {
        loadInterstitial();
        loadRewardAd();
    }

    /**
     * Reset retry counters and try loading ads again
     * Call this when user returns to app or network becomes available
     */
    public void resetAndReloadAds() {
        interstitialRetryCount = 0;
        rewardedRetryCount = 0;
        if (interstitialAd == null && !isInterstitialLoading) {
            loadInterstitial();
        }
        if (rewardedAd == null && !isRewardedLoading) {
            loadRewardAd();
        }
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        if (mrecAdView != null) {
            mrecAdView.destroy();
            mrecAdView = null;
        }
        interstitialAd = null;
        rewardedAd = null;
    }
}
