package com.example.deptflow.communication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

import java.util.ArrayList;

public class NotificationsActivity extends AppCompatActivity {

    private ListView listNotifications;
    private TextView tvNoNotifications;

    private final ArrayList<String> notifications =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notifications);

        listNotifications =
                findViewById(R.id.listNotifications);

        tvNoNotifications =
                findViewById(R.id.tvNoNotifications);

        loadNotifications();
    }

    private void loadNotifications() {

        notifications.clear();

        // Temporary notification data.
        // Later this will come from Firestore/HOD assignment.
        notifications.add(
                "New Task Assigned\n" +
                        "Result Analysis\n" +
                        "Please complete the task before the deadline."
        );

        notifications.add(
                "Reminder\n" +
                        "Attendance Report is due soon."
        );

        if (notifications.isEmpty()) {

            listNotifications.setVisibility(ListView.GONE);
            tvNoNotifications.setVisibility(TextView.VISIBLE);

            return;
        }

        listNotifications.setVisibility(ListView.VISIBLE);
        tvNoNotifications.setVisibility(TextView.GONE);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        notifications
                );

        listNotifications.setAdapter(adapter);

        listNotifications.setOnItemClickListener(
                (parent, view, position, id) -> {

                    showNotificationPopup(
                            notifications.get(position)
                    );
                }
        );
    }

    private void showNotificationPopup(String notification) {

        new AlertDialog.Builder(this)
                .setTitle("Notification")
                .setMessage(notification)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }
}