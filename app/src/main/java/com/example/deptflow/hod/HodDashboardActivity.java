package com.example.deptflow.hod;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.auth.LoginActivity;

public class HodDashboardActivity extends AppCompatActivity {

    Button btnAssignTask, btnViewTasks, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_dashboard);

        btnAssignTask = findViewById(R.id.btnAssignTask);
        btnViewTasks = findViewById(R.id.btnViewTasks);
        btnLogout = findViewById(R.id.btnLogout);

        // Assign Task
        btnAssignTask.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HodDashboardActivity.this,
                    AssignTaskActivity.class);
            startActivity(intent);
        });

        // View Tasks
        btnViewTasks.setOnClickListener(v -> {
            Intent intent = new Intent(
                    HodDashboardActivity.this,
                    ViewTasksActivity.class);
            startActivity(intent);
        });

        // Logout
        btnLogout.setOnClickListener(v -> {

            // Clear login session
            com.example.deptflow.auth.AuthManager
                    .getInstance(HodDashboardActivity.this)
                    .logout();

            Intent intent = new Intent(
                    HodDashboardActivity.this,
                    LoginActivity.class);

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        });
    }
}