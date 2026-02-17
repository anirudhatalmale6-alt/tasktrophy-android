package com.webviewgold.myappname;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
 * Step King Bridge - Google Fit for steps + heart points.
 * Uses Google Fit API as the primary and only source.
 */
public class StepKingBridge {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_HEART_POINTS = "heart_points_today";

    public static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 9002;

    private final Context context;
    private final WebView webView;
    private long todaySteps = 0;
    private float todayHeartPoints = 0;

    private static FitnessOptions fitnessOptions;

    private static FitnessOptions getFitnessOptions() {
        if (fitnessOptions == null) {
            fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_READ)
                .build();
        }
        return fitnessOptions;
    }

    public StepKingBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        loadCachedData();
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_STEPS_DATE, "");
        String today = java.time.LocalDate.now().toString();
        if (today.equals(cachedDate)) {
            todaySteps = prefs.getLong(KEY_STEPS_TODAY, 0);
            todayHeartPoints = prefs.getFloat(KEY_HEART_POINTS, 0);
        } else {
            todaySteps = 0;
            todayHeartPoints = 0;
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, 0)
                .putString(KEY_STEPS_DATE, today)
                .putFloat(KEY_HEART_POINTS, 0)
                .apply();
        }
    }

    private void saveData(long steps, float heartPoints) {
        todaySteps = steps;
        todayHeartPoints = heartPoints;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putLong(KEY_STEPS_TODAY, steps)
            .putString(KEY_STEPS_DATE, java.time.LocalDate.now().toString())
            .putFloat(KEY_HEART_POINTS, heartPoints)
            .apply();
    }

    // ─── JS Interface Methods ───

    @JavascriptInterface
    public boolean isAvailable() {
        try {
            return com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean hasPermission() {
        try {
            GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, getFitnessOptions());
            return GoogleSignIn.hasPermissions(account, getFitnessOptions());
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public void requestPermission() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, getFitnessOptions());
                    if (!GoogleSignIn.hasPermissions(account, getFitnessOptions())) {
                        GoogleSignIn.requestPermissions(
                            (Activity) context,
                            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                            account,
                            getFitnessOptions()
                        );
                    } else {
                        refreshSteps();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting Google Fit permission: " + e.getMessage());
                    callJsCallback(-1, "Failed to request permission: " + e.getMessage());
                }
            });
        }
    }

    @JavascriptInterface
    public long getTodaySteps() {
        return todaySteps;
    }

    @JavascriptInterface
    public void refreshSteps() {
        if (!(context instanceof Activity)) return;

        ((Activity) context).runOnUiThread(() -> {
            try {
                GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, getFitnessOptions());
                if (!GoogleSignIn.hasPermissions(account, getFitnessOptions())) {
                    callJsCallback(-1, "Google Fit permission not granted");
                    return;
                }

                Fitness.getHistoryClient((Activity) context, account)
                    .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener(dataSet -> {
                        long steps = 0;
                        if (!dataSet.isEmpty()) {
                            steps = dataSet.getDataPoints().get(0)
                                .getValue(Field.FIELD_STEPS).asInt();
                        }
                        saveData(steps, todayHeartPoints);
                        callJsCallback(steps, null);
                        Log.d(TAG, "Google Fit steps: " + steps);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to read steps: " + e.getMessage());
                        callJsCallback(-1, "Failed to read steps: " + e.getMessage());
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error reading steps: " + e.getMessage());
                callJsCallback(-1, e.getMessage());
            }
        });
    }

    @JavascriptInterface
    public void getHeartPoints() {
        if (!(context instanceof Activity)) return;

        ((Activity) context).runOnUiThread(() -> {
            try {
                GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, getFitnessOptions());
                if (!GoogleSignIn.hasPermissions(account, getFitnessOptions())) {
                    callJsHeartPoints(-1, "Google Fit permission not granted");
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
                        callJsHeartPoints(-1, "Failed to read heart points");
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error reading heart points: " + e.getMessage());
                callJsHeartPoints(-1, e.getMessage());
            }
        });
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
            info.put("googleFitAvailable", isAvailable());
            info.put("hasPermission", hasPermission());
            info.put("todaySteps", todaySteps);
            info.put("todayHeartPoints", todayHeartPoints);
            info.put("source", "google_fit");
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Called from MainActivity when Google Fit permission is granted */
    public void onPermissionGranted() {
        refreshSteps();
        getHeartPoints();
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
