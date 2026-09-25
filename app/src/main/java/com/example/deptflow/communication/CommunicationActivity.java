package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

/**
 * CommunicationActivity — Central navigation hub for the DeptFlow Communication Module.
 * Connects Faculty Chat, Task Discussions, Notifications, and Task Reminders
 * with live badge status counters.
 */
public class CommunicationActivity extends AppCompatActivity {

    private ImageButton ibBackComm;
    private TextView tvCommWelcome;
    private TextView tvCommDeptBadge;

    private MaterialCardView cardFacultyChat;
    private MaterialCardView cardTaskDiscussion;
    private MaterialCardView cardNotifications;
    private MaterialCardView cardReminders;
    private MaterialCardView cardBroadcast;

    private TextView badgeChatUnread;
    private TextView badgeTaskCount;
    private TextView badgeNotifCount;
    private TextView badgeRemindersCount;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration chatsListener;
    private ListenerRegistration tasksListener;
    private ListenerRegistration notifsListener;
    private ListenerRegistration remindersListener;

    private String currentCanonicalId = "";
    private String currentUserId = "";
    private String currentUid = "";
    private String currentUserName = "Faculty";
    private String currentUserRole = "FACULTY";
    private final java.util.Set<String> myIdentifiers = new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        initViews();
        loadUserInfo();
        setupClickListeners();
        listenToSummaryCounts();
    }

    private void initViews() {
        ibBackComm = findViewById(R.id.ibBackComm);
        tvCommWelcome = findViewById(R.id.tvCommWelcome);
        tvCommDeptBadge = findViewById(R.id.tvCommDeptBadge);

        cardFacultyChat = findViewById(R.id.cardFacultyChat);
        cardTaskDiscussion = findViewById(R.id.cardTaskDiscussion);
        cardNotifications = findViewById(R.id.cardNotifications);
        cardReminders = findViewById(R.id.cardReminders);
        cardBroadcast = findViewById(R.id.cardBroadcast);

        badgeChatUnread = findViewById(R.id.badgeChatUnread);
        badgeTaskCount = findViewById(R.id.badgeTaskCount);
        badgeNotifCount = findViewById(R.id.badgeNotifCount);
        badgeRemindersCount = findViewById(R.id.badgeRemindersCount);

        if (ibBackComm != null) {
            ibBackComm.setOnClickListener(v -> finish());
        }
    }

    private void loadUserInfo() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        myIdentifiers.clear();

        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            currentUid = fbUser.getUid();
            addIdentifier(currentUid);
            if (fbUser.getEmail() != null) addIdentifier(fbUser.getEmail());
            if (fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                currentUserName = fbUser.getDisplayName().trim();
                addIdentifier(currentUserName);
            }
        }

        FacultyUser sessionUser = AuthManager.getInstance(this).getCurrentUser();
        if (sessionUser != null) {
            if (sessionUser.getUserId() != null && !sessionUser.getUserId().trim().isEmpty()) {
                currentUserId = sessionUser.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if (sessionUser.getName() != null && !sessionUser.getName().trim().isEmpty()) {
                currentUserName = sessionUser.getName().trim();
                addIdentifier(currentUserName);
            }
            if (sessionUser.getEmail() != null && !sessionUser.getEmail().trim().isEmpty()) {
                addIdentifier(sessionUser.getEmail());
            }
            if (sessionUser.getRole() != null && !sessionUser.getRole().trim().isEmpty()) {
                currentUserRole = sessionUser.getRole().trim().toUpperCase();
            }
            if (sessionUser.getDepartment() != null && !sessionUser.getDepartment().trim().isEmpty()) {
                if (tvCommDeptBadge != null) tvCommDeptBadge.setText(sessionUser.getDepartment());
            }

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(sessionUser);
            if (canonical != null) {
                currentCanonicalId = canonical.getUserId();
                currentUserName = canonical.getName();
                addIdentifier(currentCanonicalId);
                addIdentifier(canonical.getName());
                addIdentifier(canonical.getEmail());
            }
        }

        if (currentCanonicalId.isEmpty() && !currentUserName.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserName);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }

        if (currentCanonicalId.isEmpty() && !currentUserId.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserId);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }

        if (currentCanonicalId.isEmpty() && !currentUid.isEmpty()) {
            currentCanonicalId = currentUid;
            addIdentifier(currentCanonicalId);
        }

        if (tvCommWelcome != null) {
            tvCommWelcome.setText("Welcome, " + currentUserName);
        }
    }

    private void addIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return;
        String trimmed = val.trim();
        myIdentifiers.add(trimmed.toLowerCase(Locale.ROOT));
        myIdentifiers.add(trimmed);
        String clean = trimmed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (!clean.isEmpty()) {
            myIdentifiers.add(clean);
        }
    }

    private boolean isMyIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return false;
        String normal = val.trim().toLowerCase(Locale.ROOT);
        return myIdentifiers.contains(normal)
                || normal.equalsIgnoreCase(currentCanonicalId)
                || normal.equalsIgnoreCase(currentUserId)
                || normal.equalsIgnoreCase(currentUid)
                || normal.equalsIgnoreCase(currentUserName);
    }

    private void setupClickListeners() {
        // Faculty Chat Action
        if (cardFacultyChat != null) {
            cardFacultyChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, FacultyListActivity.class);
                startActivity(intent);
            });
        }

        // Task Discussion Action
        if (cardTaskDiscussion != null) {
            cardTaskDiscussion.setOnClickListener(v -> {
                Intent intent = new Intent(this, TaskDiscussionActivity.class);
                startActivity(intent);
            });
        }

        // Notifications Action
        if (cardNotifications != null) {
            cardNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationsActivity.class);
                startActivity(intent);
            });
        }

        // Reminders Action
        if (cardReminders != null) {
            cardReminders.setOnClickListener(v -> {
                Intent intent = new Intent(this, RemindersActivity.class);
                startActivity(intent);
            });
        }

        // Broadcast Message Action
        if (cardBroadcast != null) {
            cardBroadcast.setOnClickListener(v -> {
                Intent intent = new Intent(this, BroadcastSelectActivity.class);
                startActivity(intent);
            });
        }
    }

    private void listenToSummaryCounts() {
        if (db == null) return;

        // 1. Live Chat Unread Count
        chatsListener = db.collection("chats").addSnapshotListener((snapshots, error) -> {
            if (snapshots != null) {
                int totalUnread = 0;
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                    Long count1 = !currentCanonicalId.isEmpty() ? doc.getLong("unreadCount_" + currentCanonicalId) : null;
                    Long count2 = !currentUserId.isEmpty() ? doc.getLong("unreadCount_" + currentUserId) : null;
                    Long count3 = !currentUid.isEmpty() ? doc.getLong("unreadCount_" + currentUid) : null;
                    Long count4 = !currentUserName.isEmpty() ? doc.getLong("unreadCount_" + currentUserName.toLowerCase(Locale.ROOT)) : null;

                    if (count1 != null && count1 > 0) totalUnread += count1.intValue();
                    else if (count2 != null && count2 > 0) totalUnread += count2.intValue();
                    else if (count3 != null && count3 > 0) totalUnread += count3.intValue();
                    else if (count4 != null && count4 > 0) totalUnread += count4.intValue();
                }

                if (badgeChatUnread != null) {
                    if (totalUnread > 0) {
                        badgeChatUnread.setVisibility(View.VISIBLE);
                        badgeChatUnread.setText(totalUnread + " NEW");
                    } else {
                        badgeChatUnread.setVisibility(View.GONE);
                    }
                }
            }
        });

        // 2. Task Discussions Count (Filter accurately for assigned tasks only)
        tasksListener = db.collection("task_assignments").addSnapshotListener((snapshots, error) -> {
            if (snapshots != null) {
                boolean isHod = "HOD".equalsIgnoreCase(currentUserRole);
                java.util.Set<String> assignedGroupIds = new java.util.HashSet<>();

                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                    String groupId = doc.getString("groupTaskId");
                    if (groupId == null || groupId.trim().isEmpty()) groupId = doc.getString("taskId");
                    if (groupId == null || groupId.trim().isEmpty()) groupId = doc.getString("id");
                    if (groupId == null || groupId.trim().isEmpty()) groupId = doc.getId();

                    if (isHod || isDocumentAssignedToMe(doc)) {
                        assignedGroupIds.add(groupId);
                    }
                }

                int count = assignedGroupIds.size();
                if (badgeTaskCount != null) {
                    badgeTaskCount.setText(count + (count == 1 ? " Task" : " Tasks"));
                }
            }
        });

        // 3. Unread Notifications Count
        notifsListener = db.collection("notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null) {
                        int unread = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            String recId = doc.getString("recipientId");
                            String recUid = doc.getString("recipientUid");
                            String recName = doc.getString("recipientName");

                            boolean mine = isMyIdentifier(recId) || isMyIdentifier(recUid) || isMyIdentifier(recName);
                            if (mine) unread++;
                        }

                        if (badgeNotifCount != null) {
                            badgeNotifCount.setText(unread + " New");
                        }
                    }
                });

        // 4. Pending Reminders Count
        remindersListener = db.collection("faculty_reminders")
                .whereEqualTo("isCompleted", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null) {
                        int pending = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            String fId = doc.getString("facultyId");
                            String fUid = doc.getString("facultyUid");
                            String fName = doc.getString("facultyName");

                            boolean mine = isMyIdentifier(fId) || isMyIdentifier(fUid) || isMyIdentifier(fName);
                            if (mine) pending++;
                        }

                        if (badgeRemindersCount != null) {
                            badgeRemindersCount.setText(pending + " Due");
                        }
                    }
                });
    }

    private boolean isDocumentAssignedToMe(com.google.firebase.firestore.DocumentSnapshot doc) {
        if (doc == null) return false;

        // Check assignedTo
        String assignedTo = doc.getString("assignedTo");
        if (isMyIdentifier(assignedTo)) return true;

        // Check faculty
        String faculty = doc.getString("faculty");
        if (isMyIdentifier(faculty)) return true;

        // Check assignedFacultyUids list
        Object uidsObj = doc.get("assignedFacultyUids");
        if (uidsObj instanceof java.util.List) {
            for (Object item : (java.util.List<?>) uidsObj) {
                if (item != null && isMyIdentifier(item.toString())) return true;
            }
        }

        // Check allAssignedFaculty / assignedFaculty list
        Object facListObj = doc.get("allAssignedFaculty");
        if (facListObj == null) facListObj = doc.get("assignedFaculty");
        if (facListObj instanceof java.util.List) {
            for (Object item : (java.util.List<?>) facListObj) {
                if (item != null) {
                    String name = item.toString();
                    if (isMyIdentifier(name)) return true;
                    FacultyUser resolved = FacultyDirectory.resolveByNameOrId(name);
                    if (resolved != null && isMyIdentifier(resolved.getUserId())) return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) chatsListener.remove();
        if (tasksListener != null) tasksListener.remove();
        if (notifsListener != null) notifsListener.remove();
        if (remindersListener != null) remindersListener.remove();
    }
}