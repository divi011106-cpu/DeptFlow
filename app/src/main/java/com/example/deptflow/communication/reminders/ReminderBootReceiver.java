package com.example.deptflow.communication.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.models.FacultyReminder;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * BroadcastReceiver triggered upon device boot to reschedule all pending upcoming faculty reminders.
 */
public class ReminderBootReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            Log.d(TAG, "Device rebooted, restoring pending reminder schedules...");

            FacultyUser currentUser = AuthManager.getInstance(context).getCurrentUser();
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();

            String facultyId = currentUser != null ? currentUser.getUserId() : "";
            String authUid = authUser != null ? authUser.getUid() : "";

            if (facultyId.isEmpty() && authUid.isEmpty()) {
                Log.d(TAG, "No logged-in faculty found on boot. Skipping alarm restoration.");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            long now = System.currentTimeMillis();

            // Query pending reminders for current faculty
            db.collection("faculty_reminders")
                    .whereEqualTo("isCompleted", false)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || snapshot.isEmpty()) return;

                        int restoredCount = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                String docFacultyId = doc.getString("facultyId");
                                String docFacultyUid = doc.getString("facultyUid");

                                boolean matches = (!facultyId.isEmpty() && facultyId.equals(docFacultyId))
                                        || (!authUid.isEmpty() && authUid.equals(docFacultyUid));

                                if (!matches) continue;

                                Long scheduledTime = doc.getLong("scheduledDateTime");
                                if (scheduledTime != null && scheduledTime > now) {
                                    FacultyReminder reminder = doc.toObject(FacultyReminder.class);
                                    if (reminder != null) {
                                        reminder.setReminderId(doc.getId());
                                        ReminderScheduler.scheduleReminder(context.getApplicationContext(), reminder);
                                        restoredCount++;
                                    }
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error restoring reminder " + doc.getId() + ": " + e.getMessage());
                            }
                        }

                        Log.d(TAG, "Successfully restored " + restoredCount + " reminder alarms after reboot.");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to restore reminders on boot: " + e.getMessage()));
        }
    }
}
