package com.example.deptflow.feature.faculty;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.Task;
import com.example.deptflow.feature.faculty.repository.TaskRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

/**
 * Detailed view of a task assigned by the HOD.
 * Enables the faculty member to review deadlines, instructions, and update task status.
 */
public class TaskDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";

    private TextView tvTaskId;
    private TextView tvPriority;
    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvAssignedBy;
    private TextView tvDeadline;
    private TextView tvCurrentStatus;
    private Spinner spinnerStatus;
    private Button btnSaveStatus;

    private TaskRepository taskRepository;
    private Task currentTask;
    private String taskId;

    private final List<String> statusOptions = Arrays.asList(
            Task.STATUS_PENDING,
            Task.STATUS_IN_PROGRESS,
            Task.STATUS_COMPLETED
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        taskRepository = TaskRepository.getInstance(this);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null || taskId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid task identifier", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadTaskData();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        tvTaskId = findViewById(R.id.tv_detail_task_id);
        tvPriority = findViewById(R.id.tv_detail_priority);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvAssignedBy = findViewById(R.id.tv_detail_assigned_by);
        tvDeadline = findViewById(R.id.tv_detail_deadline);
        tvCurrentStatus = findViewById(R.id.tv_detail_current_status);
        spinnerStatus = findViewById(R.id.spinner_task_status);
        btnSaveStatus = findViewById(R.id.btn_save_status);

        ImageView btnBack = findViewById(R.id.btn_back_details);
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadTaskData() {
        currentTask = taskRepository.getTaskById(taskId);
        if (currentTask != null) {
            populateTaskData(currentTask);
            return;
        }

        // Live Firestore query fallback if cache is warming up
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("task_assignments").document(taskId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            currentTask = taskRepository.taskFromDocument(doc);
                            if (currentTask != null) {
                                populateTaskData(currentTask);
                                setupSpinner();
                                return;
                            }
                        }
                        // Query by groupTaskId or taskId fields
                        db.collection("task_assignments")
                                .whereEqualTo("groupTaskId", taskId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnap -> {
                                    if (querySnap != null && !querySnap.isEmpty()) {
                                        currentTask = taskRepository.taskFromDocument(querySnap.getDocuments().get(0));
                                        if (currentTask != null) {
                                            populateTaskData(currentTask);
                                            setupSpinner();
                                            return;
                                        }
                                    }
                                    Toast.makeText(TaskDetailsActivity.this, "Task not found (ID: " + taskId + ")", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(TaskDetailsActivity.this, "Task not found (ID: " + taskId + ")", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(TaskDetailsActivity.this, "Task not found: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Task not found (ID: " + taskId + ")", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateTaskData(Task task) {
        if (task == null) return;
        tvTaskId.setText(task.getTaskId());
        tvTitle.setText(task.getTaskTitle());
        tvDescription.setText(task.getDescription());
        tvAssignedBy.setText(task.getAssignedBy());
        tvDeadline.setText(task.getDeadline());

        updateStatusBadge(task.getStatus());
        updatePriorityBadge(task.getPriority());
    }

    private void updateStatusBadge(String status) {
        if (status == null) status = Task.STATUS_PENDING;
        tvCurrentStatus.setText(status.toUpperCase());

        if (Task.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            tvCurrentStatus.setBackgroundResource(R.drawable.bg_status_completed);
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_completed_text));
        } else if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
            tvCurrentStatus.setBackgroundResource(R.drawable.bg_status_inprogress);
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inprogress_text));
        } else {
            tvCurrentStatus.setBackgroundResource(R.drawable.bg_status_pending);
            tvCurrentStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending_text));
        }
    }

    private void updatePriorityBadge(String priority) {
        if (priority == null) priority = Task.PRIORITY_MEDIUM;
        tvPriority.setText(priority.toUpperCase());

        if (Task.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
            tvPriority.setBackgroundResource(R.drawable.bg_priority_high);
            tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_high_text));
        } else if (Task.PRIORITY_LOW.equalsIgnoreCase(priority)) {
            tvPriority.setBackgroundResource(R.drawable.bg_priority_low);
            tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_low_text));
        } else {
            tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
            tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_medium_text));
        }
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                statusOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        // Pre-select current task status
        if (currentTask != null && currentTask.getStatus() != null) {
            int position = statusOptions.indexOf(currentTask.getStatus().toUpperCase());
            if (position >= 0) {
                spinnerStatus.setSelection(position);
            }
        }
    }

    private void setupListeners() {
        btnSaveStatus.setOnClickListener(v -> saveTaskStatus());
    }

    private void saveTaskStatus() {
        Object selectedItem = spinnerStatus.getSelectedItem();
        if (selectedItem == null) {
            Toast.makeText(this, R.string.invalid_status_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        String newStatus = selectedItem.toString().trim().toUpperCase();

        // Validate selection against accepted values
        if (!statusOptions.contains(newStatus)) {
            Toast.makeText(this, R.string.invalid_status_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if status is actually changing
        if (currentTask != null && newStatus.equalsIgnoreCase(currentTask.getStatus())) {
            Toast.makeText(this, R.string.status_update_unchanged, Toast.LENGTH_SHORT).show();
            return;
        }

        // Update in persistent repository
        boolean success = taskRepository.updateTaskStatus(taskId, newStatus);
        if (success) {
            currentTask.setStatus(newStatus);
            updateStatusBadge(newStatus);
            Toast.makeText(this, R.string.status_update_success, Toast.LENGTH_SHORT).show();

            // Return to My Tasks with updated status
            finish();
        } else {
            Toast.makeText(this, "Failed to update task status", Toast.LENGTH_SHORT).show();
        }
    }
}
