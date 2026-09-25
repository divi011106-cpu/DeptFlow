package com.example.deptflow.communication.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.deptflow.communication.models.FacultyReminder;

/**
 * Utility for scheduling, rescheduling, and cancelling Android system alarms
 * for Faculty Reminders using AlarmManager and PendingIntent.
 */
public class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";

    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";
    public static final String EXTRA_REMINDER_TITLE = "extra_reminder_title";
    public static final String EXTRA_REMINDER_DESC = "extra_reminder_desc";
    public static final String EXTRA_FACULTY_ID = "extra_faculty_id";

    /**
     * Schedules an Android system alarm for the specified reminder.
     * Uses AlarmManager.setExactAndAllowWhileIdle where permitted.
     */
    public static void scheduleReminder(Context context, FacultyReminder reminder) {
        if (context == null || reminder == null) return;

        if (reminder.isCompleted() || reminder.isPastDue()) {
            Log.d(TAG, "Skipping schedule: reminder is completed or past due (id=" + reminder.getReminderId() + ")");
            return;
        }

        long triggerAtMillis = reminder.getScheduledDateTime();
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Skipping schedule: trigger time is in past");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return;
        }

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.deptflow.ACTION_TRIGGER_REMINDER_" + reminder.getReminderId());
        intent.putExtra(EXTRA_REMINDER_ID, reminder.getReminderId());
        intent.putExtra(EXTRA_REMINDER_TITLE, reminder.getTitle());
        intent.putExtra(EXTRA_REMINDER_DESC, reminder.getDescription());
        intent.putExtra(EXTRA_FACULTY_ID, reminder.getFacultyId());

        int requestCode = getRequestCode(reminder.getReminderId());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    Log.d(TAG, "Exact alarm scheduled (Android 12+) for " + reminder.getTitle() + " at " + triggerAtMillis);
                } else {
                    // Fallback to inexact alarm if exact alarm permission not granted
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                    Log.w(TAG, "Inexact alarm scheduled (canScheduleExactAlarms=false) for " + reminder.getTitle());
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(TAG, "Exact alarm scheduled (Marshmallow+) for " + reminder.getTitle() + " at " + triggerAtMillis);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d(TAG, "Exact alarm scheduled for " + reminder.getTitle() + " at " + triggerAtMillis);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while scheduling exact alarm: " + se.getMessage(), se);
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } catch (Exception ex) {
                Log.e(TAG, "Failed fallback alarm scheduling: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Cancels an existing scheduled alarm for a reminder.
     */
    public static void cancelReminder(Context context, String reminderId) {
        if (context == null || reminderId == null || reminderId.trim().isEmpty()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.deptflow.ACTION_TRIGGER_REMINDER_" + reminderId);

        int requestCode = getRequestCode(reminderId);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm for reminder id=" + reminderId);
        }
    }

    /**
     * Checks if exact alarms can be scheduled on Android 12+ (API 31+).
     */
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    /**
     * Generates a unique positive integer request code from a reminder string ID.
     */
    public static int getRequestCode(String reminderId) {
        if (reminderId == null) return 0;
        return Math.abs(reminderId.hashCode());
    }
}
