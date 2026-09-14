package com.example.deptflow.feature.faculty.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.example.deptflow.feature.faculty.models.Task;
import com.example.deptflow.hod.TaskData;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskRepository — Faculty-side task data manager.
 *
 * SOURCE OF TRUTH for task assignments is HOD's TaskData.tasks (in-memory static list).
 *
 * PERSISTENCE BRIDGE:
 * Because TaskData.tasks is in-memory only and is cleared when the app process restarts,
 * this repository persists the raw HOD task strings to SharedPreferences whenever it
 * reads a non-empty TaskData.tasks. On subsequent launches (where TaskData.tasks is
 * empty), it restores from SharedPreferences back into TaskData.tasks, ensuring
 * continuity across HOD → Faculty login transitions without modifying the HOD module.
 *
 * DEBUG:
 * Set VERBOSE_DEBUG = true and filter Logcat by "DEPTFLOW_TASK_DEBUG" to trace
 * exactly what is loaded, parsed, and matched at runtime.
 */
public class TaskRepository {

    private static final String TAG = "DEPTFLOW_TASK_DEBUG";
    private static final boolean VERBOSE_DEBUG = true;

    // SharedPreferences: sample/cached task JSON
    private static final String PREF_NAME = "deptflow_tasks_pref";
    private static final String KEY_SAMPLE_TASKS_JSON = "tasks_json_list";

    // SharedPreferences: persisted HOD raw task strings (cross-session bridge)
    private static final String KEY_HOD_RAW_TASKS = "hod_raw_tasks_json";

    // HOD task ID prefix — index in TaskData.tasks (or persisted list) is the suffix
    private static final String HOD_TASK_PREFIX = "HOD-TASK-";

    private static TaskRepository instance;
    private final SharedPreferences preferences;

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------

    private TaskRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Restore any previously persisted HOD tasks back into TaskData.tasks
        // so the in-memory list is consistent even after app restarts.
        restoreHodTasksFromPrefs();
    }

    public static synchronized TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context);
        }
        return instance;
    }

    // -----------------------------------------------------------------------
    // HOD task persistence bridge (Faculty-only; HOD untouched)
    // -----------------------------------------------------------------------

    /**
     * Persists current TaskData.tasks raw strings to SharedPreferences.
     * Called every time we read a non-empty TaskData.tasks so the data survives
     * the next app restart (when TaskData.tasks would otherwise be empty again).
     */
    private synchronized void persistHodTasksToPrefs() {
        if (TaskData.tasks == null || TaskData.tasks.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray();
            for (String raw : TaskData.tasks) {
                arr.put(raw);
            }
            preferences.edit().putString(KEY_HOD_RAW_TASKS, arr.toString()).apply();
            Log.d(TAG, "persistHodTasksToPrefs: saved " + TaskData.tasks.size() + " HOD task(s) to SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "persistHodTasksToPrefs: error", e);
        }
    }

    /**
     * On startup, if TaskData.tasks is empty (app was restarted), reload raw HOD task
     * strings from SharedPreferences back into TaskData.tasks so the rest of the code
     * can read from it as normal. Does NOT modify HOD code.
     */
    private synchronized void restoreHodTasksFromPrefs() {
        if (TaskData.tasks != null && !TaskData.tasks.isEmpty()) {
            Log.d(TAG, "restoreHodTasksFromPrefs: TaskData.tasks already has " + TaskData.tasks.size() + " entry/entries — no restore needed");
            return;
        }
        String json = preferences.getString(KEY_HOD_RAW_TASKS, null);
        if (json == null || json.isEmpty()) {
            Log.d(TAG, "restoreHodTasksFromPrefs: no persisted HOD tasks found — starting fresh");
            return;
        }
        try {
            JSONArray arr = new JSONArray(json);
            if (TaskData.tasks == null) {
                // TaskData.tasks should never be null (it's initialized to new ArrayList<>())
                // but guard anyway
                Log.e(TAG, "restoreHodTasksFromPrefs: TaskData.tasks is null — cannot restore");
                return;
            }
            TaskData.tasks.clear();
            for (int i = 0; i < arr.length(); i++) {
                TaskData.tasks.add(arr.getString(i));
            }
            Log.d(TAG, "restoreHodTasksFromPrefs: restored " + TaskData.tasks.size() + " HOD task(s) from SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "restoreHodTasksFromPrefs: error", e);
        }
    }

    // -----------------------------------------------------------------------
    // HOD task parsing
    // -----------------------------------------------------------------------

    /**
     * Parses one raw HOD task string (from TaskData.tasks) into a Task model.
     *
     * HOD stores exactly this format (from AssignTaskActivity line 157-161):
     *
     *   "Title: " + title +
     *   "\nDescription: " + description +
     *   "\nDeadline: " + deadline +
     *   "\nFaculty: " + tvSelectedFaculty.getText()   ← may end with \n if multi-select
     *
     * Example for a single-faculty selection:
     *   Title: Attendance
     *   Description: less than 70 percent
     *   Deadline: 15/9/2026
     *   Faculty: Dr. T. Sarnya
     *
     * For multi-faculty selection (names separated by \n inside Faculty: block):
     *   Faculty: Dr. T. Sarnya
     *   Dr. R. Vijayalakshmi
     */
    public static Task parseHodTask(String rawText, int index) {
        if (rawText == null || rawText.trim().isEmpty()) return null;

        String taskId    = HOD_TASK_PREFIX + index;
        String title     = "";
        String deadline  = "";
        String status    = Task.STATUS_PENDING;
        String priority  = Task.PRIORITY_MEDIUM;
        String assignedBy = "HOD";

        StringBuilder descBuilder    = new StringBuilder();
        StringBuilder facultyBuilder = new StringBuilder();

        String currentSection = "";

        // Split on newlines; handle \r\n and \n
        String[] lines = rawText.split("\r?\n");

        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;

            if (t.startsWith("Title:")) {
                currentSection = "TITLE";
                title = t.substring("Title:".length()).trim();

            } else if (t.startsWith("Description:")) {
                currentSection = "DESC";
                String part = t.substring("Description:".length()).trim();
                if (!part.isEmpty()) descBuilder.append(part);

            } else if (t.startsWith("Deadline:")) {
                currentSection = "DEADLINE";
                deadline = t.substring("Deadline:".length()).trim();

            } else if (t.startsWith("Faculty:")) {
                currentSection = "FACULTY";
                String part = t.substring("Faculty:".length()).trim();
                if (!part.isEmpty()) facultyBuilder.append(part);

            } else if (t.startsWith("Status:")) {
                currentSection = "STATUS";
                String st = t.substring("Status:".length()).trim();
                if (!st.isEmpty()) status = normalizeStatus(st);

            } else if (t.startsWith("Priority:")) {
                currentSection = "PRIORITY";
                String pr = t.substring("Priority:".length()).trim();
                if (!pr.isEmpty()) priority = pr.toUpperCase();

            } else if (t.startsWith("Assigned By:")) {
                currentSection = "ASSIGNED_BY";
                assignedBy = t.substring("Assigned By:".length()).trim();

            } else {
                // Continuation line — append to current section
                if ("DESC".equals(currentSection)) {
                    if (descBuilder.length() > 0) descBuilder.append("\n");
                    descBuilder.append(t);
                } else if ("FACULTY".equals(currentSection)) {
                    // Multi-faculty: additional names appear as continuation lines
                    if (facultyBuilder.length() > 0) facultyBuilder.append("\n");
                    facultyBuilder.append(t);
                }
            }
        }

        if (title.isEmpty())    title    = "Task #" + (index + 1);
        if (deadline.isEmpty()) deadline = "No Deadline";

        String faculty     = facultyBuilder.toString().trim();
        String description = descBuilder.toString().trim();

        return new Task(taskId, title, description, faculty, assignedBy, deadline, status, priority);
    }

    private static String normalizeStatus(String status) {
        if (status == null) return Task.STATUS_PENDING;
        String s = status.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        if (s.contains("COMPLET"))  return Task.STATUS_COMPLETED;
        if (s.contains("PROGRESS")) return Task.STATUS_IN_PROGRESS;
        return Task.STATUS_PENDING;
    }

    // -----------------------------------------------------------------------
    // Live HOD task list
    // -----------------------------------------------------------------------

    /**
     * Returns all HOD-assigned tasks from TaskData.tasks (in-memory).
     * Also persists them to SharedPreferences so they survive the next restart.
     */
    public synchronized List<Task> getHodTasks() {
        // First try to restore from prefs if TaskData.tasks is empty
        if (TaskData.tasks == null || TaskData.tasks.isEmpty()) {
            restoreHodTasksFromPrefs();
        }

        List<Task> hodTasks = new ArrayList<>();
        if (TaskData.tasks == null) {
            Log.d(TAG, "getHodTasks: TaskData.tasks is null");
            return hodTasks;
        }

        Log.d(TAG, "getHodTasks: TaskData.tasks.size() = " + TaskData.tasks.size());

        for (int i = 0; i < TaskData.tasks.size(); i++) {
            String raw = TaskData.tasks.get(i);
            if (VERBOSE_DEBUG) {
                Log.d(TAG, "RAW HOD TASK[" + i + "] = [" + raw + "]");
            }
            Task task = parseHodTask(raw, i);
            if (task != null) {
                if (VERBOSE_DEBUG) {
                    Log.d(TAG, "  Parsed taskId    = " + task.getTaskId());
                    Log.d(TAG, "  Parsed title     = " + task.getTaskTitle());
                    Log.d(TAG, "  Parsed faculty   = [" + task.getAssignedTo() + "]");
                    Log.d(TAG, "  Parsed deadline  = " + task.getDeadline());
                    Log.d(TAG, "  Parsed status    = " + task.getStatus());
                }
                hodTasks.add(task);
            }
        }

        // Persist after reading so data survives restarts
        if (!hodTasks.isEmpty()) {
            persistHodTasksToPrefs();
        }

        return hodTasks;
    }

    // -----------------------------------------------------------------------
    // Faculty matching
    // -----------------------------------------------------------------------

    /**
     * Determines whether a task's assignedTo field matches the given faculty user.
     *
     * HOD stores faculty as their display name (e.g. "Dr. T. Sarnya").
     * The Faculty session stores the same name from Firebase Firestore.
     * Matching uses:
     * 1. Direct case-insensitive equality
     * 2. Cleaned name equality (strips Dr., Mrs., Mr., Ms., Prof., spaces, punctuation)
     * 3. Core token matching (matches key name tokens like "sarnya", "vijayalakshmi", etc.)
     * 4. User email prefix matching (e.g. "sarnya" in sarnya@domain.com)
     * 5. User ID equality (for legacy/sample tasks)
     *
     * Multi-faculty tasks (multiple names separated by newlines/commas) are split
     * and each candidate is tested individually.
     */
    private boolean isTaskAssignedToFaculty(Task task, FacultyUser user) {
        if (task == null || user == null) return false;

        String assignedTo = task.getAssignedTo();
        if (assignedTo == null || assignedTo.trim().isEmpty()) return false;

        String userName  = user.getName();
        String userId    = user.getUserId();
        String userEmail = user.getEmail();

        if (VERBOSE_DEBUG) {
            Log.d(TAG, "  isTaskAssignedToFaculty: assignedTo=[" + assignedTo + "] userName=[" + userName + "] userId=[" + userId + "] userEmail=[" + userEmail + "]");
        }

        // Split on newlines (multi-faculty) or commas
        String[] candidates = assignedTo.split("[\r\n,]+");
        for (String candidate : candidates) {
            String c = candidate.trim();
            if (c.isEmpty()) continue;

            // 1. Direct case-insensitive equality
            if (userName != null && c.equalsIgnoreCase(userName.trim())) {
                if (VERBOSE_DEBUG) Log.d(TAG, "  MATCH (direct): [" + c + "] == [" + userName + "]");
                return true;
            }

            // 2. Cleaned name comparison (strips Dr., Mrs., etc. and non-alphanumerics)
            if (userName != null) {
                String cleanC = cleanName(c);
                String cleanU = cleanName(userName);
                if (!cleanC.isEmpty() && !cleanU.isEmpty() && cleanC.equalsIgnoreCase(cleanU)) {
                    if (VERBOSE_DEBUG) Log.d(TAG, "  MATCH (cleaned): [" + cleanC + "] == [" + cleanU + "]");
                    return true;
                }
            }

            // 3. Core token matching (matches "sarnya", "vijayalakshmi", "sudharsan", etc.)
            if (isCoreNameMatch(c, userName, userEmail)) {
                if (VERBOSE_DEBUG) Log.d(TAG, "  MATCH (core token): [" + c + "] matches user [" + userName + " / " + userEmail + "]");
                return true;
            }

            // 4. User ID equality (for legacy sample tasks or direct ID assignments)
            if (userId != null && c.equalsIgnoreCase(userId.trim())) {
                if (VERBOSE_DEBUG) Log.d(TAG, "  MATCH (userId): [" + c + "] == [" + userId + "]");
                return true;
            }
        }

        if (VERBOSE_DEBUG) Log.d(TAG, "  NO MATCH for task [" + task.getTaskTitle() + "]");
        return false;
    }

    /**
     * Checks if any significant name token (>= 3 chars, excluding titles) matches
     * between the assigned candidate string and the logged-in user's name or email.
     */
    private boolean isCoreNameMatch(String candidate, String userName, String userEmail) {
        if (candidate == null) return false;
        List<String> candTokens = extractSignificantTokens(candidate);
        if (candTokens.isEmpty()) return false;

        // Check against userName tokens
        if (userName != null) {
            List<String> userTokens = extractSignificantTokens(userName);
            for (String ct : candTokens) {
                for (String ut : userTokens) {
                    if (ct.equalsIgnoreCase(ut)) {
                        return true;
                    }
                    // Handle minor spelling variations (e.g. sarnya vs saranya)
                    if (ct.length() >= 5 && ut.length() >= 5 && (ct.contains(ut) || ut.contains(ct) || levenshteinDistance(ct, ut) <= 1)) {
                        return true;
                    }
                }
            }
        }

        // Check against userEmail prefix (part before @)
        if (userEmail != null && userEmail.contains("@")) {
            String emailPrefix = userEmail.substring(0, userEmail.indexOf("@")).toLowerCase();
            for (String ct : candTokens) {
                if (ct.length() >= 4 && emailPrefix.contains(ct)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<String> extractSignificantTokens(String input) {
        List<String> tokens = new ArrayList<>();
        if (input == null) return tokens;
        String[] parts = input.split("[^a-zA-Z0-9]+");
        for (String p : parts) {
            String s = p.trim().toLowerCase();
            // Exclude common honorific titles
            if (s.length() >= 3 && !s.equals("dr") && !s.equals("mrs") && !s.equals("mr")
                    && !s.equals("ms") && !s.equals("prof") && !s.equals("faculty")) {
                tokens.add(s);
            }
        }
        return tokens;
    }

    private int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    /**
     * Strips honorific prefixes (Dr., Mrs., Mr., Ms., Prof.) and all non-alphanumeric
     * characters, then lowercases. Used for safe fuzzy comparison.
     *
     * Example: "Dr. T. Sarnya"  → "tsarnya"
     *          "Dr.T.Sarnya"    → "tsarnya"
     *          "DR T SARNYA"    → "tsarnya"
     */
    private String cleanName(String name) {
        if (name == null) return "";
        String s = name.toLowerCase().trim();
        s = s.replaceAll("^(dr\\.?|mr\\.?|mrs\\.?|ms\\.?|prof\\.?)\\s*", "");
        s = s.replaceAll("[^a-z0-9]", "");
        return s.trim();
    }

    // -----------------------------------------------------------------------
    // Public task query API
    // -----------------------------------------------------------------------

    /**
     * Returns all tasks assigned to the given faculty user.
     * Reads live from TaskData.tasks (restoring from SharedPreferences if needed).
     * Never shows tasks belonging to other faculty.
     */
    public synchronized List<Task> getTasksForFaculty(FacultyUser user) {
        List<Task> result = new ArrayList<>();

        if (user == null) {
            Log.d(TAG, "getTasksForFaculty: user is null — returning empty list");
            return result;
        }

        Log.d(TAG, "getTasksForFaculty: currentUser.name=[" + user.getName() + "] userId=[" + user.getUserId() + "]");

        List<Task> hodTasks = getHodTasks();
        Log.d(TAG, "getTasksForFaculty: HOD task count = " + hodTasks.size());

        for (Task task : hodTasks) {
            boolean matched = isTaskAssignedToFaculty(task, user);
            Log.d(TAG, "  task[" + task.getTaskId() + "] title=[" + task.getTaskTitle() + "] MATCH=" + matched);
            if (matched) result.add(task);
        }

        Log.d(TAG, "getTasksForFaculty: returning " + result.size() + " task(s) for [" + user.getName() + "]");
        return result;
    }

    /**
     * Retrieves a task by its unique ID.
     * For HOD tasks (prefix HOD-TASK-), re-reads from TaskData.tasks/prefs live
     * so status changes are always reflected.
     */
    public synchronized Task getTaskById(String taskId) {
        if (taskId == null) return null;

        if (taskId.startsWith(HOD_TASK_PREFIX)) {
            // Ensure list is populated
            if (TaskData.tasks == null || TaskData.tasks.isEmpty()) {
                restoreHodTasksFromPrefs();
            }
            try {
                int index = Integer.parseInt(taskId.substring(HOD_TASK_PREFIX.length()));
                if (TaskData.tasks != null && index >= 0 && index < TaskData.tasks.size()) {
                    Task t = parseHodTask(TaskData.tasks.get(index), index);
                    Log.d(TAG, "getTaskById: found HOD task at index " + index + " title=[" + (t != null ? t.getTaskTitle() : "null") + "]");
                    return t;
                } else {
                    Log.e(TAG, "getTaskById: index " + index + " out of bounds (size=" + (TaskData.tasks != null ? TaskData.tasks.size() : "null") + ")");
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "getTaskById: bad taskId=" + taskId, e);
            }
        }

        return null;
    }

    /**
     * Updates the status of a task.
     *
     * For HOD tasks: updates the raw string inside TaskData.tasks (in-memory)
     * AND re-persists the updated list to SharedPreferences so the change
     * survives app restarts and is visible to both HOD ViewTasksActivity
     * (same session) and Faculty on subsequent opens.
     */
    public synchronized boolean updateTaskStatus(String taskId, String newStatus) {
        if (taskId == null || newStatus == null) return false;
        String status = newStatus.trim().toUpperCase();

        if (taskId.startsWith(HOD_TASK_PREFIX)) {
            // Ensure list is populated first
            if (TaskData.tasks == null || TaskData.tasks.isEmpty()) {
                restoreHodTasksFromPrefs();
            }
            try {
                int index = Integer.parseInt(taskId.substring(HOD_TASK_PREFIX.length()));
                if (TaskData.tasks != null && index >= 0 && index < TaskData.tasks.size()) {
                    String raw = TaskData.tasks.get(index);
                    String updated;
                    if (raw.contains("Status:")) {
                        // Replace existing Status: line
                        updated = raw.replaceAll("(?i)Status:[^\r\n]*", "Status: " + status);
                    } else {
                        // Append Status: line
                        updated = raw.trim() + "\nStatus: " + status;
                    }
                    TaskData.tasks.set(index, updated);
                    // Re-persist so Faculty sees the change across restarts
                    persistHodTasksToPrefs();
                    Log.d(TAG, "updateTaskStatus: HOD task[" + index + "] status -> " + status);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "updateTaskStatus: error", e);
            }
            return false;
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Dashboard statistics (FacultyUser-based)
    // -----------------------------------------------------------------------

    public synchronized int getTotalTaskCount(FacultyUser user) {
        return getTasksForFaculty(user).size();
    }

    public synchronized int getPendingTaskCount(FacultyUser user) {
        int c = 0;
        for (Task t : getTasksForFaculty(user))
            if (Task.STATUS_PENDING.equalsIgnoreCase(t.getStatus())) c++;
        return c;
    }

    public synchronized int getInProgressTaskCount(FacultyUser user) {
        int c = 0;
        for (Task t : getTasksForFaculty(user))
            if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(t.getStatus())) c++;
        return c;
    }

    public synchronized int getCompletedTaskCount(FacultyUser user) {
        int c = 0;
        for (Task t : getTasksForFaculty(user))
            if (Task.STATUS_COMPLETED.equalsIgnoreCase(t.getStatus())) c++;
        return c;
    }

    // -----------------------------------------------------------------------
    // Legacy String overloads (kept for any compile-time compatibility)
    // -----------------------------------------------------------------------

    public synchronized List<Task> getTasksForFaculty(String facultyNameOrId) {
        // Wrap in a temporary FacultyUser so matching logic is consistent
        FacultyUser temp = new FacultyUser(facultyNameOrId, facultyNameOrId, "", "", "FACULTY");
        return getTasksForFaculty(temp);
    }

    public synchronized int getTotalTaskCount(String n)     { return getTasksForFaculty(n).size(); }
    public synchronized int getPendingTaskCount(String n)   {
        int c = 0;
        for (Task t : getTasksForFaculty(n))
            if (Task.STATUS_PENDING.equalsIgnoreCase(t.getStatus())) c++;
        return c;
    }
    public synchronized int getInProgressTaskCount(String n) {
        int c = 0;
        for (Task t : getTasksForFaculty(n))
            if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(t.getStatus())) c++;
        return c;
    }
    public synchronized int getCompletedTaskCount(String n) {
        int c = 0;
        for (Task t : getTasksForFaculty(n))
            if (Task.STATUS_COMPLETED.equalsIgnoreCase(t.getStatus())) c++;
        return c;
    }
}
