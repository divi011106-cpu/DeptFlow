package com.example.deptflow.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.FacultyDashboardActivity;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.hod.HodDashboardActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * LoginActivity — Login & Auth Module (Member 1).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Session check on launch → routes to correct dashboard if already logged in</li>
 *   <li>Role selection (FACULTY / HOD) with visual toggle</li>
 *   <li>Email + password input with full validation</li>
 *   <li>Delegates credential verification to {@link AuthManager}</li>
 *   <li>Role-mismatch detection after successful password check</li>
 *   <li>Navigation to correct existing dashboard after successful login</li>
 *   <li>Links to ForgotPasswordActivity and RegisterActivity</li>
 * </ul>
 * </p>
 *
 * <p><b>Other modules are NOT modified.</b> Navigation reaches the existing
 * {@link FacultyDashboardActivity} and {@link HodDashboardActivity} directly.</p>
 */
public class LoginActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────
    private TextInputLayout    tilEmail;
    private TextInputLayout    tilPassword;
    private TextInputEditText  etEmail;
    private TextInputEditText  etPassword;
    private Button             btnRoleFaculty;
    private Button             btnRoleHod;
    private Button             btnLogin;
    private ProgressBar        progressLogin;
    private TextView           tvForgotPassword;
    private TextView           tvRegisterLink;

    // ── State ─────────────────────────────────────────────────────────────
    private AuthManager authManager;
    private String      selectedRole = AuthManager.ROLE_FACULTY; // default

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = AuthManager.getInstance(this);

        // ── Session check: if already logged in, skip login screen ────────
        if (authManager.isLoggedIn()) {
            FacultyUser user = authManager.getCurrentUser();
            if (user != null) {
                routeToDashboard(user.getRole(), true);
                return; // don't inflate layout; we navigated away
            }
        }

        setContentView(R.layout.activity_login);
        initViews();
        applyRoleSelection(AuthManager.ROLE_FACULTY); // default FACULTY selected
        setupClickListeners();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────────

    private void initViews() {
        tilEmail        = findViewById(R.id.til_email);
        tilPassword     = findViewById(R.id.til_password);
        etEmail         = findViewById(R.id.et_email);
        etPassword      = findViewById(R.id.et_password);
        btnRoleFaculty  = findViewById(R.id.btn_role_faculty);
        btnRoleHod      = findViewById(R.id.btn_role_hod);
        btnLogin        = findViewById(R.id.btn_login_faculty);   // id preserved for compatibility
        progressLogin   = findViewById(R.id.progress_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegisterLink  = findViewById(R.id.tv_register_link);
    }

    private void setupClickListeners() {
        // ── Role toggle ───────────────────────────────────────────────────
        btnRoleFaculty.setOnClickListener(v -> applyRoleSelection(AuthManager.ROLE_FACULTY));
        btnRoleHod.setOnClickListener(v -> applyRoleSelection(AuthManager.ROLE_HOD));

        // ── Login ─────────────────────────────────────────────────────────
        btnLogin.setOnClickListener(v -> attemptLogin());

        // ── Forgot password ───────────────────────────────────────────────
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // ── Register ──────────────────────────────────────────────────────
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Role toggle
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Visually highlights the selected role button and dims the other.
     * Updates {@link #selectedRole} used during login.
     */
    private void applyRoleSelection(String role) {
        selectedRole = role;

        if (AuthManager.ROLE_FACULTY.equals(role)) {
            btnRoleFaculty.setBackground(getDrawable(R.drawable.bg_role_selected));
            btnRoleFaculty.setTextColor(getColor(R.color.text_on_primary));
            btnRoleHod.setBackground(getDrawable(R.drawable.bg_role_unselected_new));
            btnRoleHod.setTextColor(getColor(R.color.text_primary));
        } else {
            btnRoleHod.setBackground(getDrawable(R.drawable.bg_role_selected));
            btnRoleHod.setTextColor(getColor(R.color.text_on_primary));
            btnRoleFaculty.setBackground(getDrawable(R.drawable.bg_role_unselected_new));
            btnRoleFaculty.setTextColor(getColor(R.color.text_primary));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Login flow
    // ─────────────────────────────────────────────────────────────────────

    private void attemptLogin() {
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString()     : "";

        // ── Validation ────────────────────────────────────────────────────
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

        // ── Authenticate (async — Firebase network call) ──────────────────
        setLoading(true);
        authManager.login(email, password, selectedRole, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FacultyUser user) {
                setLoading(false);
                routeToDashboard(user.getRole(), false);
            }

            @Override
            public void onFailure(AuthManager.AuthResult.Code code) {
                setLoading(false);
                handleLoginError(code);
            }
        });
    }

    /** Maps AuthResult error codes to user-visible error messages. */
    private void handleLoginError(AuthManager.AuthResult.Code code) {
        switch (code) {
            case ACCOUNT_NOT_FOUND:
                tilEmail.setError(getString(R.string.error_account_not_found));
                tilEmail.requestFocus();
                break;
            case INVALID_CREDENTIALS:
                tilPassword.setError(getString(R.string.error_invalid_credentials));
                tilPassword.requestFocus();
                break;
            case ROLE_MISMATCH_IS_HOD:
                tilPassword.setError(getString(R.string.error_role_mismatch_faculty));
                break;
            case ROLE_MISMATCH_IS_FACULTY:
                tilPassword.setError(getString(R.string.error_role_mismatch_hod));
                break;
            default:
                tilPassword.setError(getString(R.string.error_generic));
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Routes the authenticated user to the correct existing dashboard.
     * Uses FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK so the login
     * screen is removed from the back-stack — back-pressing the dashboard
     * will exit the app, not return to login.
     *
     * @param role         the verified role from backend storage
     * @param isAutoLogin  true when routing from a pre-existing session
     */
    private void routeToDashboard(String role, boolean isAutoLogin) {
        Intent intent;
        if (AuthManager.ROLE_HOD.equalsIgnoreCase(role)) {
            intent = new Intent(this, HodDashboardActivity.class);
        } else {
            intent = new Intent(this, FacultyDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRoleFaculty.setEnabled(!loading);
        btnRoleHod.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
}
