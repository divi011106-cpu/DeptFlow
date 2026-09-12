package com.example.deptflow.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthManager
 *
 * Handles:
 * 1. Firebase registration
 * 2. Firebase login
 * 3. Firestore user profile
 * 4. Session storage
 * 5. Password reset
 * 6. Logout
 */
public class AuthManager {

    // Firestore collection and fields
    private static final String COLLECTION_USERS = "users";

    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_DEPT = "department";
    private static final String FIELD_USER_ID = "userId";

    // SharedPreferences
    private static final String PREF_PROFILE_STORE = "deptflow_session_pref";

    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_DEPARTMENT = "user_dept";
    private static final String KEY_ROLE = "user_role";

    // Roles
    public static final String ROLE_FACULTY = "FACULTY";
    public static final String ROLE_HOD = "HOD";

    // SHA-256 hash for HOD admin key
    private static final String HOD_ADMIN_KEY_HASH =
            "8f90bf28db7e2a1e543a6d80b3cec9e2f28a620c3e6e5e56c91d4c22a1d1c3f7";

    // Firebase instances
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final SharedPreferences profilePrefs;

    // Singleton instance
    private static AuthManager instance;

    private AuthManager(Context context) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        profilePrefs = context.getApplicationContext()
                .getSharedPreferences(
                        PREF_PROFILE_STORE,
                        Context.MODE_PRIVATE
                );
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }

        return instance;
    }

    // ----------------------------------------------------
    // Callback interfaces
    // ----------------------------------------------------

    public interface AuthCallback {
        void onSuccess(FacultyUser user);

        void onFailure(AuthResult.Code code);
    }

    public interface ResetCallback {
        void onSuccess();

        void onFailure(String errorCode);
    }

    // ----------------------------------------------------
    // REGISTER
    // ----------------------------------------------------

    /**
     * Registers a user using Firebase Authentication
     * and stores the profile in Firestore.
     */
    public void register(
            String name,
            String email,
            String password,
            String role,
            String adminKey,
            AuthCallback callback
    ) {

        // Validate HOD admin key
        if (ROLE_HOD.equalsIgnoreCase(role)) {

            if (adminKey == null || adminKey.trim().isEmpty()) {
                Log.e("AuthManager", "register: HOD admin key is empty");
                callback.onFailure(AuthResult.Code.INVALID_ADMIN_KEY);
                return;
            }

            if (!HOD_ADMIN_KEY_HASH.equals(sha256(adminKey.trim()))) {
                Log.e("AuthManager", "register: Invalid HOD admin key");
                callback.onFailure(AuthResult.Code.INVALID_ADMIN_KEY);
                return;
            }
        }

        String normalEmail = email == null
                ? ""
                : email.trim().toLowerCase();

        String normalName = name == null
                ? ""
                : name.trim();

        String normalRole = role == null
                ? ROLE_FACULTY
                : role.trim().toUpperCase();

        String department = ROLE_HOD.equalsIgnoreCase(normalRole)
                ? "Department Head"
                : "Information Technology";

        Log.d(
                "AuthManager",
                "register: attempting email=" + normalEmail
                        + " | role=" + normalRole
        );

        // Create Firebase Authentication account
        firebaseAuth
                .createUserWithEmailAndPassword(normalEmail, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser == null) {
                        Log.e(
                                "AuthManager",
                                "register: Firebase Auth returned null user"
                        );

                        callback.onFailure(AuthResult.Code.UNKNOWN_ERROR);
                        return;
                    }

                    String generatedUserId = generateUserId(normalRole);

                    Map<String, Object> profile = new HashMap<>();

                    profile.put(FIELD_NAME, normalName);
                    profile.put(FIELD_EMAIL, normalEmail);
                    profile.put(FIELD_ROLE, normalRole);
                    profile.put(FIELD_DEPT, department);
                    profile.put(FIELD_USER_ID, generatedUserId);

                    Log.d(
                            "AuthManager",
                            "register: Firebase Auth SUCCESS, uid="
                                    + firebaseUser.getUid()
                    );

                    // Save profile in Firestore
                    firestore
                            .collection(COLLECTION_USERS)
                            .document(firebaseUser.getUid())
                            .set(profile)
                            .addOnSuccessListener(unused -> {

                                Log.d(
                                        "AuthManager",
                                        "register: Firestore write SUCCESS"
                                );

                                FacultyUser user = new FacultyUser(
                                        generatedUserId,
                                        normalName,
                                        normalEmail,
                                        department,
                                        normalRole
                                );

                                callback.onSuccess(user);
                            })
                            .addOnFailureListener(e -> {

                                Log.e(
                                        "AuthManager",
                                        "register: Firestore write FAILED for uid="
                                                + firebaseUser.getUid()
                                                + " | "
                                                + e.getMessage(),
                                        e
                                );

                                /*
                                 * If Firestore write fails after Firebase Auth
                                 * account creation, delete the Firebase Auth
                                 * account to avoid an orphaned account.
                                 */
                                firebaseUser
                                        .delete()
                                        .addOnCompleteListener(deleteTask -> {

                                            Log.w(
                                                    "AuthManager",
                                                    "register: orphaned Auth user deleted="
                                                            + deleteTask.isSuccessful()
                                            );
                                        });

                                callback.onFailure(AuthResult.Code.UNKNOWN_ERROR);
                            });
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            "AuthManager",
                            "register: Firebase Auth FAILED | "
                                    + e.getMessage(),
                            e
                    );

                    if (e instanceof FirebaseAuthUserCollisionException) {
                        callback.onFailure(AuthResult.Code.EMAIL_TAKEN);
                    } else {
                        callback.onFailure(AuthResult.Code.UNKNOWN_ERROR);
                    }
                });
    }

    // ----------------------------------------------------
    // LOGIN
    // ----------------------------------------------------

    /**
     * Logs in using Firebase Authentication.
     *
     * After successful Firebase login:
     * 1. Fetches user profile from Firestore.
     * 2. Checks selected role.
     * 3. Saves session locally.
     */
    public void login(
            String email,
            String password,
            String selectedRole,
            AuthCallback callback
    ) {

        String normalEmail = email == null
                ? ""
                : email.trim().toLowerCase();

        String normalRole = selectedRole == null
                ? ""
                : selectedRole.trim();

        Log.d(
                "AuthManager",
                "login: attempting email=" + normalEmail
                        + " | selectedRole=" + normalRole
        );

        // Firebase email/password login
        firebaseAuth
                .signInWithEmailAndPassword(normalEmail, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser == null) {
                        Log.e(
                                "AuthManager",
                                "login: Firebase returned null user"
                        );

                        callback.onFailure(AuthResult.Code.UNKNOWN_ERROR);
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    Log.d(
                            "AuthManager",
                            "login: Firebase Auth SUCCESS, uid=" + uid
                    );

                    // Fetch profile from Firestore
                    firestore
                            .collection(COLLECTION_USERS)
                            .document(uid)
                            .get()
                            .addOnSuccessListener(document -> {

                                if (!document.exists()) {

                                    Log.e(
                                            "AuthManager",
                                            "login: Firestore profile NOT FOUND for uid="
                                                    + uid
                                    );

                                    firebaseAuth.signOut();

                                    callback.onFailure(
                                            AuthResult.Code.ACCOUNT_NOT_FOUND
                                    );

                                    return;
                                }

                                String storedRole =
                                        document.getString(FIELD_ROLE);

                                if (storedRole == null) {
                                    storedRole = "";
                                }

                                Log.d(
                                        "AuthManager",
                                        "login: storedRole=" + storedRole
                                                + " | selectedRole=" + normalRole
                                );

                                // Check selected role with Firestore role
                                if (!storedRole.equalsIgnoreCase(normalRole)) {

                                    Log.e(
                                            "AuthManager",
                                            "login: ROLE MISMATCH"
                                    );

                                    firebaseAuth.signOut();

                                    if (ROLE_HOD.equalsIgnoreCase(storedRole)) {
                                        callback.onFailure(
                                                AuthResult.Code.ROLE_MISMATCH_IS_HOD
                                        );
                                    } else {
                                        callback.onFailure(
                                                AuthResult.Code.ROLE_MISMATCH_IS_FACULTY
                                        );
                                    }

                                    return;
                                }

                                FacultyUser user = new FacultyUser(
                                        getStr(document, FIELD_USER_ID),
                                        getStr(document, FIELD_NAME),
                                        getStr(document, FIELD_EMAIL),
                                        getStr(document, FIELD_DEPT),
                                        storedRole
                                );

                                // Save session
                                saveSession(user);

                                Log.d(
                                        "AuthManager",
                                        "login: COMPLETE SUCCESS"
                                );

                                callback.onSuccess(user);
                            })
                            .addOnFailureListener(e -> {

                                Log.e(
                                        "AuthManager",
                                        "login: Firestore profile read FAILED | "
                                                + e.getMessage(),
                                        e
                                );

                                firebaseAuth.signOut();

                                callback.onFailure(
                                        AuthResult.Code.UNKNOWN_ERROR
                                );
                            });
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            "AuthManager",
                            "login: Firebase Auth FAILED | "
                                    + e.getMessage(),
                            e
                    );

                    if (e instanceof FirebaseAuthInvalidUserException) {

                        Log.e(
                                "AuthManager",
                                "login: Firebase user does not exist or is disabled"
                        );

                        callback.onFailure(
                                AuthResult.Code.ACCOUNT_NOT_FOUND
                        );

                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {

                        Log.e(
                                "AuthManager",
                                "login: Invalid email or password"
                        );

                        callback.onFailure(
                                AuthResult.Code.INVALID_CREDENTIALS
                        );

                    } else {

                        callback.onFailure(
                                AuthResult.Code.UNKNOWN_ERROR
                        );
                    }
                });
    }

    // ----------------------------------------------------
    // SESSION
    // ----------------------------------------------------

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public FacultyUser getCurrentUser() {

        if (!isLoggedIn()) {
            return null;
        }

        String userId = profilePrefs.getString(KEY_USER_ID, "");
        String name = profilePrefs.getString(KEY_USER_NAME, "");
        String email = profilePrefs.getString(KEY_USER_EMAIL, "");
        String department = profilePrefs.getString(KEY_DEPARTMENT, "");
        String role = profilePrefs.getString(KEY_ROLE, "");

        if (userId == null || userId.isEmpty()) {
            return null;
        }

        return new FacultyUser(
                userId,
                name,
                email,
                department,
                role
        );
    }

    private void saveSession(FacultyUser user) {

        profilePrefs
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, user.getUserId())
                .putString(KEY_USER_NAME, user.getName())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_DEPARTMENT, user.getDepartment())
                .putString(KEY_ROLE, user.getRole())
                .apply();

        Log.d(
                "AuthManager",
                "saveSession: session saved for " + user.getEmail()
        );
    }

    // ----------------------------------------------------
    // PASSWORD RESET
    // ----------------------------------------------------

    public void resetPassword(
            String email,
            ResetCallback callback
    ) {

        String normalEmail = email == null
                ? ""
                : email.trim().toLowerCase();

        Log.d(
                "AuthManager",
                "resetPassword: attempting email=" + normalEmail
        );

        firebaseAuth
                .sendPasswordResetEmail(normalEmail)
                .addOnSuccessListener(unused -> {

                    Log.d(
                            "AuthManager",
                            "resetPassword: email sent successfully"
                    );

                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {

                    Log.e(
                            "AuthManager",
                            "resetPassword: FAILED | " + e.getMessage(),
                            e
                    );

                    if (e instanceof FirebaseAuthInvalidUserException) {
                        callback.onFailure("no_user");
                    } else {
                        callback.onFailure("generic");
                    }
                });
    }

    // ----------------------------------------------------
    // LOGOUT
    // ----------------------------------------------------

    public void logout() {

        firebaseAuth.signOut();

        profilePrefs
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_DEPARTMENT)
                .remove(KEY_ROLE)
                .apply();

        Log.d("AuthManager", "logout: Firebase and local session cleared");
    }

    // ----------------------------------------------------
    // PRIVATE HELPERS
    // ----------------------------------------------------

    private String generateUserId(String role) {

        String prefix = ROLE_HOD.equalsIgnoreCase(role)
                ? "HOD"
                : "FAC";

        return prefix + "-" + (100 + (int) (Math.random() * 900));
    }

    private String getStr(
            DocumentSnapshot document,
            String field
    ) {

        String value = document.getString(field);

        return value != null ? value : "";
    }

    /**
     * SHA-256 is used only for HOD admin-key validation.
     * Firebase itself handles password security.
     */
    static String sha256(String input) {

        if (input == null) {
            return "";
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                    input.getBytes("UTF-8")
            );

            StringBuilder builder = new StringBuilder();

            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();

        } catch (Exception e) {

            Log.e(
                    "AuthManager",
                    "sha256: error while hashing",
                    e
            );

            return "";
        }
    }

    // ----------------------------------------------------
    // AUTH RESULT CODES
    // ----------------------------------------------------

    public static class AuthResult {

        public enum Code {
            SUCCESS,
            ACCOUNT_NOT_FOUND,
            INVALID_CREDENTIALS,
            EMAIL_TAKEN,
            INVALID_ADMIN_KEY,
            ROLE_MISMATCH_IS_HOD,
            ROLE_MISMATCH_IS_FACULTY,
            UNKNOWN_ERROR
        }
    }
}