package com.webviewgold.myappname;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

/**
 * Step King Bridge - uses hardware step counter sensor.
 * No Google Cloud / Firebase setup required.
 * Only needs ACTIVITY_RECOGNITION permission (Android 10+).
 */
public class StepKingBridge implements SensorEventListener {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_SENSOR_BASELINE = "sensor_baseline";
    private static final String KEY_SENSOR_BASELINE_DATE = "sensor_baseline_date";

    public static final int PERMISSION_REQUEST_CODE = 9002;

    private final Context context;
    private final WebView webView;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private boolean sensorRegistered = false;

    private long todaySteps = 0;
    private float sensorBaseline = -1; // first sensor reading of the day

    public StepKingBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        loadCachedData();
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_STEPS_DATE, "");
        String today = java.time.LocalDate.now().toString();

        if (today.equals(cachedDate)) {
            todaySteps = prefs.getLong(KEY_STEPS_TODAY, 0);
            // Restore baseline for today
            String baselineDate = prefs.getString(KEY_SENSOR_BASELINE_DATE, "");
            if (today.equals(baselineDate)) {
                sensorBaseline = prefs.getFloat(KEY_SENSOR_BASELINE, -1);
            } else {
                sensorBaseline = -1;
            }
        } else {
            // New day - reset
            todaySteps = 0;
            sensorBaseline = -1;
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, 0)
                .putString(KEY_STEPS_DATE, today)
                .putFloat(KEY_SENSOR_BASELINE, -1)
                .putString(KEY_SENSOR_BASELINE_DATE, today)
                .apply();
        }
    }

    private void saveData(long steps) {
        todaySteps = steps;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = java.time.LocalDate.now().toString();
        SharedPreferences.Editor editor = prefs.edit()
            .putLong(KEY_STEPS_TODAY, steps)
            .putString(KEY_STEPS_DATE, today);

        if (sensorBaseline >= 0) {
            editor.putFloat(KEY_SENSOR_BASELINE, sensorBaseline);
            editor.putString(KEY_SENSOR_BASELINE_DATE, today);
        }
        editor.apply();
    }

    // ─── SensorEventListener ───

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];
            String today = java.time.LocalDate.now().toString();
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String baselineDate = prefs.getString(KEY_SENSOR_BASELINE_DATE, "");

            // Reset baseline if it's a new day
            if (!today.equals(baselineDate) || sensorBaseline < 0) {
                sensorBaseline = totalStepsSinceBoot;
                todaySteps = 0;
                prefs.edit()
                    .putFloat(KEY_SENSOR_BASELINE, sensorBaseline)
                    .putString(KEY_SENSOR_BASELINE_DATE, today)
                    .putLong(KEY_STEPS_TODAY, 0)
                    .putString(KEY_STEPS_DATE, today)
                    .apply();
            }

            long stepsToday = (long) (totalStepsSinceBoot - sensorBaseline);
            if (stepsToday < 0) {
                // Device rebooted (sensor reset), use totalStepsSinceBoot as new baseline
                sensorBaseline = totalStepsSinceBoot;
                // Keep previously accumulated steps
                stepsToday = todaySteps;
            }

            saveData(stepsToday);
            Log.d(TAG, "Steps today: " + stepsToday + " (sensor total: " + totalStepsSinceBoot + ", baseline: " + sensorBaseline + ")");

            // Notify JS
            callJsCallback(stepsToday, null);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    // ─── JS Interface Methods ───

    @JavascriptInterface
    public boolean isAvailable() {
        return stepCounterSensor != null;
    }

    @JavascriptInterface
    public boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not needed below Android 10
    }

    @JavascriptInterface
    public void requestPermission() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!hasPermission()) {
                    activity.requestPermissions(
                        new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_CODE
                    );
                    return;
                }
            }
            // Already have permission, start sensor
            startListening();
            callJsCallback(todaySteps, null);
        }
    }

    @JavascriptInterface
    public long getTodaySteps() {
        return todaySteps;
    }

    @JavascriptInterface
    public void refreshSteps() {
        if (!hasPermission()) {
            callJsCallback(-1, "Activity Recognition permission not granted");
            return;
        }

        if (!isAvailable()) {
            callJsCallback(-1, "Step counter sensor not available on this device");
            return;
        }

        // Start/restart sensor listener
        startListening();

        // Return cached value immediately (sensor will update async)
        callJsCallback(todaySteps, null);
    }

    @JavascriptInterface
    public void getHeartPoints() {
        // Heart points not available with hardware sensor
        // Return 0 - the web UI can hide this if not supported
        callJsHeartPoints(0, null);
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
    public void startBackgroundSync() {
        StepSyncWorker.schedule(context);
    }

    @JavascriptInterface
    public void stopBackgroundSync() {
        StepSyncWorker.cancel(context);
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("model", Build.MODEL);
            info.put("manufacturer", Build.MANUFACTURER);
            info.put("sdk", Build.VERSION.SDK_INT);
            info.put("stepSensorAvailable", isAvailable());
            info.put("hasPermission", hasPermission());
            info.put("todaySteps", todaySteps);
            info.put("source", "hardware_sensor");
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ─── Sensor Management ───

    public void startListening() {
        if (sensorRegistered || stepCounterSensor == null) return;
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorRegistered = true;
        Log.d(TAG, "Step counter sensor listener registered");
    }

    public void stopListening() {
        if (!sensorRegistered) return;
        sensorManager.unregisterListener(this);
        sensorRegistered = false;
        Log.d(TAG, "Step counter sensor listener unregistered");
    }

    /** Called from MainActivity when ACTIVITY_RECOGNITION permission is granted */
    public void onPermissionGranted() {
        startListening();
        callJsCallback(todaySteps, null);
    }

    // ─── JS Callbacks ───

    private void callJsCallback(long steps, String error) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
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

    private void callJsHeartPoints(float points, String error) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                String js;
                if (error != null) {
                    js = "if(window.onHeartPointsError) window.onHeartPointsError('" +
                         error.replace("'", "\\'") + "');";
                } else {
                    js = "if(window.onHeartPoints) window.onHeartPoints(" + points + ");";
                }
                webView.evaluateJavascript(js, null);
            });
        }
    }
}
