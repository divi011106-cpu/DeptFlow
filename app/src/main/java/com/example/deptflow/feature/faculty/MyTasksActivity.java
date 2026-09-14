package com.example.deptflow.feature.faculty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.adapters.TaskAdapter;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.models.Task;
import com.example.deptflow.feature.faculty.repository.SessionManager;
import com.example.deptflow.feature.faculty.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying all tasks assigned to the faculty member.
 * Supports interactive filtering by status (All, Pending, In Progress, Completed)
 * and navigates to TaskDetailsActivity on click.
 */
public class MyTasksActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private enum FilterStatus {
        ALL, PENDING, IN_PROGRESS, COMPLETED
    }

    private RecyclerView rvTasks;
    private View layoutEmptyState;
    private TextView chipAll;
    private TextView chipPending;
    private TextView chipInProgress;
    private TextView chipCompleted;

    private TaskAdapter adapter;
    private TaskRepository taskRepository;
    private SessionManager sessionManager;
    private FacultyUser currentUser;

    private FilterStatus currentFilter = FilterStatus.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tasks);

        taskRepository = TaskRepository.getInstance(this);
        sessionManager = SessionManager.getInstance(this);
        currentUser = sessionManager.getCurrentUser();

        initViews();
        setupRecyclerView();
        setupFilterChips();
        loadTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tasks when returning from TaskDetailsActivity with updated status or when new HOD tasks arrive
        currentUser = sessionManager.getCurrentUser();
        loadTasks();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rv_tasks);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        chipAll = findViewById(R.id.chip_all);
        chipPending = findViewById(R.id.chip_pending);
        chipInProgress = findViewById(R.id.chip_in_progress);
        chipCompleted = findViewById(R.id.chip_completed);

        findViewById(R.id.btn_back_tasks).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, new ArrayList<>(), this);
        rvTasks.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> setFilter(FilterStatus.ALL));
        chipPending.setOnClickListener(v -> setFilter(FilterStatus.PENDING));
        chipInProgress.setOnClickListener(v -> setFilter(FilterStatus.IN_PROGRESS));
        chipCompleted.setOnClickListener(v -> setFilter(FilterStatus.COMPLETED));
    }

    private void setFilter(FilterStatus filter) {
        currentFilter = filter;
        updateFilterChipStyles();
        loadTasks();
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

    private void loadTasks() {
        currentUser = sessionManager.getCurrentUser();
        List<Task> allFacultyTasks = taskRepository.getTasksForFaculty(currentUser);
        List<Task> filteredTasks = new ArrayList<>();

        for (Task task : allFacultyTasks) {
            switch (currentFilter) {
                case PENDING:
                    if (Task.STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
                        filteredTasks.add(task);
                    }
                    break;
                case IN_PROGRESS:
                    if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
                        filteredTasks.add(task);
                    }
                    break;
                case COMPLETED:
                    if (Task.STATUS_COMPLETED.equalsIgnoreCase(task.getStatus())) {
                        filteredTasks.add(task);
                    }
                    break;
                case ALL:
                default:
                    filteredTasks.add(task);
                    break;
            }
        }

        adapter.updateList(filteredTasks);

        if (filteredTasks.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(MyTasksActivity.this, TaskDetailsActivity.class);
        intent.putExtra("EXTRA_TASK_ID", task.getTaskId());
        startActivity(intent);
    }
}
