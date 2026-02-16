package com.tasktrophy.official;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager worker that periodically syncs cached step count to the server.
 * Steps are read by StepKingBridge (sensor listener) and cached in SharedPreferences.
 * This worker just pushes the cached value to the server.
 *
 * Runs every 15 minutes (WorkManager minimum interval).
 */
public class StepSyncWorker extends Worker {

    private static final String TAG = "StepSyncWorker";
    private static final String WORK_NAME = "stepking_sync";
    private static final String PREFS_NAME = "stepking_prefs";
    private static final String KEY_STEPS_TODAY = "steps_today";
    private static final String KEY_STEPS_DATE = "steps_date";

    private static final String SYNC_URL = "https://tasktrophy.in/games/step-king/api.php?action=sync_steps";

    public StepSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            String today = java.time.LocalDate.now().toString();
            String cachedDate = prefs.getString(KEY_STEPS_DATE, "");

            // Only sync if we have today's data
            if (!today.equals(cachedDate)) {
                Log.d(TAG, "No step data for today, skipping sync");
                return Result.success();
            }

            long steps = prefs.getLong(KEY_STEPS_TODAY, 0);
            if (steps <= 0) {
                Log.d(TAG, "Zero steps, skipping sync");
                return Result.success();
            }

            String deviceId = Settings.Secure.getString(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
            );

            syncToServer(steps, deviceId);
            Log.d(TAG, "Synced " + steps + " steps to server");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Step sync failed: " + e.getMessage(), e);
            return Result.retry();
        }
    }

    private void syncToServer(long steps, String deviceId) {
        try {
            // Get WordPress cookies from WebView CookieManager
            String cookies = android.webkit.CookieManager.getInstance()
                .getCookie("https://tasktrophy.in");

            if (cookies == null || cookies.isEmpty()) {
                Log.d(TAG, "No WordPress cookies - user not logged in, skipping");
                return;
            }

            URL url = new URL(SYNC_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Cookie", cookies);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            String postData = "steps=" + steps + "&device_id=" + deviceId;
            OutputStream os = conn.getOutputStream();
            os.write(postData.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Server sync response: " + responseCode);
            conn.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Server sync failed: " + e.getMessage());
        }
    }

    /**
     * Schedule periodic step sync (every 15 minutes)
     */
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
            StepSyncWorker.class,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        );

        Log.d(TAG, "Background step sync scheduled (every 15 min)");
    }

    /**
     * Cancel periodic step sync
     */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Background step sync cancelled");
    }
}
