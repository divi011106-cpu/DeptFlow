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
import com.example.deptflow.auth.AuthManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private TextView tvChatTitle;
    private TextView tvStatus;

    private EditText etMessage;
    private Button btnSend;
    private Button btnBack;

    private LinearLayout messageContainer;
    private ScrollView scrollMessages;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messageListener;

    private String facultyName;

    private String currentUserId = "";
    private String currentUserName = "";
    private String currentUserRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        // Connect XML views
        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvStatus = findViewById(R.id.tvStatus);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        messageContainer = findViewById(R.id.messageContainer);
        scrollMessages = findViewById(R.id.scrollMessages);

        // Firebase
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Get selected faculty
        facultyName = getIntent().getStringExtra("facultyName");

        if (facultyName == null || facultyName.trim().isEmpty()) {
            facultyName = "Faculty";
        }

        tvChatTitle.setText(facultyName);
        tvStatus.setText("Faculty");

        // Get logged-in user
        loadCurrentUser();

        // Start listening for messages
        listenForMessages();

        // Back button
        btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // Send button
        btnSend.setOnClickListener(v -> {

            String message = etMessage.getText()
                    .toString()
                    .trim();

            if (message.isEmpty()) {

                Toast.makeText(
                        ChatActivity.this,
                        "Please enter a message",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            sendMessage(message);
        });
    }

    /**
     * Get currently logged-in faculty/user information.
     */
    private void loadCurrentUser() {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();

            if (firebaseUser.getDisplayName() != null &&
                    !firebaseUser.getDisplayName().isEmpty()) {

                currentUserName = firebaseUser.getDisplayName();

            } else {
                currentUserName = "You";
            }
        } else {
            currentUserName = "You";
        }

        currentUserRole = "FACULTY";
    }

    /**
     * Send message to Firestore.
     */
    private void sendMessage(String message) {

        Map<String, Object> messageData =
                new HashMap<>();

        // New sender information
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", currentUserName);
        messageData.put("senderRole", currentUserRole);

        // Keep old field for compatibility
        messageData.put("sender", currentUserName);

        messageData.put("message", message);
        messageData.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("chats")
                .document(facultyName)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {

                    // Clear input
                    etMessage.setText("");

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ChatActivity.this,
                            "Message could not be sent.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    /**
     * Listen for messages in real time.
     */
    private void listenForMessages() {

        messageListener = db.collection("chats")
                .document(facultyName)
                .collection("messages")
                .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                )
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {

                        // Firestore permission issue will
                        // be fixed by your friend later.
                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    messageContainer.removeAllViews();

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String senderId =
                                document.getString("senderId");

                        String senderName =
                                document.getString("senderName");

                        String sender =
                                document.getString("sender");

                        String message =
                                document.getString("message");

                        // New format
                        if (senderName == null ||
                                senderName.trim().isEmpty()) {

                            senderName = sender;
                        }

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

                    scrollMessages.post(() ->
                            scrollMessages.fullScroll(
                                    ScrollView.FOCUS_DOWN
                            )
                    );
                });
    }

    /**
     * Add a message bubble to the chat.
     */
    private void addMessage(
            String senderId,
            String senderName,
            String message) {

        TextView messageView =
                new TextView(this);

        boolean isMyMessage =
                currentUserId != null &&
                        !currentUserId.isEmpty() &&
                        currentUserId.equals(senderId);

        // Message text
        String displayText;

        if (isMyMessage) {

            displayText = message;

        } else {

            displayText =
                    senderName + "\n" + message;
        }

        messageView.setText(displayText);

        messageView.setTextSize(16);
        messageView.setTextColor(Color.BLACK);

        messageView.setMaxWidth(
                (int) (getResources()
                        .getDisplayMetrics()
                        .widthPixels * 0.75)
        );

        // Layout parameters
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        if (isMyMessage) {

            // My message → RIGHT
            params.gravity = Gravity.END;

            messageView.setBackgroundResource(
                    R.drawable.bg_message_sent
            );

        } else {

            // Other faculty → LEFT
            params.gravity = Gravity.START;

            messageView.setBackgroundResource(
                    R.drawable.bg_message_received
            );
        }

        params.setMargins(
                8,
                5,
                8,
                5
        );

        messageView.setLayoutParams(params);

        messageContainer.addView(messageView);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (messageListener != null) {
            messageListener.remove();
        }
    }
}