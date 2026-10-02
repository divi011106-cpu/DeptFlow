package com.example.deptflow.communication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.deptflow.feature.faculty.repository.SessionManager;
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

/**
 * TaskDiscussionActivity
 *
 * Dynamically queries the real HOD assigned tasks from Firestore collection 'task_assignments'
 * using the exact same identity resolution and document matching logic as CommunicationActivity.
 */
public class TaskDiscussionActivity extends AppCompatActivity
        implements TaskDiscussionAdapter.OnTaskClickListener {

    private static final String TAG = "TASK_DISCUSSION_DEBUG";

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
    private String currentUserName = "Faculty";
    private String currentUserEmail = "";
    private String currentUserRole = "FACULTY";
    private final Set<String> myIdentifiers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_discussion);

        initViews();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentUserInfo();
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

        // 1. Firebase Auth user
        FirebaseUser fbUser = auth.getCurrentUser();
        if (fbUser != null) {
            currentUid = fbUser.getUid();
            addIdentifier(currentUid);
            if (fbUser.getEmail() != null) {
                currentUserEmail = fbUser.getEmail().trim().toLowerCase(Locale.ROOT);
                addIdentifier(currentUserEmail);
            }
            if (fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                currentUserName = fbUser.getDisplayName().trim();
                addIdentifier(currentUserName);
            }
        }

        // 2. AuthManager session
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
                currentUserEmail = sessionUser.getEmail().trim().toLowerCase(Locale.ROOT);
                addIdentifier(currentUserEmail);
            }
            if (sessionUser.getRole() != null && !sessionUser.getRole().trim().isEmpty()) {
                currentUserRole = sessionUser.getRole().trim().toUpperCase(Locale.ROOT);
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

        // 3. SessionManager fallback
        FacultyUser facultySession = SessionManager.getInstance(this).getCurrentUser();
        if (facultySession != null) {
            if (currentUserId.isEmpty() && facultySession.getUserId() != null && !facultySession.getUserId().trim().isEmpty()) {
                currentUserId = facultySession.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if ((currentUserName.isEmpty() || "Faculty".equals(currentUserName)) && facultySession.getName() != null && !facultySession.getName().trim().isEmpty()) {
                currentUserName = facultySession.getName().trim();
                addIdentifier(currentUserName);
            }
            if (currentUserEmail.isEmpty() && facultySession.getEmail() != null && !facultySession.getEmail().trim().isEmpty()) {
                currentUserEmail = facultySession.getEmail().trim().toLowerCase(Locale.ROOT);
                addIdentifier(currentUserEmail);
            }
            if (currentUserRole.isEmpty() && facultySession.getRole() != null && !facultySession.getRole().trim().isEmpty()) {
                currentUserRole = facultySession.getRole().trim().toUpperCase(Locale.ROOT);
            }
        }

        // 4. SharedPreferences direct fallback
        SharedPreferences sp = getSharedPreferences("deptflow_session_pref", Context.MODE_PRIVATE);
        if (currentUserId.isEmpty()) {
            currentUserId = sp.getString("user_id", "");
            if (!currentUserId.isEmpty()) addIdentifier(currentUserId);
        }
        if (currentNameIsEmpty()) {
            String spName = sp.getString("user_name", "");
            if (!spName.isEmpty()) {
                currentUserName = spName;
                addIdentifier(currentUserName);
            }
        }
        if (currentUserEmail.isEmpty()) {
            currentUserEmail = sp.getString("user_email", "");
            if (!currentUserEmail.isEmpty()) addIdentifier(currentUserEmail);
        }
        String spRole = sp.getString("user_role", "");
        if (!spRole.isEmpty()) {
            currentUserRole = spRole.toUpperCase(Locale.ROOT);
        }

        // 5. Canonical resolution via FacultyDirectory
        if (currentCanonicalId.isEmpty() && !currentNameIsEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserName);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }
        if (currentCanonicalId.isEmpty() && !currentUserId.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserId);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }
        if (currentCanonicalId.isEmpty() && !currentUserEmail.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserEmail);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }
        if (currentCanonicalId.isEmpty() && !currentUid.isEmpty()) {
            currentCanonicalId = currentUid;
            addIdentifier(currentCanonicalId);
        }

        Log.d(TAG, "currentUserId = " + currentUserId);
        Log.d(TAG, "currentUserName = " + currentUserName);
        Log.d(TAG, "currentUserRole = " + currentUserRole);
    }

    private boolean currentNameIsEmpty() {
        return currentUserName == null || currentUserName.trim().isEmpty() || "Faculty".equalsIgnoreCase(currentUserName.trim());
    }

    private void addIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return;
        String trimmed = val.trim();
        myIdentifiers.add(trimmed);
        myIdentifiers.add(trimmed.toLowerCase(Locale.ROOT));

        // Add without title (Dr., Mrs., Mr., etc.)
        String cleanTitle = trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(dr|mr|mrs|ms|prof)\\b[.]?", "")
                .trim();
        if (!cleanTitle.isEmpty()) {
            myIdentifiers.add(cleanTitle);
        }

        // Add alphanumeric cleaned token
        String clean = trimmed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (!clean.isEmpty()) {
            myIdentifiers.add(clean);
        }

        // If email, add prefix
        if (trimmed.contains("@")) {
            String prefix = trimmed.substring(0, trimmed.indexOf('@')).trim().toLowerCase(Locale.ROOT);
            if (!prefix.isEmpty()) {
                myIdentifiers.add(prefix);
            }
        }
    }

    private boolean isMyIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return false;
        String normal = val.trim().toLowerCase(Locale.ROOT);
        return myIdentifiers.contains(normal)
                || myIdentifiers.contains(val.trim())
                || normal.equalsIgnoreCase(currentCanonicalId)
                || normal.equalsIgnoreCase(currentUserId)
                || normal.equalsIgnoreCase(currentUid)
                || normal.equalsIgnoreCase(currentUserName);
    }

    private void setupRecyclerView() {
        adapter = new TaskDiscussionAdapter(this, taskList, this);
        rvTaskDiscussions.setLayoutManager(new LinearLayoutManager(this));
        rvTaskDiscussions.setAdapter(adapter);
    }

    private void listenToFirestoreTasks() {
        if (tasksListener != null) {
            tasksListener.remove();
        }

        pbLoadingTasks.setVisibility(View.VISIBLE);

        Log.d(TAG, "Loading task discussions...");

        tasksListener = db.collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    pbLoadingTasks.setVisibility(View.GONE);

                    if (error != null) {
                        String code = "UNKNOWN";
                        if (error instanceof FirebaseFirestoreException) {
                            code = ((FirebaseFirestoreException) error).getCode().name();
                        }
                        Log.e(TAG, "Firestore task_assignments listener error. code=" + code + " message=" + error.getMessage(), error);
                        String friendlyError;
                        if ("PERMISSION_DENIED".equals(code)) {
                            friendlyError = "Firestore permission denied. Check authentication.";
                        } else if ("UNAUTHENTICATED".equals(code)) {
                            friendlyError = "Firebase Authentication session missing.";
                        } else {
                            friendlyError = "Error loading tasks: " + error.getMessage();
                        }
                        Toast.makeText(TaskDiscussionActivity.this, friendlyError, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int docCount = snapshots != null ? snapshots.size() : 0;
                    Log.d(TAG, "Task documents returned = " + docCount);

                    if (snapshots == null || snapshots.isEmpty()) {
                        showTasks(new ArrayList<>());
                        return;
                    }

                    boolean isHod = "HOD".equalsIgnoreCase(currentUserRole);
                    Map<String, DiscussionTask> uniqueTasks = new LinkedHashMap<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        try {
                            Log.d(TAG, "Task ID = " + doc.getId());
                            Log.d(TAG, "Task data = " + doc.getData());

                            // Check assignment matching
                            boolean assignedToMe = isHod || isDocumentAssignedToMe(doc);
                            Log.d(TAG, "Task ID: " + doc.getId() + " => assignedToMe = " + assignedToMe);

                            if (!assignedToMe) {
                                continue;
                            }

                            // Extract groupId (matching CommunicationActivity logic)
                            String groupId = doc.getString("groupTaskId");
                            if (groupId == null || groupId.trim().isEmpty()) groupId = doc.getString("taskId");
                            if (groupId == null || groupId.trim().isEmpty()) groupId = doc.getString("id");
                            if (groupId == null || groupId.trim().isEmpty()) groupId = doc.getId();

                            DiscussionTask task = parseTaskDocument(doc, groupId);

                            if (uniqueTasks.containsKey(groupId)) {
                                mergeTask(uniqueTasks.get(groupId), task);
                            } else {
                                uniqueTasks.put(groupId, task);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing doc: " + doc.getId(), e);
                        }
                    }

                    List<DiscussionTask> matchedTasks = new ArrayList<>(uniqueTasks.values());

                    // Sort newest first
                    Collections.sort(matchedTasks, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    showTasks(matchedTasks);
                });
    }

    private boolean isDocumentAssignedToMe(DocumentSnapshot doc) {
        if (doc == null) return false;

        // 1. Check if assigned to ALL
        String assignedTo = doc.getString("assignedTo");
        String faculty = doc.getString("faculty");
        String assignmentType = doc.getString("assignmentType");
        if (isAllToken(assignedTo) || isAllToken(faculty) || isAllToken(assignmentType) || Boolean.TRUE.equals(doc.getBoolean("isAll"))) {
            return true;
        }

        // 2. Check direct assignedTo / faculty strings
        if (isMyIdentifier(assignedTo)) return true;
        if (isMyIdentifier(faculty)) return true;

        // 3. Check assignedFacultyUids list
        Object uidsObj = doc.get("assignedFacultyUids");
        if (uidsObj == null) uidsObj = doc.get("facultyUids");
        if (uidsObj instanceof List) {
            for (Object item : (List<?>) uidsObj) {
                if (item != null && isMyIdentifier(item.toString())) return true;
            }
        }

        // 4. Check assignedFacultyIds list
        Object fidsObj = doc.get("assignedFacultyIds");
        if (fidsObj == null) fidsObj = doc.get("facultyIds");
        if (fidsObj instanceof List) {
            for (Object item : (List<?>) fidsObj) {
                if (item != null && isMyIdentifier(item.toString())) return true;
            }
        }

        // 5. Check allAssignedFaculty / assignedFaculty list
        Object facListObj = doc.get("allAssignedFaculty");
        if (facListObj == null) facListObj = doc.get("assignedFaculty");
        if (facListObj instanceof List) {
            List<?> list = (List<?>) facListObj;
            if (list.size() >= FacultyDirectory.FACULTY_NAMES.length) return true;
            for (Object item : list) {
                if (item != null) {
                    String name = item.toString().trim();
                    if (isAllToken(name)) return true;
                    if (isMyIdentifier(name)) return true;
                    FacultyUser resolved = FacultyDirectory.resolveByNameOrId(name);
                    if (resolved != null && isMyIdentifier(resolved.getUserId())) return true;
                    if (resolved != null && isMyIdentifier(resolved.getName())) return true;
                }
            }
        }

        // 6. Split multi-name string in assignedTo or faculty
        if (assignedTo != null && !assignedTo.trim().isEmpty()) {
            String[] parts = assignedTo.split("[\r\n,]+");
            for (String p : parts) {
                String pt = p.trim();
                if (isMyIdentifier(pt)) return true;
                FacultyUser resolved = FacultyDirectory.resolveByNameOrId(pt);
                if (resolved != null && (isMyIdentifier(resolved.getUserId()) || isMyIdentifier(resolved.getName()))) return true;
            }
        }
        if (faculty != null && !faculty.trim().isEmpty()) {
            String[] parts = faculty.split("[\r\n,]+");
            for (String p : parts) {
                String pt = p.trim();
                if (isMyIdentifier(pt)) return true;
                FacultyUser resolved = FacultyDirectory.resolveByNameOrId(pt);
                if (resolved != null && (isMyIdentifier(resolved.getUserId()) || isMyIdentifier(resolved.getName()))) return true;
            }
        }

        return false;
    }

    private boolean isAllToken(String val) {
        if (val == null) return false;
        String t = val.trim().toUpperCase(Locale.ROOT);
        return t.equals("ALL")
                || t.equals("ALL FACULTY")
                || t.equals("ALL FACULTIES")
                || t.equals("ALL_FACULTY")
                || t.equals("ALL_FACULTIES")
                || t.equals("ALL MEMBERS")
                || t.equals("EVERYONE");
    }

    private DiscussionTask parseTaskDocument(DocumentSnapshot doc, String groupId) {
        String title = doc.getString("taskTitle");
        if (isBlank(title)) title = doc.getString("title");
        if (isBlank(title)) title = doc.getString("taskName");
        if (isBlank(title)) title = "Task " + groupId;

        String description = doc.getString("description");
        if (description == null) description = doc.getString("subtitle");
        if (description == null) description = doc.getString("message");
        if (description == null) description = "";

        String deadline = doc.getString("deadline");
        if (deadline == null) deadline = doc.getString("scheduledDateStr");
        if (deadline == null) deadline = "";

        String priority = doc.getString("priority");
        if (priority == null) priority = "MEDIUM";

        String status = doc.getString("status");
        if (status == null) status = "PENDING";

        String assignedBy = doc.getString("assignedBy");
        if (isBlank(assignedBy)) assignedBy = doc.getString("sender");
        if (isBlank(assignedBy)) assignedBy = "HOD (Department Head)";

        String assignedTo = doc.getString("assignedTo");
        if (isBlank(assignedTo)) assignedTo = doc.getString("faculty");

        List<String> assignedFaculty = new ArrayList<>();
        addStringList(doc.get("allAssignedFaculty"), assignedFaculty);
        addStringList(doc.get("assignedFaculty"), assignedFaculty);

        if (!isBlank(assignedTo)) {
            String[] parts = assignedTo.split("[\r\n,]+");
            for (String p : parts) {
                String pt = p.trim();
                if (!pt.isEmpty() && !assignedFaculty.contains(pt)) {
                    assignedFaculty.add(pt);
                }
            }
        }

        List<String> assignedFacultyIds = new ArrayList<>();
        addStringList(doc.get("assignedFacultyIds"), assignedFacultyIds);
        addStringList(doc.get("facultyIds"), assignedFacultyIds);

        List<String> assignedUids = new ArrayList<>();
        addStringList(doc.get("assignedFacultyUids"), assignedUids);
        addStringList(doc.get("facultyUids"), assignedUids);

        int assignedCount = 0;
        Object countObj = doc.get("assignedFacultyCount");
        if (countObj instanceof Number) {
            assignedCount = ((Number) countObj).intValue();
        } else if (countObj instanceof String) {
            try {
                assignedCount = Integer.parseInt(((String) countObj).trim());
            } catch (Exception ignored) {}
        }
        if (assignedCount <= 0) {
            assignedCount = assignedFaculty.size();
        }

        // ALL detection
        boolean isAll = false;
        String assignmentType = doc.getString("assignmentType");
        if (assignmentType != null && assignmentType.toUpperCase(Locale.ROOT).contains("ALL")) {
            isAll = true;
        }
        if (Boolean.TRUE.equals(doc.getBoolean("isAll"))) {
            isAll = true;
        }
        if (isAllToken(assignedTo) || isAllToken(doc.getString("faculty"))) {
            isAll = true;
        }
        for (String f : assignedFaculty) {
            if (isAllToken(f)) {
                isAll = true;
                break;
            }
        }
        if (assignedCount >= FacultyDirectory.FACULTY_NAMES.length || assignedFaculty.size() >= FacultyDirectory.FACULTY_NAMES.length) {
            isAll = true;
        }

        long timestamp = parseTimestampSafe(doc.get("timestamp"));
        if (timestamp <= 0) {
            timestamp = parseTimestampSafe(doc.get("createdAt"));
        }

        return new DiscussionTask(
                groupId,
                title,
                description,
                deadline,
                priority,
                status,
                assignedBy,
                assignedFaculty,
                assignedUids,
                assignedFacultyIds,
                assignedCount,
                assignedTo != null ? assignedTo : "",
                assignedTo != null ? assignedTo : "",
                isAll,
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

        for (String fid : incoming.getAssignedFacultyIds()) {
            if (!existing.getAssignedFacultyIds().contains(fid)) {
                existing.getAssignedFacultyIds().add(fid);
            }
        }

        if (incoming.isAll()) {
            existing.setAll(true);
        }

        if (incoming.getAssignedFacultyCount() > existing.getAssignedFacultyCount()) {
            existing.setAssignedFacultyCount(incoming.getAssignedFacultyCount());
        }

        if (incoming.getTimestamp() > existing.getTimestamp()) {
            existing.setTimestamp(incoming.getTimestamp());
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

        String taskId = task.getId();
        String taskTitle = task.getTitle();
        String taskDescription = task.getDescription();
        String deadline = task.getDeadline();
        List<String> assignedMembers = task.getAssignedFaculty();

        Log.d("TASK_DISCUSSION", "Current faculty UID = " + currentUid);
        Log.d("TASK_DISCUSSION", "Task ID = " + taskId);
        Log.d("TASK_DISCUSSION", "Task title = " + taskTitle);
        Log.d("TASK_DISCUSSION", "Assigned members = " + assignedMembers);
        Log.d("TASK_DISCUSSION", "OPENING CHAT WITH TASK ID = " + taskId);

        Intent intent = new Intent(this, TaskDiscussionChatActivity.class);
        intent.putExtra("taskId", taskId);
        intent.putExtra("taskTitle", taskTitle);
        intent.putExtra("taskDescription", taskDescription);
        intent.putExtra("deadline", deadline);
        intent.putExtra("priority", task.getPriority());
        intent.putStringArrayListExtra("assignedMembers", new ArrayList<>(assignedMembers));

        // Backwards compatibility extras
        intent.putExtra("EXTRA_TASK_ID", taskId);
        intent.putExtra("EXTRA_TASK_TITLE", taskTitle);
        intent.putExtra("EXTRA_TASK_DEADLINE", deadline);
        intent.putExtra("EXTRA_TASK_PRIORITY", task.getPriority());
        intent.putExtra("EXTRA_IS_ALL", task.isAll());
        intent.putStringArrayListExtra("EXTRA_PARTICIPANTS", new ArrayList<>(assignedMembers));
        intent.putExtra("EXTRA_PARTICIPANT_COUNT", task.getAssignedFacultyCount());

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