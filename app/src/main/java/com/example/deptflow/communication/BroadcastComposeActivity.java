package com.example.deptflow.communication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BroadcastComposeActivity — Composes and executes individual delivery of a broadcast message
 * to all selected faculty members, saving to individual private 1-on-1 chats and creating
 * recipient-specific notifications.
 */
public class BroadcastComposeActivity extends AppCompatActivity {

    private static final String TAG = "BroadcastCompose";

    private ImageButton ibBackBroadcastCompose;
    private TextView tvComposeRecipientCount;
    private TextView tvRecipientNamesList;
    private TextView tvEditRecipients;
    private EditText etBroadcastMessage;
    private LinearLayout layoutBroadcastProgress;
    private ProgressBar pbBroadcastSending;
    private TextView tvBroadcastStatus;
    private MaterialButton btnSendBroadcast;

    private final List<String> recipientIds = new ArrayList<>();
    private final List<String> recipientNames = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String currentSenderId = "FAC-101";
    private String currentSenderCanonicalId = "FAC-101";
    private String currentSenderUid = "";
    private String currentSenderName = "Faculty";

    private boolean isSending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_compose);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        extractIntentData();
        loadCurrentUserInfo();
        setupSendAction();
    }

    private void initViews() {
        ibBackBroadcastCompose = findViewById(R.id.ibBackBroadcastCompose);
        tvComposeRecipientCount = findViewById(R.id.tvComposeRecipientCount);
        tvRecipientNamesList = findViewById(R.id.tvRecipientNamesList);
        tvEditRecipients = findViewById(R.id.tvEditRecipients);
        etBroadcastMessage = findViewById(R.id.etBroadcastMessage);
        layoutBroadcastProgress = findViewById(R.id.layoutBroadcastProgress);
        pbBroadcastSending = findViewById(R.id.pbBroadcastSending);
        tvBroadcastStatus = findViewById(R.id.tvBroadcastStatus);
        btnSendBroadcast = findViewById(R.id.btnSendBroadcast);

        if (ibBackBroadcastCompose != null) {
            ibBackBroadcastCompose.setOnClickListener(v -> finish());
        }

        if (tvEditRecipients != null) {
            tvEditRecipients.setOnClickListener(v -> finish());
        }
    }

    private void extractIntentData() {
        ArrayList<String> ids = getIntent().getStringArrayListExtra("EXTRA_SELECTED_IDS");
        ArrayList<String> names = getIntent().getStringArrayListExtra("EXTRA_SELECTED_NAMES");

        if (ids != null) recipientIds.addAll(ids);
        if (names != null) recipientNames.addAll(names);

        if (recipientIds.isEmpty()) {
            Toast.makeText(this, "No recipients selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvComposeRecipientCount.setText(recipientIds.size() + " Recipients");
        tvRecipientNamesList.setText(TextUtils.join(", ", recipientNames));
    }

    private void loadCurrentUserInfo() {
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            currentSenderUid = fbUser.getUid();
            if (fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                currentSenderName = fbUser.getDisplayName().trim();
            }
        }

        FacultyUser sessionUser = AuthManager.getInstance(this).getCurrentUser();
        if (sessionUser != null) {
            if (sessionUser.getUserId() != null) currentSenderId = sessionUser.getUserId().trim();
            if (sessionUser.getName() != null) currentSenderName = sessionUser.getName().trim();

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(sessionUser);
            if (canonical != null) {
                currentSenderCanonicalId = canonical.getUserId();
                currentSenderId = canonical.getUserId();
                currentSenderName = canonical.getName();
            }
        }

        if (currentSenderCanonicalId.isEmpty() && !currentSenderName.isEmpty()) {
            currentSenderCanonicalId = FacultyDirectory.resolveCanonicalId(currentSenderName);
            if (!currentSenderCanonicalId.isEmpty()) currentSenderId = currentSenderCanonicalId;
        }
    }

    private void setupSendAction() {
        btnSendBroadcast.setOnClickListener(v -> {
            String text = etBroadcastMessage.getText() != null ? etBroadcastMessage.getText().toString().trim() : "";
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter a message to broadcast", Toast.LENGTH_SHORT).show();
                return;
            }

            sendBroadcastMessages(text);
        });
    }

    private void sendBroadcastMessages(String messageText) {
        if (isSending) return;
        isSending = true;

        btnSendBroadcast.setEnabled(false);
        etBroadcastMessage.setEnabled(false);
        layoutBroadcastProgress.setVisibility(View.VISIBLE);
        pbBroadcastSending.setMax(recipientIds.size());
        pbBroadcastSending.setProgress(0);
        tvBroadcastStatus.setText("Preparing broadcast to " + recipientIds.size() + " recipients...");

        final int totalRecipients = recipientIds.size();
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final List<String> failedRecipients = new ArrayList<>();
        final List<String> failureReasons = new ArrayList<>();

        String canonicalSender = !currentSenderCanonicalId.isEmpty() ? currentSenderCanonicalId : currentSenderId;

        Log.d(TAG, "Broadcast started");
        Log.d(TAG, "Current faculty: " + canonicalSender + " (" + currentSenderName + "), UID: " + currentSenderUid);
        Log.d(TAG, "Selected recipients: " + recipientIds);
        Log.d(TAG, "Recipient count: " + totalRecipients);

        // We execute writes for each recipient
        for (int i = 0; i < totalRecipients; i++) {
            final String targetId = recipientIds.get(i);
            final String targetName = (i < recipientNames.size()) ? recipientNames.get(i) : targetId;

            sendToIndividualRecipient(targetId, targetName, messageText, new DeliveryCallback() {
                @Override
                public void onSuccess() {
                    int completed = successCount.incrementAndGet();
                    runOnUiThread(() -> {
                        pbBroadcastSending.setProgress(completed + failureCount.get());
                        tvBroadcastStatus.setText("Sent " + completed + " of " + totalRecipients + "...");
                        checkCompletion(totalRecipients, successCount.get(), failureCount.get(), failedRecipients, failureReasons);
                    });
                }

                @Override
                public void onFailure(String errorCode, String errorMessage) {
                    failureCount.incrementAndGet();
                    failedRecipients.add(targetName);
                    failureReasons.add(errorCode + ": " + errorMessage);
                    runOnUiThread(() -> {
                        pbBroadcastSending.setProgress(successCount.get() + failureCount.get());
                        checkCompletion(totalRecipients, successCount.get(), failureCount.get(), failedRecipients, failureReasons);
                    });
                }
            });
        }
    }

    private void checkCompletion(int total, int successes, int failures, List<String> failedNames, List<String> failureReasons) {
        if (successes + failures >= total) {
            isSending = false;
            btnSendBroadcast.setEnabled(true);
            etBroadcastMessage.setEnabled(true);

            if (failures == 0) {
                Toast.makeText(this, "✅ Broadcast delivered to all " + successes + " faculty members!", Toast.LENGTH_LONG).show();
                finish();
            } else if (successes > 0) {
                String firstReason = !failureReasons.isEmpty() ? failureReasons.get(0) : "Unknown error";
                Toast.makeText(this, "⚠️ Broadcast sent to " + successes + " (" + failures + " failed). Reason: " + firstReason, Toast.LENGTH_LONG).show();
                tvBroadcastStatus.setText("Partially completed: " + successes + " sent, " + failures + " failed.");
            } else {
                String errorMsg = getFriendlyErrorMessage(failureReasons);
                Toast.makeText(this, "❌ " + errorMsg, Toast.LENGTH_LONG).show();
                tvBroadcastStatus.setText("Delivery failed: " + errorMsg);
            }
        }
    }

    private String getFriendlyErrorMessage(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "Broadcast failed. Unknown error.";
        }
        String first = reasons.get(0);
        if (first.contains("PERMISSION_DENIED")) {
            return "Firestore permission denied. Check authentication/rules.";
        } else if (first.contains("UNAUTHENTICATED")) {
            return "Firebase Authentication session is missing.";
        } else if (first.contains("UNAVAILABLE")) {
            return "Firebase is temporarily unavailable. Please retry.";
        } else if (first.contains("NOT_FOUND")) {
            return "The requested task/chat document was not found.";
        } else if (first.contains("FAILED_PRECONDITION")) {
            return "Firestore precondition issue.";
        }
        return "Broadcast failed: " + first;
    }

    private interface DeliveryCallback {
        void onSuccess();
        void onFailure(String errorCode, String errorMessage);
    }

    private void sendToIndividualRecipient(String recipientId, String recipientName, String messageText, DeliveryCallback callback) {
        String canonicalSender = !currentSenderCanonicalId.isEmpty() ? currentSenderCanonicalId : currentSenderId;
        String canonicalRecipient = recipientId;

        FacultyUser resolved = FacultyDirectory.resolveByNameOrId(recipientId);
        if (resolved != null) {
            canonicalRecipient = resolved.getUserId();
            recipientName = resolved.getName();
        }

        String chatId = FacultyDirectory.getDeterministicChatId(canonicalSender, canonicalRecipient);
        long nowMillis = System.currentTimeMillis();
        String messageId = "msg_bcast_" + nowMillis + "_" + canonicalRecipient;
        String notifId = "notif_bcast_" + nowMillis + "_" + canonicalRecipient;

        Log.d(TAG, "Broadcast recipient: " + recipientName + " | ID: " + canonicalRecipient + " | Chat ID: " + chatId);
        Log.d(TAG, "Firestore path: chats/" + chatId + "/messages/" + messageId);

        WriteBatch batch = db.batch();

        // 1. Message Document in chats/{chatId}/messages/{messageId}
        DocumentReference msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId);
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("id", messageId);
        msgData.put("senderId", canonicalSender);
        msgData.put("senderUid", currentSenderUid);
        msgData.put("senderName", currentSenderName);
        msgData.put("receiverId", canonicalRecipient);
        msgData.put("receiverName", recipientName);
        msgData.put("message", messageText);
        msgData.put("timestamp", FieldValue.serverTimestamp());
        msgData.put("status", "SENT");
        msgData.put("read", false);
        msgData.put("isBroadcast", true);
        batch.set(msgRef, msgData);

        // 2. Parent Chat Conversation Document in chats/{chatId}
        DocumentReference chatRef = db.collection("chats").document(chatId);
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", chatId);
        chatData.put("conversationId", chatId);
        chatData.put("faculty1Id", canonicalSender.compareTo(canonicalRecipient) < 0 ? canonicalSender : canonicalRecipient);
        chatData.put("faculty2Id", canonicalSender.compareTo(canonicalRecipient) < 0 ? canonicalRecipient : canonicalSender);
        chatData.put("faculty1Name", canonicalSender.compareTo(canonicalRecipient) < 0 ? currentSenderName : recipientName);
        chatData.put("faculty2Name", canonicalSender.compareTo(canonicalRecipient) < 0 ? recipientName : currentSenderName);
        chatData.put("participantIds", Arrays.asList(canonicalSender, canonicalRecipient));
        chatData.put("participants", Arrays.asList(canonicalSender, canonicalRecipient, currentSenderName, recipientName));
        chatData.put("lastMessage", messageText);
        chatData.put("lastSenderId", canonicalSender);
        chatData.put("lastSenderName", currentSenderName);
        chatData.put("lastMessageTime", FieldValue.serverTimestamp());
        chatData.put("timestamp", FieldValue.serverTimestamp());
        chatData.put("lastTimestamp", FieldValue.serverTimestamp());
        chatData.put("unreadCount_" + canonicalRecipient, FieldValue.increment(1));
        batch.set(chatRef, chatData, SetOptions.merge());

        // 3. Recipient Notification in notifications/{notifId}
        DocumentReference notifRef = db.collection("notifications").document(notifId);
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", notifId);
        notifData.put("type", "CHAT");
        notifData.put("title", "Broadcast Message");
        notifData.put("subtitle", "New message from " + currentSenderName);
        notifData.put("message", messageText);
        notifData.put("messagePreview", messageText);
        notifData.put("senderId", canonicalSender);
        notifData.put("senderUid", currentSenderUid);
        notifData.put("senderName", currentSenderName);
        notifData.put("recipientId", canonicalRecipient);
        notifData.put("recipientFacultyId", canonicalRecipient);
        notifData.put("recipientName", recipientName);
        notifData.put("chatId", chatId);
        notifData.put("messageId", messageId);
        notifData.put("isRead", false);
        notifData.put("timestamp", FieldValue.serverTimestamp());
        batch.set(notifRef, notifData);

        final String finalRecipientId = canonicalRecipient;

        // Commit batch write
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully delivered broadcast to " + finalRecipientId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    String code = "UNKNOWN";
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        code = ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode().name();
                    }
                    Log.e(TAG, "Operation failed. code=" + code + " message=" + e.getMessage(), e);
                    callback.onFailure(code, e.getMessage());
                });
    }
}
