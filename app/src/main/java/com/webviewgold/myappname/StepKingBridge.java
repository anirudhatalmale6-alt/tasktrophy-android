package com.webviewgold.myappname;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.DistanceRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.AggregateRequest;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.aggregate.AggregationResult;
import androidx.health.connect.client.response.ReadRecordsResponse;
import androidx.health.connect.client.time.TimeRangeFilter;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlinx.coroutines.BuildersKt;

/**
 * Step King Bridge - Health Connect for steps, heart rate, and distance.
 * Simplified: no manual entry detection (not needed with Health Connect).
 * Uses new thread per request to avoid executor deadlock.
 */
public class StepKingBridge {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_HEART_POINTS = "heart_points_today";

    private static final String HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata";

    private final Context context;
    private final WebView webView;
    private volatile long todaySteps = 0;
    private volatile float todayHeartPoints = 0;

    private HealthConnectClient healthConnectClient;
    private volatile boolean cachedPermissionState = false;

    // Permission launcher - set from MainActivity
    private ActivityResultLauncher<Set<String>> permissionLauncher;

    @SuppressWarnings("unchecked")
    private static final KClass<StepsRecord> STEPS_KCLASS =
        JvmClassMappingKt.getKotlinClass(StepsRecord.class);
    @SuppressWarnings("unchecked")
    private static final KClass<HeartRateRecord> HR_KCLASS =
        JvmClassMappingKt.getKotlinClass(HeartRateRecord.class);
    @SuppressWarnings("unchecked")
    private static final KClass<DistanceRecord> DISTANCE_KCLASS =
        JvmClassMappingKt.getKotlinClass(DistanceRecord.class);

    public static final Set<String> REQUIRED_PERMISSIONS = new HashSet<>(Arrays.asList(
        HealthPermission.getReadPermission(STEPS_KCLASS),
        HealthPermission.getReadPermission(HR_KCLASS),
        HealthPermission.getReadPermission(DISTANCE_KCLASS)
    ));

    public StepKingBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        loadCachedData();
        initHealthConnect();
    }

    private void initHealthConnect() {
        try {
            int status = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE);
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                healthConnectClient = HealthConnectClient.getOrCreate(context);
                Log.d(TAG, "Health Connect client initialized");
                refreshPermissionState();
            } else {
                Log.w(TAG, "Health Connect not available, status: " + status);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to init Health Connect: " + e.getMessage());
        }
    }

    private void refreshPermissionState() {
        if (healthConnectClient == null) {
            cachedPermissionState = false;
            return;
        }
        new Thread(() -> {
            try {
                @SuppressWarnings("unchecked")
                Set<String> granted = (Set<String>) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> healthConnectClient.getPermissionController()
                        .getGrantedPermissions(continuation)
                );
                cachedPermissionState = granted.containsAll(REQUIRED_PERMISSIONS);
                Log.d(TAG, "Permission state refreshed: " + cachedPermissionState);
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing permission state: " + e.getMessage());
                cachedPermissionState = false;
            }
        }, "HC-PermCheck").start();
    }

    public void setPermissionLauncher(ActivityResultLauncher<Set<String>> launcher) {
        this.permissionLauncher = launcher;
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_STEPS_DATE, "");
        String today = LocalDate.now().toString();
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

    private void saveSteps(long steps) {
        todaySteps = steps;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putLong(KEY_STEPS_TODAY, steps)
            .putString(KEY_STEPS_DATE, LocalDate.now().toString())
            .apply();
    }

    private void saveHeartPoints(float hp) {
        todayHeartPoints = hp;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putFloat(KEY_HEART_POINTS, hp)
            .putString(KEY_STEPS_DATE, LocalDate.now().toString())
            .apply();
    }

    private Instant getStartOfDayInstant() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    // ─── JS Interface Methods ───

    @JavascriptInterface
    public boolean isAvailable() {
        try {
            int status = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE);
            return status == HealthConnectClient.SDK_AVAILABLE;
        } catch (Exception e) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean hasPermission() {
        if (healthConnectClient == null) return false;
        return cachedPermissionState;
    }

    @JavascriptInterface
    public void requestPermission() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    if (healthConnectClient == null) {
                        int status = HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE);
                        if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                            String uriString = "market://details?id=" + HEALTH_CONNECT_PACKAGE
                                + "&url=healthconnect%3A%2F%2Fonboarding";
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setPackage("com.android.vending");
                            intent.setData(Uri.parse(uriString));
                            intent.putExtra("overlay", true);
                            intent.putExtra("callerId", context.getPackageName());
                            context.startActivity(intent);
                        } else {
                            callJsCallback(-1, "Health Connect not available on this device");
                        }
                        return;
                    }

                    if (permissionLauncher != null) {
                        permissionLauncher.launch(REQUIRED_PERMISSIONS);
                    } else {
                        Log.e(TAG, "Permission launcher not set");
                        callJsCallback(-1, "Permission launcher not initialized");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting Health Connect permission: " + e.getMessage());
                    callJsCallback(-1, "Failed to request permission: " + e.getMessage());
                }
            });
        }
    }

    @JavascriptInterface
    public long getTodaySteps() {
        return todaySteps;
    }

    /**
     * Reads today's total steps from Health Connect.
     * Uses a new thread to avoid deadlocking single-thread executors.
     * Calls window.onStepsRead(steps) or window.onStepsError(error) on completion.
     */
    @JavascriptInterface
    public void refreshSteps() {
        if (healthConnectClient == null) {
            callJsCallback(-1, "Health Connect not available");
            return;
        }

        new Thread(() -> {
            try {
                Instant startTime = getStartOfDayInstant();
                Instant endTime = Instant.now();

                Log.d(TAG, "Reading steps from Health Connect: " + startTime + " to " + endTime);

                AggregateRequest aggregateRequest = new AggregateRequest(
                    new HashSet<>(Arrays.asList(StepsRecord.COUNT_TOTAL)),
                    TimeRangeFilter.between(startTime, endTime),
                    new HashSet<>()
                );

                AggregationResult result = (AggregationResult) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> healthConnectClient.aggregate(aggregateRequest, continuation)
                );

                Long totalSteps = result.get(StepsRecord.COUNT_TOTAL);
                long stepsCount = (totalSteps != null) ? totalSteps : 0;
                Log.d(TAG, "Health Connect total steps: " + stepsCount);

                saveSteps(stepsCount);
                callJsCallback(stepsCount, null);

            } catch (Exception e) {
                Log.e(TAG, "Failed to read steps: " + e.getMessage(), e);
                callJsCallback(-1, "Failed to read steps: " + e.getMessage());
            }
        }, "HC-Steps").start();
    }

    /**
     * Reads today's heart rate data from Health Connect and calculates heart points.
     * Moderate (BPM > 100): 1 pt/min, Vigorous (BPM > 130): 2 pts/min.
     * Fallback: if no heart rate data, calculates from steps (1 pt per 1000 steps).
     */
    @JavascriptInterface
    public void getHeartPoints() {
        if (healthConnectClient == null) {
            callJsHeartPoints(-1, "Health Connect not available");
            return;
        }

        new Thread(() -> {
            try {
                Instant startTime = getStartOfDayInstant();
                Instant endTime = Instant.now();

                Log.d(TAG, "Reading heart rate from Health Connect: " + startTime + " to " + endTime);

                // Try heart rate data first
                float heartRatePoints = 0;
                boolean hasHeartRateData = false;

                try {
                    ReadRecordsRequest<HeartRateRecord> readRequest = new ReadRecordsRequest<>(
                        HR_KCLASS,
                        TimeRangeFilter.between(startTime, endTime),
                        new HashSet<>(),
                        true,
                        1000,
                        null
                    );

                    @SuppressWarnings("unchecked")
                    ReadRecordsResponse<HeartRateRecord> response = (ReadRecordsResponse<HeartRateRecord>) BuildersKt.runBlocking(
                        EmptyCoroutineContext.INSTANCE,
                        (scope, continuation) -> healthConnectClient.readRecords(readRequest, continuation)
                    );

                    for (HeartRateRecord record : response.getRecords()) {
                        long durationMs = record.getEndTime().toEpochMilli() - record.getStartTime().toEpochMilli();
                        float durationMins = durationMs / 60000f;

                        List<HeartRateRecord.Sample> samples = record.getSamples();
                        if (samples.isEmpty()) continue;

                        hasHeartRateData = true;
                        long totalBpm = 0;
                        for (HeartRateRecord.Sample sample : samples) {
                            totalBpm += sample.getBeatsPerMinute();
                        }
                        float avgBpm = (float) totalBpm / samples.size();

                        if (avgBpm > 130) {
                            heartRatePoints += durationMins * 2;
                        } else if (avgBpm > 100) {
                            heartRatePoints += durationMins;
                        }
                    }

                    Log.d(TAG, "Heart rate records: " + response.getRecords().size() +
                        ", heart rate points: " + heartRatePoints);
                } catch (Exception e) {
                    Log.w(TAG, "Heart rate read failed, will use step fallback: " + e.getMessage());
                }

                float totalPoints = heartRatePoints;

                // Fallback: if no heart rate data, calculate from steps
                // 1 heart point per 1000 steps (walking is moderate activity)
                if (!hasHeartRateData || heartRatePoints < 0.1f) {
                    try {
                        AggregateRequest stepsRequest = new AggregateRequest(
                            new HashSet<>(Arrays.asList(StepsRecord.COUNT_TOTAL)),
                            TimeRangeFilter.between(startTime, endTime),
                            new HashSet<>()
                        );

                        AggregationResult stepsResult = (AggregationResult) BuildersKt.runBlocking(
                            EmptyCoroutineContext.INSTANCE,
                            (scope, continuation) -> healthConnectClient.aggregate(stepsRequest, continuation)
                        );

                        Long stepCount = stepsResult.get(StepsRecord.COUNT_TOTAL);
                        long steps = (stepCount != null) ? stepCount : 0;

                        // 1 heart point per 1000 steps (walking activity)
                        float stepPoints = steps / 1000f;
                        totalPoints = Math.max(totalPoints, stepPoints);

                        Log.d(TAG, "Step-based heart points fallback: " + steps +
                            " steps = " + stepPoints + " pts");
                    } catch (Exception e) {
                        Log.w(TAG, "Step fallback also failed: " + e.getMessage());
                    }
                }

                // Round to 1 decimal
                totalPoints = Math.round(totalPoints * 10f) / 10f;

                Log.d(TAG, "Final heart points: " + totalPoints +
                    " (hasHeartRate=" + hasHeartRateData + ")");

                saveHeartPoints(totalPoints);
                callJsHeartPoints(totalPoints, null);

            } catch (Exception e) {
                Log.e(TAG, "Failed to calculate heart points: " + e.getMessage(), e);
                callJsHeartPoints(-1, "Failed to calculate heart points: " + e.getMessage());
            }
        }, "HC-HeartRate").start();
    }

    /**
     * Reads today's total distance from Health Connect (meters).
     * Used by Ghost Runner to track distance without GPS.
     * Calls window.onDistanceRead(meters) or window.onDistanceError(error).
     */
    @JavascriptInterface
    public void getDistance() {
        if (healthConnectClient == null) {
            callJsDistance(-1, "Health Connect not available");
            return;
        }

        new Thread(() -> {
            try {
                Instant startTime = getStartOfDayInstant();
                Instant endTime = Instant.now();

                Log.d(TAG, "Reading distance from Health Connect: " + startTime + " to " + endTime);

                AggregateRequest aggregateRequest = new AggregateRequest(
                    new HashSet<>(Arrays.asList(DistanceRecord.DISTANCE_TOTAL)),
                    TimeRangeFilter.between(startTime, endTime),
                    new HashSet<>()
                );

                AggregationResult result = (AggregationResult) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> healthConnectClient.aggregate(aggregateRequest, continuation)
                );

                // Distance is returned as Length which has getMeters()
                Object distObj = result.get(DistanceRecord.DISTANCE_TOTAL);
                double distMeters = 0;
                if (distObj != null) {
                    // DistanceRecord.DISTANCE_TOTAL returns a Length object
                    androidx.health.connect.client.units.Length length =
                        (androidx.health.connect.client.units.Length) distObj;
                    distMeters = length.getMeters();
                }

                Log.d(TAG, "Health Connect total distance: " + distMeters + "m");

                callJsDistance(distMeters, null);

            } catch (Exception e) {
                Log.e(TAG, "Failed to read distance: " + e.getMessage(), e);
                callJsDistance(-1, "Failed to read distance: " + e.getMessage());
            }
        }, "HC-Distance").start();
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
            info.put("healthConnectAvailable", isAvailable());
            info.put("hasPermission", hasPermission());
            info.put("todaySteps", todaySteps);
            info.put("todayHeartPoints", todayHeartPoints);
            info.put("source", "health_connect");
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    @JavascriptInterface
    public String getManualWarning() {
        // No longer used — kept for backward compatibility
        return "{\"hasManualEntries\":false,\"manualSteps\":0,\"manualHeartPoints\":0}";
    }

    /** Called from MainActivity when Health Connect permission is granted */
    public void onPermissionGranted() {
        cachedPermissionState = true;
        refreshPermissionState();
        refreshSteps();
        getHeartPoints();
    }

    @JavascriptInterface
    public void requestActivityRecognition() {
        Log.d(TAG, "requestActivityRecognition called - not needed for Health Connect");
    }

    public void onActivityRecognitionGranted() {
        // No-op
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

    private void callJsDistance(double meters, String error) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                String js;
                if (error != null) {
                    js = "if(window.onDistanceError) window.onDistanceError('" +
                         error.replace("'", "\\'") + "');";
                } else {
                    js = "if(window.onDistanceRead) window.onDistanceRead(" + meters + ");";
                }
                webView.evaluateJavascript(js, null);
            });
        }
    }
}
