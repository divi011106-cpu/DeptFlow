package com.example.deptflow.feature.faculty;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.repository.SessionManager;

/**
 * Activity presenting the faculty member's profile, credentials, and departmental role.
 */
public class FacultyProfileActivity extends AppCompatActivity {

    private TextView tvHeaderName;
    private TextView tvRolePill;
    private TextView tvFullName;
    private TextView tvEmail;
    private TextView tvDepartment;
    private TextView tvRole;
    private TextView tvUserId;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_profile);

        sessionManager = SessionManager.getInstance(this);

        initViews();
        loadProfileData();
        setupListeners();
    }

    private void initViews() {
        tvHeaderName = findViewById(R.id.tv_profile_name_header);
        tvRolePill = findViewById(R.id.tv_profile_role_pill);
        tvFullName = findViewById(R.id.tv_profile_fullname);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvDepartment = findViewById(R.id.tv_profile_dept);
        tvRole = findViewById(R.id.tv_profile_role);
        tvUserId = findViewById(R.id.tv_profile_user_id);

        ImageView btnBack = findViewById(R.id.btn_back_profile);
        btnBack.setOnClickListener(v -> finish());

        Button btnBackBottom = findViewById(R.id.btn_back_bottom);
        btnBackBottom.setOnClickListener(v -> finish());
    }

    private void loadProfileData() {
        FacultyUser user = sessionManager.getCurrentUser();
        if (user != null) {
            tvHeaderName.setText(user.getName());
            tvRolePill.setText(user.getRole());
            tvFullName.setText(user.getName());
            tvEmail.setText(user.getEmail());
            tvDepartment.setText(user.getDepartment());
            tvRole.setText(user.getRole());
            tvUserId.setText(user.getUserId());
        }
    }

    private void setupListeners() {
        Button btnEdit = findViewById(R.id.btn_edit_profile);
        btnEdit.setOnClickListener(v -> {
            new AlertDialog.Builder(FacultyProfileActivity.this)
                    .setTitle(R.string.profile_edit_dialog_title)
                    .setMessage(R.string.profile_edit_msg)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        });
    }
}
