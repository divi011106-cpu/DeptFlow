package com.example.deptflow.feature.faculty.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.deptflow.feature.faculty.models.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskRepository handles task data storage and retrieval.
 * Uses persistent SharedPreferences storage so status updates remain saved
 * and are immediately reflected throughout the app.
 * Can later be replaced seamlessly with a Room DB or Remote REST API.
 */
public class TaskRepository {

    private static final String TAG = "TaskRepository";
    private static final String PREF_NAME = "deptflow_tasks_pref";
    private static final String KEY_TASKS_JSON = "tasks_json_list";

    private static TaskRepository instance;
    private final SharedPreferences preferences;
    private final List<Task> cachedTasks = new ArrayList<>();

    private TaskRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadTasks();
    }

    public static synchronized TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context);
        }
        return instance;
    }

    /**
     * Loads tasks from SharedPreferences. If empty, populates default tasks.
     */
    private synchronized void loadTasks() {
        cachedTasks.clear();
        String json = preferences.getString(KEY_TASKS_JSON, null);

        if (json == null || json.trim().isEmpty()) {
            populateDefaultTasks();
            saveTasksToPreferences();
            return;
        }

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Task task = new Task(
                        obj.optString("taskId"),
                        obj.optString("taskTitle"),
                        obj.optString("description"),
                        obj.optString("assignedTo"),
                        obj.optString("assignedBy"),
                        obj.optString("deadline"),
                        obj.optString("status"),
                        obj.optString("priority")
                );
                cachedTasks.add(task);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing tasks json", e);
            populateDefaultTasks();
            saveTasksToPreferences();
        }
    }

    /**
     * Seeds initial realistic department tasks assigned to faculty member (FAC-102).
     */
    private void populateDefaultTasks() {
        cachedTasks.clear();
        cachedTasks.add(new Task(
                "TASK-1001",
                "Prepare Monthly Department Activity Report",
                "Compile academic metrics, guest lecture details, and student workshop participation for the IT department monthly review.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "15 Sep 2026",
                Task.STATUS_PENDING,
                Task.PRIORITY_HIGH
        ));

        cachedTasks.add(new Task(
                "TASK-1002",
                "Submit Mid-Term Examination Question Papers",
                "Prepare two sets of question papers along with answer keys for Course IT402 - Distributed Systems as per Bloom's Taxonomy.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "18 Sep 2026",
                Task.STATUS_IN_PROGRESS,
                Task.PRIORITY_HIGH
        ));

        cachedTasks.add(new Task(
                "TASK-1003",
                "Review Final Year Capstone Project Proposals",
                "Evaluate batch 2026 semester VII project proposals with rubric evaluation sheets and submit shortlisted list.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "22 Sep 2026",
                Task.STATUS_PENDING,
                Task.PRIORITY_MEDIUM
        ));

        cachedTasks.add(new Task(
                "TASK-1004",
                "Audit Cloud Computing Laboratory Systems",
                "Inspect hardware configurations, software licenses, and cloud IDE access for 60 lab workstations.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "25 Sep 2026",
                Task.STATUS_IN_PROGRESS,
                Task.PRIORITY_LOW
        ));

        cachedTasks.add(new Task(
                "TASK-1005",
                "ABET Accreditation Outcome Assessment",
                "Compute Course Outcome (CO) to Program Outcome (PO) mapping percentages for previous academic semester.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "05 Sep 2026",
                Task.STATUS_COMPLETED,
                Task.PRIORITY_HIGH
        ));

        cachedTasks.add(new Task(
                "TASK-1006",
                "Organize Guest Lecture on GenAI Architecture",
                "Coordinate venue, speaker honorarium approval, and student attendance for industry speaker from Google Cloud.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "02 Sep 2026",
                Task.STATUS_COMPLETED,
                Task.PRIORITY_MEDIUM
        ));

        cachedTasks.add(new Task(
                "TASK-1007",
                "Update Student Mentoring Portfolios",
                "Verify attendance records, internal marks, and counseling notes for 25 assigned mentee students.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "28 Sep 2026",
                Task.STATUS_PENDING,
                Task.PRIORITY_MEDIUM
        ));

        cachedTasks.add(new Task(
                "TASK-1008",
                "Department Library Book Recommendation",
                "Submit requisition list of newly published technical reference titles for upcoming academic year.",
                "FAC-102",
                "HOD (Dr. Robert Vance)",
                "01 Sep 2026",
                Task.STATUS_COMPLETED,
                Task.PRIORITY_LOW
        ));
    }

    /**
     * Saves cached tasks list into persistent SharedPreferences.
     */
    private synchronized void saveTasksToPreferences() {
        try {
            JSONArray array = new JSONArray();
            for (Task task : cachedTasks) {
                JSONObject obj = new JSONObject();
                obj.put("taskId", task.getTaskId());
                obj.put("taskTitle", task.getTaskTitle());
                obj.put("description", task.getDescription());
                obj.put("assignedTo", task.getAssignedTo());
                obj.put("assignedBy", task.getAssignedBy());
                obj.put("deadline", task.getDeadline());
                obj.put("status", task.getStatus());
                obj.put("priority", task.getPriority());
                array.put(obj);
            }
            preferences.edit().putString(KEY_TASKS_JSON, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving tasks json", e);
        }
    }

    /**
     * Returns tasks assigned to the given faculty user ID.
     */
    public synchronized List<Task> getTasksForFaculty(String facultyUserId) {
        List<Task> result = new ArrayList<>();
        for (Task task : cachedTasks) {
            if (facultyUserId == null || facultyUserId.equalsIgnoreCase(task.getAssignedTo())) {
                result.add(task);
            }
        }
        return result;
    }

    /**
     * Retrieves a task by its unique ID.
     */
    public synchronized Task getTaskById(String taskId) {
        if (taskId == null) return null;
        for (Task task : cachedTasks) {
            if (taskId.equalsIgnoreCase(task.getTaskId())) {
                return task;
            }
        }
        return null;
    }

    /**
     * Updates the status of a task and persists to storage.
     *
     * @param taskId    the unique task identifier
     * @param newStatus PENDING, IN_PROGRESS, or COMPLETED
     * @return true if updated successfully, false otherwise
     */
    public synchronized boolean updateTaskStatus(String taskId, String newStatus) {
        if (taskId == null || newStatus == null) return false;

        for (Task task : cachedTasks) {
            if (taskId.equalsIgnoreCase(task.getTaskId())) {
                task.setStatus(newStatus.trim().toUpperCase());
                saveTasksToPreferences();
                return true;
            }
        }
        return false;
    }

    public synchronized int getTotalTaskCount(String facultyUserId) {
        return getTasksForFaculty(facultyUserId).size();
    }

    public synchronized int getPendingTaskCount(String facultyUserId) {
        int count = 0;
        for (Task task : getTasksForFaculty(facultyUserId)) {
            if (Task.STATUS_PENDING.equalsIgnoreCase(task.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getInProgressTaskCount(String facultyUserId) {
        int count = 0;
        for (Task task : getTasksForFaculty(facultyUserId)) {
            if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(task.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getCompletedTaskCount(String facultyUserId) {
        int count = 0;
        for (Task task : getTasksForFaculty(facultyUserId)) {
            if (Task.STATUS_COMPLETED.equalsIgnoreCase(task.getStatus())) {
                count++;
            }
        }
        return count;
    }
}
