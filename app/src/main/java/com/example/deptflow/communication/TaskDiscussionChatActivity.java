package com.example.deptflow.communication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.DiscussionChatAdapter;
import com.example.deptflow.communication.models.DiscussionMessage;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaskDiscussionChatActivity — Real-time team collaboration channel
 * for all faculty members and HOD assigned to a specific task.
 */
public class TaskDiscussionChatActivity extends AppCompatActivity {

    private static final String TAG = "TaskDiscussionChat";

    private TextView tvTaskTitle;
    private TextView tvTaskSubtitle;
    private ImageButton ibBackDiscussion;
    private RecyclerView rvDiscussionMessages;
    private View layoutEmptyChat;
    private EditText etDiscussionMessage;
    private ImageButton btnDiscussionSend;

    private DiscussionChatAdapter adapter;
    private final List<DiscussionMessage> messageList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messageListener;

    private String taskId = "";
    private String taskTitle = "";
    private String taskDeadline = "";
    private String discussionDocId = "";

    private String currentUid = "";
    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentUserName = "Faculty";
    private String currentUserRole = "FACULTY";

    private boolean isSending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_discussion_chat);

        initViews();
        extractIntentData();
        loadCurrentUserInfo();
        setupRecyclerView();
        listenForDiscussionMessages();
        setupSendButton();
    }

    private void initViews() {
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskSubtitle = findViewById(R.id.tvTaskSubtitle);
        ibBackDiscussion = findViewById(R.id.ibBackDiscussion);
        rvDiscussionMessages = findViewById(R.id.rvDiscussionMessages);
        layoutEmptyChat = findViewById(R.id.layoutEmptyChat);
        etDiscussionMessage = findViewById(R.id.etDiscussionMessage);
        btnDiscussionSend = findViewById(R.id.btnDiscussionSend);

        if (ibBackDiscussion != null) {
            ibBackDiscussion.setOnClickListener(v -> finish());
        }
    }

    private void extractIntentData() {
        taskId = getIntent().getStringExtra("EXTRA_TASK_ID");
        taskTitle = getIntent().getStringExtra("EXTRA_TASK_TITLE");
        taskDeadline = getIntent().getStringExtra("EXTRA_TASK_DEADLINE");

        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = getIntent().getStringExtra("taskName");
        }
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = "Task Discussion";
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            taskId = taskTitle;
        }

        discussionDocId = taskId;

        tvTaskTitle.setText(taskTitle);

        if (taskDeadline != null && !taskDeadline.trim().isEmpty()) {
            tvTaskSubtitle.setText("ID: " + taskId + " • Due: " + taskDeadline);
        } else {
            tvTaskSubtitle.setText("Task ID: " + taskId + " • Team Discussion");
        }
    }

    private void loadCurrentUserInfo() {
        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentUserName = firebaseUser.getDisplayName().trim();
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
                currentUserRole = cachedUser.getRole().trim().toUpperCase();
            }

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(cachedUser);
            if (canonical != null) {
                currentCanonicalId = canonical.getUserId();
                currentUserName = canonical.getName();
            }
        }

        if (currentCanonicalId.isEmpty() && !currentNameIsEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserName);
        }

        if (currentCanonicalId.isEmpty() && !currentUserId.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserId);
        }

        if (currentCanonicalId.isEmpty() && !currentUid.isEmpty()) {
            currentCanonicalId = currentUid;
        }

        Log.d(TAG, "Current user loaded: UID=" + currentUid
                + ", CanonicalId=" + currentCanonicalId
                + ", Name=" + currentUserName
                + ", Role=" + currentUserRole);
    }

    private boolean currentNameIsEmpty() {
        return currentUserName == null || currentUserName.trim().isEmpty();
    }

    private void setupRecyclerView() {
        // Pass the primary ID used for message matching
        String activeUserId = !currentCanonicalId.isEmpty() ? currentCanonicalId : (!currentUserId.isEmpty() ? currentUserId : currentUid);
        adapter = new DiscussionChatAdapter(this, messageList, activeUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvDiscussionMessages.setLayoutManager(layoutManager);
        rvDiscussionMessages.setAdapter(adapter);
    }

    private void listenForDiscussionMessages() {
        if (discussionDocId.isEmpty()) return;

        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener = db.collection("task_discussions")
                .document(discussionDocId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        String code = "UNKNOWN";
                        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                            code = error.getCode().name();
                        }
                        Log.e(TAG, "Operation failed. code=" + code + " message=" + error.getMessage(), error);
                        return;
                    }

                    if (snapshots == null) return;

                    messageList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String msgId = doc.getId();
                        String senderId = doc.getString("senderId");
                        String senderName = doc.getString("senderName");
                        String senderRole = doc.getString("senderRole");
                        String text = doc.getString("message");
                        long timestamp = 0L;

                        Object tsObj = doc.get("timestamp");
                        if (tsObj instanceof Timestamp) {
                            timestamp = ((Timestamp) tsObj).toDate().getTime();
                        } else if (tsObj instanceof Number) {
                            timestamp = ((Number) tsObj).longValue();
                        } else if (tsObj instanceof java.util.Date) {
                            timestamp = ((java.util.Date) tsObj).getTime();
                        } else if (tsObj instanceof String) {
                            try {
                                timestamp = Long.parseLong(((String) tsObj).trim());
                            } catch (Exception ignored) {}
                        }

                        if (text != null && !text.trim().isEmpty()) {
                            DiscussionMessage msg = new DiscussionMessage(
                                     msgId,
                                     senderId != null ? senderId : "",
                                     senderName != null ? senderName : "Faculty",
                                     senderRole != null ? senderRole : "Faculty",
                                     text,
                                     timestamp
                            );
                            messageList.add(msg);
                        }
                    }

                    adapter.setMessages(messageList);

                    if (messageList.isEmpty()) {
                        layoutEmptyChat.setVisibility(View.VISIBLE);
                        rvDiscussionMessages.setVisibility(View.GONE);
                    } else {
                        layoutEmptyChat.setVisibility(View.GONE);
                        rvDiscussionMessages.setVisibility(View.VISIBLE);
                        rvDiscussionMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void setupSendButton() {
        btnDiscussionSend.setOnClickListener(v -> {
            String text = etDiscussionMessage.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Type a message first", Toast.LENGTH_SHORT).show();
                return;
            }
            sendDiscussionMessage(text);
        });
    }

    private void sendDiscussionMessage(String messageText) {
        if (isSending) return;
        isSending = true;
        btnDiscussionSend.setEnabled(false);

        String activeSenderId = !currentCanonicalId.isEmpty() ? currentCanonicalId : (!currentUserId.isEmpty() ? currentUserId : currentUid);

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", activeSenderId);
        msgMap.put("senderUid", currentUid);
        msgMap.put("senderCanonicalId", currentCanonicalId);
        msgMap.put("senderName", currentUserName);
        msgMap.put("senderRole", currentUserRole);
        msgMap.put("message", messageText);
        msgMap.put("timestamp", FieldValue.serverTimestamp());

        db.collection("task_discussions")
                .document(discussionDocId)
                .collection("messages")
                .add(msgMap)
                .addOnSuccessListener(ref -> {
                    isSending = false;
                    btnDiscussionSend.setEnabled(true);
                    etDiscussionMessage.setText("");

                    // Update parent doc last updated timestamp
                    Map<String, Object> parentDoc = new HashMap<>();
                    parentDoc.put("taskId", taskId);
                    parentDoc.put("taskTitle", taskTitle);
                    parentDoc.put("lastMessage", messageText);
                    parentDoc.put("lastSenderName", currentUserName);
                    parentDoc.put("lastUpdated", FieldValue.serverTimestamp());

                    db.collection("task_discussions")
                            .document(discussionDocId)
                            .set(parentDoc, com.google.firebase.firestore.SetOptions.merge());
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    btnDiscussionSend.setEnabled(true);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}