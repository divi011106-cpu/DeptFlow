package com.example.deptflow.hod;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.Task;
import com.example.deptflow.feature.faculty.repository.TaskRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ViewTasksActivity extends AppCompatActivity implements HodTaskAdapter.OnTaskClickListener {

    private static final String TAG = "ViewTasksActivity";

    private enum TaskFilter {
        ALL, PENDING, IN_PROGRESS, COMPLETED
    }

    private RecyclerView rvTasks;
    private ProgressBar progressBarTasks;
    private View layoutEmptyState;

    private TextView chipAll;
    private TextView chipPending;
    private TextView chipInProgress;
    private TextView chipCompleted;

    private HodTaskAdapter adapter;
    private final List<Task> masterTaskList = new ArrayList<>();
    private TaskFilter currentFilter = TaskFilter.ALL;

    private FirebaseFirestore firestore;
    private ListenerRegistration tasksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        initViews();
        setupRecyclerView();
        setupFilterChips();

        // Check if a filter was requested via Intent from dashboard stat card
        String filterExtra = getIntent().getStringExtra("filter_status");
        if (filterExtra != null) {
            if ("Assigned".equalsIgnoreCase(filterExtra) || "Pending".equalsIgnoreCase(filterExtra)) {
                currentFilter = TaskFilter.PENDING;
            } else if ("In Progress".equalsIgnoreCase(filterExtra)) {
                currentFilter = TaskFilter.IN_PROGRESS;
            } else if ("Completed".equalsIgnoreCase(filterExtra)) {
                currentFilter = TaskFilter.COMPLETED;
            }
        }
        updateFilterChipStyles();

        loadTasksFromFirestore();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvTasks = findViewById(R.id.rvHodTasks);
        progressBarTasks = findViewById(R.id.progressBarTasks);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        chipAll = findViewById(R.id.chip_all);
        chipPending = findViewById(R.id.chip_pending);
        chipInProgress = findViewById(R.id.chip_in_progress);
        chipCompleted = findViewById(R.id.chip_completed);
    }

    private void setupRecyclerView() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HodTaskAdapter(this, new ArrayList<>(), this);
        rvTasks.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> setFilter(TaskFilter.ALL));
        chipPending.setOnClickListener(v -> setFilter(TaskFilter.PENDING));
        chipInProgress.setOnClickListener(v -> setFilter(TaskFilter.IN_PROGRESS));
        chipCompleted.setOnClickListener(v -> setFilter(TaskFilter.COMPLETED));
    }

    private void setFilter(TaskFilter filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        applyFilter();
    }

    private void updateFilterChipStyles() {
        resetChip(chipAll);
        resetChip(chipPending);
        resetChip(chipInProgress);
        resetChip(chipCompleted);

        TextView activeChip;
        switch (currentFilter) {
            case PENDING:
                activeChip = chipPending;
                break;
            case IN_PROGRESS:
                activeChip = chipInProgress;
                break;
            case COMPLETED:
                activeChip = chipCompleted;
                break;
            case ALL:
            default:
                activeChip = chipAll;
                break;
        }

        activeChip.setBackgroundResource(R.drawable.bg_chip_selected);
        activeChip.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
    }

    private void resetChip(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_unselected);
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void loadTasksFromFirestore() {
        if (progressBarTasks != null) progressBarTasks.setVisibility(View.VISIBLE);

        try {
            firestore = FirebaseFirestore.getInstance();
            tasksListener = firestore.collection("task_assignments")
                    .addSnapshotListener((snapshots, error) -> {
                        if (progressBarTasks != null) progressBarTasks.setVisibility(View.GONE);

                        if (error != null) {
                            Log.w(TAG, "Firestore error, falling back to local tasks: " + error.getMessage());
                            loadFallbackTasks();
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            masterTaskList.clear();
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Task task = parseTaskDocument(doc);
                                if (task != null) {
                                    masterTaskList.add(task);
                                }
                            }
                            applyFilter();
                        } else {
                            // If Firestore has no documents, try local tasks
                            loadFallbackTasks();
                        }
                    });
        } catch (Exception e) {
            if (progressBarTasks != null) progressBarTasks.setVisibility(View.GONE);
            Log.e(TAG, "Firestore initialization error: " + e.getMessage());
            loadFallbackTasks();
        }
    }

    private void loadFallbackTasks() {
        masterTaskList.clear();
        try {
            List<Task> hodTasks = TaskRepository.getInstance(this).getHodTasks();
            if (hodTasks != null) {
                masterTaskList.addAll(hodTasks);
            }
        } catch (Exception ignored) {}
        applyFilter();
    }

    private Task parseTaskDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        String id = doc.getString("taskId");
        if (id == null || id.isEmpty()) id = doc.getString("id");
        if (id == null || id.isEmpty()) id = doc.getId();

        String title = doc.getString("taskTitle");
        if (title == null || title.isEmpty()) title = doc.getString("title");
        if (title == null || title.isEmpty()) title = "Task " + id;

        String description = doc.getString("description");
        if (description == null) description = "";

        String assignedTo = doc.getString("assignedTo");
        if (assignedTo == null || assignedTo.isEmpty()) assignedTo = doc.getString("faculty");
        if (assignedTo == null || assignedTo.isEmpty()) {
            Object obj = doc.get("allAssignedFaculty");
            if (obj == null) obj = doc.get("assignedFaculty");
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    sb.append(String.valueOf(list.get(i)));
                    if (i < list.size() - 1) sb.append(", ");
                }
                assignedTo = sb.toString();
            } else {
                assignedTo = "Department Faculty";
            }
        }

        String assignedBy = doc.getString("assignedBy");
        if (assignedBy == null || assignedBy.isEmpty()) assignedBy = "HOD (Department Head)";

        String deadline = doc.getString("deadline");
        if (deadline == null || deadline.isEmpty()) deadline = "No Deadline";

        String rawStatus = doc.getString("status");
        String status = Task.STATUS_PENDING;
        if (rawStatus != null) {
            String s = rawStatus.trim().toUpperCase();
            if (s.contains("COMPLET")) status = Task.STATUS_COMPLETED;
            else if (s.contains("PROGRESS")) status = Task.STATUS_IN_PROGRESS;
        }

        String priority = doc.getString("priority");
        if (priority == null || priority.isEmpty()) priority = Task.PRIORITY_MEDIUM;
        else priority = priority.toUpperCase();

        return new Task(id, title, description, assignedTo, assignedBy, deadline, status, priority);
    }

    private void applyFilter() {
        List<Task> filtered = new ArrayList<>();
        for (Task task : masterTaskList) {
            switch (currentFilter) {
                case PENDING:
                    if (Task.STATUS_PENDING.equalsIgnoreCase(task.getStatus()) || "Assigned".equalsIgnoreCase(task.getStatus())) {
                        filtered.add(task);
                    }
                    break;
                case IN_PROGRESS:
                    if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
                        filtered.add(task);
                    }
                    break;
                case COMPLETED:
                    if (Task.STATUS_COMPLETED.equalsIgnoreCase(task.getStatus())) {
                        filtered.add(task);
                    }
                    break;
                case ALL:
                default:
                    filtered.add(task);
                    break;
            }
        }

        adapter.updateList(filtered);

        if (filtered.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (rvTasks != null) rvTasks.setVisibility(View.GONE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (rvTasks != null) rvTasks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskClick(Task task) {
        if (task == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle(task.getTaskTitle())
                .setMessage("Description:\n" + task.getDescription() + "\n\n"
                        + "Assigned To:\n" + task.getAssignedTo() + "\n\n"
                        + "Deadline: " + task.getDeadline() + "\n"
                        + "Priority: " + task.getPriority() + "\n"
                        + "Status: " + task.getStatus())
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) {
            tasksListener.remove();
        }
    }
}