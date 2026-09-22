package com.example.deptflow.hod;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.auth.LoginActivity;
import com.example.deptflow.feature.faculty.models.Task;
import com.example.deptflow.feature.faculty.repository.TaskRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class HodDashboardActivity extends AppCompatActivity {

    private ImageView ivHodProfile;
    private TextView tvHodName;

    private MaterialCardView cardAssignTask, cardViewTasks, cardReports, cardProfile, cardLogout;
    private MaterialCardView cardStatTotal, cardStatAssigned, cardStatInProgress, cardStatCompleted;

    private TextView tvTotalTasksCount, tvAssignedTasksCount, tvInProgressTasksCount, tvCompletedTasksCount;
    private TextView tvLiveStatusIndicator;

    private FirebaseFirestore firestore;
    private ListenerRegistration tasksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_dashboard);

        initViews();
        setupNavigation();
        loadProfileHeader();
        setupFirestoreLiveStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileHeader();
    }

    private void initViews() {
        ivHodProfile = findViewById(R.id.ivHodProfile);
        tvHodName = findViewById(R.id.tvHodName);

        cardAssignTask = findViewById(R.id.cardAssignTask);
        cardViewTasks = findViewById(R.id.cardViewTasks);
        cardReports = findViewById(R.id.cardReports);
        cardProfile = findViewById(R.id.cardProfile);
        cardLogout = findViewById(R.id.cardLogout);

        cardStatTotal = findViewById(R.id.cardStatTotal);
        cardStatAssigned = findViewById(R.id.cardStatAssigned);
        cardStatInProgress = findViewById(R.id.cardStatInProgress);
        cardStatCompleted = findViewById(R.id.cardStatCompleted);

        tvTotalTasksCount = findViewById(R.id.tvTotalTasksCount);
        tvAssignedTasksCount = findViewById(R.id.tvAssignedTasksCount);
        tvInProgressTasksCount = findViewById(R.id.tvInProgressTasksCount);
        tvCompletedTasksCount = findViewById(R.id.tvCompletedTasksCount);
        tvLiveStatusIndicator = findViewById(R.id.tvLiveStatusIndicator);
    }

    private void loadProfileHeader() {
        SharedPreferences prefs = getSharedPreferences(HodProfileActivity.PREF_HOD_PROFILE, Context.MODE_PRIVATE);
        String savedName = prefs.getString(HodProfileActivity.KEY_HOD_NAME, "Dr. R. Kavitha");
        String base64Img = prefs.getString(HodProfileActivity.KEY_HOD_IMAGE_BASE64, "");

        if (tvHodName != null) {
            tvHodName.setText(savedName);
        }

        if (ivHodProfile != null && !base64Img.isEmpty()) {
            Bitmap bitmap = HodProfileActivity.decodeBase64ToBitmap(base64Img);
            if (bitmap != null) {
                ivHodProfile.setImageBitmap(bitmap);
            }
        }
    }

    private void setupNavigation() {
        // 1. Profile Picture Click
        if (ivHodProfile != null) {
            ivHodProfile.setOnClickListener(v -> openHodProfile());
        }

        // 2. Assign Task
        if (cardAssignTask != null) {
            cardAssignTask.setOnClickListener(v -> {
                Intent intent = new Intent(HodDashboardActivity.this, AssignTaskActivity.class);
                startActivity(intent);
            });
        }

        // 3. View Tasks
        if (cardViewTasks != null) {
            cardViewTasks.setOnClickListener(v -> {
                Intent intent = new Intent(HodDashboardActivity.this, ViewTasksActivity.class);
                startActivity(intent);
            });
        }

        // 4. Reports & Analytics
        if (cardReports != null) {
            cardReports.setOnClickListener(v -> showReportsSummaryDialog());
        }

        // 5. HOD Profile Card
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> openHodProfile());
        }

        // 6. Logout
        if (cardLogout != null) {
            cardLogout.setOnClickListener(v -> showLogoutDialog());
        }

        // Quick Stats Click Handlers
        if (cardStatTotal != null) {
            cardStatTotal.setOnClickListener(v -> navigateToTasksWithFilter("ALL"));
        }
        if (cardStatAssigned != null) {
            cardStatAssigned.setOnClickListener(v -> navigateToTasksWithFilter("Assigned"));
        }
        if (cardStatInProgress != null) {
            cardStatInProgress.setOnClickListener(v -> navigateToTasksWithFilter("In Progress"));
        }
        if (cardStatCompleted != null) {
            cardStatCompleted.setOnClickListener(v -> navigateToTasksWithFilter("Completed"));
        }
    }

    private void openHodProfile() {
        Intent intent = new Intent(HodDashboardActivity.this, HodProfileActivity.class);
        startActivity(intent);
    }

    private void navigateToTasksWithFilter(String status) {
        Intent intent = new Intent(HodDashboardActivity.this, ViewTasksActivity.class);
        intent.putExtra("filter_status", status);
        startActivity(intent);
    }

    private void setupFirestoreLiveStats() {
        try {
            firestore = FirebaseFirestore.getInstance();
            tasksListener = firestore.collection("task_assignments")
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            if (tvLiveStatusIndicator != null) {
                                tvLiveStatusIndicator.setText("• Offline Mode");
                                tvLiveStatusIndicator.setTextColor(getColor(R.color.text_secondary));
                            }
                            updateFallbackStats();
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            int total = snapshots.size();
                            int assigned = 0;
                            int inProgress = 0;
                            int completed = 0;

                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String status = doc.getString("status");
                                if (status == null) status = "";
                                String s = status.trim().toUpperCase();

                                if (s.contains("COMPLET")) {
                                    completed++;
                                } else if (s.contains("PROGRESS")) {
                                    inProgress++;
                                } else {
                                    assigned++;
                                }
                            }

                            if (tvTotalTasksCount != null) tvTotalTasksCount.setText(String.valueOf(total));
                            if (tvAssignedTasksCount != null) tvAssignedTasksCount.setText(String.valueOf(assigned));
                            if (tvInProgressTasksCount != null) tvInProgressTasksCount.setText(String.valueOf(inProgress));
                            if (tvCompletedTasksCount != null) tvCompletedTasksCount.setText(String.valueOf(completed));

                            if (tvLiveStatusIndicator != null) {
                                tvLiveStatusIndicator.setText("• Live Firestore");
                                tvLiveStatusIndicator.setTextColor(getColor(R.color.status_completed));
                            }
                        } else {
                            updateFallbackStats();
                        }
                    });
        } catch (Exception e) {
            if (tvLiveStatusIndicator != null) {
                tvLiveStatusIndicator.setText("• Local Cache");
            }
            updateFallbackStats();
        }
    }

    private void updateFallbackStats() {
        try {
            List<Task> tasks = TaskRepository.getInstance(this).getHodTasks();
            int total = tasks.size();
            int assigned = 0;
            int inProgress = 0;
            int completed = 0;

            for (Task t : tasks) {
                String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
                if (s.contains("COMPLET")) completed++;
                else if (s.contains("PROGRESS")) inProgress++;
                else assigned++;
            }

            if (tvTotalTasksCount != null) tvTotalTasksCount.setText(String.valueOf(total));
            if (tvAssignedTasksCount != null) tvAssignedTasksCount.setText(String.valueOf(assigned));
            if (tvInProgressTasksCount != null) tvInProgressTasksCount.setText(String.valueOf(inProgress));
            if (tvCompletedTasksCount != null) tvCompletedTasksCount.setText(String.valueOf(completed));
        } catch (Exception ignored) {}
    }

    private void showReportsSummaryDialog() {
        String total = tvTotalTasksCount != null ? tvTotalTasksCount.getText().toString() : "0";
        String completed = tvCompletedTasksCount != null ? tvCompletedTasksCount.getText().toString() : "0";
        String assigned = tvAssignedTasksCount != null ? tvAssignedTasksCount.getText().toString() : "0";
        String inProg = tvInProgressTasksCount != null ? tvInProgressTasksCount.getText().toString() : "0";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Department Performance Summary")
                .setMessage("Total Tasks Delegated: " + total + "\n"
                        + "Assigned Tasks: " + assigned + "\n"
                        + "In Progress: " + inProg + "\n"
                        + "Completed Tasks: " + completed + "\n\n"
                        + "Department Health: Active & Synchronized with Firestore.")
                .setPositiveButton("Close", null)
                .show();
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to securely exit the HOD Portal?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    AuthManager.getInstance(HodDashboardActivity.this).logout();

                    Intent intent = new Intent(HodDashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
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