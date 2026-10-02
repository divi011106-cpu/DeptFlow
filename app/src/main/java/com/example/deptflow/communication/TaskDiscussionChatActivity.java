package com.example.deptflow.communication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.DiscussionChatAdapter;
import com.example.deptflow.communication.adapters.EmojiPickerAdapter;
import com.example.deptflow.communication.models.DiscussionMessage;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.repository.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * TaskDiscussionChatActivity — Real-time team collaboration channel
 * for all faculty members and HOD assigned to a specific task.
 *
 * Firestore structure:
 * task_discussions/{taskId}/messages/{messageId}
 */
public class TaskDiscussionChatActivity extends AppCompatActivity {

    private static final String TAG_SEND = "TEAM_CHAT_SEND";
    private static final String TAG_READ = "TEAM_CHAT_READ";
    private static final String TAG_STATUS = "TEAM_CHAT_STATUS";
    private static final String TAG_EMOJI = "TEAM_CHAT_EMOJI";
    private static final String TAG = "TEAM_CHAT";

    private TextView tvTaskTitle;
    private TextView tvTaskSubtitle;
    private ImageButton ibBackDiscussion;
    private RecyclerView rvDiscussionMessages;
    private View layoutEmptyChat;
    private TextView tvEmptyChatTitle;
    private TextView tvEmptyChatSubtitle;
    private EditText etDiscussionMessage;
    private ImageButton btnDiscussionEmoji;
    private ImageButton btnDiscussionSend;
    private LinearLayout layoutEmojiPicker;
    private RecyclerView rvEmojiPicker;

    private MaterialCardView cardTeamMembers;
    private TextView tvTeamMembersHeader;
    private TextView tvTeamMembersList;
    private TextView tvChatDeadline;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration messageListener;

    private DiscussionChatAdapter adapter;
    private final List<DiscussionMessage> messageList = new ArrayList<>();

    private final List<String> emojiList = Arrays.asList(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
            "😘", "😎", "🤔", "😐", "😑", "😶", "🤗", "🤩",
            "👍", "👎", "👏", "🙌", "❤️", "💯", "🔥", "🎉",
            "✅", "❌", "🤝", "🙏", "🎯", "📌", "⭐", "💡",
            "✨", "💪", "🚀", "🏆", "📊", "📝", "📅", "🕒",
            "📎", "📢", "💼", "☕", "🎓", "📚", "💻", "🏫"
    );

    private String taskId = "";
    private String taskTitle = "";
    private String taskDescription = "";
    private String taskDeadline = "";
    private boolean isAll = false;
    private final List<String> taskParticipants = new ArrayList<>();
    private int participantCount = 0;

    private String currentUid = "";
    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentUserName = "Faculty";
    private String currentUserEmail = "";
    private String currentUserRole = "FACULTY";
    private final Set<String> myIdentifiers = new HashSet<>();

    private boolean isSending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_discussion_chat);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        Log.d("FIREBASE_DEBUG", "FirebaseFirestore instance created");
        Log.d("FIREBASE_DEBUG", "FirebaseAuth instance created");

        FirebaseUser user = firebaseAuth.getCurrentUser();
        Log.d("FIREBASE_DEBUG", "Firebase user = " + (user != null ? user.getUid() : "NULL"));
        Log.d(TAG_SEND, "Firestore initialized");

        initViews();
        extractIntentData();

        if (taskId == null || taskId.trim().isEmpty()) {
            Log.e(TAG_SEND, "TASK ID IS NULL OR EMPTY");
            Toast.makeText(this, "Task ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadCurrentUserInfo();
        setupRecyclerView();
        setupEmojiPicker();
        loadTaskDetailsFromFirestore();
        listenForDiscussionMessages();
        setupSendButton();
    }

    private void initViews() {
        tvTaskTitle = findViewById(R.id.tvTaskTitle);
        tvTaskSubtitle = findViewById(R.id.tvTaskSubtitle);
        ibBackDiscussion = findViewById(R.id.ibBackDiscussion);
        rvDiscussionMessages = findViewById(R.id.rvDiscussionMessages);
        layoutEmptyChat = findViewById(R.id.layoutEmptyChat);
        tvEmptyChatTitle = findViewById(R.id.tvEmptyChatTitle);
        tvEmptyChatSubtitle = findViewById(R.id.tvEmptyChatSubtitle);
        etDiscussionMessage = findViewById(R.id.etDiscussionMessage);
        btnDiscussionEmoji = findViewById(R.id.btnDiscussionEmoji);
        btnDiscussionSend = findViewById(R.id.btnDiscussionSend);
        layoutEmojiPicker = findViewById(R.id.layoutEmojiPicker);
        rvEmojiPicker = findViewById(R.id.rvEmojiPicker);

        if (btnDiscussionSend != null) {
            btnDiscussionSend.setEnabled(true);
            btnDiscussionSend.setClickable(true);
        }

        cardTeamMembers = findViewById(R.id.cardTeamMembers);
        tvTeamMembersHeader = findViewById(R.id.tvTeamMembersHeader);
        tvTeamMembersList = findViewById(R.id.tvTeamMembersList);
        tvChatDeadline = findViewById(R.id.tvChatDeadline);

        if (ibBackDiscussion != null) {
            ibBackDiscussion.setOnClickListener(v -> finish());
        }

        if (btnDiscussionEmoji != null) {
            btnDiscussionEmoji.setOnClickListener(v -> toggleEmojiPicker());
        }

        if (etDiscussionMessage != null) {
            etDiscussionMessage.setOnClickListener(v -> hideEmojiPicker());
            etDiscussionMessage.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    hideEmojiPicker();
                }
            });
        }
    }

    private void setupEmojiPicker() {
        if (rvEmojiPicker == null) return;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 7);
        rvEmojiPicker.setLayoutManager(gridLayoutManager);
        EmojiPickerAdapter emojiAdapter = new EmojiPickerAdapter(emojiList, emoji -> {
            insertEmoji(emoji);
            Log.d(TAG_EMOJI, "Emoji selected: " + emoji);
        });
        rvEmojiPicker.setAdapter(emojiAdapter);
    }

    private void toggleEmojiPicker() {
        if (layoutEmojiPicker == null) return;
        if (layoutEmojiPicker.getVisibility() == View.VISIBLE) {
            layoutEmojiPicker.setVisibility(View.GONE);
        } else {
            hideKeyboard();
            layoutEmojiPicker.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmojiPicker() {
        if (layoutEmojiPicker != null && layoutEmojiPicker.getVisibility() == View.VISIBLE) {
            layoutEmojiPicker.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    private void insertEmoji(String emoji) {
        if (etDiscussionMessage == null || emoji == null) return;
        int start = Math.max(etDiscussionMessage.getSelectionStart(), 0);
        int end = Math.max(etDiscussionMessage.getSelectionEnd(), 0);
        etDiscussionMessage.getText().replace(Math.min(start, end), Math.max(start, end), emoji, 0, emoji.length());
    }

    @Override
    public void onBackPressed() {
        if (layoutEmojiPicker != null && layoutEmojiPicker.getVisibility() == View.VISIBLE) {
            layoutEmojiPicker.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

    private void extractIntentData() {
        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.trim().isEmpty()) {
            taskId = getIntent().getStringExtra("EXTRA_TASK_ID");
        }
        if (taskId != null) {
            taskId = taskId.trim();
        }

        Log.d(TAG_SEND, "taskId = " + taskId);

        taskTitle = getIntent().getStringExtra("taskTitle");
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = getIntent().getStringExtra("EXTRA_TASK_TITLE");
        }
        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            taskTitle = getIntent().getStringExtra("taskName");
        }
        if (taskTitle != null) {
            taskTitle = taskTitle.trim();
        }

        taskDescription = getIntent().getStringExtra("taskDescription");
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            taskDescription = getIntent().getStringExtra("description");
        }

        taskDeadline = getIntent().getStringExtra("deadline");
        if (taskDeadline == null || taskDeadline.trim().isEmpty()) {
            taskDeadline = getIntent().getStringExtra("EXTRA_TASK_DEADLINE");
        }

        isAll = getIntent().getBooleanExtra("EXTRA_IS_ALL", false);
        if (!isAll) {
            String allStr = getIntent().getStringExtra("isAll");
            if ("true".equalsIgnoreCase(allStr)) isAll = true;
        }

        ArrayList<String> members = getIntent().getStringArrayListExtra("assignedMembers");
        if (members == null || members.isEmpty()) {
            members = getIntent().getStringArrayListExtra("EXTRA_PARTICIPANTS");
        }
        if (members != null && !members.isEmpty()) {
            taskParticipants.clear();
            taskParticipants.addAll(members);
        }
        participantCount = getIntent().getIntExtra("EXTRA_PARTICIPANT_COUNT", taskParticipants.size());

        if (tvTaskTitle != null) {
            if (taskTitle != null && !taskTitle.isEmpty()) {
                tvTaskTitle.setText(taskTitle);
            } else {
                tvTaskTitle.setText("Team Task Discussion");
            }
        }

        updateSubtitleUI();
        updateTeamMembersUI();
    }

    private void updateSubtitleUI() {
        if (tvTaskSubtitle == null) return;
        if (isAll || participantCount >= FacultyDirectory.FACULTY_NAMES.length) {
            tvTaskSubtitle.setText("Team Discussion • All Faculty");
        } else if (participantCount > 1 || taskParticipants.size() > 1) {
            int count = Math.max(participantCount, taskParticipants.size());
            tvTaskSubtitle.setText("Team Discussion • " + count + " Members");
        } else if (participantCount == 1 || taskParticipants.size() == 1) {
            String name = !taskParticipants.isEmpty() ? taskParticipants.get(0) : "";
            if (!name.isEmpty()) {
                tvTaskSubtitle.setText("Individual Discussion • " + name);
            } else {
                tvTaskSubtitle.setText("Individual Discussion");
            }
        } else {
            tvTaskSubtitle.setText("Task Discussion");
        }
    }

    private void updateTeamMembersUI() {
        if (cardTeamMembers == null) return;

        if (isAll || participantCount >= FacultyDirectory.FACULTY_NAMES.length) {
            if (tvTeamMembersHeader != null) {
                tvTeamMembersHeader.setText("Team Discussion (All Faculty)");
            }
            if (tvTeamMembersList != null) {
                tvTeamMembersList.setText("All Department Faculty members are assigned to this task.");
            }
        } else if (!taskParticipants.isEmpty()) {
            int count = Math.max(participantCount, taskParticipants.size());
            if (tvTeamMembersHeader != null) {
                tvTeamMembersHeader.setText("Members (" + count + ")");
            }
            if (tvTeamMembersList != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < taskParticipants.size(); i++) {
                    sb.append("• ").append(taskParticipants.get(i));
                    if (i < taskParticipants.size() - 1) sb.append("\n");
                }
                tvTeamMembersList.setText(sb.toString());
            }
        } else {
            if (tvTeamMembersHeader != null) {
                tvTeamMembersHeader.setText("Assigned Faculty");
            }
            if (tvTeamMembersList != null) {
                tvTeamMembersList.setText("Assigned by HOD");
            }
        }

        if (tvChatDeadline != null) {
            if (taskDeadline != null && !taskDeadline.trim().isEmpty()) {
                tvChatDeadline.setVisibility(View.VISIBLE);
                tvChatDeadline.setText("Due: " + taskDeadline.trim());
            } else {
                tvChatDeadline.setVisibility(View.GONE);
            }
        }
    }

    private void loadTaskDetailsFromFirestore() {
        if (taskId == null || taskId.isEmpty()) return;

        db.collection("task_assignments")
                .whereEqualTo("groupTaskId", taskId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        processTaskAssignmentDocs(snapshots.getDocuments());
                    } else {
                        db.collection("task_assignments").document(taskId).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc != null && doc.exists()) {
                                        List<DocumentSnapshot> singleList = new ArrayList<>();
                                        singleList.add(doc);
                                        processTaskAssignmentDocs(singleList);
                                    }
                                });
                    }
                });
    }

    private void processTaskAssignmentDocs(List<DocumentSnapshot> docs) {
        if (docs == null || docs.isEmpty()) return;

        for (DocumentSnapshot doc : docs) {
            String title = doc.getString("taskTitle");
            if (title == null || title.isEmpty()) title = doc.getString("title");
            if (title != null && !title.isEmpty()) {
                taskTitle = title;
                if (tvTaskTitle != null) tvTaskTitle.setText(taskTitle);
            }

            String dl = doc.getString("deadline");
            if (dl != null && !dl.isEmpty()) {
                taskDeadline = dl;
            }

            Object facListObj = doc.get("allAssignedFaculty");
            if (facListObj == null) facListObj = doc.get("assignedFaculty");
            if (facListObj instanceof List) {
                for (Object item : (List<?>) facListObj) {
                    if (item != null) {
                        String name = item.toString().trim();
                        if (!name.isEmpty() && !taskParticipants.contains(name)) {
                            taskParticipants.add(name);
                        }
                    }
                }
            }

            String assignedTo = doc.getString("assignedTo");
            if (assignedTo == null || assignedTo.isEmpty()) assignedTo = doc.getString("faculty");
            if (assignedTo != null && !assignedTo.trim().isEmpty()) {
                String[] parts = assignedTo.split("[\r\n,]+");
                for (String p : parts) {
                    String pt = p.trim();
                    if (!pt.isEmpty() && !taskParticipants.contains(pt)) {
                        taskParticipants.add(pt);
                    }
                }
            }

            if (Boolean.TRUE.equals(doc.getBoolean("isAll"))) {
                isAll = true;
            }
        }

        participantCount = taskParticipants.size();
        updateSubtitleUI();
        updateTeamMembersUI();
    }

    private void loadCurrentUserInfo() {
        myIdentifiers.clear();

        // 1. Firebase Auth User
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            addIdentifier(currentUid);
            if (firebaseUser.getEmail() != null) {
                currentUserEmail = firebaseUser.getEmail().trim().toLowerCase(Locale.ROOT);
                addIdentifier(currentUserEmail);
            }
            if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentUserName = firebaseUser.getDisplayName().trim();
                addIdentifier(currentUserName);
            }
        }

        // 2. AuthManager session
        FacultyUser cachedUser = AuthManager.getInstance(this).getCurrentUser();
        if (cachedUser != null) {
            if (cachedUser.getUserId() != null && !cachedUser.getUserId().trim().isEmpty()) {
                currentUserId = cachedUser.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if (cachedUser.getName() != null && !cachedUser.getName().trim().isEmpty()) {
                currentUserName = cachedUser.getName().trim();
                addIdentifier(currentUserName);
            }
            if (cachedUser.getEmail() != null && !cachedUser.getEmail().trim().isEmpty()) {
                currentUserEmail = cachedUser.getEmail().trim().toLowerCase(Locale.ROOT);
                addIdentifier(currentUserEmail);
            }
            if (cachedUser.getRole() != null && !cachedUser.getRole().trim().isEmpty()) {
                currentUserRole = cachedUser.getRole().trim().toUpperCase(Locale.ROOT);
            }
        }

        // 3. SessionManager fallback
        FacultyUser sessionUser = SessionManager.getInstance(this).getCurrentUser();
        if (sessionUser != null) {
            if (currentUserId.isEmpty() && sessionUser.getUserId() != null && !sessionUser.getUserId().trim().isEmpty()) {
                currentUserId = sessionUser.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if ((currentUserName.isEmpty() || "Faculty".equals(currentUserName)) && sessionUser.getName() != null && !sessionUser.getName().trim().isEmpty()) {
                currentUserName = sessionUser.getName().trim();
                addIdentifier(currentUserName);
            }
            if (currentUserEmail.isEmpty() && sessionUser.getEmail() != null && !sessionUser.getEmail().trim().isEmpty()) {
                currentUserEmail = sessionUser.getEmail().trim().toLowerCase(Locale.ROOT);
                addIdentifier(currentUserEmail);
            }
            if (currentUserRole.isEmpty() && sessionUser.getRole() != null && !sessionUser.getRole().trim().isEmpty()) {
                currentUserRole = sessionUser.getRole().trim().toUpperCase(Locale.ROOT);
            }
        }

        // 4. Direct SharedPreferences fallback
        SharedPreferences sp = getSharedPreferences("deptflow_session_pref", Context.MODE_PRIVATE);
        if (currentUserId.isEmpty()) {
            currentUserId = sp.getString("user_id", "");
            if (!currentUserId.isEmpty()) addIdentifier(currentUserId);
        }
        if (currentUserName.isEmpty() || "Faculty".equals(currentUserName)) {
            currentUserName = sp.getString("user_name", "Faculty");
            if (!currentUserName.isEmpty()) addIdentifier(currentUserName);
        }
        if (currentUserEmail.isEmpty()) {
            currentUserEmail = sp.getString("user_email", "");
            if (!currentUserEmail.isEmpty()) addIdentifier(currentUserEmail);
        }
        String spRole = sp.getString("user_role", "");
        if (!spRole.isEmpty()) {
            currentUserRole = spRole.toUpperCase(Locale.ROOT);
        }

        // 5. Canonical resolution
        FacultyUser canonical = null;
        if (!currentUserName.isEmpty() && !"Faculty".equals(currentUserName)) {
            canonical = FacultyDirectory.resolveByNameOrId(currentUserName);
        }
        if (canonical == null && !currentUserId.isEmpty()) {
            canonical = FacultyDirectory.resolveByNameOrId(currentUserId);
        }
        if (canonical == null && !currentUserEmail.isEmpty()) {
            canonical = FacultyDirectory.resolveByNameOrId(currentUserEmail);
        }

        if (canonical != null) {
            currentCanonicalId = canonical.getUserId();
            currentUserName = canonical.getName();
            addIdentifier(currentCanonicalId);
            addIdentifier(canonical.getName());
            addIdentifier(canonical.getEmail());
        }

        Log.d(TAG, "Current user: UID=" + currentUid
                + ", CanonicalId=" + currentCanonicalId
                + ", Name=" + currentUserName
                + ", Role=" + currentUserRole);
    }

    private void addIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return;
        String trimmed = val.trim();
        myIdentifiers.add(trimmed);
        myIdentifiers.add(trimmed.toLowerCase(Locale.ROOT));

        String cleanTitle = trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(dr|mr|mrs|ms|prof)\\b[.]?", "")
                .trim();
        if (!cleanTitle.isEmpty()) {
            myIdentifiers.add(cleanTitle);
        }

        String clean = trimmed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (!clean.isEmpty()) {
            myIdentifiers.add(clean);
        }

        if (trimmed.contains("@")) {
            String prefix = trimmed.substring(0, trimmed.indexOf('@')).trim().toLowerCase(Locale.ROOT);
            if (!prefix.isEmpty()) {
                myIdentifiers.add(prefix);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new DiscussionChatAdapter(this, messageList, myIdentifiers);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvDiscussionMessages.setLayoutManager(layoutManager);
        rvDiscussionMessages.setAdapter(adapter);
    }

    private void listenForDiscussionMessages() {
        if (taskId == null || taskId.isEmpty()) return;

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        Log.d(TAG_READ, "Listener triggered");
        Log.d(TAG_READ, "taskId = " + taskId);

        // Read all messages under task_discussions/{taskId}/messages in real-time
        messageListener = db.collection("task_discussions")
                .document(taskId)
                .collection("messages")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        Log.e(TAG_READ, "LISTENER FAILED", error);
                        // STATE 2: Show error state
                        if (layoutEmptyChat != null) layoutEmptyChat.setVisibility(View.VISIBLE);
                        if (rvDiscussionMessages != null) rvDiscussionMessages.setVisibility(View.GONE);
                        if (tvEmptyChatTitle != null) tvEmptyChatTitle.setText("Could not load messages");
                        if (tvEmptyChatSubtitle != null) {
                            tvEmptyChatSubtitle.setText(error.getMessage() != null ? error.getMessage() : "Error loading team discussion");
                        }
                        return;
                    }

                    if (snapshots == null) {
                        Log.d(TAG_READ, "SNAPSHOT IS NULL");
                        return;
                    }

                    Log.d(TAG_READ, "MESSAGE COUNT = " + snapshots.size());

                    messageList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String msgId = doc.getId();
                        String senderId = doc.getString("senderId");
                        String senderUid = doc.getString("senderUid");
                        String senderCanonicalId = doc.getString("senderCanonicalId");
                        String senderName = doc.getString("senderName");
                        String senderRole = doc.getString("senderRole");
                        String text = doc.getString("message");
                        String status = doc.getString("status");
                        if (status == null || status.trim().isEmpty()) status = "sent";

                        List<String> deliveredTo = new ArrayList<>();
                        Object delObj = doc.get("deliveredTo");
                        if (delObj instanceof List) {
                            for (Object item : (List<?>) delObj) {
                                if (item != null) deliveredTo.add(item.toString().trim());
                            }
                        }

                        List<String> readBy = new ArrayList<>();
                        Object readObj = doc.get("readBy");
                        if (readObj instanceof List) {
                            for (Object item : (List<?>) readObj) {
                                if (item != null) readBy.add(item.toString().trim());
                            }
                        }

                        long timestamp = 0L;
                        Log.d(TAG_READ, "messageDocId = " + msgId);
                        Log.d(TAG_READ, "message = " + text);
                        Log.d(TAG_READ, "senderId = " + senderId);
                        Log.d(TAG_READ, "senderName = " + senderName);

                        Object tsObj = doc.get("timestamp");
                        if (tsObj instanceof Timestamp) {
                            timestamp = ((Timestamp) tsObj).toDate().getTime();
                        } else if (tsObj instanceof Number) {
                            timestamp = ((Number) tsObj).longValue();
                        } else if (tsObj instanceof Date) {
                            timestamp = ((Date) tsObj).getTime();
                        } else if (tsObj instanceof String) {
                            try {
                                timestamp = Long.parseLong(((String) tsObj).trim());
                            } catch (Exception ignored) {}
                        }

                        // Fallback timestamp handling so pending messages without server timestamp are NOT discarded
                        if (timestamp <= 0) {
                            Object createdObj = doc.get("createdAt");
                            if (createdObj instanceof Number) {
                                timestamp = ((Number) createdObj).longValue();
                            } else {
                                timestamp = System.currentTimeMillis();
                            }
                        }

                        if (text != null && !text.trim().isEmpty()) {
                            DiscussionMessage msg = new DiscussionMessage(
                                    msgId,
                                    senderId != null ? senderId : "",
                                    senderUid != null ? senderUid : "",
                                    senderCanonicalId != null ? senderCanonicalId : "",
                                    senderName != null ? senderName : "Faculty",
                                    senderRole != null ? senderRole : "Faculty",
                                    text,
                                    timestamp,
                                    status,
                                    deliveredTo,
                                    readBy
                            );
                            messageList.add(msg);

                            // Update delivery and read status for messages from other users
                            String myId = (currentUid != null && !currentUid.isEmpty()) ? currentUid : currentUserId;
                            if (myId != null && !myId.isEmpty() && !msg.isSentBy(myIdentifiers)) {
                                boolean isAlreadyRead = readBy.contains(myId);
                                boolean isAlreadyDelivered = deliveredTo.contains(myId);

                                if (!isAlreadyRead || !isAlreadyDelivered) {
                                    doc.getReference().update(
                                            "deliveredTo", FieldValue.arrayUnion(myId),
                                            "readBy", FieldValue.arrayUnion(myId),
                                            "status", "read"
                                    ).addOnSuccessListener(aVoid -> {
                                        Log.d(TAG_STATUS, "Message delivered to " + myId);
                                        Log.d(TAG_STATUS, "Message read by " + myId);
                                    }).addOnFailureListener(e -> {
                                        Log.w(TAG_STATUS, "Failed to update read status for " + msgId + ": " + e.getMessage());
                                    });
                                }
                            } else if (msg.isSentBy(myIdentifiers)) {
                                Log.d(TAG_STATUS, "Message " + msgId + " status updated");
                            }
                        }
                    }

                    // Sort ascending by timestamp in memory
                    Collections.sort(messageList, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                    if (adapter != null) {
                        adapter.setMessages(messageList);
                    }

                    Log.d(TAG_READ, "Adapter updated with " + messageList.size() + " messages");

                    if (messageList.isEmpty()) {
                        // STATE 1: Zero messages returned successfully
                        if (layoutEmptyChat != null) layoutEmptyChat.setVisibility(View.VISIBLE);
                        if (rvDiscussionMessages != null) rvDiscussionMessages.setVisibility(View.GONE);
                        if (tvEmptyChatTitle != null) tvEmptyChatTitle.setText("No messages yet");
                        if (tvEmptyChatSubtitle != null) tvEmptyChatSubtitle.setText("Start the discussion for this task.");
                    } else {
                        // STATE 3: Messages exist
                        if (layoutEmptyChat != null) layoutEmptyChat.setVisibility(View.GONE);
                        if (rvDiscussionMessages != null) {
                            rvDiscussionMessages.setVisibility(View.VISIBLE);
                            rvDiscussionMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void setupSendButton() {
        if (btnDiscussionSend != null) {
            btnDiscussionSend.setOnClickListener(v -> {
                Log.d(TAG_SEND, "SEND BUTTON CLICKED");
                sendMessage();
            });
        }
    }

    private void sendMessage() {
        if (isSending) return;

        if (etDiscussionMessage == null) {
            Log.e(TAG_SEND, "EDITTEXT IS NULL");
            return;
        }

        String message = etDiscussionMessage.getText().toString().trim();
        Log.d(TAG_SEND, "message = " + message);

        if (message.isEmpty()) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG_SEND, "AUTH USER IS NULL");
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        currentUid = uid;
        Log.d(TAG_SEND, "firebaseUid = " + currentUid);

        if (taskId == null || taskId.trim().isEmpty()) {
            Log.e(TAG_SEND, "TASK ID IS NULL");
            Toast.makeText(this, "Task ID missing", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG_SEND, "taskId = " + taskId);

        isSending = true;
        if (btnDiscussionSend != null) btnDiscussionSend.setEnabled(false);

        String senderName = currentUserName;
        if (senderName == null || senderName.trim().isEmpty()) {
            senderName = user.getDisplayName();
            if (senderName == null || senderName.trim().isEmpty()) {
                senderName = "Faculty";
            }
        }
        senderName = senderName.trim();

        Map<String, Object> data = new HashMap<>();
        data.put("senderId", uid);
        data.put("senderUid", uid);
        data.put("senderCanonicalId", currentCanonicalId != null ? currentCanonicalId : "");
        data.put("senderName", senderName);
        data.put("senderRole", currentUserRole != null ? currentUserRole : "FACULTY");
        data.put("message", message);
        data.put("timestamp", FieldValue.serverTimestamp());
        data.put("createdAt", System.currentTimeMillis());
        data.put("status", "sent");
        data.put("deliveredTo", new ArrayList<String>());
        data.put("readBy", new ArrayList<String>());

        Log.d(TAG_SEND, "Writing to task_discussions/" + taskId + "/messages");

        db.collection("task_discussions")
                .document(taskId)
                .collection("messages")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    isSending = false;
                    if (btnDiscussionSend != null) btnDiscussionSend.setEnabled(true);
                    etDiscussionMessage.setText("");

                    Log.d(TAG_SEND, "MESSAGE SENT SUCCESSFULLY");
                    Log.d(TAG_SEND, "messageId = " + documentReference.getId());

                    // Asynchronously update parent discussion metadata
                    try {
                        Map<String, Object> parentDoc = new HashMap<>();
                        parentDoc.put("taskId", taskId);
                        parentDoc.put("taskTitle", taskTitle);
                        parentDoc.put("lastMessage", message);
                        parentDoc.put("lastSenderName", currentUserName);
                        parentDoc.put("lastUpdated", FieldValue.serverTimestamp());
                        parentDoc.put("isAll", isAll);
                        if (taskParticipants != null && !taskParticipants.isEmpty()) {
                            parentDoc.put("participants", new ArrayList<>(taskParticipants));
                        }

                        db.collection("task_discussions")
                                .document(taskId)
                                .set(parentDoc, SetOptions.merge());
                    } catch (Exception e) {
                        Log.w(TAG_SEND, "Parent metadata merge error: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    isSending = false;
                    if (btnDiscussionSend != null) btnDiscussionSend.setEnabled(true);

                    Log.e(TAG_SEND, "MESSAGE SEND FAILED", e);
                    Toast.makeText(this, "Message failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
    }
}