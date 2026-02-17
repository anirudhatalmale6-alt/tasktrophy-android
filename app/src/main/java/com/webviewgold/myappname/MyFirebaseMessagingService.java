package com.webviewgold.myappname;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.graphics.Color;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.firebase.PushwooshFcmHelper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = ">>>>>>>>>";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)){
            PushwooshFcmHelper.onMessageReceived(getApplicationContext(), remoteMessage);
            return;
        }
        int color = Color.parseColor(getResources().getString(R.color.colorAccent));
        super.onMessageReceived(remoteMessage);
        if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "onMessageReceived: " + remoteMessage);
        String url = "";
        for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if(key.equals("url")){
                url = value;
            }
            if(key.equals("color")){
                color = Color.parseColor(value);;
            }
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "key, " + key + " value " + value);
        }
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            sendNotification(remoteMessage.getNotification(), url, color);
            if (BuildConfig.IS_DEBUG_MODE) Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        AlertManager.updateFirebaseToken(this, token);
        PushwooshFcmHelper.onTokenRefresh(token);
    }


    @SuppressLint("UnspecifiedImmutableFlag")
    private void sendNotification(RemoteMessage.Notification messageBody, String url, int color) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(!url.equals("")) {
            intent.putExtra("ONESIGNAL_URL", url);
        }
      //  PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
               // PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_ONE_SHOT);
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        }


        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(messageBody.getTitle())
                        .setContentText(messageBody.getBody())
                        .setAutoCancel(true)
                        .setColorized(true)
                        .setColor(color)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(1 /* ID of notification */, notificationBuilder.build());
    }
}
