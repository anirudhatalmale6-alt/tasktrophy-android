package com.tasktrophy.official;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

/**
 * JavaScript bridge for Step King challenge.
 * Exposed to WebView as window.StepKing
 *
 * Uses Android's built-in step counter sensor (TYPE_STEP_COUNTER)
 * which counts steps since last device reboot. We track the daily
 * delta by storing the sensor value at midnight/first read.
 *
 * Methods available from JavaScript:
 * - StepKing.isAvailable() -> boolean (step sensor exists)
 * - StepKing.hasPermission() -> boolean (ACTIVITY_RECOGNITION granted)
 * - StepKing.requestPermission() -> void (requests ACTIVITY_RECOGNITION)
 * - StepKing.getTodaySteps() -> long (current daily step count)
 * - StepKing.refreshSteps() -> void (re-reads sensor, calls window.onStepsRead)
 * - StepKing.getDeviceId() -> String (ANDROID_ID for anti-cheat)
 * - StepKing.startBackgroundSync() -> void (WorkManager every 15 min)
 * - StepKing.stopBackgroundSync() -> void
 * - StepKing.getDeviceInfo() -> String (JSON debug info)
 */
public class StepKingBridge implements SensorEventListener {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_SENSOR_BASELINE = "sensor_baseline";
    private static final int PERMISSION_REQUEST_CODE = 9001;

    private final Context context;
    private final WebView webView;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean sensorAvailable = false;
    private long todaySteps = 0;
    private float sensorBaseline = -1; // sensor value at start of day

    public StepKingBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        initSensor();
        loadCachedSteps();
    }

    private void initSensor() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorAvailable = (stepSensor != null);
            if (sensorAvailable) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "Step counter sensor registered");
            } else {
                Log.w(TAG, "No step counter sensor available on this device");
            }
        }
    }

    private void loadCachedSteps() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_STEPS_DATE, "");
        String today = java.time.LocalDate.now().toString();
        if (today.equals(cachedDate)) {
            todaySteps = prefs.getLong(KEY_STEPS_TODAY, 0);
            sensorBaseline = prefs.getFloat(KEY_SENSOR_BASELINE, -1);
        } else {
            // New day - reset
            todaySteps = 0;
            sensorBaseline = -1;
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, 0)
                .putString(KEY_STEPS_DATE, today)
                .putFloat(KEY_SENSOR_BASELINE, -1)
                .apply();
        }
    }

    private void saveSteps(long steps, float baseline) {
        todaySteps = steps;
        sensorBaseline = baseline;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putLong(KEY_STEPS_TODAY, steps)
            .putString(KEY_STEPS_DATE, java.time.LocalDate.now().toString())
            .putFloat(KEY_SENSOR_BASELINE, baseline)
            .apply();
    }

    // ═══ SensorEventListener ═══

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];

            // First reading of the day - set baseline
            if (sensorBaseline < 0) {
                sensorBaseline = totalStepsSinceBoot;
                saveSteps(0, sensorBaseline);
                Log.d(TAG, "Baseline set: " + sensorBaseline);
                return;
            }

            // Calculate today's steps as delta from baseline
            long steps = (long)(totalStepsSinceBoot - sensorBaseline);
            if (steps < 0) {
                // Device rebooted - reset baseline
                sensorBaseline = totalStepsSinceBoot;
                steps = 0;
            }

            saveSteps(steps, sensorBaseline);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    // ═══ JavaScript Interface Methods ═══

    /**
     * Check if step counter sensor is available
     */
    @JavascriptInterface
    public boolean isAvailable() {
        return sensorAvailable;
    }

    /**
     * Check if ACTIVITY_RECOGNITION permission is granted (needed on API 29+)
     */
    @JavascriptInterface
    public boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not needed before Android 10
    }

    /**
     * Request ACTIVITY_RECOGNITION permission
     */
    @JavascriptInterface
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).runOnUiThread(() -> {
                    ActivityCompat.requestPermissions(
                        (MainActivity) context,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_CODE
                    );
                });
            }
        }
    }

    /**
     * Get today's step count (synchronous, returns cached value)
     */
    @JavascriptInterface
    public long getTodaySteps() {
        return todaySteps;
    }

    /**
     * Re-read sensor and call window.onStepsRead(steps) with latest value
     */
    @JavascriptInterface
    public void refreshSteps() {
        // The sensor listener updates continuously, so just return cached value
        callJsCallback(todaySteps, null);
    }

    /**
     * Get a unique device identifier for anti-cheat
     */
    @JavascriptInterface
    public String getDeviceId() {
        try {
            return Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Start background step sync via WorkManager
     */
    @JavascriptInterface
    public void startBackgroundSync() {
        StepSyncWorker.schedule(context);
    }

    /**
     * Stop background step sync
     */
    @JavascriptInterface
    public void stopBackgroundSync() {
        StepSyncWorker.cancel(context);
    }

    /**
     * Get device info for debugging (returns JSON string)
     */
    @JavascriptInterface
    public String getDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("sdk", Build.VERSION.SDK_INT);
            info.put("sensorAvailable", sensorAvailable);
            info.put("hasPermission", hasPermission());
            info.put("todaySteps", todaySteps);
            info.put("sensorBaseline", sensorBaseline);
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Clean up sensor listener
     */
    public void destroy() {
        if (sensorManager != null && stepSensor != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void callJsCallback(long steps, String error) {
        if (context instanceof MainActivity) {
            ((MainActivity) context).runOnUiThread(() -> {
                String js;
                if (error != null) {
                    js = "if(window.onStepsError) window.onStepsError('" +
                         error.replace("'", "\\'") + "');";
                } else {
                    js = "if(window.onStepsRead) window.onStepsRead(" + steps + ");";
                }
                webView.evaluateJavascript(js, null);
            });
        }
    }
}
