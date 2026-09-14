package com.example.deptflow.feature.faculty;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.repository.SessionManager;
import com.example.deptflow.feature.faculty.repository.TaskRepository;

/**
 * Main dashboard for Faculty members in DeptFlow.
 * Displays greeting, role, department, calculated task statistics,
 * and quick-action navigation cards.
 */
public class FacultyDashboardActivity extends AppCompatActivity {

    private TextView tvFacultyName;
    private TextView tvFacultyRole;
    private TextView tvFacultyDept;
    private TextView tvCountTotal;
    private TextView tvCountPending;
    private TextView tvCountInProgress;
    private TextView tvCountCompleted;

    private SessionManager sessionManager;
    private TaskRepository taskRepository;
    private FacultyUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        sessionManager = SessionManager.getInstance(this);
        taskRepository = TaskRepository.getInstance(this);

        initViews();
        setupUserData();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data and statistics when returning from MyTasks or TaskDetails
        setupUserData();
    }

    private void initViews() {
        tvFacultyName = findViewById(R.id.tv_faculty_name);
        tvFacultyRole = findViewById(R.id.tv_faculty_role);
        tvFacultyDept = findViewById(R.id.tv_faculty_dept);

        tvCountTotal = findViewById(R.id.tv_count_total);
        tvCountPending = findViewById(R.id.tv_count_pending);
        tvCountInProgress = findViewById(R.id.tv_count_inprogress);
        tvCountCompleted = findViewById(R.id.tv_count_completed);
    }

    private void setupUserData() {
        currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            tvFacultyName.setText(currentUser.getName());
            tvFacultyRole.setText(currentUser.getRole());
            tvFacultyDept.setText(currentUser.getDepartment());
        }
        updateStatistics();
    }

    private void updateStatistics() {
        currentUser = sessionManager.getCurrentUser();
        int total = taskRepository.getTotalTaskCount(currentUser);
        int pending = taskRepository.getPendingTaskCount(currentUser);
        int inProgress = taskRepository.getInProgressTaskCount(currentUser);
        int completed = taskRepository.getCompletedTaskCount(currentUser);

        tvCountTotal.setText(String.valueOf(total));
        tvCountPending.setText(String.valueOf(pending));
        tvCountInProgress.setText(String.valueOf(inProgress));
        tvCountCompleted.setText(String.valueOf(completed));

        android.util.Log.d("DEPTFLOW_SYNC_DEBUG", "Faculty TaskData class = " + com.example.deptflow.hod.TaskData.class.getName());
        android.util.Log.d("DEPTFLOW_SYNC_DEBUG", "Faculty TaskData tasks size = " + com.example.deptflow.hod.TaskData.tasks.size());
        android.util.Log.d("DEPTFLOW_SYNC_DEBUG", "Faculty task = " + (com.example.deptflow.hod.TaskData.tasks.isEmpty() ? "none" : com.example.deptflow.hod.TaskData.tasks.get(0)));
        android.util.Log.d("DEPTFLOW_SYNC_DEBUG", "Faculty TaskData list identity = " + System.identityHashCode(com.example.deptflow.hod.TaskData.tasks));
        android.util.Log.d("DEPTFLOW_SYNC_DEBUG", "Current faculty = " + (currentUser != null ? currentUser.getName() : "null"));
        android.util.Log.d("DEPTFLOW_SYNC_DEBUG", "Matched task count = " + total);
    }

    private void setupClickListeners() {
        // My Tasks Action
        findViewById(R.id.card_action_my_tasks).setOnClickListener(v -> {
            Intent intent = new Intent(FacultyDashboardActivity.this, MyTasksActivity.class);
            startActivity(intent);
        });

        // Communication Action
        findViewById(R.id.card_action_communication).setOnClickListener(v -> {
            try {
                // Navigate to Communication module if implemented
                Class<?> commClass = Class.forName("com.example.deptflow.communication.CommunicationActivity");
                Intent intent = new Intent(FacultyDashboardActivity.this, commClass);
                startActivity(intent);
            } catch (ClassNotFoundException e) {
                Toast.makeText(FacultyDashboardActivity.this,
                        "Communication module is being developed by Member 4",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Profile Action
        findViewById(R.id.card_action_profile).setOnClickListener(v -> {
            Intent intent = new Intent(FacultyDashboardActivity.this, FacultyProfileActivity.class);
            startActivity(intent);
        });

        // Logout Action
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_logout)
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    // 1. Sign out from Firebase — without this, LoginActivity sees
                    //    getCurrentUser() != null and immediately re-routes back here.
                    com.example.deptflow.auth.AuthManager.getInstance(FacultyDashboardActivity.this).logout();

                    // 2. Clear SessionManager's SharedPreferences flag for consistency.
                    sessionManager.logout();

                    // 3. Navigate to LoginActivity and clear the entire back stack so the
                    //    user cannot return to the dashboard with the Back button.
                    Intent intent = new Intent(FacultyDashboardActivity.this,
                            com.example.deptflow.auth.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    // 4. Destroy this activity explicitly.
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}
