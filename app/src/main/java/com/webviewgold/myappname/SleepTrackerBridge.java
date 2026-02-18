package com.webviewgold.myappname;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * Sleep Tracker Bridge for Sleep Discipline Challenge.
 * Detects screen off/on events during sleep/wake windows
 * and notifies the web page to record bedtime/waketime.
 *
 * Bedtime window: 8 PM - 3 AM (screen off = going to sleep)
 * Wake window: 4 AM - 12 PM (screen on = waking up)
 *
 * JS bridge exposed as window.SleepTracker
 */
public class SleepTrackerBridge {

    private static final String TAG = "SleepTrackerBridge";
    private static final String PREFS_NAME = "sleep_tracker_prefs";
    private static final String KEY_TRACKING_ACTIVE = "tracking_active";
    private static final String KEY_TRACKING_DATE = "tracking_date";
    private static final String KEY_BEDTIME_RECORDED = "bedtime_recorded";
    private static final String KEY_BEDTIME_TIMESTAMP = "bedtime_timestamp";
    private static final String KEY_WAKETIME_RECORDED = "waketime_recorded";
    private static final String KEY_WAKETIME_TIMESTAMP = "waketime_timestamp";
    private static final String KEY_TARGET_BED_HOUR = "target_bed_hour";
    private static final String KEY_TARGET_BED_MINUTE = "target_bed_minute";
    private static final String KEY_TARGET_WAKE_HOUR = "target_wake_hour";
    private static final String KEY_TARGET_WAKE_MINUTE = "target_wake_minute";

    private final Context context;
    private final WebView webView;

    private boolean trackingActive = false;
    private boolean bedtimeRecorded = false;
    private boolean waketimeRecorded = false;
    private long bedtimeTimestamp = 0;
    private long waketimeTimestamp = 0;
    private int targetBedHour = 23;
    private int targetBedMinute = 0;
    private int targetWakeHour = 6;
    private int targetWakeMinute = 30;

    private BroadcastReceiver screenReceiver;
    private boolean receiverRegistered = false;

    public SleepTrackerBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        loadCachedData();
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_TRACKING_DATE, "");
        String today = java.time.LocalDate.now().toString();

        // Sleep challenge spans two calendar days (e.g. join at 10 PM, wake at 6 AM next day)
        // We consider the challenge "active" for the date user joined
        // Reset only if it's a completely new day AND we already recorded wake time
        boolean shouldReset = false;
        if (!today.equals(cachedDate)) {
            boolean prevWakeRecorded = prefs.getBoolean(KEY_WAKETIME_RECORDED, false);
            boolean prevBedRecorded = prefs.getBoolean(KEY_BEDTIME_RECORDED, false);
            // Reset if previous challenge is complete (both recorded) or if it's stale (no tracking)
            if ((prevBedRecorded && prevWakeRecorded) || !prefs.getBoolean(KEY_TRACKING_ACTIVE, false)) {
                shouldReset = true;
            }
            // If bedtime recorded yesterday but no wake yet, keep tracking (user is sleeping!)
        }

        if (shouldReset) {
            trackingActive = false;
            bedtimeRecorded = false;
            waketimeRecorded = false;
            bedtimeTimestamp = 0;
            waketimeTimestamp = 0;
            prefs.edit()
                .putBoolean(KEY_TRACKING_ACTIVE, false)
                .putBoolean(KEY_BEDTIME_RECORDED, false)
                .putBoolean(KEY_WAKETIME_RECORDED, false)
                .putLong(KEY_BEDTIME_TIMESTAMP, 0)
                .putLong(KEY_WAKETIME_TIMESTAMP, 0)
                .putString(KEY_TRACKING_DATE, today)
                .apply();
        } else {
            trackingActive = prefs.getBoolean(KEY_TRACKING_ACTIVE, false);
            bedtimeRecorded = prefs.getBoolean(KEY_BEDTIME_RECORDED, false);
            waketimeRecorded = prefs.getBoolean(KEY_WAKETIME_RECORDED, false);
            bedtimeTimestamp = prefs.getLong(KEY_BEDTIME_TIMESTAMP, 0);
            waketimeTimestamp = prefs.getLong(KEY_WAKETIME_TIMESTAMP, 0);
            targetBedHour = prefs.getInt(KEY_TARGET_BED_HOUR, 23);
            targetBedMinute = prefs.getInt(KEY_TARGET_BED_MINUTE, 0);
            targetWakeHour = prefs.getInt(KEY_TARGET_WAKE_HOUR, 6);
            targetWakeMinute = prefs.getInt(KEY_TARGET_WAKE_MINUTE, 30);

            // Resume receiver if tracking
            if (trackingActive && !waketimeRecorded) {
                registerScreenReceiver();
            }
        }
    }

    private void saveData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_TRACKING_ACTIVE, trackingActive)
            .putBoolean(KEY_BEDTIME_RECORDED, bedtimeRecorded)
            .putBoolean(KEY_WAKETIME_RECORDED, waketimeRecorded)
            .putLong(KEY_BEDTIME_TIMESTAMP, bedtimeTimestamp)
            .putLong(KEY_WAKETIME_TIMESTAMP, waketimeTimestamp)
            .putInt(KEY_TARGET_BED_HOUR, targetBedHour)
            .putInt(KEY_TARGET_BED_MINUTE, targetBedMinute)
            .putInt(KEY_TARGET_WAKE_HOUR, targetWakeHour)
            .putInt(KEY_TARGET_WAKE_MINUTE, targetWakeMinute)
            .putString(KEY_TRACKING_DATE, java.time.LocalDate.now().toString())
            .apply();
    }

    // ─── Time Window Checks ───

    /**
     * Check if current time is in bedtime window (8 PM - 3 AM)
     */
    private boolean isInBedtimeWindow() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 20 || hour < 3; // 8 PM to 3 AM
    }

    /**
     * Check if current time is in wake window (4 AM - 12 PM)
     */
    private boolean isInWakeWindow() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 4 && hour < 12; // 4 AM to 12 PM
    }

    // ─── Screen Receiver ───

    private void registerScreenReceiver() {
        if (receiverRegistered) return;

        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null || intent.getAction() == null) return;

                switch (intent.getAction()) {
                    case Intent.ACTION_SCREEN_OFF:
                        onScreenOff();
                        break;
                    case Intent.ACTION_SCREEN_ON:
                        onScreenOn();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(screenReceiver, filter);
        }

        receiverRegistered = true;
        Log.d(TAG, "Screen receiver registered for sleep tracking");
    }

    private void unregisterScreenReceiver() {
        if (!receiverRegistered || screenReceiver == null) return;
        try {
            context.unregisterReceiver(screenReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }
        receiverRegistered = false;
    }

    private void onScreenOff() {
        if (!trackingActive || bedtimeRecorded || waketimeRecorded) return;

        // Auto-detect bedtime only in bedtime window
        if (isInBedtimeWindow()) {
            bedtimeRecorded = true;
            bedtimeTimestamp = System.currentTimeMillis();
            saveData();
            Log.d(TAG, "Bedtime auto-detected at " + new java.util.Date(bedtimeTimestamp));
            notifyJs("bedtime");
        }
    }

    private void onScreenOn() {
        if (!trackingActive || !bedtimeRecorded || waketimeRecorded) return;

        // Auto-detect wake time only in wake window
        if (isInWakeWindow()) {
            waketimeRecorded = true;
            waketimeTimestamp = System.currentTimeMillis();
            saveData();
            unregisterScreenReceiver(); // Done tracking
            Log.d(TAG, "Wake time auto-detected at " + new java.util.Date(waketimeTimestamp));
            notifyJs("wakeup");
        }
    }

    // ─── JS Interface Methods ───

    @JavascriptInterface
    public boolean isAvailable() {
        return true;
    }

    /**
     * Start tracking for today's sleep challenge.
     * Call after user joins the challenge.
     */
    @JavascriptInterface
    public void startTracking(int bedHour, int bedMinute, int wakeHour, int wakeMinute) {
        targetBedHour = bedHour;
        targetBedMinute = bedMinute;
        targetWakeHour = wakeHour;
        targetWakeMinute = wakeMinute;
        trackingActive = true;
        bedtimeRecorded = false;
        waketimeRecorded = false;
        bedtimeTimestamp = 0;
        waketimeTimestamp = 0;
        saveData();
        registerScreenReceiver();
        Log.d(TAG, "Sleep tracking started. Target bed: " + bedHour + ":" + bedMinute +
              ", Target wake: " + wakeHour + ":" + wakeMinute);
    }

    /**
     * Manually record bedtime (user presses "Going to Sleep" button)
     */
    @JavascriptInterface
    public boolean recordBedtimeManual() {
        if (!trackingActive || bedtimeRecorded) return false;
        if (!isInBedtimeWindow()) return false;

        bedtimeRecorded = true;
        bedtimeTimestamp = System.currentTimeMillis();
        saveData();
        Log.d(TAG, "Bedtime manually recorded at " + new java.util.Date(bedtimeTimestamp));
        return true;
    }

    /**
     * Manually record wake time (user presses "I'm Awake" button)
     */
    @JavascriptInterface
    public boolean recordWaketimeManual() {
        if (!trackingActive || !bedtimeRecorded || waketimeRecorded) return false;
        if (!isInWakeWindow()) return false;

        waketimeRecorded = true;
        waketimeTimestamp = System.currentTimeMillis();
        saveData();
        unregisterScreenReceiver();
        Log.d(TAG, "Wake time manually recorded at " + new java.util.Date(waketimeTimestamp));
        return true;
    }

    @JavascriptInterface
    public boolean isTrackingActive() {
        return trackingActive;
    }

    @JavascriptInterface
    public boolean isBedtimeRecorded() {
        return bedtimeRecorded;
    }

    @JavascriptInterface
    public boolean isWaketimeRecorded() {
        return waketimeRecorded;
    }

    @JavascriptInterface
    public long getBedtimeTimestamp() {
        return bedtimeTimestamp;
    }

    @JavascriptInterface
    public long getWaketimeTimestamp() {
        return waketimeTimestamp;
    }

    @JavascriptInterface
    public String getDeviceId() {
        try {
            return Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            return "unknown";
        }
    }

    @JavascriptInterface
    public String getStatus() {
        try {
            JSONObject status = new JSONObject();
            status.put("trackingActive", trackingActive);
            status.put("bedtimeRecorded", bedtimeRecorded);
            status.put("waketimeRecorded", waketimeRecorded);
            status.put("bedtimeTimestamp", bedtimeTimestamp);
            status.put("waketimeTimestamp", waketimeTimestamp);
            status.put("targetBedHour", targetBedHour);
            status.put("targetBedMinute", targetBedMinute);
            status.put("targetWakeHour", targetWakeHour);
            status.put("targetWakeMinute", targetWakeMinute);
            status.put("inBedtimeWindow", isInBedtimeWindow());
            status.put("inWakeWindow", isInWakeWindow());
            return status.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Stop tracking and clean up
     */
    @JavascriptInterface
    public void stopTracking() {
        trackingActive = false;
        saveData();
        unregisterScreenReceiver();
        Log.d(TAG, "Sleep tracking stopped");
    }

    // ─── Lifecycle ───

    public void onDestroy() {
        saveData();
        unregisterScreenReceiver();
    }

    // ─── JS Notification ───

    private void notifyJs(String event) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    JSONObject data = new JSONObject();
                    data.put("event", event);
                    data.put("timestamp", System.currentTimeMillis());
                    data.put("bedtimeRecorded", bedtimeRecorded);
                    data.put("waketimeRecorded", waketimeRecorded);
                    data.put("bedtimeTimestamp", bedtimeTimestamp);
                    data.put("waketimeTimestamp", waketimeTimestamp);
                    String js = "if(window.onSleepEvent) window.onSleepEvent('" + event + "', " + data.toString() + ");";
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    String js = "if(window.onSleepEvent) window.onSleepEvent('" + event + "');";
                    webView.evaluateJavascript(js, null);
                }
            });
        }
    }
}
