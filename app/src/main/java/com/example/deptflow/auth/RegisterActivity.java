package com.example.deptflow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


/**
 * RegisterActivity — Login & Auth Module (Member 1).
 *
 * <p>Allows new users to create a FACULTY account. HOD accounts require
 * an institution-issued admin key to prevent unauthorized role escalation.</p>
 *
 * <p>Passwords are NEVER stored in plain text. All credential handling is
 * delegated to {@link AuthManager} which applies SHA-256 hashing.</p>
 */
public class RegisterActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────
    private TextInputLayout    tilName;
    private TextInputLayout    tilEmail;
    private TextInputLayout    tilPassword;
    private TextInputLayout    tilConfirmPassword;
    private TextInputLayout    tilAdminKey;
    private TextInputEditText  etName;
    private TextInputEditText  etEmail;
    private TextInputEditText  etPassword;
    private TextInputEditText  etConfirmPassword;
    private TextInputEditText  etAdminKey;
    private Button             btnRoleFaculty;
    private Button             btnRoleHod;
    private Button             btnRegister;
    private ProgressBar        progressRegister;
    private TextView           tvHodNote;
    private TextView           tvBackToLogin;

    // ── State ─────────────────────────────────────────────────────────────
    private AuthManager authManager;
    private String      selectedRole = AuthManager.ROLE_FACULTY;

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authManager = AuthManager.getInstance(this);
        initViews();
        applyRoleSelection(AuthManager.ROLE_FACULTY);
        setupClickListeners();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────

    private void initViews() {
        tilName            = findViewById(R.id.til_reg_name);
        tilEmail           = findViewById(R.id.til_reg_email);
        tilPassword        = findViewById(R.id.til_reg_password);
        tilConfirmPassword = findViewById(R.id.til_reg_confirm_password);
        tilAdminKey        = findViewById(R.id.til_reg_admin_key);
        etName             = findViewById(R.id.et_reg_name);
        etEmail            = findViewById(R.id.et_reg_email);
        etPassword         = findViewById(R.id.et_reg_password);
        etConfirmPassword  = findViewById(R.id.et_reg_confirm_password);
        etAdminKey         = findViewById(R.id.et_reg_admin_key);
        btnRoleFaculty     = findViewById(R.id.btn_reg_role_faculty);
        btnRoleHod         = findViewById(R.id.btn_reg_role_hod);
        btnRegister        = findViewById(R.id.btn_register);
        progressRegister   = findViewById(R.id.progress_register);
        tvHodNote          = findViewById(R.id.tv_hod_note);
        tvBackToLogin      = findViewById(R.id.tv_back_to_login);
    }

    private void setupClickListeners() {
        btnRoleFaculty.setOnClickListener(v -> applyRoleSelection(AuthManager.ROLE_FACULTY));
        btnRoleHod.setOnClickListener(v -> applyRoleSelection(AuthManager.ROLE_HOD));
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Role toggle
    // ─────────────────────────────────────────────────────────────────────

    private void applyRoleSelection(String role) {
        selectedRole = role;
        boolean isHod = AuthManager.ROLE_HOD.equals(role);

        if (isHod) {
            btnRoleHod.setBackground(getDrawable(R.drawable.bg_role_selected));
            btnRoleHod.setTextColor(getColor(R.color.text_on_primary));
            btnRoleFaculty.setBackground(getDrawable(R.drawable.bg_role_unselected_new));
            btnRoleFaculty.setTextColor(getColor(R.color.text_primary));
        } else {
            btnRoleFaculty.setBackground(getDrawable(R.drawable.bg_role_selected));
            btnRoleFaculty.setTextColor(getColor(R.color.text_on_primary));
            btnRoleHod.setBackground(getDrawable(R.drawable.bg_role_unselected_new));
            btnRoleHod.setTextColor(getColor(R.color.text_primary));
        }

        // Show/hide admin key field and HOD note
        tilAdminKey.setVisibility(isHod ? View.VISIBLE : View.GONE);
        tvHodNote.setVisibility(isHod ? View.VISIBLE : View.GONE);
        if (!isHod && etAdminKey.getText() != null) {
            etAdminKey.getText().clear();
            tilAdminKey.setError(null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Registration flow
    // ─────────────────────────────────────────────────────────────────────

    private void attemptRegister() {
        // Clear errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilAdminKey.setError(null);

        String name     = etName.getText()            != null ? etName.getText().toString().trim()            : "";
        String email    = etEmail.getText()           != null ? etEmail.getText().toString().trim()            : "";
        String password = etPassword.getText()        != null ? etPassword.getText().toString()               : "";
        String confirm  = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString()        : "";
        String adminKey = etAdminKey.getText()        != null ? etAdminKey.getText().toString().trim()        : "";

        // ── Validation ────────────────────────────────────────────────────
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_empty_name));
            tilName.requestFocus();
            return;
        }
        if (name.trim().length() < 2) {
            tilName.setError(getString(R.string.error_short_name));
            tilName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_email));
            tilEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            tilEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_password));
            tilPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_short_password));
            tilPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            tilConfirmPassword.requestFocus();
            return;
        }
        if (AuthManager.ROLE_HOD.equals(selectedRole) && TextUtils.isEmpty(adminKey)) {
            tilAdminKey.setError(getString(R.string.error_empty_admin_key));
            tilAdminKey.requestFocus();
            return;
        }

        // ── Register (async — Firebase network call) ──────────────────────
        setLoading(true);
        authManager.register(name, email, password, selectedRole, adminKey,
                new AuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(FacultyUser user) {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.register_success),
                                Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(AuthManager.AuthResult.Code code) {
                        setLoading(false);
                        handleRegisterError(code);
                    }
                });
    }


    private void handleRegisterError(AuthManager.AuthResult.Code code) {
        switch (code) {
            case EMAIL_TAKEN:
                tilEmail.setError(getString(R.string.error_email_taken));
                tilEmail.requestFocus();
                break;
            case INVALID_ADMIN_KEY:
                tilAdminKey.setError(getString(R.string.error_invalid_admin_key));
                tilAdminKey.requestFocus();
                break;
            default:
                Toast.makeText(this, getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
