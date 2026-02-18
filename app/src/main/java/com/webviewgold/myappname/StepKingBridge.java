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
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Step King Bridge - Google Fit for steps + heart points.
 * Uses Google Fit History API with data source filtering to exclude
 * manually entered data (e.g. "Trial 1" entries from Google Fit app).
 */
public class StepKingBridge {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_HEART_POINTS = "heart_points_today";
    private static final String KEY_MANUAL_STEPS = "manual_steps_today";
    private static final String KEY_MANUAL_HEART = "manual_heart_today";

    public static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 9002;

    private static final String GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness";

    private final Context context;
    private final WebView webView;
    private long todaySteps = 0;
    private float todayHeartPoints = 0;
    private long manualSteps = 0;
    private float manualHeartPoints = 0;

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
            manualSteps = prefs.getLong(KEY_MANUAL_STEPS, 0);
            manualHeartPoints = prefs.getFloat(KEY_MANUAL_HEART, 0);
        } else {
            todaySteps = 0;
            todayHeartPoints = 0;
            manualSteps = 0;
            manualHeartPoints = 0;
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, 0)
                .putString(KEY_STEPS_DATE, today)
                .putFloat(KEY_HEART_POINTS, 0)
                .putLong(KEY_MANUAL_STEPS, 0)
                .putFloat(KEY_MANUAL_HEART, 0)
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
            .putLong(KEY_MANUAL_STEPS, manualSteps)
            .putFloat(KEY_MANUAL_HEART, manualHeartPoints)
            .apply();
    }

    /**
     * Determines whether a data point was manually entered.
     * Manual entries come from the Google Fit app itself, or have
     * "user_input" in the stream name, or are raw data with no device.
     */
    private boolean isManualEntry(DataPoint dp) {
        DataSource source = dp.getOriginalDataSource();
        if (source == null) {
            source = dp.getDataSource();
        }
        if (source == null) {
            return false;
        }

        // Check app package name - Google Fit app = manual entry
        String pkg = source.getAppPackageName();
        if (pkg != null && pkg.equals(GOOGLE_FIT_PACKAGE)) {
            return true;
        }

        // Check stream name for "user_input"
        String stream = source.getStreamName();
        if (stream != null && stream.toLowerCase().contains("user_input")) {
            return true;
        }

        // Check for raw type with no device (another indicator of manual entry)
        if (source.getType() == DataSource.TYPE_RAW && source.getDevice() == null) {
            // Only flag as manual if there is also no known app behind it
            if (pkg == null || pkg.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the start-of-day timestamp in millis for today.
     */
    private long getStartOfDayMillis() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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

                long startTime = getStartOfDayMillis();
                long endTime = System.currentTimeMillis();

                DataReadRequest readRequest = new DataReadRequest.Builder()
                    .read(DataType.TYPE_STEP_COUNT_DELTA)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .enableServerQueries()
                    .build();

                Fitness.getHistoryClient((Activity) context, account)
                    .readData(readRequest)
                    .addOnSuccessListener(response -> {
                        long validSteps = 0;
                        long manualStepsLocal = 0;

                        for (DataSet dataSet : response.getDataSets()) {
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                int stepVal = dp.getValue(Field.FIELD_STEPS).asInt();
                                if (isManualEntry(dp)) {
                                    manualStepsLocal += stepVal;
                                    Log.d(TAG, "FILTERED manual steps: " + stepVal
                                        + " from " + dp.getOriginalDataSource());
                                } else {
                                    validSteps += stepVal;
                                }
                            }
                        }

                        manualSteps = manualStepsLocal;
                        saveData(validSteps, todayHeartPoints);
                        callJsCallback(validSteps, null);
                        Log.d(TAG, "Google Fit steps (valid): " + validSteps
                            + ", manual (filtered out): " + manualSteps);

                        // Notify frontend if manual entries were detected
                        if (manualSteps > 0) {
                            callJsManualEntryDetected();
                        }
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

                long startTime = getStartOfDayMillis();
                long endTime = System.currentTimeMillis();

                DataReadRequest readRequest = new DataReadRequest.Builder()
                    .read(DataType.TYPE_HEART_POINTS)
                    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                    .enableServerQueries()
                    .build();

                Fitness.getHistoryClient((Activity) context, account)
                    .readData(readRequest)
                    .addOnSuccessListener(response -> {
                        float validPoints = 0;
                        float manualPointsLocal = 0;

                        for (DataSet dataSet : response.getDataSets()) {
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                float pointVal = dp.getValue(Field.FIELD_INTENSITY).asFloat();
                                if (isManualEntry(dp)) {
                                    manualPointsLocal += pointVal;
                                    Log.d(TAG, "FILTERED manual heart points: " + pointVal
                                        + " from " + dp.getOriginalDataSource());
                                } else {
                                    validPoints += pointVal;
                                }
                            }
                        }

                        manualHeartPoints = manualPointsLocal;
                        todayHeartPoints = validPoints;
                        saveData(todaySteps, validPoints);
                        callJsHeartPoints(validPoints, null);
                        Log.d(TAG, "Google Fit heart points (valid): " + validPoints
                            + ", manual (filtered out): " + manualHeartPoints);

                        // Notify frontend if manual entries were detected
                        if (manualHeartPoints > 0) {
                            callJsManualEntryDetected();
                        }
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
            info.put("manualSteps", manualSteps);
            info.put("manualHeartPoints", manualHeartPoints);
            info.put("source", "google_fit_filtered");
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Returns a JSON string with manual entry warning info.
     * JS can call: var warning = JSON.parse(window.StepKing.getManualWarning());
     */
    @JavascriptInterface
    public String getManualWarning() {
        try {
            JSONObject warning = new JSONObject();
            boolean hasManual = (manualSteps > 0 || manualHeartPoints > 0);
            warning.put("hasManualEntries", hasManual);
            warning.put("manualSteps", manualSteps);
            warning.put("manualHeartPoints", manualHeartPoints);
            warning.put("validSteps", todaySteps);
            warning.put("validHeartPoints", todayHeartPoints);
            return warning.toString();
        } catch (Exception e) {
            return "{\"hasManualEntries\":false,\"manualSteps\":0,\"manualHeartPoints\":0}";
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

    /**
     * Calls window.onManualEntryDetected(manualSteps, manualHeartPoints)
     * so the frontend can show a warning about filtered manual data.
     */
    private void callJsManualEntryDetected() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                String js = "if(window.onManualEntryDetected) window.onManualEntryDetected("
                    + manualSteps + "," + manualHeartPoints + ");";
                webView.evaluateJavascript(js, null);
            });
        }
    }
}
