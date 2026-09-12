package com.example.deptflow.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.deptflow.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * ForgotPasswordActivity — Login & Auth Module (Member 1).
 *
 * <p>Allows a user to reset their password by entering their registered email.
 * A temporary password is generated and displayed on screen. The user must
 * login with this temporary password immediately after.</p>
 *
 * <p>Because this project uses local auth (no email server), the temporary
 * password is shown inline. In a Firebase-based project this would instead
 * call FirebaseAuth.sendPasswordResetEmail() and show a confirmation message.</p>
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────
    private TextInputLayout   tilResetEmail;
    private TextInputEditText etResetEmail;
    private Button            btnSendReset;
    private Button            btnBackToLogin;
    private ProgressBar       progressReset;
    private CardView          cardResetResult;
    private TextView          tvTempPassword;

    // ── State ─────────────────────────────────────────────────────────────
    private AuthManager authManager;

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authManager = AuthManager.getInstance(this);
        initViews();
        setupClickListeners();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────

    private void initViews() {
        tilResetEmail   = findViewById(R.id.til_reset_email);
        etResetEmail    = findViewById(R.id.et_reset_email);
        btnSendReset    = findViewById(R.id.btn_send_reset);
        btnBackToLogin  = findViewById(R.id.btn_back_to_login);
        progressReset   = findViewById(R.id.progress_reset);
        cardResetResult = findViewById(R.id.card_reset_result);
        tvTempPassword  = findViewById(R.id.tv_temp_password);
    }

    private void setupClickListeners() {
        btnSendReset.setOnClickListener(v -> attemptPasswordReset());
        btnBackToLogin.setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Password reset flow
    // ─────────────────────────────────────────────────────────────────────

    private void attemptPasswordReset() {
        tilResetEmail.setError(null);
        cardResetResult.setVisibility(View.GONE);

        String email = etResetEmail.getText() != null
                ? etResetEmail.getText().toString().trim() : "";

        // ── Validation ────────────────────────────────────────────────────
        if (TextUtils.isEmpty(email)) {
            tilResetEmail.setError(getString(R.string.error_empty_email));
            tilResetEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilResetEmail.setError(getString(R.string.error_invalid_email));
            tilResetEmail.requestFocus();
            return;
        }

        // ── Send reset email (async — Firebase network call) ───────────────
        setLoading(true);
        authManager.resetPassword(email, new AuthManager.ResetCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                // Show success card — Firebase has sent the reset email
                cardResetResult.setVisibility(View.VISIBLE);
                tvTempPassword.setText(getString(R.string.reset_email_sent_msg));
                btnSendReset.setEnabled(false);
                tilResetEmail.setEnabled(false);
            }

            @Override
            public void onFailure(String errorCode) {
                setLoading(false);
                if ("no_user".equals(errorCode)) {
                    tilResetEmail.setError(getString(R.string.error_email_not_registered));
                } else {
                    tilResetEmail.setError(getString(R.string.error_generic));
                }
                tilResetEmail.requestFocus();
            }
        });
    }


    // ─────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressReset.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!loading);
    }
}
