package com.example.deptflow.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.FacultyDashboardActivity;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.repository.SessionManager;
import com.example.deptflow.hod.HodDashboardActivity;

/**
 * Authentication placeholder for Member 1 (Login/Auth Module).
 * Provides clean entry points into the Faculty Module and HOD Module.
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final SessionManager sessionManager = SessionManager.getInstance(this);

        findViewById(R.id.btn_login_faculty).setOnClickListener(v -> {
            FacultyUser faculty = new FacultyUser(
                    "FAC-102",
                    "Dr. Sarah Jenkins",
                    "sarah.jenkins@deptflow.edu",
                    "Information Technology",
                    SessionManager.ROLE_FACULTY
            );
            sessionManager.saveFacultyUser(faculty);

            Intent intent = new Intent(LoginActivity.this, FacultyDashboardActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_login_hod).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, HodDashboardActivity.class);
            startActivity(intent);
        });
    }
}
