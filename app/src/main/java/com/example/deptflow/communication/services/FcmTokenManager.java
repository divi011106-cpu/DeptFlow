package com.example.deptflow.communication.services;

import android.content.Context;
import android.util.Log;

import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to register and maintain device FCM tokens associated with the authenticated faculty account.
 */
public class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    /**
     * Obtains the current FCM token and registers it in Firestore for the currently logged-in user.
     */
    public static void registerDeviceToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.trim().isEmpty()) {
                        Log.d(TAG, "Current FCM token retrieved: " + token);
                        saveTokenToFirestore(context, token);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to retrieve FCM token: " + e.getMessage()));
    }

    /**
     * Stores the FCM token in Firestore under `users/{uid}` and `fcm_tokens/{facultyId}`.
     */
    public static void saveTokenToFirestore(Context context, String token) {
        if (token == null || token.trim().isEmpty()) return;

        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        FacultyUser cachedUser = context != null ? AuthManager.getInstance(context).getCurrentUser() : null;

        String authUid = authUser != null ? authUser.getUid() : "";
        String facultyId = cachedUser != null ? cachedUser.getUserId() : "";

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        tokenData.put("lastTokenUpdate", FieldValue.serverTimestamp());

        // 1. Update under users/{uid}
        if (!authUid.isEmpty()) {
            db.collection("users").document(authUid)
                    .update(tokenData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated FCM token on user doc: " + authUid))
                    .addOnFailureListener(e -> {
                        // Document set fallback if update fails
                        db.collection("users").document(authUid).set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(v -> Log.d(TAG, "Merged FCM token on user doc: " + authUid));
                    });
        }

        // 2. Store in top-level fcm_tokens/{facultyId or authUid}
        String key = !facultyId.isEmpty() ? facultyId : authUid;
        if (!key.isEmpty()) {
            Map<String, Object> record = new HashMap<>();
            record.put("facultyId", facultyId);
            record.put("authUid", authUid);
            record.put("token", token);
            record.put("updatedAt", FieldValue.serverTimestamp());

            db.collection("fcm_tokens").document(key)
                    .set(record, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Stored FCM token in fcm_tokens/" + key))
                    .addOnFailureListener(e -> Log.w(TAG, "Could not store in fcm_tokens: " + e.getMessage()));
        }
    }
}
