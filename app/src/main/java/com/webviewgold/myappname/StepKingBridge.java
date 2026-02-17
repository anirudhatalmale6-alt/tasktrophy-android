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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;

import org.json.JSONObject;

/**
 * Step King Bridge - Hybrid approach:
 * 1. Hardware step sensor for immediate step counting (no setup needed)
 * 2. Google Fit for heart points + cross-device sync (needs Cloud setup)
 *
 * Steps: Hardware sensor (primary) - works out of the box
 * Heart Points: Google Fit (requires Fitness API enabled + SHA-1 in console)
 */
public class StepKingBridge implements SensorEventListener {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_HEART_POINTS = "heart_points_today";
    private static final String KEY_SENSOR_BASELINE = "sensor_baseline";
    private static final String KEY_SENSOR_BASELINE_DATE = "sensor_baseline_date";

    public static final int PERMISSION_REQUEST_CODE = 9002;
    public static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 9003;

    private final Context context;
    private final WebView webView;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private boolean sensorRegistered = false;

    private long todaySteps = 0;
    private float todayHeartPoints = 0;
    private float sensorBaseline = -1;

    // Google Fit - lazy initialized
    private static FitnessOptions fitnessOptions;

    private static FitnessOptions getFitnessOptions() {
        if (fitnessOptions == null) {
            try {
                fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                    .build();
            } catch (Exception e) {
                Log.e(TAG, "Failed to build FitnessOptions: " + e.getMessage());
            }
        }
        return fitnessOptions;
    }

    public StepKingBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;

        // Hardware step sensor
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
            todayHeartPoints = prefs.getFloat(KEY_HEART_POINTS, 0);
            String baselineDate = prefs.getString(KEY_SENSOR_BASELINE_DATE, "");
            if (today.equals(baselineDate)) {
                sensorBaseline = prefs.getFloat(KEY_SENSOR_BASELINE, -1);
            } else {
                sensorBaseline = -1;
            }
        } else {
            todaySteps = 0;
            todayHeartPoints = 0;
            sensorBaseline = -1;
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, 0)
                .putString(KEY_STEPS_DATE, today)
                .putFloat(KEY_HEART_POINTS, 0)
                .putFloat(KEY_SENSOR_BASELINE, -1)
                .putString(KEY_SENSOR_BASELINE_DATE, today)
                .apply();
        }
    }

    private void saveData(long steps, float heartPoints) {
        todaySteps = steps;
        todayHeartPoints = heartPoints;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = java.time.LocalDate.now().toString();
        SharedPreferences.Editor editor = prefs.edit()
            .putLong(KEY_STEPS_TODAY, steps)
            .putString(KEY_STEPS_DATE, today)
            .putFloat(KEY_HEART_POINTS, heartPoints);

        if (sensorBaseline >= 0) {
            editor.putFloat(KEY_SENSOR_BASELINE, sensorBaseline);
            editor.putString(KEY_SENSOR_BASELINE_DATE, today);
        }
        editor.apply();
    }

    // ─── Hardware Step Sensor ───

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalStepsSinceBoot = event.values[0];
            String today = java.time.LocalDate.now().toString();
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String baselineDate = prefs.getString(KEY_SENSOR_BASELINE_DATE, "");

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
                sensorBaseline = totalStepsSinceBoot;
                stepsToday = todaySteps;
            }

            saveData(stepsToday, todayHeartPoints);
            Log.d(TAG, "Steps today: " + stepsToday + " (sensor total: " + totalStepsSinceBoot + ")");
            callJsCallback(stepsToday, null);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

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
        return true;
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
        startListening();
        callJsCallback(todaySteps, null);
    }

    // ─── Google Fit (Heart Points + optional steps) ───

    @JavascriptInterface
    public boolean isGoogleFitAvailable() {
        try {
            return com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean hasGoogleFitPermission() {
        try {
            FitnessOptions opts = getFitnessOptions();
            if (opts == null) return false;
            GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, opts);
            return GoogleSignIn.hasPermissions(account, opts);
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public void requestGoogleFitPermission() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    FitnessOptions opts = getFitnessOptions();
                    if (opts == null) {
                        callJsHeartPoints(-1, "Google Fit not available");
                        return;
                    }
                    GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, opts);
                    if (!GoogleSignIn.hasPermissions(account, opts)) {
                        GoogleSignIn.requestPermissions(
                            (Activity) context,
                            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                            account,
                            opts
                        );
                    } else {
                        fetchHeartPoints();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting Google Fit permission: " + e.getMessage());
                    callJsHeartPoints(-1, "Failed to request Google Fit: " + e.getMessage());
                }
            });
        }
    }

    @JavascriptInterface
    public void getHeartPoints() {
        if (!hasGoogleFitPermission()) {
            callJsHeartPoints(0, null);
            return;
        }
        fetchHeartPoints();
    }

    private void fetchHeartPoints() {
        if (!(context instanceof Activity)) return;

        ((Activity) context).runOnUiThread(() -> {
            try {
                FitnessOptions opts = getFitnessOptions();
                if (opts == null) {
                    callJsHeartPoints(0, null);
                    return;
                }
                GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, opts);
                if (!GoogleSignIn.hasPermissions(account, opts)) {
                    callJsHeartPoints(0, null);
                    return;
                }

                Fitness.getHistoryClient((Activity) context, account)
                    .readDailyTotal(DataType.TYPE_HEART_POINTS)
                    .addOnSuccessListener(dataSet -> {
                        float points = 0;
                        if (!dataSet.isEmpty()) {
                            points = dataSet.getDataPoints().get(0)
                                .getValue(Field.FIELD_INTENSITY).asFloat();
                        }
                        todayHeartPoints = points;
                        saveData(todaySteps, points);
                        callJsHeartPoints(points, null);
                        Log.d(TAG, "Google Fit heart points: " + points);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to read heart points: " + e.getMessage());
                        callJsHeartPoints(0, null);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error reading heart points: " + e.getMessage());
                callJsHeartPoints(0, null);
            }
        });
    }

    /** Also fetch steps from Google Fit (optional, used for cross-device sync) */
    @JavascriptInterface
    public void getGoogleFitSteps() {
        if (!(context instanceof Activity)) return;

        ((Activity) context).runOnUiThread(() -> {
            try {
                FitnessOptions opts = getFitnessOptions();
                if (opts == null) {
                    callJsCallback(todaySteps, null);
                    return;
                }
                GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, opts);
                if (!GoogleSignIn.hasPermissions(account, opts)) {
                    callJsCallback(todaySteps, null);
                    return;
                }

                Fitness.getHistoryClient((Activity) context, account)
                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener(dataSet -> {
                        long fitSteps = 0;
                        if (!dataSet.isEmpty()) {
                            fitSteps = dataSet.getDataPoints().get(0)
                                .getValue(Field.FIELD_STEPS).asInt();
                        }
                        // Use the higher value between hardware sensor and Google Fit
                        long bestSteps = Math.max(todaySteps, fitSteps);
                        saveData(bestSteps, todayHeartPoints);
                        callJsCallback(bestSteps, null);
                        Log.d(TAG, "Google Fit steps: " + fitSteps + ", sensor steps: " + todaySteps + ", using: " + bestSteps);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to read Google Fit steps: " + e.getMessage());
                        callJsCallback(todaySteps, null);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error reading Google Fit steps: " + e.getMessage());
                callJsCallback(todaySteps, null);
            }
        });
    }

    // ─── Utility Methods ───

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
            info.put("googleFitAvailable", isGoogleFitAvailable());
            info.put("googleFitPermission", hasGoogleFitPermission());
            info.put("todaySteps", todaySteps);
            info.put("todayHeartPoints", todayHeartPoints);
            info.put("source", "hybrid");
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

    /** Called from MainActivity when Google Fit permission is granted */
    public void onGoogleFitPermissionGranted() {
        fetchHeartPoints();
        getGoogleFitSteps();
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
