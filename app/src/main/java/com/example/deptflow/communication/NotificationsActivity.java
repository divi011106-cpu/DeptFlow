package com.example.deptflow.communication;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class NotificationsActivity extends AppCompatActivity {

    private TextView tvNotifications;

    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notifications);

        tvNotifications = findViewById(R.id.tvNotifications);

        db = FirebaseFirestore.getInstance();

        listenForTaskNotifications();
    }

    private void listenForTaskNotifications() {

        notificationListener = db.collection("task_assignments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {

                        Toast.makeText(
                                NotificationsActivity.this,
                                "Unable to load notifications",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {

                        tvNotifications.setText(
                                "No new notifications"
                        );

                        return;
                    }

                    StringBuilder notifications =
                            new StringBuilder();

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String title =
                                document.getString("title");

                        String description =
                                document.getString("description");

                        String deadline =
                                document.getString("deadline");

                        String assignedBy =
                                document.getString("assignedBy");

                        if (title == null) {
                            title = "New Task";
                        }

                        if (description == null) {
                            description = "";
                        }

                        if (deadline == null) {
                            deadline = "Not specified";
                        }

                        if (assignedBy == null) {
                            assignedBy = "HOD";
                        }

                        notifications
                                .append("📢 New Task Assigned\n\n")
                                .append("Title: ")
                                .append(title)
                                .append("\n\n")
                                .append("Description: ")
                                .append(description)
                                .append("\n\n")
                                .append("Deadline: ")
                                .append(deadline)
                                .append("\n\n")
                                .append("Assigned by: ")
                                .append(assignedBy)
                                .append("\n")
                                .append("--------------------------------\n\n");
                    }

                    tvNotifications.setText(
                            notifications.toString()
                    );
                });
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}