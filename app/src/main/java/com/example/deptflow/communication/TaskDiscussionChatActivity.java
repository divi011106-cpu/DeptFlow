package com.example.deptflow.communication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class TaskDiscussionChatActivity extends AppCompatActivity {

    TextView tvTaskTitle;
    TextView tvDiscussionMessages;
    EditText etDiscussionMessage;
    Button btnDiscussionSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_discussion_chat);

        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvDiscussionMessages =
                findViewById(R.id.tvDiscussionMessages);
        etDiscussionMessage =
                findViewById(R.id.etDiscussionMessage);
        btnDiscussionSend =
                findViewById(R.id.btnDiscussionSend);

        String taskName =
                getIntent().getStringExtra("taskName");

        if (taskName != null) {
            tvTaskTitle.setText("Discussion - " + taskName);
        }

        btnDiscussionSend.setOnClickListener(v -> {

            String message =
                    etDiscussionMessage.getText()
                            .toString()
                            .trim();

            if (message.isEmpty()) {

                Toast.makeText(
                        TaskDiscussionChatActivity.this,
                        "Please enter a message",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                tvDiscussionMessages.append(
                        "\n\nYou: " + message
                );

                etDiscussionMessage.setText("");
            }
        });
    }
}