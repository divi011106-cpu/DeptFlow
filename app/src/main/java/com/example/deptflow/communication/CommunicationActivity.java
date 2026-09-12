package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class CommunicationActivity extends AppCompatActivity {

    Button btnFacultyChat;
    Button btnTaskDiscussion;
    Button btnNotifications;
    Button btnReminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_communication);

        btnFacultyChat = findViewById(R.id.btnFacultyChat);
        btnTaskDiscussion = findViewById(R.id.btnTaskDiscussion);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnReminders = findViewById(R.id.btnReminders);

        // Faculty Chat
        btnFacultyChat.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CommunicationActivity.this,
                    FacultyListActivity.class
            );

            startActivity(intent);
        });

        // Task Discussion
        btnTaskDiscussion.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CommunicationActivity.this,
                    TaskDiscussionActivity.class
            );

            startActivity(intent);
        });

        // Notifications
        btnNotifications.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CommunicationActivity.this,
                    NotificationsActivity.class
            );

            startActivity(intent);
        });

        // Reminders
        btnReminders.setOnClickListener(v -> {

            Intent intent = new Intent(
                    CommunicationActivity.this,
                    RemindersActivity.class
            );

            startActivity(intent);
        });
    }
}