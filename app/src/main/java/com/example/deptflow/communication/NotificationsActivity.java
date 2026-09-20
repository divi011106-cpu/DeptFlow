
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.NotificationAdapter;
import com.example.deptflow.communication.models.AppNotification;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationsActivity extends AppCompatActivity
        implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationsActivity";
    private static final String PREF_NOTIFS = "deptflow_notifications_pref";

    private static final List<String> DEPARTMENT_FACULTY = Arrays.asList(
            "Dr. R. Vijayalakshmi",
            "Dr. R. Raja Sudharsan",
            "Dr. K. M. Alaaudeen",
            "Dr. T. Saranya",
            "Mrs. M. Prabha",
            "Mrs. P. Saraswathi",
            "Mr. S. Jegadeesan",
            "Mrs. A. Meena",
            "Dr. T. Venkatesh Kanna",
            "Mrs. M. Ishvarya",
            "Mrs. R. Nancy Deborah",
            "Mrs. C. Manjula Devi",
            "Mrs. A. Vinora",
            "Mr. A. Srinivasan",
            "Mr. P. KalyanaKumar",
            "Ms. G. Sivakarthi",
            "Mrs. M. Soundarya",
            "Mrs. J. John Shiny",
            "Mr. R. Umesh",
            "Mrs. A. Periya Nayaki",
            "Mrs. A. Elavarasi",
            "Dr. S. Esakki Muthu",
            "Mr. K. Loganathan"
    );

    private RecyclerView rvNotifications;
    private ProgressBar pbLoadingNotifications;
    private View layoutEmptyNotifications;
    private ImageButton ibBackNotifications;
    private TextView btnMarkAllRead;

    private NotificationAdapter adapter;

    private final List<AppNotification> notificationList = new ArrayList<>();
    private final Map<String, AppNotification> taskNotificationsMap =
            new LinkedHashMap<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration tasksListener;

    private SharedPreferences prefs;

    private String currentUid = "";
    private String currentName = "";
    private String currentUserRole = "FACULTY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        prefs = getSharedPreferences(PREF_NOTIFS, MODE_PRIVATE);

        initViews();
        loadCurrentUserInfo();
        setupRecyclerView();

        if (currentUid.isEmpty()) {
            pbLoadingNotifications.setVisibility(View.GONE);
            showEmptyState();
            Toast.makeText(this,
                    "Please log in to view notifications",
                    Toast.LENGTH_LONG).show();
            return;
        }

        listenToTaskNotifications();
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

        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();

            if (firebaseUser.getDisplayName() != null
                    && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentName = firebaseUser.getDisplayName().trim();
            }
        }

        FacultyUser cachedUser =
                AuthManager.getInstance(this).getCurrentUser();

        if (cachedUser != null) {
            if (cachedUser.getName() != null
                    && !cachedUser.getName().trim().isEmpty()) {
                currentName = cachedUser.getName().trim();
            }

            if (cachedUser.getRole() != null
                    && !cachedUser.getRole().trim().isEmpty()) {
                currentUserRole = cachedUser.getRole()
                        .trim().toUpperCase();
            }
        }

        Log.d(TAG, "User UID=" + currentUid
                + ", name=" + currentName
                + ", role=" + currentUserRole);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(
                this, notificationList, this);

        rvNotifications.setLayoutManager(
                new LinearLayoutManager(this));

        rvNotifications.setAdapter(adapter);
    }

    private Set<String> getReadNotificationIds() {
        return new HashSet<>(
                prefs.getStringSet(
                        "read_ids_" + currentUid,
                        new HashSet<>()));
    }

    private void saveReadNotificationId(String id) {
        if (id == null || id.trim().isEmpty()) return;

        Set<String> readSet = getReadNotificationIds();
        readSet.add(id);

        prefs.edit()
                .putStringSet("read_ids_" + currentUid, readSet)
                .apply();
    }

    private void markAllNotificationsAsRead() {
        if (notificationList.isEmpty()) {
            Toast.makeText(this,
                    "No notifications to mark",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> readSet = getReadNotificationIds();

        for (AppNotification item : notificationList) {
            readSet.add(item.getId());
            item.setRead(true);
        }

        prefs.edit()
                .putStringSet("read_ids_" + currentUid, readSet)
                .apply();

        adapter.markAllAsRead();

        Toast.makeText(this,
                "All notifications marked as read",
                Toast.LENGTH_SHORT).show();
    }

    private void listenToTaskNotifications() {
        pbLoadingNotifications.setVisibility(View.VISIBLE);

        // IMPORTANT: Use the actual collection name.
        tasksListener = db.collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    pbLoadingNotifications.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG,
                                "Task assignment listener failed",
                                error);

                        Toast.makeText(this,
                                "Unable to load task notifications. Check Firestore permissions.",
                                Toast.LENGTH_LONG).show();

                        showEmptyState();
                        return;
                    }

                    if (snapshots == null) {
                        showEmptyState();
                        return;
                    }

                    Set<String> readSet = getReadNotificationIds();
                    boolean isHod =
                            "HOD".equalsIgnoreCase(currentUserRole);

                    taskNotificationsMap.clear();

                    // Group repeated assignment documents by groupTaskId.
                    Map<String, DocumentSnapshot> uniqueTasks =
                            new LinkedHashMap<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String groupId = getString(doc, "groupTaskId");

                        if (groupId.isEmpty()) {
                            groupId = getString(doc, "taskId");
                        }

                        if (groupId.isEmpty()) {
                            groupId = getString(doc, "id");
                        }

                        if (groupId.isEmpty()) {
                            groupId = doc.getId();
                        }

                        // Keep one card per group task.
                        if (!uniqueTasks.containsKey(groupId)) {
                            uniqueTasks.put(groupId, doc);
                        }
                    }

                    for (Map.Entry<String, DocumentSnapshot> entry
                            : uniqueTasks.entrySet()) {

                        String groupId = entry.getKey();
                        DocumentSnapshot doc = entry.getValue();

                        String title = getString(doc, "taskTitle");

                        if (title.isEmpty()) {
                            title = getString(doc, "title");
                        }

                        if (title.isEmpty()) {
                            title = "Assigned Task";
                        }

                        String description =
                                getString(doc, "description");

                        String deadline =
                                getString(doc, "deadline");

                        String priority =
                                getString(doc, "priority");

                        if (priority.isEmpty()) {
                            priority = "MEDIUM";
                        }

                        String status =
                                getString(doc, "status");

                        List<String> assignedNames =
                                getStringList(doc, "allAssignedFaculty");

                        addIfMissing(assignedNames,
                                getString(doc, "assignedTo"));

                        addIfMissing(assignedNames,
                                getString(doc, "assignedFaculty"));

                        addIfMissing(assignedNames,
                                getString(doc, "faculty"));

                        List<String> assignedUids =
                                getStringList(doc, "assignedFacultyUids");

                        boolean assignedToCurrentUser =
                                isAssignedToCurrentUser(
                                        doc, assignedNames, assignedUids);

                        // HOD sees all tasks; faculty sees only matching tasks.
                        if (!isHod && !assignedToCurrentUser) {
                            continue;
                        }

                        long timestamp = getTimestamp(doc);

                        String notificationId = "task_" + groupId;
                        boolean isRead =
                                readSet.contains(notificationId);

                        String message = description;

                        if (!status.isEmpty()) {
                            if (!message.isEmpty()) {
                                message += "\n";
                            }
                            message += "Status: " + status;
                        }

                        AppNotification notification =
                                new AppNotification(
                                        notificationId,
                                        AppNotification.TYPE_TASK,
                                        "Task Assignment",
                                        title,
                                        message,
                                        deadline,
                                        priority,
                                        timestamp,
                                        "HOD",
                                        groupId,
                                        isRead
                                );

                        taskNotificationsMap.put(
                                notificationId, notification);
                    }

                    refreshNotificationsList();
                });
    }

    private boolean isAssignedToCurrentUser(
            DocumentSnapshot doc,
            List<String> assignedNames,
            List<String> assignedUids) {

        if (!currentUid.isEmpty()
                && assignedUids.contains(currentUid)) {
            return true;
        }

        String assignedTo = getString(doc, "assignedTo");

        if (matchesCurrentUser(assignedTo)) {
            return true;
        }

        String faculty = getString(doc, "faculty");

        if (matchesCurrentUser(faculty)) {
            return true;
        }

        for (String name : assignedNames) {
            if (matchesCurrentUser(name)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesCurrentUser(String assignedName) {
        if (assignedName == null || assignedName.trim().isEmpty()) {
            return false;
        }

        if (!currentName.isEmpty()
                && assignedName.trim().equalsIgnoreCase(currentName.trim())) {
            return true;
        }

        // Also support name-based matching against known department names.
        for (String facultyName : DEPARTMENT_FACULTY) {
            if (facultyName.equalsIgnoreCase(assignedName.trim())
                    && facultyName.equalsIgnoreCase(currentName.trim())) {
                return true;
            }
        }

        return false;
    }

    private String getString(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);

        if (value == null) return "";

        return value.toString().trim();
    }

    private List<String> getStringList(
            DocumentSnapshot doc, String field) {

        List<String> result = new ArrayList<>();
        Object value = doc.get(field);

        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    String text = item.toString().trim();
                    if (!text.isEmpty() && !result.contains(text)) {
                        result.add(text);
                    }
                }
            }
        }

        return result;
    }

    private void addIfMissing(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()
                && !list.contains(value.trim())) {
            list.add(value.trim());
        }
    }

    private long getTimestamp(DocumentSnapshot doc) {
        Object value = doc.get("timestamp");

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        return 0L;
    }

    private void refreshNotificationsList() {
        List<AppNotification> merged = new ArrayList<>();
        merged.addAll(taskNotificationsMap.values());

        Collections.sort(merged,
                (a, b) -> Long.compare(
                        b.getTimestamp(), a.getTimestamp()));

        notificationList.clear();
        notificationList.addAll(merged);

        adapter.setNotifications(notificationList);

        if (notificationList.isEmpty()) {
            showEmptyState();
        } else {
            layoutEmptyNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState() {
        layoutEmptyNotifications.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
    }

    @Override
    public void onNotificationClick(AppNotification notification) {
        saveReadNotificationId(notification.getId());
        notification.setRead(true);
        adapter.notifyDataSetChanged();

        if (!notification.getTaskId().trim().isEmpty()) {
            Intent intent = new Intent(
                    NotificationsActivity.this,
                    TaskDiscussionChatActivity.class);

            intent.putExtra(
                    "EXTRA_TASK_ID", notification.getTaskId());

            intent.putExtra(
                    "EXTRA_TASK_TITLE", notification.getSubtitle());

            intent.putExtra(
                    "EXTRA_TASK_DEADLINE", notification.getDeadline());

            intent.putExtra(
                    "EXTRA_TASK_PRIORITY", notification.getPriority());

            intent.putExtra(
                    "taskName", notification.getSubtitle());

            startActivity(intent);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(notification.getSubtitle())
                    .setMessage(
                            notification.getMessage()
                                    + "\n\nIssued by: "
                                    + notification.getSender()
                                    + "\nDate: "
                                    + notification.getFormattedDate())
                    .setPositiveButton("Close", null)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tasksListener != null) {
            tasksListener.remove();
        }
    }
}