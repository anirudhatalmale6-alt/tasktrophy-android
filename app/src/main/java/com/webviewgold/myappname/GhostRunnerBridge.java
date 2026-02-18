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
import java.util.List;

/**
 * Ghost Runner (GPS Race) Bridge
 * Tracks GPS location during a 5km race.
 *
 * JS bridge exposed as window.GhostRunner
 *
 * - Uses FusedLocationProviderClient for high-accuracy GPS
 * - Calculates distance using Haversine formula
 * - Persists race state in SharedPreferences for crash recovery
 * - Starts foreground service for background GPS tracking
 * - Auto-finishes when distance >= 5000m
 */
public class GhostRunnerBridge {

    private static final String TAG = "GhostRunnerBridge";
    private static final String PREFS_NAME = "ghostrunner_prefs";
    private static final int LOCATION_PERMISSION_REQUEST = 9001;
    private static final int TARGET_DISTANCE_M = 5000;

    // GPS update intervals
    private static final long INTERVAL_MS = 5000;       // 5 seconds
    private static final long FASTEST_INTERVAL_MS = 2000; // 2 seconds

    // Anti-cheat thresholds
    private static final float MAX_SPEED_MS = 6.94f;     // 25 km/h in m/s
    private static final float MIN_ACCURACY_M = 25f;      // Skip noisy points
    private static final float STANDSTILL_SPEED_MS = 0.5f; // Ignore GPS drift when still

    private final Context context;
    private final WebView webView;

    // Location client
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private boolean trackingActive = false;

    // Race state
    private boolean raceActive = false;
    private long raceStartTime = 0;
    private double totalDistanceMeters = 0;
    private double maxSpeedKmh = 0;
    private int gpsPointsCount = 0;
    private Location lastValidLocation = null;

    // GPS breadcrumbs
    private final List<GpsPoint> breadcrumbs = new ArrayList<>();

    // Current pace tracking
    private double lastKmDistance = 0;
    private long lastKmTime = 0;
    private int currentPaceSecsPerKm = 0;

    // SharedPreferences keys
    private static final String KEY_RACE_DATE = "race_date";
    private static final String KEY_RACE_ACTIVE = "race_active";
    private static final String KEY_RACE_START = "race_start_time";
    private static final String KEY_DISTANCE = "total_distance";
    private static final String KEY_MAX_SPEED = "max_speed";
    private static final String KEY_GPS_COUNT = "gps_count";
    private static final String KEY_GPS_DATA = "gps_data";

    static class GpsPoint {
        double lat, lng;
        float accuracy, speed, altitude;
        long timestamp;
        int seq;

        GpsPoint(double lat, double lng, float accuracy, float speed, float altitude, long timestamp, int seq) {
            this.lat = lat;
            this.lng = lng;
            this.accuracy = accuracy;
            this.speed = speed;
            this.altitude = altitude;
            this.timestamp = timestamp;
            this.seq = seq;
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
        String cachedDate = prefs.getString(KEY_RACE_DATE, "");
        String today = java.time.LocalDate.now().toString();

        if (today.equals(cachedDate)) {
            raceActive = prefs.getBoolean(KEY_RACE_ACTIVE, false);
            raceStartTime = prefs.getLong(KEY_RACE_START, 0);
            totalDistanceMeters = Double.longBitsToDouble(prefs.getLong(KEY_DISTANCE, 0));
            maxSpeedKmh = Double.longBitsToDouble(prefs.getLong(KEY_MAX_SPEED, 0));
            gpsPointsCount = prefs.getInt(KEY_GPS_COUNT, 0);

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
                        pt.getLong("timestamp"), pt.getInt("seq")
                    ));
                }
                // Restore lastValidLocation from last breadcrumb
                if (!breadcrumbs.isEmpty()) {
                    GpsPoint last = breadcrumbs.get(breadcrumbs.size() - 1);
                    lastValidLocation = new Location("cached");
                    lastValidLocation.setLatitude(last.lat);
                    lastValidLocation.setLongitude(last.lng);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore GPS data: " + e.getMessage());
            }

            // Resume GPS tracking if race was active
            if (raceActive) {
                Log.d(TAG, "Resuming active race: " + totalDistanceMeters + "m, " + gpsPointsCount + " points");
                startGpsTracking();
            }
        } else {
            // New day - reset
            resetRaceState();
            prefs.edit().putString(KEY_RACE_DATE, today).apply();
        }
    }

    private void saveData() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Save last 200 GPS points max (trim older ones to save space)
        JSONArray gpsArr = new JSONArray();
        int startIdx = Math.max(0, breadcrumbs.size() - 200);
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
                gpsArr.put(obj);
            } catch (Exception e) { /* skip */ }
        }

        prefs.edit()
            .putBoolean(KEY_RACE_ACTIVE, raceActive)
            .putLong(KEY_RACE_START, raceStartTime)
            .putLong(KEY_DISTANCE, Double.doubleToLongBits(totalDistanceMeters))
            .putLong(KEY_MAX_SPEED, Double.doubleToLongBits(maxSpeedKmh))
            .putInt(KEY_GPS_COUNT, gpsPointsCount)
            .putString(KEY_GPS_DATA, gpsArr.toString())
            .putString(KEY_RACE_DATE, java.time.LocalDate.now().toString())
            .apply();
    }

    private void resetRaceState() {
        raceActive = false;
        raceStartTime = 0;
        totalDistanceMeters = 0;
        maxSpeedKmh = 0;
        gpsPointsCount = 0;
        lastValidLocation = null;
        currentPaceSecsPerKm = 0;
        lastKmDistance = 0;
        lastKmTime = 0;
        breadcrumbs.clear();
    }

    // ─── Haversine Distance ───

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
                if (result == null || !raceActive) return;

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

        Log.d(TAG, "GPS tracking started");
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
        if (!raceActive) return;

        float accuracy = location.getAccuracy();
        float speed = location.getSpeed(); // m/s

        // Filter: Skip noisy points
        if (accuracy > MIN_ACCURACY_M * 2) return; // Hard reject > 50m

        // Filter: Skip if mock location (anti-cheat)
        if (location.isFromMockProvider()) {
            Log.w(TAG, "Mock location detected! Ignoring.");
            notifyJs("gpsError", "{\"error\":\"Mock location detected\"}");
            return;
        }

        // Calculate distance from last point
        double addedDistance = 0;
        if (lastValidLocation != null) {
            addedDistance = haversineMeters(
                lastValidLocation.getLatitude(), lastValidLocation.getLongitude(),
                location.getLatitude(), location.getLongitude()
            );

            // Filter: Ignore GPS drift when standing still
            if (speed < STANDSTILL_SPEED_MS && accuracy > MIN_ACCURACY_M) {
                addedDistance = 0;
            }

            // Filter: Skip if teleport detected (> max speed * time_delta)
            if (lastValidLocation.getTime() > 0) {
                double timeDelta = (location.getTime() - lastValidLocation.getTime()) / 1000.0;
                if (timeDelta > 0) {
                    double maxPossibleDistance = MAX_SPEED_MS * timeDelta * 1.5; // 50% tolerance
                    if (addedDistance > maxPossibleDistance && addedDistance > 100) {
                        Log.w(TAG, "Teleport detected: " + addedDistance + "m in " + timeDelta + "s");
                        addedDistance = 0;
                    }
                }
            }
        }

        // Add distance (only from good-accuracy points)
        if (accuracy <= MIN_ACCURACY_M && addedDistance > 0) {
            totalDistanceMeters += addedDistance;
        }

        // Track max speed
        double speedKmh = speed * 3.6;
        if (speedKmh > maxSpeedKmh) {
            maxSpeedKmh = speedKmh;
        }

        // Update pace tracking
        if (raceStartTime > 0) {
            double currentKm = totalDistanceMeters / 1000.0;
            if (currentKm >= lastKmDistance + 0.1) { // Update every 100m
                long now = System.currentTimeMillis();
                if (lastKmTime > 0) {
                    double distDelta = totalDistanceMeters - (lastKmDistance * 1000);
                    double timeDelta = (now - lastKmTime) / 1000.0;
                    if (distDelta > 0) {
                        currentPaceSecsPerKm = (int) ((timeDelta / distDelta) * 1000);
                    }
                }
                lastKmDistance = currentKm;
                lastKmTime = now;
            }
        }

        // Store breadcrumb
        gpsPointsCount++;
        GpsPoint point = new GpsPoint(
            location.getLatitude(), location.getLongitude(),
            accuracy, speed,
            location.hasAltitude() ? (float) location.getAltitude() : 0,
            location.getTime(), gpsPointsCount
        );
        breadcrumbs.add(point);

        lastValidLocation = location;

        // Save periodically (every 10 points)
        if (gpsPointsCount % 10 == 0) {
            saveData();
        }

        // Notify JS with location update
        notifyJs("locationUpdate", buildRaceInfoJson());

        // Auto-finish check
        if (totalDistanceMeters >= TARGET_DISTANCE_M) {
            Log.d(TAG, "Target distance reached! Auto-finishing race.");
            raceActive = false;
            saveData();
            stopGpsTracking();
            notifyJs("raceFinished", buildRaceInfoJson());
        }
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

    @JavascriptInterface
    public void startRace() {
        if (raceActive) return;

        // Reset for new race
        resetRaceState();

        raceActive = true;
        raceStartTime = System.currentTimeMillis();
        lastKmTime = raceStartTime;

        saveData();
        startGpsTracking();

        Log.d(TAG, "Race started");
        notifyJs("raceStarted", buildRaceInfoJson());
    }

    @JavascriptInterface
    public void stopRace() {
        if (!raceActive) return;

        raceActive = false;
        saveData();
        stopGpsTracking();

        Log.d(TAG, "Race stopped (DNF). Distance: " + totalDistanceMeters + "m");
        notifyJs("raceStopped", buildRaceInfoJson());
    }

    @JavascriptInterface
    public boolean isRaceActive() {
        return raceActive;
    }

    @JavascriptInterface
    public int getDistanceMeters() {
        return (int) totalDistanceMeters;
    }

    @JavascriptInterface
    public int getElapsedSeconds() {
        if (raceStartTime <= 0) return 0;
        return (int) ((System.currentTimeMillis() - raceStartTime) / 1000);
    }

    @JavascriptInterface
    public String getRaceInfo() {
        return buildRaceInfoJson();
    }

    @JavascriptInterface
    public String getGpsPoints(int fromIndex) {
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < breadcrumbs.size(); i++) {
                GpsPoint pt = breadcrumbs.get(i);
                if (pt.seq <= fromIndex) continue;

                JSONObject obj = new JSONObject();
                obj.put("lat", pt.lat);
                obj.put("lng", pt.lng);
                obj.put("accuracy", pt.accuracy);
                obj.put("speed", pt.speed);
                obj.put("altitude", pt.altitude);
                obj.put("timestamp", pt.timestamp);
                obj.put("seq", pt.seq);
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting GPS points: " + e.getMessage());
            return "[]";
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

    // ─── Helper: Build race info JSON ───

    private String buildRaceInfoJson() {
        try {
            JSONObject info = new JSONObject();
            info.put("raceActive", raceActive);
            info.put("distanceMeters", (int) totalDistanceMeters);
            info.put("elapsedSeconds", getElapsedSeconds());
            info.put("currentPaceSecsPerKm", currentPaceSecsPerKm);
            info.put("maxSpeedKmh", Math.round(maxSpeedKmh * 10) / 10.0);
            info.put("gpsPointsCount", gpsPointsCount);

            // Average pace
            int elapsed = getElapsedSeconds();
            int avgPace = 0;
            if (totalDistanceMeters > 0 && elapsed > 0) {
                avgPace = (int) ((elapsed / totalDistanceMeters) * 1000);
            }
            info.put("avgPaceSecsPerKm", avgPace);

            // Finish time in seconds (for finish_race API)
            info.put("finishTimeSeconds", elapsed);

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
        if (raceActive) {
            saveData();
        }
        stopGpsTracking();
    }

    /**
     * Call from Activity's onRequestPermissionsResult
     */
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
