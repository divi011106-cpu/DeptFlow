package com.example.deptflow.communication.models;

import com.example.deptflow.feature.faculty.models.FacultyUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FacultyDirectory — Source of Truth for Department Faculty Members.
 *
 * Provides:
 * 1. Stable, unique canonical IDs (FAC-101 to FAC-123) for all 23 faculty members
 *    derived directly from the HOD module.
 * 2. Deterministic Chat ID generation (min_id + "_" + max_id) ensuring symmetric,
 *    two-way private messaging.
 * 3. Robust multi-token and fuzzy identity resolution matching names, IDs,
 *    emails, and cleaned tokens.
 */
public final class FacultyDirectory {

    public static final String[] FACULTY_NAMES = {
            "Dr. R. Vijayalakshmi",
            "Dr. R. Raja Sudharsan",
            "Dr. K. M. Alaaudeen",
            "Dr. T. Saranya",
            "Mrs. M. Prabha",
            "Mrs. P. Saraswathi",
            "Mr. S. Jegadeesan",
            "Mrs. A. Meena",
            "Dr. T. Venkatesh Kanna",
            "Mrs. M. Ishvarya",
            "Mrs. R. Nancy Deborah",
            "Mrs. C. Manjula Devi",
            "Mrs. A. Vinora",
            "Mr. A. Srinivasan",
            "Mr. P. KalyanaKumar",
            "Ms. G. Sivakarthi",
            "Mrs. M. Soundarya",
            "Mrs. J. John Shiny",
            "Mr. R. Umesh",
            "Mrs. A. Periya Nayaki",
            "Mrs. A. Elavarasi",
            "Dr. S. Esakki Muthu",
            "Mr. K. Loganathan"
    };

    private static final List<FacultyUser> CANONICAL_FACULTY_LIST = new ArrayList<>();
    private static final Map<String, FacultyUser> BY_ID_MAP = new HashMap<>();
    private static final Map<String, FacultyUser> BY_NAME_MAP = new HashMap<>();
    private static final Map<String, FacultyUser> BY_EMAIL_MAP = new HashMap<>();

    static {
        for (int i = 0; i < FACULTY_NAMES.length; i++) {
            String name = FACULTY_NAMES[i];
            String facultyId = "FAC-" + (101 + i);
            String emailPrefix = cleanTokenForEmail(name);
            String email = emailPrefix + "@deptflow.edu";

            FacultyUser user = new FacultyUser(
                    facultyId,
                    name,
                    email,
                    "Information Technology",
                    "FACULTY"
            );

            CANONICAL_FACULTY_LIST.add(user);
            BY_ID_MAP.put(facultyId.toLowerCase(Locale.ROOT), user);
            BY_NAME_MAP.put(name.toLowerCase(Locale.ROOT), user);
            BY_EMAIL_MAP.put(email.toLowerCase(Locale.ROOT), user);
        }
    }

    private FacultyDirectory() {
        // Utility class
    }

    /**
     * Returns an unmodifiable list of all 23 canonical department faculty members.
     */
    public static List<FacultyUser> getAllFacultyContacts() {
        List<FacultyUser> copy = new ArrayList<>();
        for (FacultyUser u : CANONICAL_FACULTY_LIST) {
            copy.add(new FacultyUser(
                    u.getUserId(),
                    u.getName(),
                    u.getEmail(),
                    u.getDepartment(),
                    u.getRole()
            ));
        }
        return copy;
    }

    /**
     * Resolves a FacultyUser or user session into a canonical FacultyUser record.
     */
    public static FacultyUser resolveCanonicalFaculty(FacultyUser user) {
        if (user == null) return null;

        // 1. Direct ID match
        if (user.getUserId() != null && !user.getUserId().trim().isEmpty()) {
            FacultyUser match = BY_ID_MAP.get(user.getUserId().trim().toLowerCase(Locale.ROOT));
            if (match != null) return match;
        }

        // 2. Direct Name match
        if (user.getName() != null && !user.getName().trim().isEmpty()) {
            FacultyUser match = BY_NAME_MAP.get(user.getName().trim().toLowerCase(Locale.ROOT));
            if (match != null) return match;
        }

        // 3. Email match
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            FacultyUser match = BY_EMAIL_MAP.get(user.getEmail().trim().toLowerCase(Locale.ROOT));
            if (match != null) return match;
        }

        // 4. Fuzzy / Token resolution
        return resolveByNameOrToken(user.getName(), user.getEmail(), user.getUserId());
    }

    /**
     * Resolves a name, ID, or email string to the canonical FacultyUser.
     */
    public static FacultyUser resolveByNameOrId(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String query = identifier.trim().toLowerCase(Locale.ROOT);

        if (BY_ID_MAP.containsKey(query)) {
            return BY_ID_MAP.get(query);
        }

        if (BY_NAME_MAP.containsKey(query)) {
            return BY_NAME_MAP.get(query);
        }

        if (BY_EMAIL_MAP.containsKey(query)) {
            return BY_EMAIL_MAP.get(query);
        }

        return resolveByNameOrToken(identifier, identifier, identifier);
    }

    /**
     * Resolves a canonical Faculty ID (e.g. "FAC-104") for any given input string.
     */
    public static String resolveCanonicalId(String nameOrId) {
        FacultyUser user = resolveByNameOrId(nameOrId);
        if (user != null && user.getUserId() != null) {
            return user.getUserId();
        }
        return nameOrId != null ? nameOrId.trim() : "";
    }

    /**
     * Resolves canonical display name for any given ID or name string.
     */
    public static String resolveCanonicalName(String nameOrId) {
        FacultyUser user = resolveByNameOrId(nameOrId);
        if (user != null && user.getName() != null) {
            return user.getName();
        }
        return nameOrId != null ? nameOrId.trim() : "Faculty";
    }

    /**
     * Generates a deterministic, symmetric conversation ID for any two faculty IDs or names.
     * Guaranteed: getDeterministicChatId(A, B).equals(getDeterministicChatId(B, A)).
     */
    public static String getDeterministicChatId(String party1, String party2) {
        String id1 = resolveCanonicalId(party1);
        String id2 = resolveCanonicalId(party2);

        if (id1.isEmpty()) id1 = party1 != null ? party1.trim() : "user1";
        if (id2.isEmpty()) id2 = party2 != null ? party2.trim() : "user2";

        List<String> sorted = new ArrayList<>(Arrays.asList(id1, id2));
        Collections.sort(sorted);
        return sorted.get(0) + "_" + sorted.get(1);
    }

    /**
     * Fuzzy / Token matching for names with titles or slight spelling variations.
     */
    private static FacultyUser resolveByNameOrToken(String name, String email, String id) {
        String combined = ((name != null ? name : "") + " "
                + (email != null ? email : "") + " "
                + (id != null ? id : "")).toLowerCase(Locale.ROOT);

        String cleanQuery = cleanName(combined);

        // Check common aliases
        if (cleanQuery.contains("saranya") || cleanQuery.contains("sarnya")) {
            return BY_ID_MAP.get("fac-104"); // Dr. T. Saranya
        }
        if (cleanQuery.contains("saraswathi") || cleanQuery.contains("saraswati")) {
            return BY_ID_MAP.get("fac-106"); // Mrs. P. Saraswathi
        }
        if (cleanQuery.contains("priya") || cleanQuery.contains("periya")) {
            // If "Priya" is specified, map to Mrs. A. Periya Nayaki (FAC-120) or Mrs. P. Saraswathi (FAC-106)
            FacultyUser periya = BY_ID_MAP.get("fac-120");
            if (periya != null) return periya;
            return BY_ID_MAP.get("fac-106");
        }
        if (cleanQuery.contains("vijayalakshmi") || cleanQuery.contains("vijaya")) {
            return BY_ID_MAP.get("fac-101");
        }
        if (cleanQuery.contains("raja") || cleanQuery.contains("sudharsan")) {
            return BY_ID_MAP.get("fac-102");
        }
        if (cleanQuery.contains("alaaudeen")) {
            return BY_ID_MAP.get("fac-103");
        }
        if (cleanQuery.contains("prabha")) {
            return BY_ID_MAP.get("fac-105");
        }
        if (cleanQuery.contains("jegadeesan")) {
            return BY_ID_MAP.get("fac-107");
        }
        if (cleanQuery.contains("meena")) {
            return BY_ID_MAP.get("fac-108");
        }
        if (cleanQuery.contains("venkatesh") || cleanQuery.contains("kanna")) {
            return BY_ID_MAP.get("fac-109");
        }
        if (cleanQuery.contains("ishvarya")) {
            return BY_ID_MAP.get("fac-110");
        }
        if (cleanQuery.contains("nancy") || cleanQuery.contains("deborah")) {
            return BY_ID_MAP.get("fac-111");
        }
        if (cleanQuery.contains("manjula") || cleanQuery.contains("devi")) {
            return BY_ID_MAP.get("fac-112");
        }
        if (cleanQuery.contains("vinora")) {
            return BY_ID_MAP.get("fac-113");
        }
        if (cleanQuery.contains("srinivasan")) {
            return BY_ID_MAP.get("fac-114");
        }
        if (cleanQuery.contains("kalyan")) {
            return BY_ID_MAP.get("fac-115");
        }
        if (cleanQuery.contains("sivakarthi")) {
            return BY_ID_MAP.get("fac-116");
        }
        if (cleanQuery.contains("soundarya")) {
            return BY_ID_MAP.get("fac-117");
        }
        if (cleanQuery.contains("shiny") || cleanQuery.contains("john")) {
            return BY_ID_MAP.get("fac-118");
        }
        if (cleanQuery.contains("umesh")) {
            return BY_ID_MAP.get("fac-119");
        }
        if (cleanQuery.contains("elavarasi")) {
            return BY_ID_MAP.get("fac-121");
        }
        if (cleanQuery.contains("esakki") || cleanQuery.contains("muthu")) {
            return BY_ID_MAP.get("fac-122");
        }
        if (cleanQuery.contains("loganathan")) {
            return BY_ID_MAP.get("fac-123");
        }

        // Generic token overlap check with faculty list
        for (FacultyUser fac : CANONICAL_FACULTY_LIST) {
            String cleanFac = cleanName(fac.getName());
            if (!cleanFac.isEmpty() && (cleanQuery.contains(cleanFac) || cleanFac.contains(cleanQuery))) {
                return fac;
            }
        }

        return null;
    }

    private static String cleanName(String str) {
        if (str == null) return "";
        return str.toLowerCase(Locale.ROOT)
                .replaceAll("\\b(dr|mr|mrs|ms|prof)\\b[.]?", "")
                .replaceAll("[^a-z0-9]", "");
    }

    private static String cleanTokenForEmail(String name) {
        if (name == null) return "faculty";
        String cleaned = cleanName(name);
        return cleaned.isEmpty() ? "faculty" : cleaned;
    }
}
