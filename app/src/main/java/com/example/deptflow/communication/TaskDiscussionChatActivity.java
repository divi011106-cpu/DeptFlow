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
 * Modern Task Discussion Chat Activity.
 * Provides a dedicated, real-time collaboration channel for all faculty members
 * and the HOD assigned to a specific task.
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

    private String currentUserId = "";
    private String currentUserName = "Faculty";
    private String currentUserRole = "FACULTY";

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

        // Fallback compatibility with older intents passing only "taskName"
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = getIntent().getStringExtra("taskName");
        }
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = "Task Discussion";
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            taskId = taskTitle;
        }

        // Use taskId as document identifier to prevent name collisions
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
            currentUserId = firebaseUser.getUid();
            if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentUserName = firebaseUser.getDisplayName().trim();
            }
        }

        FacultyUser cachedUser = AuthManager.getInstance(this).getCurrentUser();
        if (cachedUser != null) {
            if (cachedUser.getName() != null && !cachedUser.getName().trim().isEmpty()) {
                currentUserName = cachedUser.getName().trim();
            }
            if (cachedUser.getRole() != null && !cachedUser.getRole().trim().isEmpty()) {
                currentUserRole = cachedUser.getRole().trim().toUpperCase();
            }
        }

        Log.d(TAG, "Current user loaded: UID=" + currentUserId + ", Name=" + currentUserName + ", Role=" + currentUserRole);
    }

    private void setupRecyclerView() {
        adapter = new DiscussionChatAdapter(this, messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvDiscussionMessages.setLayoutManager(layoutManager);
        rvDiscussionMessages.setAdapter(adapter);
    }

    private void listenForDiscussionMessages() {
        messageListener = db.collection("task_discussions")
                .document(discussionDocId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for discussion messages: " + error.getMessage(), error);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        messageList.clear();
                        adapter.setMessages(messageList);
                        layoutEmptyChat.setVisibility(View.VISIBLE);
                        return;
                    }

                    layoutEmptyChat.setVisibility(View.GONE);
                    List<DiscussionMessage> freshMessages = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String messageId = doc.getId();
                        String senderId = doc.getString("senderId");
                        String senderName = doc.getString("senderName");
                        String sender = doc.getString("sender");
                        String senderRole = doc.getString("senderRole");
                        String messageText = doc.getString("message");

                        if (senderName == null || senderName.trim().isEmpty()) {
                            senderName = sender != null ? sender : "Faculty";
                        }
                        if (senderRole == null || senderRole.trim().isEmpty()) {
                            senderRole = "Faculty";
                        }

                        long ts = 0L;
                        Object tsObj = doc.get("timestamp");
                        if (tsObj instanceof Timestamp) {
                            ts = ((Timestamp) tsObj).toDate().getTime();
                        } else if (tsObj instanceof Long) {
                            ts = (Long) tsObj;
                        } else if (tsObj instanceof Double) {
                            ts = ((Double) tsObj).longValue();
                        }

                        if (messageText != null && !messageText.trim().isEmpty()) {
                            freshMessages.add(new DiscussionMessage(
                                    messageId, senderId, senderName, senderRole, messageText, ts
                            ));
                        }
                    }

                    messageList.clear();
                    messageList.addAll(freshMessages);
                    adapter.setMessages(messageList);

                    if (!messageList.isEmpty()) {
                        rvDiscussionMessages.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void setupSendButton() {
        btnDiscussionSend.setOnClickListener(v -> {
            String message = etDiscussionMessage.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(TaskDiscussionChatActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            sendMessage(message);
        });
    }

    private void sendMessage(String message) {
        btnDiscussionSend.setEnabled(false);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", currentUserName);
        messageData.put("sender", currentUserName); // Legacy compatibility
        messageData.put("senderRole", currentUserRole);
        messageData.put("message", message);
        messageData.put("timestamp", System.currentTimeMillis());

        db.collection("task_discussions")
                .document(discussionDocId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    etDiscussionMessage.setText("");
                    btnDiscussionSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    btnDiscussionSend.setEnabled(true);
                    Log.e(TAG, "Failed to send discussion message", e);
                    Toast.makeText(TaskDiscussionChatActivity.this, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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