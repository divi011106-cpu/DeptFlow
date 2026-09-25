package com.example.deptflow.communication.reminders;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.deptflow.R;
import com.example.deptflow.communication.RemindersActivity;

/**
 * BroadcastReceiver triggered by AlarmManager when a faculty reminder date/time is reached.
 * Displays a high-priority system notification with vibration and sound.
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";
    public static final String CHANNEL_ID = "deptflow_reminders_channel";
    public static final String CHANNEL_NAME = "Faculty Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID);
        String title = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE);
        String desc = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DESC);

        if (title == null || title.trim().isEmpty()) {
            title = "Faculty Reminder";
        }

        Log.d(TAG, "Reminder alarm fired: id=" + reminderId + ", title=" + title);

        createNotificationChannel(context);

        // Target intent opens RemindersActivity
        Intent openIntent = new Intent(context, RemindersActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openIntent.putExtra("highlightReminderId", reminderId);

        int requestCode = ReminderScheduler.getRequestCode(reminderId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, openIntent, flags);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("⏰ Reminder: " + title)
                .setContentText(desc != null && !desc.trim().isEmpty() ? desc : "Scheduled reminder deadline")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle("⏰ " + title)
                        .bigText(desc != null && !desc.trim().isEmpty() ? desc : "Scheduled reminder deadline"))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int notificationId = ReminderScheduler.getRequestCode(reminderId);
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "System notification dispatched for reminder id=" + reminderId);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
                if (existing == null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    channel.setDescription("Notifications for scheduled faculty reminders");
                    channel.enableLights(true);
                    channel.setLightColor(Color.BLUE);
                    channel.enableVibration(true);
                    manager.createNotificationChannel(channel);
                }
            }
        }
    }
}
