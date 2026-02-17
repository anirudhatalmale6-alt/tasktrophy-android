package com.webviewgold.myappname;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import android.util.Log;

public class GPSService extends Service {

    private static final int ID = 121;                        // The id of the notification

    private NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;
    private PowerManager.WakeLock wakeLock;                 // PARTIAL_WAKELOCK

    /**
     * Returns the instance of the service
     */
    public class LocalBinder extends Binder {
        public GPSService getServiceInstance(){
            return GPSService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();      // IBinder

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"GPSLogger:wakelock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(ID, getNotification());
        }
        Log.w("myApp", "[#] GPSService.java - START = onStartCommand");
        return START_NOT_STICKY;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public IBinder onBind(Intent intent) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.w("myApp", "[#] GPSService.java - WAKELOCK acquired");
        }
        Log.w("myApp", "[#] GPSService.java - BIND = onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        // PARTIAL_WAKELOCK
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.w("myApp", "[#] GPSService.java - WAKELOCK released");
        }
        Log.w("myApp", "[#] GPSService.java - DESTROY = onDestroy");
        // THREAD FOR DEBUG PURPOSE
        //if (t.isAlive()) t.interrupt();
        super.onDestroy();
    }

    private Notification getNotification() {
        final String CHANNEL_ID = "GPSLoggerServiceChannel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Foreground Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Location channel for foreground service notification");
            mNotificationManager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(getString(R.string.app_name))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(composeContentText());

        final Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
        startIntent.setAction(Intent.ACTION_MAIN);
        startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, startIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(contentIntent);
        return builder.build();
    }

    private String composeContentText () {
        return "Observing location";
    }
}
