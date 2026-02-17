package com.tasktrophy.official;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.google.firebase.FirebaseApp;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;
import com.pushwoosh.Pushwoosh;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class WebViewApp extends MultiDexApplication {
    private static final String ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID;
    static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        try { setupActivityListener(); } catch (Throwable t) { Log.e("WebViewApp", "setupActivityListener failed", t); }
        try { initFirebase(); } catch (Throwable t) { Log.e("WebViewApp", "initFirebase failed", t); }
        try { initOneSignal(); } catch (Throwable t) { Log.e("WebViewApp", "initOneSignal failed", t); }
        try { initAdsSDK(); } catch (Throwable t) { Log.e("WebViewApp", "initAdsSDK failed", t); }
        try { initPushWooshSDK(); } catch (Throwable t) { Log.e("WebViewApp", "initPushWooshSDK failed", t); }
        try { initRevenueCat(); } catch (Throwable t) { Log.e("WebViewApp", "initRevenueCat failed", t); }
    }

    private void initRevenueCat() {
        try {
            String key = Config.REVENUECAT_API_KEY;
            if (key == null || key.isEmpty() || key.contains("xxxxx")) {
                Log.w("WebViewApp", "RevenueCat API key not configured, skipping init");
                return;
            }
            Purchases.setLogLevel(com.revenuecat.purchases.LogLevel.DEBUG);
            PurchasesConfiguration purchaseConfigs = new PurchasesConfiguration.Builder(
                    this,
                    key
            ).build();
            Purchases.configure(purchaseConfigs);
        } catch (Exception e) {
            Log.e("WebViewApp", "RevenueCat init failed: " + e.getMessage());
        }
    }

    private void initPushWooshSDK() {
        if (Config.PUSHWOOSH_ENABLED) {
            Pushwoosh.getInstance().registerForPushNotifications();
        }
    }

    private void initAdsSDK() {
        if ((Config.SHOW_BANNER_AD) || (Config.SHOW_FULL_SCREEN_AD)) {
            if (Config.USE_FACEBOOK_ADS) {
                AudienceNetworkAds.initialize(this);
                AdSettings.addTestDevice("bf26e52d-43b9-4814-99ee-2b82136d7077");
            } else {
            }
        }
    }

    private void initOneSignal() {
        if (Config.PUSH_ENABLED) {
            OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
            OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
            OneSignal.getNotifications().addClickListener(
                    new INotificationClickListener() {
                        @Override
                        public void onClick(@NonNull INotificationClickEvent iNotificationClickEvent) {
                            JSONObject data = iNotificationClickEvent.getNotification().getAdditionalData();
                            String notification_topic;
                            if (data != null) {
                                notification_topic = data.optString("trigger", null);
                                if (notification_topic != null) {
                                    OneSignal.getInAppMessages().addTrigger("trigger", notification_topic);
                                }
                            }
                            String launchUrl = iNotificationClickEvent.getNotification().getLaunchURL();
                            String url = null;
                            if (data != null && data.has("url")) {
                                try {
                                    url = data.getString("url");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("openURL", launchUrl);
                            intent.putExtra("ONESIGNAL_URL", url);
                            if (BuildConfig.IS_DEBUG_MODE)
                                Log.d("OneSignalExample", "openURL = " + launchUrl);
                            startActivity(intent);
                        }
                    }
            );
            OneSignal.getNotifications().requestPermission(false, Continue.none());
        }
    }

    private void initFirebase() {
        if (Config.FIREBASE_PUSH_ENABLED) {
            FirebaseApp.initializeApp(this);
        }
    }

    private void setupActivityListener() {
        registerActivityLifecycleCallbacks(new MyLifecycleObserver());
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStart(owner);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStop(owner);
            }
        });
    }

    private class MyLifecycleObserver implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            if (Config.PREVENT_SCREEN_CAPTURE) {
                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityResumed(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}
    }
}
