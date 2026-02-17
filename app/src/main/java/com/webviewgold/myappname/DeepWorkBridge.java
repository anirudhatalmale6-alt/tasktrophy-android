package com.webviewgold.myappname;

import android.app.Activity;
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

/**
 * Deep Work (Focus Duel) Bridge
 * Tracks screen-off time to measure focus sessions.
 *
 * When user starts a focus session:
 * - Register screen on/off BroadcastReceiver
 * - Track cumulative screen-off minutes
 * - Count screen unlocks (penalties)
 * - Track longest unbroken streak
 *
 * JS bridge exposed as window.DeepWork
 */
public class DeepWorkBridge {

    private static final String TAG = "DeepWorkBridge";
    private static final String PREFS_NAME = "deepwork_prefs";
    private static final String KEY_FOCUS_MINUTES = "focus_minutes";
    private static final String KEY_LONGEST_STREAK = "longest_streak";
    private static final String KEY_UNLOCKS = "unlocks";
    private static final String KEY_SESSION_DATE = "session_date";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_SCREEN_OFF_SINCE = "screen_off_since";
    private static final String KEY_CURRENT_STREAK = "current_streak_minutes";

    private final Context context;
    private final WebView webView;

    private int focusMinutes = 0;
    private int longestStreak = 0;
    private int currentStreak = 0;
    private int unlocks = 0;
    private boolean sessionActive = false;
    private long screenOffSince = 0; // timestamp when screen turned off

    private BroadcastReceiver screenReceiver;
    private boolean receiverRegistered = false;

    public DeepWorkBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        loadCachedData();
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_SESSION_DATE, "");
        String today = java.time.LocalDate.now().toString();

        if (today.equals(cachedDate)) {
            focusMinutes = prefs.getInt(KEY_FOCUS_MINUTES, 0);
            longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0);
            unlocks = prefs.getInt(KEY_UNLOCKS, 0);
            sessionActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false);
            screenOffSince = prefs.getLong(KEY_SCREEN_OFF_SINCE, 0);
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0);

            // If session was active and screen was off, calculate elapsed time
            if (sessionActive && screenOffSince > 0) {
                long now = System.currentTimeMillis();
                int elapsed = (int) ((now - screenOffSince) / 60000);
                if (elapsed > 0) {
                    focusMinutes += elapsed;
                    currentStreak += elapsed;
                    if (currentStreak > longestStreak) {
                        longestStreak = currentStreak;
                    }
                    screenOffSince = now; // reset baseline
                    saveData();
                }
            }

            // Resume receiver if session was active
            if (sessionActive) {
                registerScreenReceiver();
            }
        } else {
            // New day - reset
            focusMinutes = 0;
            longestStreak = 0;
            currentStreak = 0;
            unlocks = 0;
            sessionActive = false;
            screenOffSince = 0;
            prefs.edit()
                .putInt(KEY_FOCUS_MINUTES, 0)
                .putInt(KEY_LONGEST_STREAK, 0)
                .putInt(KEY_CURRENT_STREAK, 0)
                .putInt(KEY_UNLOCKS, 0)
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .putLong(KEY_SCREEN_OFF_SINCE, 0)
                .putString(KEY_SESSION_DATE, today)
                .apply();
        }
    }

    private void saveData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putInt(KEY_FOCUS_MINUTES, focusMinutes)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_UNLOCKS, unlocks)
            .putBoolean(KEY_SESSION_ACTIVE, sessionActive)
            .putLong(KEY_SCREEN_OFF_SINCE, screenOffSince)
            .putString(KEY_SESSION_DATE, java.time.LocalDate.now().toString())
            .apply();
    }

    // ─── Screen On/Off Receiver ───

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
                    case Intent.ACTION_USER_PRESENT:
                        onUserUnlock();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(screenReceiver, filter);
        }

        receiverRegistered = true;
        Log.d(TAG, "Screen receiver registered");

        // Check current screen state
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            // Screen is already off
            onScreenOff();
        }
    }

    private void unregisterScreenReceiver() {
        if (!receiverRegistered || screenReceiver == null) return;
        try {
            context.unregisterReceiver(screenReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }
        receiverRegistered = false;
        Log.d(TAG, "Screen receiver unregistered");
    }

    private void onScreenOff() {
        if (!sessionActive) return;
        screenOffSince = System.currentTimeMillis();
        saveData();
        Log.d(TAG, "Screen OFF - focus timer started");
        notifyJs("screenOff");
    }

    private void onScreenOn() {
        if (!sessionActive || screenOffSince <= 0) return;

        // Calculate how long screen was off
        long now = System.currentTimeMillis();
        int elapsed = (int) ((now - screenOffSince) / 60000);

        if (elapsed > 0) {
            focusMinutes += elapsed;
            currentStreak += elapsed;
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak;
            }
        }

        screenOffSince = 0;
        saveData();
        Log.d(TAG, "Screen ON - added " + elapsed + "m. Total: " + focusMinutes + "m, streak: " + currentStreak + "m");
        notifyJs("screenOn");
    }

    private void onUserUnlock() {
        if (!sessionActive) return;

        // User actually unlocked the phone (not just screen on from notification)
        unlocks++;
        currentStreak = 0; // Reset current streak on unlock
        saveData();
        Log.d(TAG, "User UNLOCKED phone - streak reset. Unlocks: " + unlocks);
        notifyJs("unlock");
    }

    // ─── JS Interface Methods ───

    @JavascriptInterface
    public boolean isAvailable() {
        return true; // Screen on/off detection available on all Android devices
    }

    @JavascriptInterface
    public void startSession() {
        if (sessionActive) return;

        sessionActive = true;
        currentStreak = 0;
        screenOffSince = 0;
        saveData();
        registerScreenReceiver();

        Log.d(TAG, "Focus session started");
        notifyJs("sessionStarted");
    }

    @JavascriptInterface
    public void stopSession() {
        if (!sessionActive) return;

        // Finalize any pending screen-off time
        if (screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int elapsed = (int) ((now - screenOffSince) / 60000);
            if (elapsed > 0) {
                focusMinutes += elapsed;
                currentStreak += elapsed;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
            }
            screenOffSince = 0;
        }

        sessionActive = false;
        saveData();
        unregisterScreenReceiver();

        Log.d(TAG, "Focus session stopped. Total: " + focusMinutes + "m");
        notifyJs("sessionStopped");
    }

    @JavascriptInterface
    public boolean isSessionActive() {
        return sessionActive;
    }

    @JavascriptInterface
    public int getFocusMinutes() {
        // Include pending screen-off time
        if (sessionActive && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return focusMinutes + pending;
        }
        return focusMinutes;
    }

    @JavascriptInterface
    public int getLongestStreak() {
        if (sessionActive && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return Math.max(longestStreak, currentStreak + pending);
        }
        return longestStreak;
    }

    @JavascriptInterface
    public int getCurrentStreak() {
        if (sessionActive && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return currentStreak + pending;
        }
        return currentStreak;
    }

    @JavascriptInterface
    public int getUnlocks() {
        return unlocks;
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
    public String getSessionInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("sessionActive", sessionActive);
            info.put("focusMinutes", getFocusMinutes());
            info.put("longestStreak", getLongestStreak());
            info.put("currentStreak", getCurrentStreak());
            info.put("unlocks", unlocks);
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ─── Lifecycle ───

    public void onDestroy() {
        // Finalize but don't stop session (keep tracking in background via saved state)
        if (sessionActive && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int elapsed = (int) ((now - screenOffSince) / 60000);
            if (elapsed > 0) {
                focusMinutes += elapsed;
                currentStreak += elapsed;
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                }
                screenOffSince = now;
            }
            saveData();
        }
        unregisterScreenReceiver();
    }

    // ─── JS Notification ───

    private void notifyJs(String event) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                String js = "if(window.onDeepWorkEvent) window.onDeepWorkEvent('" + event + "', " +
                    getFocusMinutes() + ", " + getLongestStreak() + ", " + getCurrentStreak() + ", " + unlocks + ");";
                webView.evaluateJavascript(js, null);
            });
        }
    }
}
