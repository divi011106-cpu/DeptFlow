
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
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskDiscussionActivity extends AppCompatActivity
        implements TaskDiscussionAdapter.OnTaskClickListener {

    private static final String TAG = "TaskDiscussion";

    private static final List<String> DEPARTMENT_FACULTY = Arrays.asList(
            "Dr. R. Vijayalakshmi",
            "Dr. R. Raja Sudharsan",
            "Dr. K. M. Alaaudeen",
            "Dr. T. Sarnya",
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
    private String currentName = "";
    private String currentUserRole = "FACULTY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_discussion);

        initViews();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadCurrentUserInfo();
        setupRecyclerView();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please log in to view task discussions",
                    Toast.LENGTH_LONG).show();
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
                currentUserRole =
                        cachedUser.getRole().trim().toUpperCase();
            }
        }

        Log.d(TAG, "User UID=" + currentUid
                + ", name=" + currentName
                + ", role=" + currentUserRole);
    }

    private void setupRecyclerView() {
        adapter = new TaskDiscussionAdapter(this, taskList, this);

        rvTaskDiscussions.setLayoutManager(
                new LinearLayoutManager(this));

        rvTaskDiscussions.setAdapter(adapter);
    }

    private void listenToFirestoreTasks() {
        pbLoadingTasks.setVisibility(View.VISIBLE);

        tasksListener = db.collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    pbLoadingTasks.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG,
                                "Error fetching task_assignments",
                                error);

                        Toast.makeText(
                                TaskDiscussionActivity.this,
                                "Error loading tasks: "
                                        + error.getCode(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        showTasks(new ArrayList<>());
                        return;
                    }

                    Map<String, DiscussionTask> uniqueTasks =
                            new LinkedHashMap<>();

                    for (DocumentSnapshot doc
                            : snapshots.getDocuments()) {
                        try {
                            DiscussionTask task = parseTask(doc);

                            if (task == null) {
                                continue;
                            }

                            String groupId = doc.getString("groupTaskId");

                            if (groupId == null
                                    || groupId.trim().isEmpty()) {
                                groupId = task.getId();
                            }

                            if (uniqueTasks.containsKey(groupId)) {
                                mergeTask(
                                        uniqueTasks.get(groupId),
                                        task
                                );
                            } else {
                                uniqueTasks.put(groupId, task);
                            }

                        } catch (Exception e) {
                            Log.w(TAG,
                                    "Could not parse task "
                                            + doc.getId(),
                                    e);
                        }
                    }

                    List<DiscussionTask> matchedTasks =
                            new ArrayList<>();

                    boolean isHod =
                            "HOD".equalsIgnoreCase(currentUserRole);

                    for (DiscussionTask task : uniqueTasks.values()) {
                        if (isHod) {
                            matchedTasks.add(task);
                        } else if (task.isAssignedToUser(
                                currentUid,
                                currentName,
                                DEPARTMENT_FACULTY)) {
                            matchedTasks.add(task);
                        }
                    }

                    Collections.sort(
                            matchedTasks,
                            (a, b) -> Long.compare(
                                    b.getTimestamp(),
                                    a.getTimestamp()
                            )
                    );

                    showTasks(matchedTasks);
                });
    }

    private DiscussionTask parseTask(DocumentSnapshot doc) {
        String taskId = doc.getString("groupTaskId");

        if (isBlank(taskId)) {
            taskId = doc.getString("id");
        }

        if (isBlank(taskId)) {
            taskId = doc.getString("taskId");
        }

        if (isBlank(taskId)) {
            taskId = doc.getId();
        }

        String title = doc.getString("taskTitle");

        if (isBlank(title)) {
            title = doc.getString("title");
        }

        if (isBlank(title)) {
            return null;
        }

        String description = doc.getString("description");
        String deadline = doc.getString("deadline");
        String priority = doc.getString("priority");
        String status = doc.getString("status");

        String assignedTo = doc.getString("assignedTo");

        if (isBlank(assignedTo)) {
            assignedTo = doc.getString("faculty");
        }

        List<String> assignedFaculty = new ArrayList<>();

        addStringList(doc.get("allAssignedFaculty"), assignedFaculty);
        addStringList(doc.get("assignedFaculty"), assignedFaculty);

        if (!isBlank(assignedTo)
                && !assignedFaculty.contains(assignedTo.trim())) {
            assignedFaculty.add(assignedTo.trim());
        }

        List<String> assignedUids = new ArrayList<>();
        addStringList(doc.get("assignedFacultyUids"), assignedUids);

        int assignedCount = 0;
        Long count = doc.getLong("assignedFacultyCount");

        if (count != null) {
            assignedCount = count.intValue();
        }

        long timestamp = 0L;
        Long ts = doc.getLong("timestamp");

        if (ts != null) {
            timestamp = ts;
        }

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
                assignedTo,
                timestamp
        );
    }

    private void addStringList(Object value, List<String> destination) {
        if (!(value instanceof List<?>)) {
            return;
        }

        for (Object item : (List<?>) value) {
            if (item == null) {
                continue;
            }

            String text = item.toString().trim();

            if (!text.isEmpty() && !destination.contains(text)) {
                destination.add(text);
            }
        }
    }

    private void mergeTask(DiscussionTask existing,
                           DiscussionTask incoming) {
        if (existing == null || incoming == null) {
            return;
        }

        for (String faculty : incoming.getAssignedFaculty()) {
            if (!existing.getAssignedFaculty().contains(faculty)) {
                existing.getAssignedFaculty().add(faculty);
            }
        }

        for (String uid : incoming.getAssignedFacultyUids()) {
            if (!existing.getAssignedFacultyUids().contains(uid)) {
                existing.getAssignedFacultyUids().add(uid);
            }
        }
    }

    private void showTasks(List<DiscussionTask> tasks) {
        taskList.clear();
        taskList.addAll(tasks);

        adapter.updateTasks(taskList);

        boolean empty = taskList.isEmpty();

        layoutEmptyState.setVisibility(
                empty ? View.VISIBLE : View.GONE);

        rvTaskDiscussions.setVisibility(
                empty ? View.GONE : View.VISIBLE);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void onTaskClick(DiscussionTask task) {
        if (task == null) {
            return;
        }

        Intent intent = new Intent(
                TaskDiscussionActivity.this,
                TaskDiscussionChatActivity.class
        );

        intent.putExtra("EXTRA_TASK_ID", task.getId());
        intent.putExtra("EXTRA_TASK_TITLE", task.getTitle());
        intent.putExtra("EXTRA_TASK_DEADLINE", task.getDeadline());
        intent.putExtra("EXTRA_TASK_PRIORITY", task.getPriority());
        intent.putExtra("taskName", task.getTitle());

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (tasksListener != null) {
            tasksListener.remove();
        }

        super.onDestroy();
    }
}