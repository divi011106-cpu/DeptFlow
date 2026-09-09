package com.example.deptflow.feature.faculty.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.deptflow.feature.faculty.models.FacultyUser;

/**
 * SessionManager handles the currently authenticated user session.
 * Reusable across Member 1 (Auth), Member 2 (HOD), and Member 3 (Faculty).
 */
public class SessionManager {

    private static final String PREF_NAME = "deptflow_session_pref";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_DEPARTMENT = "user_dept";
    private static final String KEY_ROLE = "user_role";

    public static final String ROLE_FACULTY = "FACULTY";
    public static final String ROLE_HOD = "HOD";

    private static SessionManager instance;
    private final SharedPreferences preferences;

    private SessionManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        ensureDefaultFacultySession();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Initializes default faculty user for seamless standalone testing
     * if no login session exists yet.
     */
    private void ensureDefaultFacultySession() {
        if (!preferences.contains(KEY_USER_ID)) {
            FacultyUser defaultFaculty = new FacultyUser(
                    "FAC-102",
                    "Dr. Sarah Jenkins",
                    "sarah.jenkins@deptflow.edu",
                    "Information Technology",
                    ROLE_FACULTY
            );
            saveFacultyUser(defaultFaculty);
        }
    }

    public void saveFacultyUser(FacultyUser user) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_DEPARTMENT, user.getDepartment());
        editor.putString(KEY_ROLE, user.getRole());
        editor.apply();
    }

    public FacultyUser getCurrentUser() {
        String userId = preferences.getString(KEY_USER_ID, "FAC-102");
        String name = preferences.getString(KEY_USER_NAME, "Dr. Sarah Jenkins");
        String email = preferences.getString(KEY_USER_EMAIL, "sarah.jenkins@deptflow.edu");
        String dept = preferences.getString(KEY_DEPARTMENT, "Information Technology");
        String role = preferences.getString(KEY_ROLE, ROLE_FACULTY);
        return new FacultyUser(userId, name, email, dept, role);
    }

    public boolean isFaculty() {
        String role = preferences.getString(KEY_ROLE, ROLE_FACULTY);
        return ROLE_FACULTY.equalsIgnoreCase(role);
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, true);
    }

    public void logout() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }
}
