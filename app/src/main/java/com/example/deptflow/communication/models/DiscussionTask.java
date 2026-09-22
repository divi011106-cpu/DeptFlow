package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a task available for group discussion in the Communication module.
 * Maps cleanly to Firestore documents from the 'tasks' collection.
 */
public class DiscussionTask implements Serializable {

    private String id;
    private String title;
    private String description;
    private String deadline;
    private String priority;
    private String status;
    private List<String> assignedFaculty;
    private List<String> assignedFacultyUids;
    private int assignedFacultyCount;
    private String faculty; // Legacy field for individual faculty records
    private long timestamp;

    public DiscussionTask() {
        // Required for Firestore deserialization
        this.assignedFaculty = new ArrayList<>();
        this.assignedFacultyUids = new ArrayList<>();
    }

    public DiscussionTask(String id, String title, String description, String deadline,
                          String priority, String status, List<String> assignedFaculty,
                          List<String> assignedFacultyUids, int assignedFacultyCount,
                          String faculty, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.status = status;
        this.assignedFaculty = assignedFaculty != null ? assignedFaculty : new ArrayList<>();
        this.assignedFacultyUids = assignedFacultyUids != null ? assignedFacultyUids : new ArrayList<>();
        this.assignedFacultyCount = assignedFacultyCount;
        this.faculty = faculty;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeadline() {
        return deadline != null ? deadline : "";
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getPriority() {
        return priority != null ? priority : "Medium";
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status != null ? status : "Assigned";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getAssignedFaculty() {
        return assignedFaculty;
    }

    public void setAssignedFaculty(List<String> assignedFaculty) {
        this.assignedFaculty = assignedFaculty != null ? assignedFaculty : new ArrayList<>();
    }

    public List<String> getAssignedFacultyUids() {
        return assignedFacultyUids;
    }

    public void setAssignedFacultyUids(List<String> assignedFacultyUids) {
        this.assignedFacultyUids = assignedFacultyUids != null ? assignedFacultyUids : new ArrayList<>();
    }

    public int getAssignedFacultyCount() {
        return assignedFacultyCount;
    }

    public void setAssignedFacultyCount(int assignedFacultyCount) {
        this.assignedFacultyCount = assignedFacultyCount;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Determines whether this task is assigned to the given faculty user.
     * 1. Primary check: Matches against assignedFacultyUids using the user's Firebase UID.
     * 2. Fallback check: Matches against assignedFaculty or faculty using display name if UID list is missing.
     * 3. Disambiguation: If multiple faculty members in the department share the exact same name,
     *    the task is NOT assumed to belong to them to avoid cross-assignment errors.
     */
    public boolean isAssignedToUser(String currentUid, String currentName, List<String> allDepartmentFacultyNames) {
        // 1. Primary Check: Match by Firebase UID
        if (assignedFacultyUids != null && !assignedFacultyUids.isEmpty()) {
            if (currentUid != null && !currentUid.trim().isEmpty()) {
                return assignedFacultyUids.contains(currentUid.trim());
            }
            return false;
        }

        // 2. Fallback Check: Match by Faculty Name for older records without UID
        if (currentName == null || currentName.trim().isEmpty()) {
            return false;
        }

        String trimmedName = currentName.trim();

        // 3. Ambiguity Check: Do not assume ownership if name matches multiple faculty members
        if (allDepartmentFacultyNames != null) {
            int count = 0;
            for (String name : allDepartmentFacultyNames) {
                if (name != null && name.trim().equalsIgnoreCase(trimmedName)) {
                    count++;
                }
            }
            if (count > 1) {
                // Ambiguous match: multiple faculty share the exact same name
                return false;
            }
        }

        // Check single faculty field
        if (faculty != null && faculty.trim().equalsIgnoreCase(trimmedName)) {
            return true;
        }

        // Check assignedFaculty array
        if (assignedFaculty != null) {
            for (String fName : assignedFaculty) {
                if (fName != null && fName.trim().equalsIgnoreCase(trimmedName)) {
                    return true;
                }
            }
        }

        return false;
    }
}
