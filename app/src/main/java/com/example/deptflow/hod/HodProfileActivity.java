package com.example.deptflow.hod;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HodProfileActivity extends AppCompatActivity {

    private static final String TAG = "HodProfileActivity";

    public static final String PREF_HOD_PROFILE = "deptflow_hod_profile_pref";
    public static final String KEY_HOD_NAME = "hod_name";
    public static final String KEY_HOD_DESIGNATION = "hod_designation";
    public static final String KEY_HOD_DEPARTMENT = "hod_department";
    public static final String KEY_HOD_EMP_ID = "hod_emp_id";
    public static final String KEY_HOD_EMAIL = "hod_email";
    public static final String KEY_HOD_OFFICE = "hod_office";
    public static final String KEY_HOD_PHONE = "hod_phone";
    public static final String KEY_HOD_IMAGE_BASE64 = "hod_image_base64";

    private ImageView ivHodProfilePicture;
    private TextView tvProfileHodName;
    private TextView tvProfileDesignation;
    private TextView tvProfileDepartment;
    private TextView tvProfileEmployeeId;
    private TextView tvProfileEmail;
    private TextView tvProfileOffice;
    private TextView tvProfilePhone;

    private SharedPreferences prefs;
    private FirebaseFirestore firestore;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processAndSaveImageUri(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hod_profile);

        prefs = getSharedPreferences(PREF_HOD_PROFILE, Context.MODE_PRIVATE);
        firestore = FirebaseFirestore.getInstance();

        initViews();
        loadProfileData();
        setupListeners();
        fetchCloudProfile();
    }

    private void initViews() {
        ivHodProfilePicture = findViewById(R.id.ivHodProfilePicture);
        tvProfileHodName = findViewById(R.id.tvProfileHodName);
        tvProfileDesignation = findViewById(R.id.tvProfileDesignation);
        tvProfileDepartment = findViewById(R.id.tvProfileDepartment);
        tvProfileEmployeeId = findViewById(R.id.tvProfileEmployeeId);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileOffice = findViewById(R.id.tvProfileOffice);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupListeners() {
        View.OnClickListener pickPhotoAction = v -> imagePickerLauncher.launch("image/*");

        findViewById(R.id.btnPickImage).setOnClickListener(pickPhotoAction);
        findViewById(R.id.btnChangePhotoBadge).setOnClickListener(pickPhotoAction);
        ivHodProfilePicture.setOnClickListener(pickPhotoAction);

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadProfileData() {
        String name = prefs.getString(KEY_HOD_NAME, "Dr. R. Kavitha");
        String designation = prefs.getString(KEY_HOD_DESIGNATION, "Head of Department");
        String dept = prefs.getString(KEY_HOD_DEPARTMENT, "Department of Information Technology");
        String empId = prefs.getString(KEY_HOD_EMP_ID, "HOD-IT-01");
        String email = prefs.getString(KEY_HOD_EMAIL, "hod.it@deptflow.edu");
        String office = prefs.getString(KEY_HOD_OFFICE, "IT Block - Cabin 204");
        String phone = prefs.getString(KEY_HOD_PHONE, "+91 94432 18765");
        String imageBase64 = prefs.getString(KEY_HOD_IMAGE_BASE64, "");

        tvProfileHodName.setText(name);
        tvProfileDesignation.setText(designation);
        tvProfileDepartment.setText(dept);
        tvProfileEmployeeId.setText(empId);
        tvProfileEmail.setText(email);
        tvProfileOffice.setText(office);
        tvProfilePhone.setText(phone);

        if (!imageBase64.isEmpty()) {
            Bitmap bitmap = decodeBase64ToBitmap(imageBase64);
            if (bitmap != null) {
                ivHodProfilePicture.setImageBitmap(bitmap);
            }
        }
    }

    private void processAndSaveImageUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (originalBitmap != null) {
                // Scale down to maximum 512x512
                int maxDim = 512;
                int width = originalBitmap.getWidth();
                int height = originalBitmap.getHeight();
                float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                int newWidth = Math.round(ratio * width);
                int newHeight = Math.round(ratio * height);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Save locally
                prefs.edit().putString(KEY_HOD_IMAGE_BASE64, base64Image).apply();
                ivHodProfilePicture.setImageBitmap(scaledBitmap);

                // Sync to Firestore
                Map<String, Object> map = new HashMap<>();
                map.put("imageBase64", base64Image);
                firestore.collection("hod_profile").document("current_hod")
                        .set(map, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile image synced to Firestore"))
                        .addOnFailureListener(e -> Log.w(TAG, "Firestore image sync failed", e));

                Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing picked image", e);
            Toast.makeText(this, "Failed to load selected image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditProfileDialog() {
        android.widget.LinearLayout dialogLayout = new android.widget.LinearLayout(this);
        dialogLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 40, 50, 10);

        final EditText etName = new EditText(this);
        etName.setHint("Full Name");
        etName.setText(tvProfileHodName.getText());
        dialogLayout.addView(etName);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email Address");
        etEmail.setText(tvProfileEmail.getText());
        dialogLayout.addView(etEmail);

        final EditText etOffice = new EditText(this);
        etOffice.setHint("Cabin / Location");
        etOffice.setText(tvProfileOffice.getText());
        dialogLayout.addView(etOffice);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("Phone Number");
        etPhone.setText(tvProfilePhone.getText());
        dialogLayout.addView(etPhone);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit HOD Profile")
                .setView(dialogLayout)
                .setPositiveButton("Save Changes", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    String newOffice = etOffice.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();

                    if (!newName.isEmpty()) tvProfileHodName.setText(newName);
                    if (!newEmail.isEmpty()) tvProfileEmail.setText(newEmail);
                    if (!newOffice.isEmpty()) tvProfileOffice.setText(newOffice);
                    if (!newPhone.isEmpty()) tvProfilePhone.setText(newPhone);

                    prefs.edit()
                            .putString(KEY_HOD_NAME, newName.isEmpty() ? tvProfileHodName.getText().toString() : newName)
                            .putString(KEY_HOD_EMAIL, newEmail.isEmpty() ? tvProfileEmail.getText().toString() : newEmail)
                            .putString(KEY_HOD_OFFICE, newOffice.isEmpty() ? tvProfileOffice.getText().toString() : newOffice)
                            .putString(KEY_HOD_PHONE, newPhone.isEmpty() ? tvProfilePhone.getText().toString() : newPhone)
                            .apply();

                    // Sync to Firestore
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", newName);
                    map.put("email", newEmail);
                    map.put("office", newOffice);
                    map.put("phone", newPhone);
                    map.put("department", "Department of Information Technology");
                    map.put("designation", "Head of Department");

                    firestore.collection("hod_profile").document("current_hod")
                            .set(map, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile details synced to Firestore"))
                            .addOnFailureListener(e -> Log.w(TAG, "Firestore details sync failed", e));

                    Toast.makeText(this, "Profile details saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void fetchCloudProfile() {
        try {
            firestore.collection("hod_profile").document("current_hod")
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            String office = doc.getString("office");
                            String phone = doc.getString("phone");
                            String imageBase64 = doc.getString("imageBase64");

                            SharedPreferences.Editor editor = prefs.edit();
                            if (name != null && !name.isEmpty()) {
                                tvProfileHodName.setText(name);
                                editor.putString(KEY_HOD_NAME, name);
                            }
                            if (email != null && !email.isEmpty()) {
                                tvProfileEmail.setText(email);
                                editor.putString(KEY_HOD_EMAIL, email);
                            }
                            if (office != null && !office.isEmpty()) {
                                tvProfileOffice.setText(office);
                                editor.putString(KEY_HOD_OFFICE, office);
                            }
                            if (phone != null && !phone.isEmpty()) {
                                tvProfilePhone.setText(phone);
                                editor.putString(KEY_HOD_PHONE, phone);
                            }
                            if (imageBase64 != null && !imageBase64.isEmpty()) {
                                editor.putString(KEY_HOD_IMAGE_BASE64, imageBase64);
                                Bitmap bitmap = decodeBase64ToBitmap(imageBase64);
                                if (bitmap != null) {
                                    ivHodProfilePicture.setImageBitmap(bitmap);
                                }
                            }
                            editor.apply();
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "fetchCloudProfile failed: " + e.getMessage()));
        } catch (Exception ignored) {}
    }

    public static Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
