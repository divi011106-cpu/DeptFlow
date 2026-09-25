package com.example.deptflow.communication.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.deptflow.R;
import com.example.deptflow.communication.ChatActivity;
import com.example.deptflow.communication.NotificationsActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging service for DeptFlow.
 * Handles incoming push notifications while app is in background or foreground,
 * and updates device FCM registration token in Firestore.
 */
public class DeptFlowMessagingService extends FirebaseMessagingService {

    private static final String TAG = "DeptFlowFCM";
    public static final String CHANNEL_CHAT_ID = "deptflow_chat_channel";
    public static final String CHANNEL_CHAT_NAME = "Faculty Messages";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM registration token: " + token);
        FcmTokenManager.saveTokenToFirestore(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Extract payload data
        Map<String, String> data = remoteMessage.getData();
        String title = "New Faculty Message";
        String messageBody = "You have a new message";
        String senderId = "";
        String senderName = "Faculty";
        String chatId = "";
        String type = "CHAT_MESSAGE";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                messageBody = remoteMessage.getNotification().getBody();
            }
        }

        if (data.size() > 0) {
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("message")) messageBody = data.get("message");
            if (data.containsKey("senderId")) senderId = data.get("senderId");
            if (data.containsKey("senderName")) senderName = data.get("senderName");
            if (data.containsKey("chatId")) chatId = data.get("chatId");
            if (data.containsKey("type")) type = data.get("type");
        }

        showPushNotification(title, messageBody, senderId, senderName, chatId, type);
    }

    private void showPushNotification(String title, String message, String senderId,
                                      String senderName, String chatId, String type) {
        createNotificationChannel();

        Intent intent;
        if ("CHAT_MESSAGE".equalsIgnoreCase(type) && !senderId.isEmpty()) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("facultyName", senderName);
            intent.putExtra("facultyId", senderId);
            intent.putExtra("chatId", chatId);
        } else {
            intent = new Intent(this, NotificationsActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = (int) System.currentTimeMillis();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, flags);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_CHAT_ID)
                        .setSmallIcon(R.drawable.ic_chat)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int notificationId = senderId.hashCode() != 0 ? Math.abs(senderId.hashCode()) : 1001;
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_CHAT_ID);
                if (existingChannel == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_CHAT_ID,
                            CHANNEL_CHAT_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Push notifications for faculty chat and communication");
                    channel.enableLights(true);
                    channel.setLightColor(Color.BLUE);
                    channel.enableVibration(true);
                    notificationManager.createNotificationChannel(channel);
                }
            }
        }
    }
}
