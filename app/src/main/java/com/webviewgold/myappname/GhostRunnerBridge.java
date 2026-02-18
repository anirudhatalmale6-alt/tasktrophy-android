package com.webviewgold.myappname;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Daily 5km Distance Challenge Bridge
 *
 * Tracks GPS movement throughout the day (5 AM - 11:59 PM).
 * Multiple walk/jog/run sessions accumulate toward 5km daily goal.
 *
 * Speed window: Only 4-15 km/h counts toward prize (brisk walk to running).
 * Below 4 km/h = strolling (ignored for prize). Above 15 km/h = too fast.
 * Above 25 km/h = flagged as vehicle (anti-cheat).
 *
 * JS bridge exposed as window.GhostRunner
 */
public class GhostRunnerBridge {

    private static final String TAG = "GhostRunnerBridge";
    private static final String PREFS_NAME = "ghostrunner_prefs";
    private static final int LOCATION_PERMISSION_REQUEST = 9001;
    private static final int TARGET_DISTANCE_M = 5000;

    // GPS update intervals
    private static final long INTERVAL_MS = 5000;       // 5 seconds
    private static final long FASTEST_INTERVAL_MS = 3000; // 3 seconds

    // Speed thresholds (m/s)
    private static final float MIN_QUALIFY_SPEED_MS = 1.11f;  // 4 km/h - brisk walk
    private static final float MAX_QUALIFY_SPEED_MS = 4.17f;  // 15 km/h - running
    private static final float MAX_SPEED_MS = 6.94f;          // 25 km/h - anti-cheat cap
    private static final float STANDSTILL_SPEED_MS = 0.8f;    // Below this = stationary

    // GPS accuracy thresholds
    private static final float MIN_ACCURACY_M = 20f;     // Good accuracy
    private static final float MAX_ACCURACY_M = 50f;     // Hard reject
    private static final float MIN_DISTANCE_BETWEEN_POINTS = 2.0f; // Minimum 2m between points

    // Time between location updates to consider valid (reject rapid-fire duplicates)
    private static final long MIN_TIME_BETWEEN_MS = 2000; // 2 seconds

    private final Context context;
    private final WebView webView;

    // Location client
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private boolean trackingActive = false;

    // Daily state (persists across sessions)
    private boolean sessionActive = false;      // Current tracking session active
    private long sessionStartTime = 0;          // Current session start
    private double totalDistanceMeters = 0;     // Total distance (all movement)
    private double qualifiedDistanceMeters = 0; // Only 4-15 km/h counts for prize
    private double maxSpeedKmh = 0;
    private int gpsPointsCount = 0;
    private int sessionsToday = 0;              // How many sessions started today
    private Location lastValidLocation = null;
    private long lastLocationTimeMs = 0;        // Timestamp of last processed location

    // GPS breadcrumbs (for sync to server)
    private final List<GpsPoint> breadcrumbs = new ArrayList<>();
    private int lastSyncedSeq = 0; // Track which points have been synced

    // Current speed (smoothed)
    private float currentSpeedMs = 0;
    private final float[] speedBuffer = new float[5];
    private int speedBufferIdx = 0;
    private int speedBufferCount = 0;

    // SharedPreferences keys
    private static final String KEY_DATE = "daily_date";
    private static final String KEY_SESSION_ACTIVE = "session_active";
    private static final String KEY_SESSION_START = "session_start_time";
    private static final String KEY_TOTAL_DISTANCE = "total_distance";
    private static final String KEY_QUALIFIED_DISTANCE = "qualified_distance";
    private static final String KEY_MAX_SPEED = "max_speed";
    private static final String KEY_GPS_COUNT = "gps_count";
    private static final String KEY_SESSIONS_TODAY = "sessions_today";
    private static final String KEY_GPS_DATA = "gps_data";
    private static final String KEY_LAST_SYNCED_SEQ = "last_synced_seq";

    static class GpsPoint {
        double lat, lng;
        float accuracy, speed, altitude;
        long timestamp;
        int seq;
        boolean qualified; // Was this point in the 4-15 km/h window?

        GpsPoint(double lat, double lng, float accuracy, float speed, float altitude,
                 long timestamp, int seq, boolean qualified) {
            this.lat = lat;
            this.lng = lng;
            this.accuracy = accuracy;
            this.speed = speed;
            this.altitude = altitude;
            this.timestamp = timestamp;
            this.seq = seq;
            this.qualified = qualified;
        }
    }

    public GhostRunnerBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        fusedClient = LocationServices.getFusedLocationProviderClient(context);
        loadCachedData();
    }

    // ─── SharedPreferences Persistence ───

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cachedDate = prefs.getString(KEY_DATE, "");
        String today = java.time.LocalDate.now().toString();

        if (today.equals(cachedDate)) {
            sessionActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false);
            sessionStartTime = prefs.getLong(KEY_SESSION_START, 0);
            totalDistanceMeters = Double.longBitsToDouble(prefs.getLong(KEY_TOTAL_DISTANCE, 0));
            qualifiedDistanceMeters = Double.longBitsToDouble(prefs.getLong(KEY_QUALIFIED_DISTANCE, 0));
            maxSpeedKmh = Double.longBitsToDouble(prefs.getLong(KEY_MAX_SPEED, 0));
            gpsPointsCount = prefs.getInt(KEY_GPS_COUNT, 0);
            sessionsToday = prefs.getInt(KEY_SESSIONS_TODAY, 0);
            lastSyncedSeq = prefs.getInt(KEY_LAST_SYNCED_SEQ, 0);

            // Restore breadcrumbs
            String gpsJson = prefs.getString(KEY_GPS_DATA, "[]");
            try {
                JSONArray arr = new JSONArray(gpsJson);
                breadcrumbs.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject pt = arr.getJSONObject(i);
                    breadcrumbs.add(new GpsPoint(
                        pt.getDouble("lat"), pt.getDouble("lng"),
                        (float) pt.getDouble("accuracy"), (float) pt.getDouble("speed"),
                        (float) pt.optDouble("altitude", 0),
                        pt.getLong("timestamp"), pt.getInt("seq"),
                        pt.optBoolean("qualified", false)
                    ));
                }
                // Restore lastValidLocation from last breadcrumb
                if (!breadcrumbs.isEmpty()) {
                    GpsPoint last = breadcrumbs.get(breadcrumbs.size() - 1);
                    lastValidLocation = new Location("cached");
                    lastValidLocation.setLatitude(last.lat);
                    lastValidLocation.setLongitude(last.lng);
                    lastValidLocation.setTime(last.timestamp);
                    lastLocationTimeMs = last.timestamp;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore GPS data: " + e.getMessage());
            }

            // Resume GPS tracking if session was active
            if (sessionActive) {
                Log.d(TAG, "Resuming tracking session: " + qualifiedDistanceMeters + "m qualified, " +
                    totalDistanceMeters + "m total");
                startGpsTracking();
            }
        } else {
            // New day - reset all daily counters
            resetDailyState();
            prefs.edit().putString(KEY_DATE, today).apply();
        }
    }

    private void saveData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Save last 300 GPS points max
        JSONArray gpsArr = new JSONArray();
        int startIdx = Math.max(0, breadcrumbs.size() - 300);
        for (int i = startIdx; i < breadcrumbs.size(); i++) {
            try {
                GpsPoint pt = breadcrumbs.get(i);
                JSONObject obj = new JSONObject();
                obj.put("lat", pt.lat);
                obj.put("lng", pt.lng);
                obj.put("accuracy", pt.accuracy);
                obj.put("speed", pt.speed);
                obj.put("altitude", pt.altitude);
                obj.put("timestamp", pt.timestamp);
                obj.put("seq", pt.seq);
                obj.put("qualified", pt.qualified);
                gpsArr.put(obj);
            } catch (Exception e) { /* skip */ }
        }

        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, sessionActive)
            .putLong(KEY_SESSION_START, sessionStartTime)
            .putLong(KEY_TOTAL_DISTANCE, Double.doubleToLongBits(totalDistanceMeters))
            .putLong(KEY_QUALIFIED_DISTANCE, Double.doubleToLongBits(qualifiedDistanceMeters))
            .putLong(KEY_MAX_SPEED, Double.doubleToLongBits(maxSpeedKmh))
            .putInt(KEY_GPS_COUNT, gpsPointsCount)
            .putInt(KEY_SESSIONS_TODAY, sessionsToday)
            .putInt(KEY_LAST_SYNCED_SEQ, lastSyncedSeq)
            .putString(KEY_GPS_DATA, gpsArr.toString())
            .putString(KEY_DATE, java.time.LocalDate.now().toString())
            .apply();
    }

    private void resetDailyState() {
        sessionActive = false;
        sessionStartTime = 0;
        totalDistanceMeters = 0;
        qualifiedDistanceMeters = 0;
        maxSpeedKmh = 0;
        gpsPointsCount = 0;
        sessionsToday = 0;
        lastValidLocation = null;
        lastLocationTimeMs = 0;
        currentSpeedMs = 0;
        speedBufferIdx = 0;
        speedBufferCount = 0;
        lastSyncedSeq = 0;
        breadcrumbs.clear();
    }

    // ─── Time Window Check ───

    /**
     * Check if current time is in the active window (5 AM - 11:59 PM)
     */
    private boolean isInActiveWindow() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 5; // 5 AM to 11:59 PM (midnight resets daily)
    }

    // ─── Haversine Distance ───

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ─── Speed Smoothing ───

    private float getSmoothedSpeed(float rawSpeed) {
        speedBuffer[speedBufferIdx] = rawSpeed;
        speedBufferIdx = (speedBufferIdx + 1) % speedBuffer.length;
        if (speedBufferCount < speedBuffer.length) speedBufferCount++;

        float sum = 0;
        for (int i = 0; i < speedBufferCount; i++) {
            sum += speedBuffer[i];
        }
        return sum / speedBufferCount;
    }

    // ─── GPS Tracking ───

    private void startGpsTracking() {
        if (trackingActive) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            notifyJs("gpsError", "{\"error\":\"Location permission not granted\"}");
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || !sessionActive) return;

                for (Location location : result.getLocations()) {
                    processLocation(location);
                }
            }
        };

        @SuppressWarnings("deprecation")
        LocationRequest request = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(INTERVAL_MS)
            .setFastestInterval(FASTEST_INTERVAL_MS);

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        trackingActive = true;

        // Start foreground service
        try {
            Intent serviceIntent = new Intent(context, GhostRunnerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service: " + e.getMessage());
        }

        Log.d(TAG, "GPS tracking started (session " + sessionsToday + ")");
    }

    private void stopGpsTracking() {
        if (!trackingActive) return;

        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        trackingActive = false;

        // Stop foreground service
        try {
            Intent serviceIntent = new Intent(context, GhostRunnerService.class);
            context.stopService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop foreground service: " + e.getMessage());
        }

        Log.d(TAG, "GPS tracking stopped");
    }

    private void processLocation(Location location) {
        if (!sessionActive) return;

        float accuracy = location.getAccuracy();
        float rawSpeed = location.getSpeed(); // m/s
        long timestamp = location.getTime();

        // ─── Filter 1: Hard reject poor accuracy ───
        if (accuracy > MAX_ACCURACY_M) return;

        // ─── Filter 2: Reject mock locations ───
        if (location.isFromMockProvider()) {
            Log.w(TAG, "Mock location detected! Ignoring.");
            notifyJs("gpsError", "{\"error\":\"Mock location detected\"}");
            return;
        }

        // ─── Filter 3: Reject rapid-fire duplicate points ───
        if (lastLocationTimeMs > 0 && (timestamp - lastLocationTimeMs) < MIN_TIME_BETWEEN_MS) {
            return;
        }

        // ─── Smooth speed ───
        float smoothedSpeed = getSmoothedSpeed(rawSpeed);
        currentSpeedMs = smoothedSpeed;

        // ─── Calculate distance from last point ───
        double addedDistance = 0;
        boolean pointQualifies = false;

        if (lastValidLocation != null) {
            addedDistance = haversineMeters(
                lastValidLocation.getLatitude(), lastValidLocation.getLongitude(),
                location.getLatitude(), location.getLongitude()
            );

            // ─── Filter 4: Ignore if distance is too small (GPS noise) ───
            if (addedDistance < MIN_DISTANCE_BETWEEN_POINTS) {
                addedDistance = 0;
            }

            // ─── Filter 5: Ignore if stationary (GPS drift fix) ───
            // Use both smoothed speed AND raw speed check
            if (smoothedSpeed < STANDSTILL_SPEED_MS && rawSpeed < STANDSTILL_SPEED_MS) {
                addedDistance = 0;
            }

            // ─── Filter 6: Skip if accuracy is mediocre and distance is small ───
            if (accuracy > MIN_ACCURACY_M && addedDistance < 5.0) {
                addedDistance = 0;
            }

            // ─── Filter 7: Teleport detection ───
            if (lastLocationTimeMs > 0) {
                double timeDelta = (timestamp - lastLocationTimeMs) / 1000.0;
                if (timeDelta > 0) {
                    double maxPossibleDistance = MAX_SPEED_MS * timeDelta * 1.5;
                    if (addedDistance > maxPossibleDistance && addedDistance > 50) {
                        Log.w(TAG, "Teleport detected: " + addedDistance + "m in " + timeDelta + "s");
                        addedDistance = 0;
                    }
                }
            }

            // ─── Qualify check: Is speed in the 4-15 km/h window? ───
            double speedKmh = smoothedSpeed * 3.6;
            if (speedKmh >= 4.0 && speedKmh <= 15.0 && addedDistance > 0 && accuracy <= MIN_ACCURACY_M) {
                pointQualifies = true;
            }
        }

        // ─── Add distance ───
        if (addedDistance > 0 && accuracy <= MIN_ACCURACY_M) {
            totalDistanceMeters += addedDistance;
            if (pointQualifies) {
                qualifiedDistanceMeters += addedDistance;
            }
        }

        // Track max speed
        double speedKmh = smoothedSpeed * 3.6;
        if (speedKmh > maxSpeedKmh && speedKmh < 50) { // Cap at 50 to avoid spikes
            maxSpeedKmh = speedKmh;
        }

        // Store breadcrumb
        gpsPointsCount++;
        GpsPoint point = new GpsPoint(
            location.getLatitude(), location.getLongitude(),
            accuracy, rawSpeed,
            location.hasAltitude() ? (float) location.getAltitude() : 0,
            timestamp, gpsPointsCount, pointQualifies
        );
        breadcrumbs.add(point);

        lastValidLocation = location;
        lastLocationTimeMs = timestamp;

        // Save periodically (every 10 points)
        if (gpsPointsCount % 10 == 0) {
            saveData();
        }

        // Notify JS with location update
        notifyJs("locationUpdate", buildInfoJson());
    }

    // ─── JS Interface Methods ───

    @JavascriptInterface
    public boolean isAvailable() {
        return true;
    }

    @JavascriptInterface
    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void requestPermission() {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions(
                (Activity) context,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST
            );
        }
    }

    /**
     * Start a tracking session. Can be called multiple times per day.
     * Each call starts GPS tracking; distance accumulates across sessions.
     */
    @JavascriptInterface
    public void startTracking() {
        if (sessionActive) return;

        if (!isInActiveWindow()) {
            notifyJs("gpsError", "{\"error\":\"Challenge active only between 5 AM and midnight\"}");
            return;
        }

        sessionActive = true;
        sessionStartTime = System.currentTimeMillis();
        sessionsToday++;

        // Reset speed buffer for new session but keep distances
        speedBufferIdx = 0;
        speedBufferCount = 0;
        currentSpeedMs = 0;
        // Clear lastValidLocation to avoid jump from last session's end point
        lastValidLocation = null;
        lastLocationTimeMs = 0;

        saveData();
        startGpsTracking();

        Log.d(TAG, "Tracking session " + sessionsToday + " started. Current qualified: " +
            qualifiedDistanceMeters + "m");
        notifyJs("trackingStarted", buildInfoJson());
    }

    /**
     * Stop the current tracking session. Distance is preserved.
     */
    @JavascriptInterface
    public void stopTracking() {
        if (!sessionActive) return;

        sessionActive = false;
        saveData();
        stopGpsTracking();

        Log.d(TAG, "Tracking session stopped. Qualified: " + qualifiedDistanceMeters +
            "m, Total: " + totalDistanceMeters + "m");
        notifyJs("trackingStopped", buildInfoJson());
    }

    @JavascriptInterface
    public boolean isSessionActive() {
        return sessionActive;
    }

    @JavascriptInterface
    public int getTotalDistanceMeters() {
        return (int) totalDistanceMeters;
    }

    @JavascriptInterface
    public int getQualifiedDistanceMeters() {
        return (int) qualifiedDistanceMeters;
    }

    @JavascriptInterface
    public int getSessionsToday() {
        return sessionsToday;
    }

    @JavascriptInterface
    public float getCurrentSpeedKmh() {
        return Math.round(currentSpeedMs * 3.6f * 10) / 10f;
    }

    @JavascriptInterface
    public String getInfo() {
        return buildInfoJson();
    }

    /**
     * Get unsent GPS points (for server sync)
     */
    @JavascriptInterface
    public String getUnsyncedPoints() {
        try {
            JSONArray arr = new JSONArray();
            for (GpsPoint pt : breadcrumbs) {
                if (pt.seq <= lastSyncedSeq) continue;

                JSONObject obj = new JSONObject();
                obj.put("lat", pt.lat);
                obj.put("lng", pt.lng);
                obj.put("accuracy", pt.accuracy);
                obj.put("speed", pt.speed);
                obj.put("altitude", pt.altitude);
                obj.put("timestamp", pt.timestamp);
                obj.put("seq", pt.seq);
                obj.put("qualified", pt.qualified);
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @JavascriptInterface
    public void markSynced(int upToSeq) {
        lastSyncedSeq = upToSeq;
        saveData();
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

    // ─── Helper: Build info JSON ───

    private String buildInfoJson() {
        try {
            JSONObject info = new JSONObject();
            info.put("sessionActive", sessionActive);
            info.put("totalDistanceMeters", (int) totalDistanceMeters);
            info.put("qualifiedDistanceMeters", (int) qualifiedDistanceMeters);
            info.put("targetDistanceMeters", TARGET_DISTANCE_M);
            info.put("maxSpeedKmh", Math.round(maxSpeedKmh * 10) / 10.0);
            info.put("currentSpeedKmh", Math.round(currentSpeedMs * 3.6 * 10) / 10.0);
            info.put("gpsPointsCount", gpsPointsCount);
            info.put("sessionsToday", sessionsToday);
            info.put("inActiveWindow", isInActiveWindow());

            // Speed zone indicator for UI
            double speedKmh = currentSpeedMs * 3.6;
            String speedZone;
            if (speedKmh < 1.0) speedZone = "stationary";
            else if (speedKmh < 4.0) speedZone = "strolling";   // Too slow
            else if (speedKmh <= 15.0) speedZone = "active";     // Counts!
            else if (speedKmh <= 25.0) speedZone = "too_fast";   // Driving suspect
            else speedZone = "flagged";                           // Vehicle
            info.put("speedZone", speedZone);

            if (lastValidLocation != null) {
                info.put("lastLat", lastValidLocation.getLatitude());
                info.put("lastLng", lastValidLocation.getLongitude());
                info.put("lastAccuracy", lastValidLocation.getAccuracy());
            }

            return info.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ─── JS Notification ───

    private void notifyJs(String event, String dataJson) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                String js = "if(window.onGhostRunnerEvent) window.onGhostRunnerEvent('" + event + "', " + dataJson + ");";
                webView.evaluateJavascript(js, null);
            });
        }
    }

    // ─── Lifecycle ───

    public void onDestroy() {
        if (sessionActive) {
            saveData();
        }
        stopGpsTracking();
    }

    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                notifyJs("permissionGranted", "{}");
            } else {
                notifyJs("permissionDenied", "{}");
            }
        }
    }
}
