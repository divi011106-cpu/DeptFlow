package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class CommunicationActivity extends AppCompatActivity {

    private Button btnFacultyChat;
    private Button btnTaskDiscussion;
    private Button btnNotifications;
    private Button btnReminders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        // Connect buttons from XML
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
            try {
                Intent intent = new Intent(
                        CommunicationActivity.this,
                        NotificationsActivity.class
                );
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Unable to open Notifications",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Reminders
        btnReminders.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(
                        CommunicationActivity.this,
                        RemindersActivity.class
                );
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        "Unable to open Reminders",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}