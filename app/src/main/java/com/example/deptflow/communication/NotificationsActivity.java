package com.example.deptflow.communication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.NotificationAdapter;
import com.example.deptflow.communication.models.AppNotification;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.communication.services.FcmTokenManager;
import com.example.deptflow.communication.services.InAppBannerManager;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.TaskDetailsActivity;
import com.example.deptflow.feature.faculty.MyTasksActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * NotificationsActivity — Displays real-time recipient-specific notifications for:
 * 1. Incoming faculty-to-faculty chat messages
 * 2. HOD task assignments
 * 3. Team task discussion updates
 * 4. Approaching deadline reminders
 *
 * Supports navigation: [Open Chat], [View Task], [Mark Read], [Dismiss].
 */
public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationsActivity";
    private static final String PREF_NOTIFS = "deptflow_notifications_pref";

    private RecyclerView rvNotifications;
    private ProgressBar pbLoadingNotifications;
    private View layoutEmptyNotifications;
    private ImageButton ibBackNotifications;
    private TextView btnMarkAllRead;

    private NotificationAdapter adapter;

    private final List<AppNotification> notificationList = new ArrayList<>();
    private final Map<String, AppNotification> taskNotificationsMap = new LinkedHashMap<>();
    private final Map<String, AppNotification> chatNotificationsMap = new LinkedHashMap<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration tasksListener;
    private ListenerRegistration notifsCollectionListener;

    private SharedPreferences prefs;

    private String currentUid = "";
    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentName = "";
    private String currentUserRole = "FACULTY";
    private final Set<String> myIdentifiers = new HashSet<>();

    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        prefs = getSharedPreferences(PREF_NOTIFS, MODE_PRIVATE);

        initViews();
        loadCurrentUserInfo();
        setupRecyclerView();

        if (currentUid.isEmpty() && currentUserId.isEmpty() && currentCanonicalId.isEmpty()) {
            pbLoadingNotifications.setVisibility(View.GONE);
            showEmptyState();
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_LONG).show();
            return;
        }

        FcmTokenManager.registerDeviceToken(this);

        listenToTaskNotifications();
        listenToNotificationsCollection();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        pbLoadingNotifications = findViewById(R.id.pbLoadingNotifications);
        layoutEmptyNotifications = findViewById(R.id.layoutEmptyNotifications);
        ibBackNotifications = findViewById(R.id.ibBackNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        ibBackNotifications.setOnClickListener(v -> finish());
        btnMarkAllRead.setOnClickListener(v -> markAllNotificationsAsRead());
    }

    private void loadCurrentUserInfo() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        myIdentifiers.clear();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            addIdentifier(currentUid);
            if (firebaseUser.getEmail() != null) {
                addIdentifier(firebaseUser.getEmail());
            }
            if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentName = firebaseUser.getDisplayName().trim();
                addIdentifier(currentName);
            }
        }

        FacultyUser cachedUser = AuthManager.getInstance(this).getCurrentUser();
        if (cachedUser != null) {
            if (cachedUser.getUserId() != null && !cachedUser.getUserId().trim().isEmpty()) {
                currentUserId = cachedUser.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if (cachedUser.getName() != null && !cachedUser.getName().trim().isEmpty()) {
                currentName = cachedUser.getName().trim();
                addIdentifier(currentName);
            }
            if (cachedUser.getEmail() != null && !cachedUser.getEmail().trim().isEmpty()) {
                addIdentifier(cachedUser.getEmail());
            }
            if (cachedUser.getRole() != null && !cachedUser.getRole().trim().isEmpty()) {
                currentUserRole = cachedUser.getRole().trim().toUpperCase();
            }

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(cachedUser);
            if (canonical != null) {
                currentCanonicalId = canonical.getUserId();
                addIdentifier(currentCanonicalId);
                addIdentifier(canonical.getName());
                addIdentifier(canonical.getEmail());
            }
        }

        if (currentCanonicalId.isEmpty() && !currentName.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentName);
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

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(this, notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private Set<String> getReadNotificationIds() {
        String key = "read_ids_" + (!currentCanonicalId.isEmpty() ? currentCanonicalId : currentUserId);
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    private void saveReadNotificationId(String id) {
        if (id == null || id.trim().isEmpty()) return;
        Set<String> readSet = getReadNotificationIds();
        readSet.add(id);

        String key = "read_ids_" + (!currentCanonicalId.isEmpty() ? currentCanonicalId : currentUserId);
        prefs.edit().putStringSet(key, readSet).apply();
    }

    private Set<String> getDismissedIds() {
        String key = "dismissed_ids_" + (!currentCanonicalId.isEmpty() ? currentCanonicalId : currentUserId);
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    private void saveDismissedId(String id) {
        if (id == null || id.trim().isEmpty()) return;
        Set<String> set = getDismissedIds();
        set.add(id);
        String key = "dismissed_ids_" + (!currentCanonicalId.isEmpty() ? currentCanonicalId : currentUserId);
        prefs.edit().putStringSet(key, set).apply();
    }

    private void markAllNotificationsAsRead() {
        if (notificationList.isEmpty()) {
            Toast.makeText(this, "No notifications to mark", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> readSet = getReadNotificationIds();
        for (AppNotification item : notificationList) {
            readSet.add(item.getId());
            item.setRead(true);

            if (AppNotification.TYPE_CHAT.equalsIgnoreCase(item.getType())) {
                db.collection("notifications").document(item.getId())
                        .update("isRead", true);
            }
        }

        String key = "read_ids_" + (!currentCanonicalId.isEmpty() ? currentCanonicalId : currentUserId);
        prefs.edit().putStringSet(key, readSet).apply();

        adapter.markAllAsRead();
        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
    }

    private void listenToTaskNotifications() {
        pbLoadingNotifications.setVisibility(View.VISIBLE);

        tasksListener = db.collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    runOnUiThread(() -> pbLoadingNotifications.setVisibility(View.GONE));

                    if (error != null) {
                        Log.e(TAG, "Task assignment listener failed", error);
                        refreshNotificationsList();
                        return;
                    }

                    if (snapshots == null) {
                        refreshNotificationsList();
                        return;
                    }

                    Set<String> readSet = getReadNotificationIds();
                    Set<String> dismissedSet = getDismissedIds();
                    boolean isHod = "HOD".equalsIgnoreCase(currentUserRole);

                    taskNotificationsMap.clear();

                    Map<String, DocumentSnapshot> uniqueTasks = new LinkedHashMap<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String groupId = getString(doc, "groupTaskId");
                        if (groupId.isEmpty()) groupId = getString(doc, "taskId");
                        if (groupId.isEmpty()) groupId = getString(doc, "id");
                        if (groupId.isEmpty()) groupId = doc.getId();

                        if (!uniqueTasks.containsKey(groupId)) {
                            uniqueTasks.put(groupId, doc);
                        }
                    }

                    for (Map.Entry<String, DocumentSnapshot> entry : uniqueTasks.entrySet()) {
                        String groupId = entry.getKey();
                        DocumentSnapshot doc = entry.getValue();

                        String title = getString(doc, "taskTitle");
                        if (title.isEmpty()) title = getString(doc, "title");
                        if (title.isEmpty()) title = "Assigned Task";

                        String description = getString(doc, "description");
                        String deadline = getString(doc, "deadline");
                        String priority = getString(doc, "priority");
                        if (priority.isEmpty()) priority = "MEDIUM";
                        String status = getString(doc, "status");

                        List<String> assignedNames = getStringList(doc, "allAssignedFaculty");
                        addIfMissing(assignedNames, getString(doc, "assignedTo"));
                        addIfMissing(assignedNames, getString(doc, "assignedFaculty"));
                        addIfMissing(assignedNames, getString(doc, "faculty"));

                        List<String> assignedUids = getStringList(doc, "assignedFacultyUids");

                        boolean assignedToCurrentUser = isAssignedToCurrentUser(doc, assignedNames, assignedUids);

                        if (!isHod && !assignedToCurrentUser) {
                            continue;
                        }

                        long timestamp = getTimestamp(doc);
                        String notificationId = "task_" + groupId;

                        if (dismissedSet.contains(notificationId)) {
                            continue;
                        }

                        boolean isRead = readSet.contains(notificationId);

                        String message = description;
                        if (!status.isEmpty()) {
                            if (!message.isEmpty()) message += "\n";
                            message += "Status: " + status;
                        }

                        AppNotification notification = new AppNotification(
                                notificationId,
                                AppNotification.TYPE_TASK,
                                "Task Assignment",
                                title,
                                message,
                                deadline,
                                priority,
                                timestamp,
                                "HOD (Department Head)",
                                groupId,
                                isRead
                        );

                        taskNotificationsMap.put(notificationId, notification);

                        // If not first load and unread, trigger in-app popup
                        if (!isFirstLoad && !isRead) {
                            InAppBannerManager.showBanner(NotificationsActivity.this, notification);
                        }
                    }

                    refreshNotificationsList();
                });
    }

    private void listenToNotificationsCollection() {
        if (notifsCollectionListener != null) {
            notifsCollectionListener.remove();
        }

        notifsCollectionListener = db.collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed() || snapshots == null || error != null) {
                        if (error != null) Log.e(TAG, "Error in notifications listener: " + error.getMessage());
                        return;
                    }

                    Set<String> readSet = getReadNotificationIds();
                    Set<String> dismissedSet = getDismissedIds();

                    chatNotificationsMap.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String notifId = doc.getId();
                        if (dismissedSet.contains(notifId)) continue;

                        String recId = getString(doc, "recipientId");
                        String recFacId = getString(doc, "recipientFacultyId");
                        String recUid = getString(doc, "recipientUid");
                        String recName = getString(doc, "recipientName");

                        boolean isForMe = isMyIdentifier(recId)
                                || isMyIdentifier(recFacId)
                                || isMyIdentifier(recUid)
                                || isMyIdentifier(recName);

                        if (!isForMe) continue;

                        String senderId = getString(doc, "senderId");
                        String senderUid = getString(doc, "senderUid");
                        String senderName = getString(doc, "senderName");
                        if (senderName.isEmpty()) senderName = getString(doc, "sender");
                        if (senderName.isEmpty()) senderName = "Faculty";

                        // Discard self-sent notifications
                        if (isMyIdentifier(senderId) || isMyIdentifier(senderUid) || isMyIdentifier(senderName)) {
                            continue;
                        }

                        String notifType = getString(doc, "type");
                        if (notifType.isEmpty()) notifType = AppNotification.TYPE_CHAT;

                        String title = getString(doc, "title");
                        if (title.isEmpty()) title = "Faculty Message";

                        String subtitle = getString(doc, "subtitle");
                        if (subtitle.isEmpty()) subtitle = "New message from " + senderName;

                        String messagePreview = getString(doc, "messagePreview");
                        if (messagePreview.isEmpty()) messagePreview = getString(doc, "message");

                        String chatId = getString(doc, "chatId");
                        if (chatId.isEmpty()) chatId = getString(doc, "conversationId");
                        String messageId = getString(doc, "messageId");
                        long timestamp = getTimestamp(doc);

                        boolean isRead = Boolean.TRUE.equals(doc.getBoolean("isRead")) || readSet.contains(notifId);

                        AppNotification notification = new AppNotification(
                                notifId,
                                notifType,
                                title,
                                subtitle,
                                messagePreview,
                                timestamp,
                                senderName,
                                senderId,
                                currentCanonicalId,
                                chatId,
                                messageId,
                                isRead
                        );

                        chatNotificationsMap.put(notifId, notification);

                        // If unread and not initial load, show in-app banner
                        if (!isFirstLoad && !isRead) {
                            InAppBannerManager.showBanner(NotificationsActivity.this, notification);
                        }
                    }

                    refreshNotificationsList();
                    isFirstLoad = false;
                });
    }

    private boolean isMyIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return false;
        String normal = val.trim().toLowerCase(Locale.ROOT);
        return myIdentifiers.contains(normal)
                || normal.equalsIgnoreCase(currentCanonicalId)
                || normal.equalsIgnoreCase(currentUserId)
                || normal.equalsIgnoreCase(currentUid)
                || normal.equalsIgnoreCase(currentName);
    }

    private boolean isAssignedToCurrentUser(DocumentSnapshot doc, List<String> assignedNames, List<String> assignedUids) {
        for (String uid : assignedUids) {
            if (isMyIdentifier(uid)) return true;
        }

        String assignedTo = getString(doc, "assignedTo");
        if (isMyIdentifier(assignedTo)) return true;

        String faculty = getString(doc, "faculty");
        if (isMyIdentifier(faculty)) return true;

        for (String name : assignedNames) {
            if (isMyIdentifier(name)) return true;
            FacultyUser resolved = FacultyDirectory.resolveByNameOrId(name);
            if (resolved != null && isMyIdentifier(resolved.getUserId())) return true;
        }

        return false;
    }

    private void refreshNotificationsList() {
        notificationList.clear();
        notificationList.addAll(chatNotificationsMap.values());
        notificationList.addAll(taskNotificationsMap.values());

        // Sort by timestamp descending
        Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (notificationList.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        layoutEmptyNotifications.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmptyNotifications.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNotificationClick(AppNotification notification) {
        if (notification == null) return;

        saveReadNotificationId(notification.getId());
        notification.setRead(true);
        if (adapter != null) adapter.notifyDataSetChanged();

        if (AppNotification.TYPE_CHAT.equalsIgnoreCase(notification.getType())) {
            // Open private chat
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("facultyName", notification.getSender());
            intent.putExtra("facultyId", notification.getSenderId());
            intent.putExtra("chatId", notification.getChatId());
            startActivity(intent);
        } else {
            // Open existing Faculty Dashboard / My Task / Task Details Activity (Problem 4 / Step 6 / Step 7)
            String taskId = notification.getTaskId();
            if (taskId != null && !taskId.trim().isEmpty()) {
                Intent intent = new Intent(this, TaskDetailsActivity.class);
                intent.putExtra(TaskDetailsActivity.EXTRA_TASK_ID, taskId.trim());
                intent.putExtra("taskId", taskId.trim());
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, MyTasksActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onNotificationDismiss(AppNotification notification) {
        if (notification == null) return;
        saveDismissedId(notification.getId());
        notificationList.remove(notification);
        taskNotificationsMap.remove(notification.getId());
        chatNotificationsMap.remove(notification.getId());

        if (adapter != null) adapter.notifyDataSetChanged();
        if (notificationList.isEmpty()) showEmptyState();
    }

    @Override
    public void onNotificationMarkRead(AppNotification notification) {
        if (notification == null) return;
        saveReadNotificationId(notification.getId());
        notification.setRead(true);

        if (AppNotification.TYPE_CHAT.equalsIgnoreCase(notification.getType())) {
            db.collection("notifications").document(notification.getId())
                    .update("isRead", true);
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private String getString(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        return val != null ? val.toString().trim() : "";
    }

    private List<String> getStringList(DocumentSnapshot doc, String field) {
        List<String> list = new ArrayList<>();
        Object val = doc.get(field);
        if (val instanceof List<?>) {
            for (Object item : (List<?>) val) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    list.add(item.toString().trim());
                }
            }
        }
        return list;
    }

    private void addIfMissing(List<String> list, String val) {
        if (val != null && !val.trim().isEmpty() && !list.contains(val.trim())) {
            list.add(val.trim());
        }
    }

    private long getTimestamp(DocumentSnapshot doc) {
        Object val = doc.get("timestamp");
        if (val instanceof Timestamp) {
            return ((Timestamp) val).toDate().getTime();
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof Date) {
            return ((Date) val).getTime();
        }
        if (val instanceof String) {
            try {
                return Long.parseLong(((String) val).trim());
            } catch (Exception ignored) {}
        }
        return System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) tasksListener.remove();
        if (notifsCollectionListener != null) notifsCollectionListener.remove();
    }
}