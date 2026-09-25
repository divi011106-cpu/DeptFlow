package com.example.deptflow.communication;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.models.AppNotification;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.communication.services.FcmTokenManager;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ChatActivity — WhatsApp-style one-to-one Faculty chat with real-time sync,
 * message bubbles, delivery/read tick marks, deterministic conversation tracking,
 * and recipient-specific unread counters & notifications.
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private TextView tvChatTitle, tvStatus, tvChatAvatarInitial;
    private EditText etMessage;
    private View btnSend, btnBack, btnEmoji;
    private LinearLayout messageContainer;
    private ScrollView scrollMessages;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messageListener;

    private String recipientFacultyName = "Faculty";
    private String recipientFacultyId = "";
    private String recipientCanonicalId = "";
    private String recipientFacultyUid = "";

    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentSenderUid = "";
    private String currentUserName = "Faculty";
    private String currentUserRole = "FACULTY";
    private String chatId = "";

    private boolean identityLoaded = false;
    private boolean chatScreenActive = false;
    private boolean isSending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        extractIntentData();
        setupClickListeners();

        // Register FCM device token
        FcmTokenManager.registerDeviceToken(this);

        loadCurrentUser();
    }

    private void initViews() {
        tvChatTitle = findViewById(R.id.tvChatTitle);
        tvStatus = findViewById(R.id.tvStatus);
        tvChatAvatarInitial = findViewById(R.id.tvChatAvatarInitial);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnEmoji = findViewById(R.id.btnEmoji);
        messageContainer = findViewById(R.id.messageContainer);
        scrollMessages = findViewById(R.id.scrollMessages);

        btnSend.setEnabled(false);
    }

    private void extractIntentData() {
        recipientFacultyName = getIntent().getStringExtra("facultyName");
        recipientFacultyId = getIntent().getStringExtra("facultyId");
        recipientFacultyUid = getIntent().getStringExtra("facultyUid");
        String passedChatId = getIntent().getStringExtra("chatId");

        if (passedChatId != null && !passedChatId.trim().isEmpty()) {
            chatId = passedChatId.trim();
        }

        if (recipientFacultyName == null || recipientFacultyName.trim().isEmpty()) {
            recipientFacultyName = "Faculty";
        }

        if (recipientFacultyId == null) {
            recipientFacultyId = "";
        }

        if (recipientFacultyUid == null) {
            recipientFacultyUid = "";
        }

        // Resolve recipient canonical info
        FacultyUser canonicalRecipient = FacultyDirectory.resolveByNameOrId(
                !recipientFacultyId.isEmpty() ? recipientFacultyId : recipientFacultyName
        );

        if (canonicalRecipient != null) {
            recipientCanonicalId = canonicalRecipient.getUserId();
            recipientFacultyName = canonicalRecipient.getName();
        } else {
            recipientCanonicalId = recipientFacultyId;
        }

        tvChatTitle.setText(recipientFacultyName);
        if (tvChatAvatarInitial != null && recipientFacultyName.length() > 0) {
            tvChatAvatarInitial.setText(recipientFacultyName.substring(0, 1).toUpperCase(Locale.ROOT));
        }
        tvStatus.setText("Connecting...");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEmoji.setOnClickListener(v -> {
            etMessage.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etMessage, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(message);
        });
    }

    private void loadCurrentUser() {
        FirebaseUser authUser = firebaseAuth.getCurrentUser();
        if (authUser != null) {
            currentSenderUid = authUser.getUid();
            if (authUser.getDisplayName() != null && !authUser.getDisplayName().trim().isEmpty()) {
                currentUserName = authUser.getDisplayName().trim();
            }
        }

        FacultyUser cachedUser = AuthManager.getInstance(this).getCurrentUser();
        if (cachedUser != null) {
            if (cachedUser.getUserId() != null && !cachedUser.getUserId().trim().isEmpty()) {
                currentUserId = cachedUser.getUserId().trim();
            }
            if (cachedUser.getName() != null && !cachedUser.getName().trim().isEmpty()) {
                currentUserName = cachedUser.getName().trim();
            }
            if (cachedUser.getRole() != null && !cachedUser.getRole().trim().isEmpty()) {
                currentUserRole = cachedUser.getRole().trim();
            }

            FacultyUser canonicalMe = FacultyDirectory.resolveCanonicalFaculty(cachedUser);
            if (canonicalMe != null) {
                currentCanonicalId = canonicalMe.getUserId();
                currentUserName = canonicalMe.getName();
            }
        }

        if (currentCanonicalId.isEmpty() && !currentUserName.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserName);
        }

        if (currentCanonicalId.isEmpty() && !currentUserId.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserId);
        }

        if (currentCanonicalId.isEmpty() && !currentSenderUid.isEmpty()) {
            currentCanonicalId = currentSenderUid;
        }

        if (currentSenderUid.isEmpty() && currentCanonicalId.isEmpty()) {
            tvStatus.setText("Please log in first.");
            return;
        }

        finishIdentitySetup();
    }

    private void finishIdentitySetup() {
        if (recipientCanonicalId.isEmpty() && !chatId.isEmpty()) {
            String[] parts = chatId.split("_");
            if (parts.length == 2) {
                if (currentCanonicalId.equalsIgnoreCase(parts[0]) || currentUserId.equalsIgnoreCase(parts[0])) {
                    recipientCanonicalId = parts[1];
                } else {
                    recipientCanonicalId = parts[0];
                }
                recipientFacultyName = FacultyDirectory.resolveCanonicalName(recipientCanonicalId);
                tvChatTitle.setText(recipientFacultyName);
            }
        }

        if (recipientCanonicalId.trim().isEmpty()) {
            tvStatus.setText("Recipient ID missing.");
            return;
        }

        if (currentCanonicalId.equalsIgnoreCase(recipientCanonicalId)) {
            tvStatus.setText("Cannot chat with yourself.");
            return;
        }

        // Generate deterministic, symmetric Chat ID
        chatId = FacultyDirectory.getDeterministicChatId(currentCanonicalId, recipientCanonicalId);

        identityLoaded = true;
        btnSend.setEnabled(true);
        tvStatus.setText("Active in Department");

        Log.d(TAG, "Chat initialized: ChatId=" + chatId
                + ", Sender=" + currentCanonicalId + " (" + currentUserName + ")"
                + ", Recipient=" + recipientCanonicalId + " (" + recipientFacultyName + ")");

        markConversationReadInFirestore();
        listenForMessages();
    }

    private void sendMessage(String messageText) {
        if (!identityLoaded || chatId.isEmpty() || isSending) {
            Toast.makeText(this, "Chat is not ready.", Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        btnSend.setEnabled(false);
        tvStatus.setText("Sending...");

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", currentCanonicalId);
        data.put("senderUid", currentSenderUid);
        data.put("senderName", currentUserName);
        data.put("senderRole", currentUserRole);
        data.put("recipientId", recipientCanonicalId);
        data.put("recipientUid", recipientFacultyUid);
        data.put("recipientName", recipientFacultyName);
        data.put("message", messageText);
        data.put("timestamp", FieldValue.serverTimestamp());
        data.put("deliveredTo", new ArrayList<String>());
        data.put("readBy", new ArrayList<String>());

        // 1. Add message to chats/{chatId}/messages
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(data)
                .addOnSuccessListener(ref -> {
                    isSending = false;
                    btnSend.setEnabled(true);
                    etMessage.setText("");
                    tvStatus.setText("Active in Department");

                    // 2. Update root conversation document with unread counter increment
                    updateRootConversation(messageText);

                    // 3. Create Notification Document for Recipient
                    createChatNotification(ref.getId(), messageText);
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    btnSend.setEnabled(true);
                    tvStatus.setText("Failed to send");
                    String code = "UNKNOWN";
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        code = ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode().name();
                    }
                    Log.e(TAG, "Operation failed. code=" + code + " message=" + e.getMessage(), e);
                    String friendly;
                    if ("PERMISSION_DENIED".equals(code)) {
                        friendly = "Firestore permission denied. Check authentication and Firestore rules.";
                    } else if ("UNAUTHENTICATED".equals(code)) {
                        friendly = "Firebase Authentication session is missing.";
                    } else if ("UNAVAILABLE".equals(code)) {
                        friendly = "Firebase is temporarily unavailable.";
                    } else if ("FAILED_PRECONDITION".equals(code)) {
                        friendly = "Firestore configuration/precondition issue.";
                    } else {
                        friendly = "Failed to send: " + e.getMessage();
                    }
                    Toast.makeText(this, friendly, Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Updates the root conversation document with participants, last message,
     * timestamp, and increments recipient's unread counter.
     */
    private void updateRootConversation(String messageText) {
        Set<String> participants = new HashSet<>();
        participants.add(currentCanonicalId);
        participants.add(recipientCanonicalId);
        participants.add(currentUserName);
        participants.add(recipientFacultyName);
        if (!currentUserId.isEmpty()) participants.add(currentUserId);
        if (!recipientFacultyId.isEmpty()) participants.add(recipientFacultyId);
        if (!currentSenderUid.isEmpty()) participants.add(currentSenderUid);
        if (!recipientFacultyUid.isEmpty()) participants.add(recipientFacultyUid);

        List<String> participantIds = new ArrayList<>();
        participantIds.add(currentCanonicalId);
        participantIds.add(recipientCanonicalId);

        List<String> participantNames = new ArrayList<>();
        participantNames.add(currentUserName);
        participantNames.add(recipientFacultyName);

        Map<String, Object> rootData = new HashMap<>();
        rootData.put("chatId", chatId);
        rootData.put("conversationId", chatId);
        rootData.put("participants", new ArrayList<>(participants));
        rootData.put("participantIds", participantIds);
        rootData.put("participantNames", participantNames);
        rootData.put("lastMessage", messageText);
        rootData.put("lastMessageTime", FieldValue.serverTimestamp());
        rootData.put("timestamp", FieldValue.serverTimestamp());
        rootData.put("lastTimestamp", FieldValue.serverTimestamp());
        rootData.put("lastSenderId", currentCanonicalId);
        rootData.put("lastSenderName", currentUserName);

        // Increment unread count for recipient
        if (!recipientCanonicalId.isEmpty()) {
            rootData.put("unreadCount_" + recipientCanonicalId, FieldValue.increment(1));
        }
        if (!recipientFacultyId.isEmpty() && !recipientFacultyId.equals(recipientCanonicalId)) {
            rootData.put("unreadCount_" + recipientFacultyId, FieldValue.increment(1));
        }
        if (!recipientFacultyUid.isEmpty()) {
            rootData.put("unreadCount_" + recipientFacultyUid, FieldValue.increment(1));
        }

        db.collection("chats").document(chatId)
                .set(rootData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Root conversation doc updated for " + chatId))
                .addOnFailureListener(e -> {
                    String code = "UNKNOWN";
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        code = ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode().name();
                    }
                    Log.e(TAG, "Operation failed. code=" + code + " message=" + e.getMessage(), e);
                });
    }

    /**
     * Resets unread counter for the current user and marks notifications as read.
     */
    private void markConversationReadInFirestore() {
        if (chatId.isEmpty()) return;

        Map<String, Object> resetMap = new HashMap<>();
        if (!currentCanonicalId.isEmpty()) {
            resetMap.put("unreadCount_" + currentCanonicalId, 0);
        }
        if (!currentUserId.isEmpty()) {
            resetMap.put("unreadCount_" + currentUserId, 0);
        }
        if (!currentSenderUid.isEmpty()) {
            resetMap.put("unreadCount_" + currentSenderUid, 0);
        }
        if (!currentUserName.isEmpty()) {
            resetMap.put("unreadCount_" + currentUserName.toLowerCase(Locale.ROOT), 0);
        }

        db.collection("chats").document(chatId)
                .set(resetMap, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "Could not reset unread counter: " + e.getMessage()));

        // Mark chat notifications read in top-level notifications collection
        markNotificationsRead();
    }

    private void markNotificationsRead() {
        if (currentCanonicalId.isEmpty() && currentUserId.isEmpty() && currentSenderUid.isEmpty()) return;

        // Query by chatId and mark read
        db.collection("notifications")
                .whereEqualTo("chatId", chatId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String recId = d.getString("recipientId");
                        String recUid = d.getString("recipientUid");
                        String recName = d.getString("recipientName");

                        boolean isMine = (recId != null && (recId.equalsIgnoreCase(currentCanonicalId) || recId.equalsIgnoreCase(currentUserId)))
                                || (recUid != null && recUid.equals(currentSenderUid))
                                || (recName != null && recName.equalsIgnoreCase(currentUserName));

                        if (isMine) {
                            d.getReference().update("isRead", true);
                        }
                    }
                });
    }

    /**
     * Creates a separate notification document for recipient in top-level 'notifications' collection.
     */
    private void createChatNotification(String messageId, String messageText) {
        if (recipientCanonicalId.trim().isEmpty()) {
            Log.w(TAG, "Cannot create notification: recipientCanonicalId is empty");
            return;
        }

        String notificationDocId = "notif_" + messageId + "_" + recipientCanonicalId;

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("notificationId", notificationDocId);
        notifData.put("recipientId", recipientCanonicalId);
        if (!recipientFacultyId.isEmpty()) notifData.put("recipientFacultyId", recipientFacultyId);
        if (!recipientFacultyUid.isEmpty()) notifData.put("recipientUid", recipientFacultyUid);
        notifData.put("recipientName", recipientFacultyName);

        notifData.put("senderId", currentCanonicalId);
        notifData.put("senderFacultyId", currentUserId);
        if (!currentSenderUid.isEmpty()) notifData.put("senderUid", currentSenderUid);
        notifData.put("senderName", currentUserName);
        notifData.put("sender", currentUserName);

        notifData.put("chatId", chatId);
        notifData.put("conversationId", chatId);
        notifData.put("messageId", messageId);
        notifData.put("messagePreview", messageText);
        notifData.put("message", messageText);
        notifData.put("title", "Faculty Message");
        notifData.put("subtitle", "New message from " + currentUserName);
        notifData.put("timestamp", FieldValue.serverTimestamp());
        notifData.put("isRead", false);
        notifData.put("type", AppNotification.TYPE_CHAT);

        db.collection("notifications")
                .document(notificationDocId)
                .set(notifData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification doc written: " + notificationDocId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed writing notification: " + e.getMessage(), e));
    }

    private void listenForMessages() {
        if (!identityLoaded || chatId.isEmpty()) return;

        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        String code = "UNKNOWN";
                        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                            code = error.getCode().name();
                        }
                        Log.e(TAG, "Operation failed. code=" + code + " message=" + error.getMessage(), error);
                        if ("PERMISSION_DENIED".equals(code)) {
                            tvStatus.setText("Permission denied. Check Firestore rules.");
                        } else if ("UNAUTHENTICATED".equals(code)) {
                            tvStatus.setText("Authentication session missing.");
                        } else if ("UNAVAILABLE".equals(code)) {
                            tvStatus.setText("Firebase temporarily unavailable.");
                        } else {
                            tvStatus.setText("Unable to load messages.");
                        }
                        return;
                    }

                    if (snapshots == null) return;

                    tvStatus.setText("Active in Department");
                    messageContainer.removeAllViews();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String senderUid = doc.getString("senderUid");
                        String senderName = doc.getString("senderName");
                        String message = doc.getString("message");
                        Timestamp ts = doc.getTimestamp("timestamp");

                        if (message == null) continue;

                        boolean isMine = isMyMessage(senderId, senderUid, senderName);

                        if (!isMine && chatScreenActive) {
                            markMessageRead(doc);
                        }

                        addMessageBubble(doc, senderName, message, ts, isMine);
                    }

                    scrollMessages.post(() ->
                            scrollMessages.fullScroll(View.FOCUS_DOWN)
                    );
                });
    }

    private boolean isMyMessage(String senderId, String senderUid, String senderName) {
        if (senderId != null && (senderId.equalsIgnoreCase(currentCanonicalId) || senderId.equalsIgnoreCase(currentUserId))) {
            return true;
        }
        if (senderUid != null && !senderUid.isEmpty() && senderUid.equals(currentSenderUid)) {
            return true;
        }
        if (senderName != null && senderName.equalsIgnoreCase(currentUserName)) {
            return true;
        }
        return false;
    }

    private void markMessageRead(DocumentSnapshot doc) {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(doc.getId())
                .update(
                        "deliveredTo",
                        FieldValue.arrayUnion(currentCanonicalId),
                        "readBy",
                        FieldValue.arrayUnion(currentCanonicalId)
                );
    }

    private void addMessageBubble(
            DocumentSnapshot doc,
            String senderName,
            String message,
            Timestamp timestamp,
            boolean isMine
    ) {
        LinearLayout bubbleLayout = new LinearLayout(this);
        bubbleLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams bubbleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        bubbleParams.gravity = isMine ? Gravity.END : Gravity.START;
        bubbleParams.setMargins(12, 6, 12, 6);
        bubbleLayout.setLayoutParams(bubbleParams);

        bubbleLayout.setBackgroundResource(
                isMine ? R.drawable.bg_message_sent : R.drawable.bg_message_received
        );
        bubbleLayout.setPadding(28, 18, 28, 16);

        // Sender Name for received messages
        if (!isMine) {
            TextView nameView = new TextView(this);
            nameView.setText(senderName != null ? senderName : recipientFacultyName);
            nameView.setTextSize(12);
            nameView.setTextColor(Color.parseColor("#1E3A8A"));
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            nameView.setPadding(0, 0, 0, 4);
            bubbleLayout.addView(nameView);
        }

        // Message text
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextSize(15);
        messageView.setTextColor(isMine ? Color.parseColor("#0F172A") : Color.parseColor("#0F172A"));
        messageView.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.75));
        bubbleLayout.addView(messageView);

        // Bottom Row: Timestamp + Tick indicator (for sent)
        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL | (isMine ? Gravity.END : Gravity.START));
        metaRow.setPadding(0, 4, 0, 0);

        TextView timeView = new TextView(this);
        String formattedTime = "";
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            formattedTime = sdf.format(timestamp.toDate());
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            formattedTime = sdf.format(new Date());
        }
        timeView.setText(formattedTime);
        timeView.setTextSize(11);
        timeView.setTextColor(Color.parseColor("#64748B"));
        metaRow.addView(timeView);

        if (isMine) {
            ArrayList<?> deliveredTo = (ArrayList<?>) doc.get("deliveredTo");
            ArrayList<?> readBy = (ArrayList<?>) doc.get("readBy");

            boolean read = readBy != null && !readBy.isEmpty();
            boolean delivered = deliveredTo != null && !deliveredTo.isEmpty();

            TextView tickView = new TextView(this);
            tickView.setText(read ? " ✓✓" : delivered ? " ✓✓" : " ✓");
            tickView.setTextColor(read ? Color.parseColor("#25D366") : Color.parseColor("#64748B"));
            tickView.setTextSize(11);
            tickView.setTypeface(null, android.graphics.Typeface.BOLD);
            metaRow.addView(tickView);
        }

        bubbleLayout.addView(metaRow);
        messageContainer.addView(bubbleLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatScreenActive = true;
        markConversationReadInFirestore();
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