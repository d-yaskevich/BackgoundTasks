package com.myfirst.backgoundtasks;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import static androidx.core.content.ContextCompat.getSystemService;

public class SomeBroadcastReceiver extends BroadcastReceiver {

    private final String CHANNEL_ID = "some_broadcast_receiver.channel.id";
    private final String NOTIFICATION_CHANNEL_NAME = "some_broadcast_receiver_channel_name";
    private final String NOTIFICATION_CHANNEL_DESCRIPTION = "some_broadcast_receiver_channel_description";

    private NotificationManager notificationManager;
    private PendingResult pendingResult;

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            //intent.toUri(Intent.URI_INTENT_SCHEME).toString()
            boolean state = false;
            if (intent.hasExtra("state")) {
                state = intent.getBooleanExtra("state", false);
            }
            Toast.makeText(context, "ACTION_AIRPLANE_MODE_CHANGED - " + state, Toast.LENGTH_SHORT).show();
            return;
        }
        createNotificationChannel(context);

        notificationManager = getSystemService(context, NotificationManager.class);
        pendingResult = goAsync();
        sendNotification(context);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
            channel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(context, NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private int tag = 0;

    private void sendNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_delete)
                        .setContentTitle("Some Broadcast receiver title")
                        .setContentText("Open the app")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pIntent)
                        .setAutoCancel(true)
                        .build();

        notificationManager.notify(tag++, notification);

        pendingResult.finish();
    }
}
