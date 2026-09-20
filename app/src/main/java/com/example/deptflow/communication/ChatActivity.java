
package com.example.deptflow.communication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private TextView tvChatTitle, tvStatus;
    private EditText etMessage;
    private Button btnSend, btnBack, btnEmoji;
    private LinearLayout messageContainer;
    private ScrollView scrollMessages;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messageListener;

    private String facultyName = "Faculty";
    private String selectedFacultyId = "";
    private String currentUserId = "";
    private String currentUserName = "User";
    private String currentUserRole = "USER";
    private String chatId = "";

    private boolean identityLoaded = false;
    private boolean chatScreenActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvStatus = findViewById(R.id.tvStatus);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnEmoji = findViewById(R.id.btnEmoji);
        messageContainer = findViewById(R.id.messageContainer);
        scrollMessages = findViewById(R.id.scrollMessages);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        facultyName = getIntent().getStringExtra("facultyName");
        selectedFacultyId = getIntent().getStringExtra("facultyId");

        if (facultyName == null || facultyName.trim().isEmpty()) {
            facultyName = "Faculty";
        }

        if (selectedFacultyId == null) {
            selectedFacultyId = "";
        }

        tvChatTitle.setText(facultyName);
        tvStatus.setText("Loading account...");
        btnSend.setEnabled(false);

        btnBack.setOnClickListener(v -> finish());

        btnEmoji.setOnClickListener(v -> {
            etMessage.requestFocus();

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE
                    );

            if (imm != null) {
                imm.showSoftInput(
                        etMessage,
                        InputMethodManager.SHOW_IMPLICIT
                );
            }
        });

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            sendMessage(message);
        });

        if (selectedFacultyId.trim().isEmpty()) {
            tvStatus.setText("Faculty ID missing. Reopen faculty list.");
            return;
        }

        loadCurrentUser();
    }

    private void loadCurrentUser() {
        FirebaseUser authUser = firebaseAuth.getCurrentUser();

        if (authUser == null || authUser.getEmail() == null) {
            tvStatus.setText("Please log in first.");
            return;
        }

        db.collection("users")
                .whereEqualTo("email", authUser.getEmail())
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        tvStatus.setText("Your account was not found.");
                        return;
                    }

                    DocumentSnapshot userDoc =
                            snapshot.getDocuments().get(0);

                    String userId = userDoc.getString("userID");
                    if (userId == null || userId.trim().isEmpty()) {
                        userId = userDoc.getId();
                    }

                    currentUserId = userId;
                    currentUserName = userDoc.getString("name") == null
                            ? "User" : userDoc.getString("name");
                    currentUserRole = userDoc.getString("role") == null
                            ? "USER" : userDoc.getString("role");

                    if (currentUserId.equals(selectedFacultyId)) {
                        tvStatus.setText("You cannot chat with yourself.");
                        return;
                    }

                    identityLoaded = true;
                    createChatId();

                    btnSend.setEnabled(true);
                    tvStatus.setText("Connected");

                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Could not load account.");
                    Toast.makeText(
                            this,
                            e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void createChatId() {
        ArrayList<String> ids = new ArrayList<>();
        ids.add(currentUserId);
        ids.add(selectedFacultyId);
        Collections.sort(ids);
        chatId = ids.get(0) + "_" + ids.get(1);
    }

    private void sendMessage(String message) {
        if (!identityLoaded || chatId.isEmpty()) {
            Toast.makeText(this, "Chat is not ready.", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        btnSend.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", currentUserId);
        data.put("senderName", currentUserName);
        data.put("senderRole", currentUserRole);
        data.put("message", message);
        data.put("timestamp", FieldValue.serverTimestamp());

        // Initial receipt state
        data.put("deliveredTo", new ArrayList<String>());
        data.put("readBy", new ArrayList<String>());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(data)
                .addOnSuccessListener(ref -> {
                    etMessage.setText("");
                    btnSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    btnSend.setEnabled(true);
                    tvStatus.setText("Message could not be sent.");
                    Toast.makeText(
                            this,
                            "Send error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void listenForMessages() {
        if (!identityLoaded || chatId.isEmpty()) return;

        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        tvStatus.setText("Unable to load messages.");
                        return;
                    }

                    if (snapshots == null) return;

                    messageContainer.removeAllViews();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String senderName = doc.getString("senderName");
                        String message = doc.getString("message");

                        if (message == null) continue;

                        boolean isMine = currentUserId.equals(senderId);

                        // When recipient sees a message, mark delivered/read.
                        if (!isMine && chatScreenActive) {
                            markMessageRead(doc);
                        }

                        addMessage(doc, senderId, senderName, message);
                    }

                    scrollMessages.post(() ->
                            scrollMessages.fullScroll(View.FOCUS_DOWN)
                    );
                });
    }

    private void markMessageRead(DocumentSnapshot doc) {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(doc.getId())
                .update(
                        "deliveredTo",
                        FieldValue.arrayUnion(currentUserId),
                        "readBy",
                        FieldValue.arrayUnion(currentUserId)
                );
    }

    private void addMessage(
            DocumentSnapshot doc,
            String senderId,
            String senderName,
            String message
    ) {
        boolean isMine = currentUserId.equals(senderId);

        TextView messageView = new TextView(this);

        if (isMine) {
            messageView.setText(message);
        } else {
            String name = senderName == null ? "Faculty" : senderName;
            messageView.setText(name + "\n" + message);
        }

        messageView.setTextSize(16);
        messageView.setTextColor(Color.BLACK);
        messageView.setPadding(24, 16, 24, 16);
        messageView.setMaxWidth(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.75)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.gravity = isMine ? Gravity.END : Gravity.START;
        params.setMargins(8, 5, 8, 5);

        messageView.setBackgroundResource(
                isMine
                        ? R.drawable.bg_message_sent
                        : R.drawable.bg_message_received
        );

        messageView.setLayoutParams(params);
        messageContainer.addView(messageView);

        // Tick indicator for your own messages
        if (isMine) {
            ArrayList<String> deliveredTo =
                    (ArrayList<String>) doc.get("deliveredTo");
            ArrayList<String> readBy =
                    (ArrayList<String>) doc.get("readBy");

            boolean delivered = deliveredTo != null
                    && deliveredTo.contains(selectedFacultyId);

            boolean read = readBy != null
                    && readBy.contains(selectedFacultyId);

            TextView tickView = new TextView(this);
            tickView.setText(read ? "✓✓" : delivered ? "✓✓" : "✓");
            tickView.setTextColor(
                    read ? Color.BLUE : Color.GRAY
            );
            tickView.setTextSize(12);

            LinearLayout.LayoutParams tickParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            tickParams.gravity = Gravity.END;
            tickParams.setMargins(0, 0, 12, 4);

            messageContainer.addView(tickView, tickParams);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatScreenActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        chatScreenActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (messageListener != null) {
            messageListener.remove();
        }
    }
}