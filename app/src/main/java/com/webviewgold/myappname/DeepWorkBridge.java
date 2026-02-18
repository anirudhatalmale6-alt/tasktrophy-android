package com.webviewgold.myappname;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * Deep Work (Focus Duel) Bridge - Trial-Based Auto-Tracking
 *
 * Flow:
 * 1. User taps "Start Deep Focus" -> trial registered, state = WAITING_FOR_LOCK
 * 2. User locks phone (ACTION_SCREEN_OFF) -> state = FOCUSING, timer starts
 * 3. User unlocks phone (ACTION_USER_PRESENT) -> trial auto-ends, time recorded, state = IDLE
 * 4. Repeat up to 3 trials per day
 *
 * No manual stop button. Lock/unlock cycle controls trials.
 *
 * JS bridge exposed as window.DeepWork
 */
public class DeepWorkBridge {

    private static final String TAG = "DeepWorkBridge";
    private static final String PREFS_NAME = "deepwork_prefs";
    private static final String KEY_FOCUS_MINUTES = "focus_minutes";
    private static final String KEY_LONGEST_STREAK = "longest_streak";
    private static final String KEY_UNLOCKS = "unlocks"; // now = completed trials
    private static final String KEY_SESSION_DATE = "session_date";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_SCREEN_OFF_SINCE = "screen_off_since";
    private static final String KEY_CURRENT_STREAK = "current_streak_minutes";
    private static final String KEY_TRIAL_STATE = "trial_state";
    private static final String KEY_TRIAL_COUNT = "trial_count";
    private static final String KEY_TIMEZONE_OFFSET = "timezone_offset";
    private static final String KEY_TIMEZONE_FLAGGED = "timezone_flagged";

    private static final int MAX_TRIALS_PER_DAY = 3;
    private static final String EXPECTED_PACKAGE = "com.tasktrophy.official";

    // Known app cloner package names
    private static final List<String> CLONER_PACKAGES = Arrays.asList(
        "com.lbe.parallel.intl",
        "com.lbe.parallel.intl.arm64",
        "com.excelliance.dualaid",
        "com.parallel.space",
        "com.parallel.space.lite",
        "com.jumobile.multiapp",
        "com.ludashi.dualspace",
        "com.ludashi.superboost",
        "com.polestar.multiaccount",
        "com.cloneapp.dual",
        "com.trigtech.privateme",
        "com.nox.mopen.app",
        "in.parallel.space",
        "com.dual.space.clone",
        "com.applisto.appcloner",
        "com.oasisfeng.island",
        "com.samsung.android.knox.containercore"
    );

    // Trial states
    private static final String STATE_IDLE = "idle";
    private static final String STATE_WAITING_FOR_LOCK = "waiting_for_lock";
    private static final String STATE_FOCUSING = "focusing";

    private final Context context;
    private final WebView webView;

    private int focusMinutes = 0;        // total focus minutes today (across all trials)
    private int longestStreak = 0;       // longest single trial in minutes
    private int currentStreak = 0;       // current trial's accumulated minutes (finalized portion)
    private int unlocks = 0;             // completed trials count today
    private boolean sessionActive = false; // receiver registered (true while trial in progress)
    private long screenOffSince = 0;     // timestamp when screen turned off during FOCUSING
    private String trialState = STATE_IDLE;
    private int trialCount = 0;          // number of trials started today (may differ from unlocks if one is in progress)
    private int savedTimezoneOffset = Integer.MIN_VALUE; // timezone offset at trial start (ms)
    private boolean timezoneFlagged = false; // true if timezone changed during trial

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
            trialState = prefs.getString(KEY_TRIAL_STATE, STATE_IDLE);
            trialCount = prefs.getInt(KEY_TRIAL_COUNT, 0);
            savedTimezoneOffset = prefs.getInt(KEY_TIMEZONE_OFFSET, Integer.MIN_VALUE);
            timezoneFlagged = prefs.getBoolean(KEY_TIMEZONE_FLAGGED, false);

            // If we were FOCUSING and screen was off, recover elapsed time
            if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
                long now = System.currentTimeMillis();
                int elapsed = (int) ((now - screenOffSince) / 60000);
                if (elapsed > 0) {
                    focusMinutes += elapsed;
                    currentStreak += elapsed;
                    if (currentStreak > longestStreak) {
                        longestStreak = currentStreak;
                    }
                    screenOffSince = now; // reset baseline
                }
            }

            // Re-register receiver if a trial is in progress (waiting or focusing)
            if (!STATE_IDLE.equals(trialState)) {
                sessionActive = true;
                registerScreenReceiver();
            }

            saveData();
        } else {
            // New day - reset everything
            focusMinutes = 0;
            longestStreak = 0;
            currentStreak = 0;
            unlocks = 0;
            sessionActive = false;
            screenOffSince = 0;
            trialState = STATE_IDLE;
            trialCount = 0;
            savedTimezoneOffset = Integer.MIN_VALUE;
            timezoneFlagged = false;
            prefs.edit()
                .putInt(KEY_FOCUS_MINUTES, 0)
                .putInt(KEY_LONGEST_STREAK, 0)
                .putInt(KEY_CURRENT_STREAK, 0)
                .putInt(KEY_UNLOCKS, 0)
                .putBoolean(KEY_SESSION_ACTIVE, false)
                .putLong(KEY_SCREEN_OFF_SINCE, 0)
                .putString(KEY_SESSION_DATE, today)
                .putString(KEY_TRIAL_STATE, STATE_IDLE)
                .putInt(KEY_TRIAL_COUNT, 0)
                .putInt(KEY_TIMEZONE_OFFSET, Integer.MIN_VALUE)
                .putBoolean(KEY_TIMEZONE_FLAGGED, false)
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
            .putString(KEY_TRIAL_STATE, trialState)
            .putInt(KEY_TRIAL_COUNT, trialCount)
            .putInt(KEY_TIMEZONE_OFFSET, savedTimezoneOffset)
            .putBoolean(KEY_TIMEZONE_FLAGGED, timezoneFlagged)
            .apply();
    }

    // ---- Screen On/Off Receiver ----

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

        // Check current screen state - if screen is already off and we're waiting, start focusing
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
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
        if (STATE_WAITING_FOR_LOCK.equals(trialState)) {
            // Timezone check before starting focus
            checkTimezoneIntegrity();

            // Transition: WAITING_FOR_LOCK -> FOCUSING
            trialState = STATE_FOCUSING;
            screenOffSince = System.currentTimeMillis();
            currentStreak = 0; // fresh trial, start counting from 0
            saveData();
            Log.d(TAG, "Screen OFF during WAITING -> FOCUSING. Timer started.");
            notifyJs("focusBegan");
        } else if (STATE_FOCUSING.equals(trialState)) {
            // User locked phone again while already focusing (e.g., they unlocked briefly
            // but USER_PRESENT wasn't fired, or they re-locked). Start a new off-period.
            // This shouldn't normally happen because USER_PRESENT ends the trial,
            // but handle it gracefully.
            if (screenOffSince <= 0) {
                screenOffSince = System.currentTimeMillis();
                saveData();
            }
            Log.d(TAG, "Screen OFF during FOCUSING - already tracking, no-op");
        }
        // If IDLE, ignore screen off entirely
    }

    private void onScreenOn() {
        if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
            Log.d(TAG, "Screen ON during FOCUSING - checking if device is unlocked");
            // Notify JS so UI can show pending minutes update
            notifyJs("screenOn");

            // ACTION_USER_PRESENT doesn't fire on all devices (especially with no lock screen).
            // Use a delayed check: if the keyguard is not locked after a short delay,
            // the user has unlocked and we should end the trial.
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!STATE_FOCUSING.equals(trialState)) return; // Already ended by USER_PRESENT

                KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                boolean deviceLocked = false;
                if (km != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        deviceLocked = km.isDeviceLocked();
                    } else {
                        deviceLocked = km.isKeyguardLocked();
                    }
                }

                if (!deviceLocked) {
                    Log.d(TAG, "Keyguard not locked after screen on - ending trial (USER_PRESENT fallback)");
                    onUserUnlock();
                } else {
                    Log.d(TAG, "Keyguard still locked - waiting for USER_PRESENT");
                }
            }, 1500); // 1.5 second delay to allow keyguard to settle
        }
    }

    private void onUserUnlock() {
        if (STATE_FOCUSING.equals(trialState)) {
            // Check timezone integrity before recording time
            checkTimezoneIntegrity();

            // Trial ends! Calculate earned minutes.
            int earnedMinutes = 0;
            if (screenOffSince > 0 && !timezoneFlagged) {
                long now = System.currentTimeMillis();
                earnedMinutes = (int) ((now - screenOffSince) / 60000);
                if (earnedMinutes > 0) {
                    focusMinutes += earnedMinutes;
                    currentStreak += earnedMinutes;
                    if (currentStreak > longestStreak) {
                        longestStreak = currentStreak;
                    }
                }
            } else if (timezoneFlagged) {
                Log.w(TAG, "Timezone change detected! Trial time NOT counted.");
                earnedMinutes = 0;
            }

            // Completed trial
            unlocks++; // unlocks = completed trials in new model
            screenOffSince = 0;
            trialState = STATE_IDLE;
            sessionActive = false;
            saveData();
            unregisterScreenReceiver();

            Log.d(TAG, "USER_PRESENT during FOCUSING -> trial #" + unlocks + " ended. Earned: " + earnedMinutes + "m. Total: " + focusMinutes + "m");
            notifyJsTrialEnded(earnedMinutes);
        } else if (STATE_WAITING_FOR_LOCK.equals(trialState)) {
            // User unlocked phone while in WAITING state - this means they
            // pressed power to lock, then immediately unlocked without the screen
            // actually going through the full off cycle. Treat as a cancelled trial attempt.
            // Keep them in WAITING state so they can try locking again.
            Log.d(TAG, "USER_PRESENT during WAITING_FOR_LOCK - still waiting for a proper lock");
        }
        // If IDLE, ignore
    }

    // ---- JS Interface Methods ----

    @JavascriptInterface
    public boolean isAvailable() {
        return true; // Screen on/off detection available on all Android devices
    }

    /**
     * Start a new trial. Sets state to WAITING_FOR_LOCK.
     * User must lock the phone to begin focusing.
     */
    @JavascriptInterface
    public void startSession() {
        // Don't start if already in a trial
        if (!STATE_IDLE.equals(trialState)) {
            Log.d(TAG, "startSession ignored - trial already in progress: " + trialState);
            return;
        }

        // Anti-cheat: App cloner detection
        if (isAppCloned()) {
            Log.w(TAG, "startSession BLOCKED - app cloner detected!");
            notifyJs("appCloned");
            return;
        }

        // Check daily trial limit
        if (trialCount >= MAX_TRIALS_PER_DAY) {
            Log.d(TAG, "startSession ignored - max trials reached (" + MAX_TRIALS_PER_DAY + ")");
            notifyJs("maxTrialsReached");
            return;
        }

        // Record timezone offset at trial start
        savedTimezoneOffset = TimeZone.getDefault().getRawOffset();
        timezoneFlagged = false;

        trialState = STATE_WAITING_FOR_LOCK;
        sessionActive = true;
        currentStreak = 0;
        screenOffSince = 0;
        trialCount++;
        saveData();
        registerScreenReceiver();

        Log.d(TAG, "Trial #" + trialCount + " started - WAITING_FOR_LOCK");
        notifyJs("trialStarted");
    }

    /**
     * No-op. Kept for backward compatibility so existing frontend calls don't crash.
     * Trials end automatically when user unlocks the phone.
     */
    @JavascriptInterface
    public void stopSession() {
        // No-op - trials end automatically on unlock
        Log.d(TAG, "stopSession called - no-op in trial mode");
    }

    @JavascriptInterface
    public boolean isSessionActive() {
        return !STATE_IDLE.equals(trialState);
    }

    @JavascriptInterface
    public int getFocusMinutes() {
        // Include pending screen-off time from current trial
        if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return focusMinutes + pending;
        }
        return focusMinutes;
    }

    @JavascriptInterface
    public int getLongestStreak() {
        if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return Math.max(longestStreak, currentStreak + pending);
        }
        return longestStreak;
    }

    @JavascriptInterface
    public int getCurrentStreak() {
        if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return currentStreak + pending;
        }
        return currentStreak;
    }

    /**
     * Returns completed trial count (same storage key as old "unlocks").
     */
    @JavascriptInterface
    public int getUnlocks() {
        return unlocks;
    }

    /**
     * Returns the current trial's focus minutes (including pending screen-off time).
     * Returns 0 if no trial is in progress.
     */
    @JavascriptInterface
    public int getCurrentTrialMinutes() {
        if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
            long now = System.currentTimeMillis();
            int pending = (int) ((now - screenOffSince) / 60000);
            return currentStreak + pending;
        }
        if (STATE_FOCUSING.equals(trialState)) {
            return currentStreak;
        }
        return 0;
    }

    /**
     * Returns the current trial state: "idle", "waiting_for_lock", or "focusing"
     */
    @JavascriptInterface
    public String getTrialState() {
        return trialState;
    }

    /**
     * Returns how many trials have been started today.
     */
    @JavascriptInterface
    public int getTrialCount() {
        return trialCount;
    }

    /**
     * Returns the maximum trials allowed per day.
     */
    @JavascriptInterface
    public int getMaxTrials() {
        return MAX_TRIALS_PER_DAY;
    }

    /**
     * Returns remaining trials for today.
     */
    @JavascriptInterface
    public int getRemainingTrials() {
        return Math.max(0, MAX_TRIALS_PER_DAY - trialCount);
    }

    /**
     * Called from frontend when the page becomes visible (user is viewing the app).
     * If we're stuck in FOCUSING while the screen is clearly on, end the trial.
     * This is a safety net for devices where ACTION_USER_PRESENT doesn't fire.
     */
    @JavascriptInterface
    public void checkTrialState() {
        if (STATE_FOCUSING.equals(trialState)) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isInteractive()) {
                // Screen is on and user is viewing the app - they've definitely unlocked
                Log.d(TAG, "checkTrialState: Screen is on during FOCUSING - ending trial (JS fallback)");
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> onUserUnlock());
                }
            }
        }
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
            info.put("sessionActive", isSessionActive());
            info.put("trialState", trialState);
            info.put("trialCount", trialCount);
            info.put("maxTrials", MAX_TRIALS_PER_DAY);
            info.put("remainingTrials", getRemainingTrials());
            info.put("focusMinutes", getFocusMinutes());
            info.put("longestStreak", getLongestStreak());
            info.put("currentStreak", getCurrentStreak());
            info.put("currentTrialMinutes", getCurrentTrialMinutes());
            info.put("unlocks", unlocks); // completed trials
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("timezoneFlagged", timezoneFlagged);
            info.put("timezoneOffset", TimeZone.getDefault().getRawOffset());
            info.put("isCloned", isAppCloned());
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ---- Timezone Lock ----

    /**
     * Checks if the timezone has changed since the trial started.
     * If it has, flags the trial so focus time won't be counted.
     */
    private void checkTimezoneIntegrity() {
        if (savedTimezoneOffset == Integer.MIN_VALUE) return; // no baseline recorded
        int currentOffset = TimeZone.getDefault().getRawOffset();
        if (currentOffset != savedTimezoneOffset) {
            timezoneFlagged = true;
            Log.w(TAG, "TIMEZONE CHANGE DETECTED! Was " + savedTimezoneOffset + "ms, now " + currentOffset + "ms. Trial flagged.");
            saveData();
            notifyJs("timezoneCheat");
        }
    }

    /**
     * Returns a warning message if timezone was changed during the trial.
     * Returns empty string if no issue.
     */
    @JavascriptInterface
    public String getTimezoneWarning() {
        if (timezoneFlagged) {
            return "Timezone change detected during focus session. This trial's time was not counted.";
        }
        return "";
    }

    /**
     * Returns the current timezone offset in milliseconds.
     */
    @JavascriptInterface
    public int getTimezoneOffset() {
        return TimeZone.getDefault().getRawOffset();
    }

    // ---- App Cloner Detection ----

    /**
     * Detects if the app is running inside a cloned/dual-space environment.
     * Checks: package name mismatch, unusual data paths, known cloner apps installed.
     */
    @JavascriptInterface
    public boolean isAppCloned() {
        try {
            // Check 1: Package name doesn't match expected
            String currentPackage = context.getPackageName();
            if (!EXPECTED_PACKAGE.equals(currentPackage)) {
                Log.w(TAG, "Package name mismatch: " + currentPackage + " != " + EXPECTED_PACKAGE);
                return true;
            }

            // Check 2: Data directory contains clone-related paths
            String dataDir = context.getApplicationInfo().dataDir;
            if (dataDir != null) {
                String lowerDataDir = dataDir.toLowerCase();
                if (lowerDataDir.contains("clone") ||
                    lowerDataDir.contains("dual") ||
                    lowerDataDir.contains("parallel") ||
                    lowerDataDir.contains("dualspace") ||
                    lowerDataDir.contains("multi") ||
                    lowerDataDir.contains("island") ||
                    lowerDataDir.contains("privateme") ||
                    lowerDataDir.contains("999")) {  // some cloners use user 999
                    Log.w(TAG, "Suspicious data directory: " + dataDir);
                    return true;
                }

                // Check for non-standard user ID in path (cloned apps run under different user)
                // Normal: /data/data/com.package or /data/user/0/com.package
                // Cloned: /data/user/10/com.package or /data/user/999/com.package
                if (dataDir.contains("/data/user/")) {
                    try {
                        String afterUser = dataDir.substring(dataDir.indexOf("/data/user/") + 11);
                        String userIdStr = afterUser.substring(0, afterUser.indexOf('/'));
                        int userId = Integer.parseInt(userIdStr);
                        if (userId > 0) {
                            Log.w(TAG, "Running under non-primary user ID: " + userId + " (path: " + dataDir + ")");
                            return true;
                        }
                    } catch (Exception e) {
                        // parsing error, ignore
                    }
                }
            }

            // Check 3: Known cloner apps installed on device
            PackageManager pm = context.getPackageManager();
            for (String clonerPkg : CLONER_PACKAGES) {
                try {
                    pm.getPackageInfo(clonerPkg, 0);
                    Log.w(TAG, "Known cloner app installed: " + clonerPkg);
                    // Don't return true just for having the app installed -
                    // only flag if combined with other signals
                    // But we log it for awareness
                } catch (PackageManager.NameNotFoundException e) {
                    // Not installed, good
                }
            }

            // Check 4: Multiple instances detection via files directory
            File filesDir = context.getFilesDir();
            if (filesDir != null) {
                String filesPath = filesDir.getAbsolutePath().toLowerCase();
                if (filesPath.contains("clone") || filesPath.contains("parallel") ||
                    filesPath.contains("dual") || filesPath.contains("multi")) {
                    Log.w(TAG, "Suspicious files directory: " + filesPath);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error in cloner detection: " + e.getMessage());
            return false; // fail open - don't block legitimate users on error
        }
    }

    // ---- Lifecycle ----

    public void onDestroy() {
        // If focusing, finalize pending time but keep state so it can resume
        if (STATE_FOCUSING.equals(trialState) && screenOffSince > 0) {
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

    // ---- JS Notification ----

    /**
     * Standard event notification.
     * Sends: trialStarted, focusBegan, screenOn, maxTrialsReached
     */
    private void notifyJs(String event) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    JSONObject data = new JSONObject();
                    data.put("event", event);
                    data.put("trialState", trialState);
                    data.put("trialCount", trialCount);
                    data.put("maxTrials", MAX_TRIALS_PER_DAY);
                    data.put("remainingTrials", getRemainingTrials());
                    data.put("focusMinutes", getFocusMinutes());
                    data.put("longestStreak", getLongestStreak());
                    data.put("currentStreak", getCurrentStreak());
                    data.put("currentTrialMinutes", getCurrentTrialMinutes());
                    data.put("completedTrials", unlocks);

                    String js = "if(window.onDeepWorkEvent) window.onDeepWorkEvent(" + data.toString() + ");";
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    Log.e(TAG, "Error in notifyJs: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Special notification for trial completion with earned minutes.
     */
    private void notifyJsTrialEnded(int earnedMinutes) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    JSONObject data = new JSONObject();
                    data.put("event", "trialEnded");
                    data.put("earnedMinutes", earnedMinutes);
                    data.put("trialState", trialState);
                    data.put("trialCount", trialCount);
                    data.put("maxTrials", MAX_TRIALS_PER_DAY);
                    data.put("remainingTrials", getRemainingTrials());
                    data.put("focusMinutes", focusMinutes); // already finalized, no pending
                    data.put("longestStreak", longestStreak);
                    data.put("currentStreak", 0); // reset after trial ends
                    data.put("currentTrialMinutes", 0);
                    data.put("completedTrials", unlocks);

                    String js = "if(window.onDeepWorkEvent) window.onDeepWorkEvent(" + data.toString() + ");";
                    webView.evaluateJavascript(js, null);
                } catch (Exception e) {
                    Log.e(TAG, "Error in notifyJsTrialEnded: " + e.getMessage());
                }
            });
        }
    }
}
