package com.example.deptflow.feature.faculty;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.repository.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity presenting and editing the faculty member's profile, credentials, and photo.
 */
public class FacultyProfileActivity extends AppCompatActivity {

    private static final String TAG = "FacultyProfileActivity";

    private TextView tvHeaderName;
    private TextView tvRolePill;
    private TextView tvFullName;
    private EditText etFullName;
    private TextView tvEmail;
    private EditText etEmail;
    private TextView tvDepartment;
    private EditText etDepartment;
    private TextView tvRole;
    private EditText etRole;
    private TextView tvUserId;

    private ImageView ivProfileAvatar;
    private FrameLayout layoutProfileAvatar;
    private Button btnEdit;
    private Button btnBackBottom;

    private SessionManager sessionManager;
    private FacultyUser currentUser;

    private boolean isEditMode = false;
    private String savedImageBase64 = "";
    private String pendingImageBase64 = "";

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processPickedImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_profile);

        sessionManager = SessionManager.getInstance(this);

        initViews();
        loadProfileData();
        setupListeners();
        fetchCloudProfile();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackAction();
            }
        });
    }

    private void initViews() {
        tvHeaderName = findViewById(R.id.tv_profile_name_header);
        tvRolePill = findViewById(R.id.tv_profile_role_pill);
        tvFullName = findViewById(R.id.tv_profile_fullname);
        etFullName = findViewById(R.id.et_profile_fullname);
        tvEmail = findViewById(R.id.tv_profile_email);
        etEmail = findViewById(R.id.et_profile_email);
        tvDepartment = findViewById(R.id.tv_profile_dept);
        etDepartment = findViewById(R.id.et_profile_dept);
        tvRole = findViewById(R.id.tv_profile_role);
        etRole = findViewById(R.id.et_profile_role);
        tvUserId = findViewById(R.id.tv_profile_user_id);

        ivProfileAvatar = findViewById(R.id.iv_profile_avatar);
        layoutProfileAvatar = findViewById(R.id.layout_profile_avatar);

        btnEdit = findViewById(R.id.btn_edit_profile);
        btnBackBottom = findViewById(R.id.btn_back_bottom);

        ImageView btnBack = findViewById(R.id.btn_back_profile);
        btnBack.setOnClickListener(v -> handleBackAction());
        btnBackBottom.setOnClickListener(v -> handleBackAction());
    }

    private void loadProfileData() {
        currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            tvHeaderName.setText(currentUser.getName());
            tvRolePill.setText(currentUser.getRole());
            tvFullName.setText(currentUser.getName());
            tvEmail.setText(currentUser.getEmail());
            tvDepartment.setText(currentUser.getDepartment());
            tvRole.setText(currentUser.getRole());
            tvUserId.setText(currentUser.getUserId());
        }

        String userId = (currentUser != null) ? currentUser.getUserId() : "FAC-102";
        savedImageBase64 = sessionManager.getProfileImage(userId);
        pendingImageBase64 = savedImageBase64;
        loadSavedProfileImage();
    }

    private void setupListeners() {
        View.OnClickListener pickPhotoAction = v -> imagePickerLauncher.launch("image/*");
        if (layoutProfileAvatar != null) {
            layoutProfileAvatar.setOnClickListener(pickPhotoAction);
        }
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(pickPhotoAction);
        }

        btnEdit.setOnClickListener(v -> {
            if (isEditMode) {
                saveProfileChanges();
            } else {
                enterEditMode();
            }
        });
    }

    private void enterEditMode() {
        isEditMode = true;
        if (currentUser != null) {
            etFullName.setText(currentUser.getName());
            etEmail.setText(currentUser.getEmail());
            etDepartment.setText(currentUser.getDepartment());
            etRole.setText(currentUser.getRole());
        }

        tvFullName.setVisibility(View.GONE);
        etFullName.setVisibility(View.VISIBLE);

        tvEmail.setVisibility(View.GONE);
        etEmail.setVisibility(View.VISIBLE);

        tvDepartment.setVisibility(View.GONE);
        etDepartment.setVisibility(View.VISIBLE);

        tvRole.setVisibility(View.GONE);
        etRole.setVisibility(View.VISIBLE);

        btnEdit.setText(R.string.btn_save_changes);
        btnBackBottom.setText(R.string.cancel);
    }

    private void cancelEditMode() {
        isEditMode = false;
        pendingImageBase64 = savedImageBase64;
        loadSavedProfileImage();

        tvFullName.setVisibility(View.VISIBLE);
        etFullName.setVisibility(View.GONE);

        tvEmail.setVisibility(View.VISIBLE);
        etEmail.setVisibility(View.GONE);

        tvDepartment.setVisibility(View.VISIBLE);
        etDepartment.setVisibility(View.GONE);

        tvRole.setVisibility(View.VISIBLE);
        etRole.setVisibility(View.GONE);

        btnEdit.setText(R.string.btn_edit_profile);
        btnBackBottom.setText(R.string.btn_back);
    }

    private void saveProfileChanges() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String dept = etDepartment.getText() != null ? etDepartment.getText().toString().trim() : "";
        String role = etRole.getText() != null ? etRole.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etFullName.setError(getString(R.string.error_empty_name));
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_empty_email));
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (dept.isEmpty()) {
            etDepartment.setError("Department cannot be empty");
            etDepartment.requestFocus();
            return;
        }

        if (role.isEmpty()) {
            etRole.setError("Role cannot be empty");
            etRole.requestFocus();
            return;
        }

        if (currentUser == null) {
            currentUser = sessionManager.getCurrentUser();
        }

        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setDepartment(dept);
        currentUser.setRole(role);

        // 1. Persist to SessionManager
        sessionManager.saveFacultyUser(currentUser);
        savedImageBase64 = pendingImageBase64;
        String userId = currentUser.getUserId();
        sessionManager.saveProfileImage(userId, savedImageBase64 != null ? savedImageBase64 : "");

        // 2. Sync to Firebase Firestore if logged in
        syncProfileToCloud(currentUser, savedImageBase64);

        // 3. Update view UI
        tvHeaderName.setText(name);
        tvRolePill.setText(role);
        tvFullName.setText(name);
        tvEmail.setText(email);
        tvDepartment.setText(dept);
        tvRole.setText(role);

        isEditMode = false;
        tvFullName.setVisibility(View.VISIBLE);
        etFullName.setVisibility(View.GONE);

        tvEmail.setVisibility(View.VISIBLE);
        etEmail.setVisibility(View.GONE);

        tvDepartment.setVisibility(View.VISIBLE);
        etDepartment.setVisibility(View.GONE);

        tvRole.setVisibility(View.VISIBLE);
        etRole.setVisibility(View.GONE);

        btnEdit.setText(R.string.btn_edit_profile);
        btnBackBottom.setText(R.string.btn_back);

        Toast.makeText(this, R.string.profile_update_success, Toast.LENGTH_SHORT).show();
    }

    private void handleBackAction() {
        if (isEditMode) {
            cancelEditMode();
        } else {
            finish();
        }
    }

    private void processPickedImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (originalBitmap != null) {
                // Square center crop to prevent distortion
                int origW = originalBitmap.getWidth();
                int origH = originalBitmap.getHeight();
                int cropDim = Math.min(origW, origH);
                int cropX = (origW - cropDim) / 2;
                int cropY = (origH - cropDim) / 2;
                Bitmap squareBitmap = Bitmap.createBitmap(originalBitmap, cropX, cropY, cropDim, cropDim);

                // Scale down to max 512x512
                int targetDim = Math.min(512, cropDim);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, targetDim, targetDim, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                savedImageBase64 = base64Image;
                pendingImageBase64 = base64Image;

                String userId = (currentUser != null) ? currentUser.getUserId() : "FAC-102";
                sessionManager.saveProfileImage(userId, base64Image);
                syncProfileToCloud(currentUser, base64Image);

                displayProfileImage(scaledBitmap);
                Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing picked image", e);
            Toast.makeText(this, "Failed to load selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedProfileImage() {
        if (savedImageBase64 != null && !savedImageBase64.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(savedImageBase64);
            if (bitmap != null) {
                displayProfileImage(bitmap);
                return;
            }
        }
        displayDefaultAvatar();
    }

    private void displayDefaultAvatar() {
        if (ivProfileAvatar == null) return;
        int pad16 = (int) (16 * getResources().getDisplayMetrics().density);
        ivProfileAvatar.setPadding(pad16, pad16, pad16, pad16);
        ivProfileAvatar.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        ivProfileAvatar.setImageResource(R.drawable.ic_person);
        ivProfileAvatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    private void displayProfileImage(Bitmap bitmap) {
        if (ivProfileAvatar == null) return;
        if (bitmap == null) {
            displayDefaultAvatar();
            return;
        }
        float cornerRadiusPx = 20 * getResources().getDisplayMetrics().density;
        Bitmap roundedBitmap = getRoundedBitmap(bitmap, cornerRadiusPx);
        ivProfileAvatar.setPadding(0, 0, 0, 0);
        ivProfileAvatar.setImageTintList(null);
        ivProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivProfileAvatar.setImageBitmap(roundedBitmap);
    }

    private void syncProfileToCloud(FacultyUser user, String imageBase64) {
        try {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                Map<String, Object> updates = new HashMap<>();
                if (user != null) {
                    updates.put("name", user.getName());
                    updates.put("email", user.getEmail());
                    updates.put("department", user.getDepartment());
                    updates.put("role", user.getRole());
                }
                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    updates.put("imageBase64", imageBase64);
                }
                FirebaseFirestore.getInstance().collection("users")
                        .document(firebaseUser.getUid())
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Faculty profile synced to Firestore"))
                        .addOnFailureListener(e -> Log.w(TAG, "Firestore sync failed", e));
            }
        } catch (Exception ignored) {
        }
    }

    private void fetchCloudProfile() {
        try {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(firebaseUser.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc != null && doc.exists() && !isEditMode) {
                                String imageBase64 = doc.getString("imageBase64");
                                if (imageBase64 != null && !imageBase64.isEmpty()) {
                                    String userId = (currentUser != null) ? currentUser.getUserId() : "FAC-102";
                                    sessionManager.saveProfileImage(userId, imageBase64);
                                    savedImageBase64 = imageBase64;
                                    loadSavedProfileImage();
                                }
                            }
                        })
                        .addOnFailureListener(e -> Log.w(TAG, "Cloud fetch failed", e));
            }
        } catch (Exception ignored) {
        }
    }

    public static Bitmap getRoundedBitmap(Bitmap bitmap, float cornerRadiusPx) {
        if (bitmap == null) return null;
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);

            RectF rect = new RectF(0, 0, width, height);
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public static Bitmap decodeBase64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
