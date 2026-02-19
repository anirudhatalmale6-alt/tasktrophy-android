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
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.metadata.DataOrigin;
import androidx.health.connect.client.request.AggregateRequest;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.aggregate.AggregationResult;
import androidx.health.connect.client.response.ReadRecordsResponse;
import androidx.health.connect.client.time.TimeRangeFilter;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlinx.coroutines.BuildersKt;

/**
 * Step King Bridge - Health Connect for steps + heart rate.
 * Replaces Google Fit with Health Connect API.
 * Uses Health Connect's DataOrigin filtering to exclude manually entered data.
 */
public class StepKingBridge {

    private static final String TAG = "StepKingBridge";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";
    private static final String KEY_HEART_POINTS = "heart_points_today";
    private static final String KEY_MANUAL_STEPS = "manual_steps_today";
    private static final String KEY_MANUAL_HEART = "manual_heart_today";

    private static final String HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata";

    // Known manual entry apps
    private static final Set<String> MANUAL_ENTRY_PACKAGES = new HashSet<>(Arrays.asList(
        "com.google.android.apps.healthdata",  // Health Connect app itself
        "com.google.android.apps.fitness"       // Google Fit app
    ));

    private final Context context;
    private final WebView webView;
    private long todaySteps = 0;
    private float todayHeartPoints = 0;
    private long manualSteps = 0;
    private float manualHeartPoints = 0;

    private HealthConnectClient healthConnectClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Permission launcher - set from MainActivity
    private ActivityResultLauncher<Set<String>> permissionLauncher;

    @SuppressWarnings("unchecked")
    private static final KClass<StepsRecord> STEPS_KCLASS =
        JvmClassMappingKt.getKotlinClass(StepsRecord.class);
    @SuppressWarnings("unchecked")
    private static final KClass<HeartRateRecord> HR_KCLASS =
        JvmClassMappingKt.getKotlinClass(HeartRateRecord.class);

    public static final Set<String> REQUIRED_PERMISSIONS = new HashSet<>(Arrays.asList(
        HealthPermission.getReadPermission(STEPS_KCLASS),
        HealthPermission.getReadPermission(HR_KCLASS)
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
            } else {
                Log.w(TAG, "Health Connect not available, status: " + status);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to init Health Connect: " + e.getMessage());
        }
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
            .putString(KEY_STEPS_DATE, LocalDate.now().toString())
            .putFloat(KEY_HEART_POINTS, heartPoints)
            .putLong(KEY_MANUAL_STEPS, manualSteps)
            .putFloat(KEY_MANUAL_HEART, manualHeartPoints)
            .apply();
    }

    /**
     * Gets start of today as Instant.
     */
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
        try {
            Set<String> granted = (Set<String>) BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> healthConnectClient.getPermissionController()
                    .getGrantedPermissions(continuation)
            );
            return granted.containsAll(REQUIRED_PERMISSIONS);
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
            return false;
        }
    }

    @JavascriptInterface
    public void requestPermission() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    if (healthConnectClient == null) {
                        // Health Connect not available - try to install it
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

    @JavascriptInterface
    public void refreshSteps() {
        if (healthConnectClient == null) {
            callJsCallback(-1, "Health Connect not available");
            return;
        }

        executor.execute(() -> {
            try {
                Instant startTime = getStartOfDayInstant();
                Instant endTime = Instant.now();

                // Aggregate total steps for today
                AggregateRequest aggregateRequest = new AggregateRequest(
                    new HashSet<>(Arrays.asList(StepsRecord.COUNT_TOTAL)),
                    TimeRangeFilter.between(startTime, endTime),
                    new HashSet<>() // no data origin filter - get all
                );

                AggregationResult result = (AggregationResult) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> healthConnectClient.aggregate(aggregateRequest, continuation)
                );

                Long totalSteps = result.get(StepsRecord.COUNT_TOTAL);
                long stepsCount = (totalSteps != null) ? totalSteps : 0;
                Log.d(TAG, "Health Connect total steps: " + stepsCount);

                // Now read individual records to detect manual entries
                detectManualSteps(startTime, endTime, stepsCount);

            } catch (Exception e) {
                Log.e(TAG, "Failed to read steps: " + e.getMessage(), e);
                callJsCallback(-1, "Failed to read steps: " + e.getMessage());
            }
        });
    }

    /**
     * Reads individual step records to detect and filter manual entries.
     * Manual entries come from Health Connect app or Google Fit app.
     */
    @SuppressWarnings("unchecked")
    private void detectManualSteps(Instant startTime, Instant endTime, long totalFromAggregate) {
        try {
            ReadRecordsRequest<StepsRecord> readRequest = new ReadRecordsRequest<>(
                STEPS_KCLASS,
                TimeRangeFilter.between(startTime, endTime),
                new HashSet<>(), // no data origin filter
                true,  // ascending
                1000,  // page size (default)
                null   // no page token
            );

            ReadRecordsResponse<StepsRecord> response = (ReadRecordsResponse<StepsRecord>) BuildersKt.runBlocking(
                EmptyCoroutineContext.INSTANCE,
                (scope, continuation) -> healthConnectClient.readRecords(readRequest, continuation)
            );

            long validSteps = 0;
            long manualStepsLocal = 0;

            for (StepsRecord record : response.getRecords()) {
                DataOrigin origin = record.getMetadata().getDataOrigin();
                String packageName = origin.getPackageName();
                long count = record.getCount();

                if (isManualSource(packageName, record)) {
                    manualStepsLocal += count;
                    Log.d(TAG, "FILTERED manual steps: " + count + " from " + packageName);
                } else {
                    validSteps += count;
                }
            }

            manualSteps = manualStepsLocal;

            if (manualStepsLocal > 0) {
                // Use filtered count
                saveData(validSteps, todayHeartPoints);
                callJsCallback(validSteps, null);
                callJsManualEntryDetected();
                Log.d(TAG, "Manual steps detected: " + manualStepsLocal + ", valid: " + validSteps);
            } else {
                // No manual entries - use aggregate total (more accurate for dedup)
                saveData(totalFromAggregate, todayHeartPoints);
                callJsCallback(totalFromAggregate, null);
            }

        } catch (Exception e) {
            // If individual read fails, use aggregate total
            Log.w(TAG, "Manual detection failed, using aggregate: " + e.getMessage());
            saveData(totalFromAggregate, todayHeartPoints);
            callJsCallback(totalFromAggregate, null);
        }
    }

    /**
     * Determines if a step record is from a manual entry source.
     */
    private boolean isManualSource(String packageName, StepsRecord record) {
        if (packageName == null) return false;

        // Known manual entry apps
        if (MANUAL_ENTRY_PACKAGES.contains(packageName)) {
            return true;
        }

        // Check recording method if available (API 34+)
        try {
            int recordingMethod = record.getMetadata().getRecordingMethod();
            // RECORDING_METHOD_MANUAL_ENTRY = 2
            if (recordingMethod == 2) {
                return true;
            }
        } catch (Exception e) {
            // recordingMethod not available on older API
        }

        return false;
    }

    @JavascriptInterface
    public void getHeartPoints() {
        if (healthConnectClient == null) {
            callJsHeartPoints(-1, "Health Connect not available");
            return;
        }

        executor.execute(() -> {
            try {
                Instant startTime = getStartOfDayInstant();
                Instant endTime = Instant.now();

                // Read heart rate records for today
                ReadRecordsRequest<HeartRateRecord> readRequest = new ReadRecordsRequest<>(
                    HR_KCLASS,
                    TimeRangeFilter.between(startTime, endTime),
                    new HashSet<>(),
                    true,
                    -1,
                    null
                );

                @SuppressWarnings("unchecked")
                ReadRecordsResponse<HeartRateRecord> response = (ReadRecordsResponse<HeartRateRecord>) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> healthConnectClient.readRecords(readRequest, continuation)
                );

                // Calculate "heart points" from heart rate data
                // Google Fit awards: 1 point per minute of moderate activity (HR > 50% max),
                // 2 points per minute of vigorous activity (HR > 70% max)
                // We approximate: count minutes with HR data as heart points
                float totalPoints = 0;
                float manualPointsLocal = 0;

                for (HeartRateRecord record : response.getRecords()) {
                    DataOrigin origin = record.getMetadata().getDataOrigin();
                    String packageName = origin.getPackageName();

                    // Calculate duration in minutes
                    long durationMs = record.getEndTime().toEpochMilli() - record.getStartTime().toEpochMilli();
                    float durationMins = durationMs / 60000f;

                    // Calculate average BPM for this record
                    List<HeartRateRecord.Sample> samples = record.getSamples();
                    if (samples.isEmpty()) continue;

                    long totalBpm = 0;
                    for (HeartRateRecord.Sample sample : samples) {
                        totalBpm += sample.getBeatsPerMinute();
                    }
                    float avgBpm = (float) totalBpm / samples.size();

                    // Award points based on intensity
                    // Moderate activity (BPM > 100): 1 point per minute
                    // Vigorous activity (BPM > 130): 2 points per minute
                    float points = 0;
                    if (avgBpm > 130) {
                        points = durationMins * 2;
                    } else if (avgBpm > 100) {
                        points = durationMins;
                    }

                    boolean isManual = MANUAL_ENTRY_PACKAGES.contains(packageName);
                    try {
                        int recordingMethod = record.getMetadata().getRecordingMethod();
                        if (recordingMethod == 2) isManual = true;
                    } catch (Exception ignored) {}

                    if (isManual) {
                        manualPointsLocal += points;
                        Log.d(TAG, "FILTERED manual heart points: " + points + " from " + packageName);
                    } else {
                        totalPoints += points;
                    }
                }

                manualHeartPoints = manualPointsLocal;
                todayHeartPoints = totalPoints;
                saveData(todaySteps, totalPoints);
                callJsHeartPoints(totalPoints, null);

                if (manualPointsLocal > 0) {
                    callJsManualEntryDetected();
                    Log.d(TAG, "Manual heart points detected: " + manualPointsLocal
                        + ", valid: " + totalPoints);
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to read heart rate: " + e.getMessage(), e);
                callJsHeartPoints(-1, "Failed to read heart rate: " + e.getMessage());
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
            info.put("healthConnectAvailable", isAvailable());
            info.put("hasPermission", hasPermission());
            info.put("todaySteps", todaySteps);
            info.put("todayHeartPoints", todayHeartPoints);
            info.put("manualSteps", manualSteps);
            info.put("manualHeartPoints", manualHeartPoints);
            info.put("source", "health_connect");
            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Returns a JSON string with manual entry warning info.
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

    /** Called from MainActivity when Health Connect permission is granted */
    public void onPermissionGranted() {
        refreshSteps();
        getHeartPoints();
    }

    /**
     * Kept for backward compatibility - no longer needed with Health Connect
     * (ACTIVITY_RECOGNITION is not required)
     */
    @JavascriptInterface
    public void requestActivityRecognition() {
        // No-op: Health Connect doesn't need ACTIVITY_RECOGNITION
        Log.d(TAG, "requestActivityRecognition called - not needed for Health Connect");
    }

    public void onActivityRecognitionGranted() {
        // No-op: not needed for Health Connect
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
