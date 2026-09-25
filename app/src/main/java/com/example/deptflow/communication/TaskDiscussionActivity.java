package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.TaskDiscussionAdapter;
import com.example.deptflow.communication.models.DiscussionTask;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

public class TaskDiscussionActivity extends AppCompatActivity
        implements TaskDiscussionAdapter.OnTaskClickListener {

    private static final String TAG = "TaskDiscussion";

    private RecyclerView rvTaskDiscussions;
    private ProgressBar pbLoadingTasks;
    private View layoutEmptyState;
    private ImageButton ibBack;

    private TaskDiscussionAdapter adapter;
    private final List<DiscussionTask> taskList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration tasksListener;

    private String currentUid = "";
    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentName = "";
    private String currentUserRole = "FACULTY";
    private final Set<String> myIdentifiers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_discussion);

        initViews();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadCurrentUserInfo();
        setupRecyclerView();

        if (currentUid.isEmpty() && currentUserId.isEmpty() && currentCanonicalId.isEmpty()) {
            Toast.makeText(this, "Please log in to view task discussions", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        listenToFirestoreTasks();
    }

    private void initViews() {
        rvTaskDiscussions = findViewById(R.id.rvTaskDiscussions);
        pbLoadingTasks = findViewById(R.id.pbLoadingTasks);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        ibBack = findViewById(R.id.ibBack);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }

    private void loadCurrentUserInfo() {
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

        Log.d(TAG, "User UID=" + currentUid
                + ", canonicalId=" + currentCanonicalId
                + ", name=" + currentName
                + ", role=" + currentUserRole);
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
        adapter = new TaskDiscussionAdapter(this, taskList, this);
        rvTaskDiscussions.setLayoutManager(new LinearLayoutManager(this));
        rvTaskDiscussions.setAdapter(adapter);
    }

    private void listenToFirestoreTasks() {
        pbLoadingTasks.setVisibility(View.VISIBLE);

        tasksListener = db.collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    pbLoadingTasks.setVisibility(View.GONE);

                    if (error != null) {
                        String code = "UNKNOWN";
                        if (error instanceof FirebaseFirestoreException) {
                            code = ((FirebaseFirestoreException) error).getCode().name();
                        }
                        Log.e(TAG, "Operation failed. code=" + code + " message=" + error.getMessage(), error);
                        String friendlyError;
                        if ("PERMISSION_DENIED".equals(code)) {
                            friendlyError = "Firestore permission denied. Check authentication and Firestore rules.";
                        } else if ("UNAUTHENTICATED".equals(code)) {
                            friendlyError = "Firebase Authentication session is missing.";
                        } else if ("UNAVAILABLE".equals(code)) {
                            friendlyError = "Firebase is temporarily unavailable.";
                        } else if ("FAILED_PRECONDITION".equals(code)) {
                            friendlyError = "Firestore configuration/precondition issue.";
                        } else {
                            friendlyError = "Error loading tasks: " + error.getMessage();
                        }
                        Toast.makeText(TaskDiscussionActivity.this, friendlyError, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showTasks(new ArrayList<>());
                        return;
                    }

                    Map<String, DiscussionTask> uniqueTasks = new LinkedHashMap<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        try {
                            DiscussionTask task = parseTask(doc);
                            if (task == null) {
                                Log.w(TAG, "Task exists but parsing failed or title missing for doc: " + doc.getId());
                                continue;
                            }

                            String groupId = doc.getString("groupTaskId");
                            if (groupId == null || groupId.trim().isEmpty()) {
                                groupId = task.getId();
                            }

                            if (uniqueTasks.containsKey(groupId)) {
                                mergeTask(uniqueTasks.get(groupId), task);
                            } else {
                                uniqueTasks.put(groupId, task);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Task ID: " + doc.getId()
                                    + ", Firestore path: task_assignments/" + doc.getId()
                                    + ", Reason: " + e.getMessage(), e);
                        }
                    }

                    List<DiscussionTask> matchedTasks = new ArrayList<>();
                    boolean isHod = "HOD".equalsIgnoreCase(currentUserRole);

                    for (DiscussionTask task : uniqueTasks.values()) {
                        if (isHod) {
                            matchedTasks.add(task);
                        } else if (isAssignedToTask(task)) {
                            matchedTasks.add(task);
                        }
                    }

                    Collections.sort(matchedTasks, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    showTasks(matchedTasks);
                });
    }

    private boolean isAssignedToTask(DiscussionTask task) {
        if (task == null) return false;

        // 1. Check UIDs
        if (task.getAssignedFacultyUids() != null) {
            for (String uid : task.getAssignedFacultyUids()) {
                if (isMyIdentifier(uid)) return true;
            }
        }

        // 2. Check assigned faculty names/IDs
        if (task.getAssignedFaculty() != null) {
            for (String assigned : task.getAssignedFaculty()) {
                if (isMyIdentifier(assigned)) return true;
                FacultyUser resolved = FacultyDirectory.resolveByNameOrId(assigned);
                if (resolved != null && isMyIdentifier(resolved.getUserId())) return true;
            }
        }

        // 3. Check legacy faculty field
        if (task.getFaculty() != null && !task.getFaculty().trim().isEmpty()) {
            String[] parts = task.getFaculty().split("[\r\n,]+");
            for (String p : parts) {
                if (isMyIdentifier(p.trim())) return true;
                FacultyUser resolved = FacultyDirectory.resolveByNameOrId(p.trim());
                if (resolved != null && isMyIdentifier(resolved.getUserId())) return true;
            }
        }

        return false;
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

    private DiscussionTask parseTask(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        String taskId = doc.getString("groupTaskId");
        if (isBlank(taskId)) taskId = doc.getString("id");
        if (isBlank(taskId)) taskId = doc.getString("taskId");
        if (isBlank(taskId)) taskId = doc.getId();

        String title = doc.getString("taskTitle");
        if (isBlank(title)) title = doc.getString("title");
        if (isBlank(title)) return null;

        String description = doc.getString("description");
        String deadline = doc.getString("deadline");
        String priority = doc.getString("priority");
        String status = doc.getString("status");

        String assignedTo = doc.getString("assignedTo");
        if (isBlank(assignedTo)) assignedTo = doc.getString("faculty");

        List<String> assignedFaculty = new ArrayList<>();
        addStringList(doc.get("allAssignedFaculty"), assignedFaculty);
        addStringList(doc.get("assignedFaculty"), assignedFaculty);

        if (!isBlank(assignedTo) && !assignedFaculty.contains(assignedTo.trim())) {
            assignedFaculty.add(assignedTo.trim());
        }

        List<String> assignedUids = new ArrayList<>();
        addStringList(doc.get("assignedFacultyUids"), assignedUids);

        int assignedCount = 0;
        Object countObj = doc.get("assignedFacultyCount");
        if (countObj instanceof Number) {
            assignedCount = ((Number) countObj).intValue();
        } else if (countObj instanceof String) {
            try {
                assignedCount = Integer.parseInt(((String) countObj).trim());
            } catch (Exception ignored) {}
        }

        long timestamp = parseTimestampSafe(doc.get("timestamp"));

        return new DiscussionTask(
                taskId,
                title,
                description,
                deadline,
                priority,
                status,
                assignedFaculty,
                assignedUids,
                assignedCount,
                assignedTo != null ? assignedTo : "",
                timestamp
        );
    }

    private long parseTimestampSafe(Object raw) {
        if (raw == null) return 0L;
        if (raw instanceof Timestamp) {
            return ((Timestamp) raw).toDate().getTime();
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof Date) {
            return ((Date) raw).getTime();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong(((String) raw).trim());
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private void addStringList(Object value, List<String> destination) {
        if (!(value instanceof List<?>)) return;
        for (Object item : (List<?>) value) {
            if (item == null) continue;
            String text = item.toString().trim();
            if (!text.isEmpty() && !destination.contains(text)) {
                destination.add(text);
            }
        }
    }

    private void mergeTask(DiscussionTask existing, DiscussionTask incoming) {
        for (String f : incoming.getAssignedFaculty()) {
            if (!existing.getAssignedFaculty().contains(f)) {
                existing.getAssignedFaculty().add(f);
            }
        }

        for (String u : incoming.getAssignedFacultyUids()) {
            if (!existing.getAssignedFacultyUids().contains(u)) {
                existing.getAssignedFacultyUids().add(u);
            }
        }

        if (incoming.getAssignedFacultyCount() > existing.getAssignedFacultyCount()) {
            existing.setAssignedFacultyCount(incoming.getAssignedFacultyCount());
        }
    }

    private void showTasks(List<DiscussionTask> tasks) {
        taskList.clear();
        taskList.addAll(tasks);
        adapter.updateTasks(taskList);

        if (taskList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvTaskDiscussions.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvTaskDiscussions.setVisibility(View.VISIBLE);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public void onTaskClick(DiscussionTask task) {
        if (task == null) return;

        Intent intent = new Intent(this, TaskDiscussionChatActivity.class);
        intent.putExtra("EXTRA_TASK_ID", task.getId());
        intent.putExtra("EXTRA_TASK_TITLE", task.getTitle());
        intent.putExtra("EXTRA_TASK_DEADLINE", task.getDeadline());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            tasksListener.remove();
        }
    }
}