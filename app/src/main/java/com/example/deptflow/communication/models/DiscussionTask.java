package com.example.deptflow.communication.models;

import com.example.deptflow.feature.faculty.models.FacultyUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Model representing a task available for group discussion in the Communication module.
 * Dynamically populated from Firestore documents in 'task_assignments'.
 */
public class DiscussionTask implements Serializable {

    private String id;
    private String title;
    private String description;
    private String deadline;
    private String priority;
    private String status;
    private String assignedBy;
    private List<String> assignedFaculty;
    private List<String> assignedFacultyUids;
    private List<String> assignedFacultyIds;
    private int assignedFacultyCount;
    private String faculty; // Legacy field for individual faculty records
    private String assignedTo;
    private boolean isAll;
    private long timestamp;
    private int unreadCount;

    public DiscussionTask() {
        this.assignedFaculty = new ArrayList<>();
        this.assignedFacultyUids = new ArrayList<>();
        this.assignedFacultyIds = new ArrayList<>();
        this.assignedBy = "HOD (Department Head)";
        this.priority = "MEDIUM";
        this.status = "PENDING";
    }

    public DiscussionTask(String id, String title, String description, String deadline,
                          String priority, String status, String assignedBy,
                          List<String> assignedFaculty, List<String> assignedFacultyUids,
                          List<String> assignedFacultyIds, int assignedFacultyCount,
                          String faculty, String assignedTo, boolean isAll, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority != null ? priority : "MEDIUM";
        this.status = status != null ? status : "PENDING";
        this.assignedBy = assignedBy != null && !assignedBy.trim().isEmpty() ? assignedBy : "HOD (Department Head)";
        this.assignedFaculty = assignedFaculty != null ? assignedFaculty : new ArrayList<>();
        this.assignedFacultyUids = assignedFacultyUids != null ? assignedFacultyUids : new ArrayList<>();
        this.assignedFacultyIds = assignedFacultyIds != null ? assignedFacultyIds : new ArrayList<>();
        this.assignedFacultyCount = assignedFacultyCount;
        this.faculty = faculty;
        this.assignedTo = assignedTo;
        this.isAll = isAll;
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
        return priority != null ? priority : "MEDIUM";
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedBy() {
        return assignedBy != null && !assignedBy.trim().isEmpty() ? assignedBy : "HOD (Department Head)";
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public List<String> getAssignedFaculty() {
        return assignedFaculty != null ? assignedFaculty : new ArrayList<>();
    }

    public void setAssignedFaculty(List<String> assignedFaculty) {
        this.assignedFaculty = assignedFaculty != null ? assignedFaculty : new ArrayList<>();
    }

    public List<String> getAssignedFacultyUids() {
        return assignedFacultyUids != null ? assignedFacultyUids : new ArrayList<>();
    }

    public void setAssignedFacultyUids(List<String> assignedFacultyUids) {
        this.assignedFacultyUids = assignedFacultyUids != null ? assignedFacultyUids : new ArrayList<>();
    }

    public List<String> getAssignedFacultyIds() {
        return assignedFacultyIds != null ? assignedFacultyIds : new ArrayList<>();
    }

    public void setAssignedFacultyIds(List<String> assignedFacultyIds) {
        this.assignedFacultyIds = assignedFacultyIds != null ? assignedFacultyIds : new ArrayList<>();
    }

    public int getAssignedFacultyCount() {
        if (isAll) {
            return Math.max(assignedFacultyCount, FacultyDirectory.FACULTY_NAMES.length);
        }
        if (assignedFaculty != null && !assignedFaculty.isEmpty()) {
            return Math.max(assignedFacultyCount, assignedFaculty.size());
        }
        return Math.max(assignedFacultyCount, 1);
    }

    public void setAssignedFacultyCount(int assignedFacultyCount) {
        this.assignedFacultyCount = assignedFacultyCount;
    }

    public String getFaculty() {
        return faculty != null ? faculty : "";
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getAssignedTo() {
        return assignedTo != null ? assignedTo : "";
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public boolean isAll() {
        return isAll;
    }

    public void setAll(boolean all) {
        this.isAll = all;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    /**
     * Determines whether this task is assigned to the current user.
     */
    public boolean isAssignedToUser(Set<String> myIdentifiers, boolean isHod) {
        // HOD can view all task discussions
        if (isHod) {
            return true;
        }

        // Tasks assigned to ALL are visible to every faculty member
        if (isAll) {
            return true;
        }

        if (myIdentifiers == null || myIdentifiers.isEmpty()) {
            return false;
        }

        // 1. Check assignedFacultyUids
        if (assignedFacultyUids != null) {
            for (String uid : assignedFacultyUids) {
                if (matchesIdentifier(uid, myIdentifiers)) return true;
            }
        }

        // 2. Check assignedFacultyIds
        if (assignedFacultyIds != null) {
            for (String fid : assignedFacultyIds) {
                if (matchesIdentifier(fid, myIdentifiers)) return true;
            }
        }

        // 3. Check assignedTo
        if (assignedTo != null && !assignedTo.trim().isEmpty()) {
            if (isAllString(assignedTo)) return true;
            if (matchesIdentifier(assignedTo, myIdentifiers)) return true;
        }

        // 4. Check faculty field
        if (faculty != null && !faculty.trim().isEmpty()) {
            if (isAllString(faculty)) return true;
            String[] parts = faculty.split("[\r\n,]+");
            for (String p : parts) {
                if (matchesIdentifier(p.trim(), myIdentifiers)) return true;
            }
        }

        // 5. Check assignedFaculty list
        if (assignedFaculty != null) {
            for (String fName : assignedFaculty) {
                if (isAllString(fName)) return true;
                if (matchesIdentifier(fName, myIdentifiers)) return true;
            }
        }

        return false;
    }

    private boolean isAllString(String s) {
        if (s == null) return false;
        String trimmed = s.trim().toUpperCase(Locale.ROOT);
        return trimmed.equals("ALL")
                || trimmed.equals("ALL FACULTY")
                || trimmed.equals("ALL FACULTIES")
                || trimmed.equals("ALL_FACULTY")
                || trimmed.equals("ALL_FACULTIES")
                || trimmed.equals("ALL MEMBERS")
                || trimmed.equals("EVERYONE");
    }

    private boolean matchesIdentifier(String val, Set<String> myIdentifiers) {
        if (val == null || val.trim().isEmpty()) return false;
        String trimmed = val.trim();
        if (myIdentifiers.contains(trimmed)) return true;
        if (myIdentifiers.contains(trimmed.toLowerCase(Locale.ROOT))) return true;

        String clean = trimmed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (!clean.isEmpty() && myIdentifiers.contains(clean)) return true;

        // Try resolving canonical faculty user
        FacultyUser resolved = FacultyDirectory.resolveByNameOrId(trimmed);
        if (resolved != null) {
            if (resolved.getUserId() != null && myIdentifiers.contains(resolved.getUserId().toLowerCase(Locale.ROOT))) return true;
            if (resolved.getName() != null && myIdentifiers.contains(resolved.getName().toLowerCase(Locale.ROOT))) return true;
            if (resolved.getEmail() != null && myIdentifiers.contains(resolved.getEmail().toLowerCase(Locale.ROOT))) return true;
        }

        return false;
    }

    public String getParticipantsSummary() {
        if (isAll) {
            return "All Faculty";
        }
        if (assignedFaculty != null && !assignedFaculty.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < assignedFaculty.size(); i++) {
                sb.append(assignedFaculty.get(i));
                if (i < assignedFaculty.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }
        if (assignedTo != null && !assignedTo.trim().isEmpty()) {
            return assignedTo.trim();
        }
        if (faculty != null && !faculty.trim().isEmpty()) {
            return faculty.trim();
        }
        return "Department Faculty";
    }
}
