package com.example.deptflow.communication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class ChatActivity extends AppCompatActivity {

    TextView tvChatTitle;
    TextView tvStatus;

    EditText etMessage;
    Button btnSend;
    Button btnBack;

    LinearLayout messageContainer;
    ScrollView scrollMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        // Connect XML components
        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvStatus = findViewById(R.id.tvStatus);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        messageContainer = findViewById(R.id.messageContainer);
        scrollMessages = findViewById(R.id.scrollMessages);


        // Get faculty name from FacultyListActivity
        String facultyName =
                getIntent().getStringExtra("facultyName");

        if (facultyName != null) {

            tvChatTitle.setText(facultyName);
            tvStatus.setText("Faculty");

        }


        // Back button
        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });


        // Send button
        btnSend.setOnClickListener(v -> {

            String message =
                    etMessage.getText().toString().trim();

            if (message.isEmpty()) {

                Toast.makeText(
                        ChatActivity.this,
                        "Please enter a message",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                addMessage(message);

                etMessage.setText("");

                // Scroll to latest message
                scrollMessages.post(() ->
                        scrollMessages.fullScroll(
                                ScrollView.FOCUS_DOWN
                        )
                );
            }

        });

    }


    // Add a message bubble
    private void addMessage(String message) {

        TextView messageView =
                new TextView(this);

        messageView.setText("You: " + message);

        messageView.setTextSize(16);

        messageView.setTextColor(Color.BLACK);

        messageView.setPadding(
                20,
                12,
                20,
                12
        );


        // Message width and height
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        // Put our message on the right
        params.gravity = Gravity.END;


        // Space around message
        params.setMargins(
                50,
                6,
                6,
                6
        );


        messageView.setLayoutParams(params);


        // Simple background
        messageView.setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
        );


        // Add message to screen
        messageContainer.addView(messageView);

    }

}