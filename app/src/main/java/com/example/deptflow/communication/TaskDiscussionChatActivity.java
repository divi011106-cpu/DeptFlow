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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class TaskDiscussionChatActivity extends AppCompatActivity {

    private TextView tvTaskTitle;
    private EditText etDiscussionMessage;
    private Button btnDiscussionSend;

    private LinearLayout discussionMessageContainer;
    private ScrollView scrollDiscussionMessages;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messageListener;

    private String taskName;

    private String currentUserId = "";
    private String currentUserName = "You";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_discussion_chat);

        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        etDiscussionMessage = findViewById(R.id.etDiscussionMessage);
        btnDiscussionSend = findViewById(R.id.btnDiscussionSend);

        discussionMessageContainer =
                findViewById(R.id.discussionMessageContainer);

        scrollDiscussionMessages =
                findViewById(R.id.scrollDiscussionMessages);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        taskName = getIntent().getStringExtra("taskName");

        if (taskName == null || taskName.trim().isEmpty()) {
            taskName = "Task Discussion";
        }

        tvTaskTitle.setText("Discussion - " + taskName);

        loadCurrentUser();
        listenForMessages();

        btnDiscussionSend.setOnClickListener(v -> {

            String message = etDiscussionMessage
                    .getText()
                    .toString()
                    .trim();

            if (message.isEmpty()) {

                Toast.makeText(
                        TaskDiscussionChatActivity.this,
                        "Please enter a message",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            sendMessage(message);
        });
    }

    private void loadCurrentUser() {

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {

            currentUserId = user.getUid();

            if (user.getDisplayName() != null &&
                    !user.getDisplayName().trim().isEmpty()) {

                currentUserName = user.getDisplayName();

            } else {

                currentUserName = "You";
            }
        }
    }

    private void sendMessage(String message) {

        Map<String, Object> messageData = new HashMap<>();

        messageData.put("senderId", currentUserId);
        messageData.put("senderName", currentUserName);
        messageData.put("sender", currentUserName);
        messageData.put("message", message);
        messageData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("task_discussions")
                .document(taskName)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {

                    etDiscussionMessage.setText("");

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            TaskDiscussionChatActivity.this,
                            "Message could not be sent.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void listenForMessages() {

        messageListener = db.collection("task_discussions")
                .document(taskName)
                .collection("messages")
                .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                )
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    discussionMessageContainer.removeAllViews();

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String senderId =
                                document.getString("senderId");

                        String senderName =
                                document.getString("senderName");

                        String message =
                                document.getString("message");

                        if (senderName == null ||
                                senderName.trim().isEmpty()) {

                            senderName = "Faculty";
                        }

                        if (message != null) {

                            addMessage(
                                    senderId,
                                    senderName,
                                    message
                            );
                        }
                    }

                    scrollDiscussionMessages.post(() ->
                            scrollDiscussionMessages.fullScroll(
                                    ScrollView.FOCUS_DOWN
                            )
                    );
                });
    }

    private void addMessage(
            String senderId,
            String senderName,
            String message) {

        TextView messageView = new TextView(this);

        boolean isMyMessage =
                currentUserId != null &&
                        !currentUserId.isEmpty() &&
                        currentUserId.equals(senderId);

        if (isMyMessage) {

            messageView.setText(message);
            messageView.setBackgroundResource(
                    R.drawable.bg_message_sent
            );

        } else {

            messageView.setText(
                    senderName + "\n" + message
            );

            messageView.setBackgroundResource(
                    R.drawable.bg_message_received
            );
        }

        messageView.setTextSize(16);
        messageView.setTextColor(Color.BLACK);

        messageView.setMaxWidth(
                (int) (
                        getResources()
                                .getDisplayMetrics()
                                .widthPixels * 0.75
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        if (isMyMessage) {
            params.gravity = Gravity.END;
        } else {
            params.gravity = Gravity.START;
        }

        params.setMargins(8, 5, 8, 5);

        messageView.setLayoutParams(params);

        discussionMessageContainer.addView(
                messageView
        );
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (messageListener != null) {
            messageListener.remove();
        }
    }
}