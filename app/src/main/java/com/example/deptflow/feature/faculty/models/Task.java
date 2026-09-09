package com.example.deptflow.feature.faculty.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Task model representing an assignment created by the HOD for a Faculty member.
 */
public class Task implements Serializable {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_LOW = "LOW";

    private String taskId;
    private String taskTitle;
    private String description;
    private String assignedTo;  // Corresponds to faculty userId (e.g., "FAC-102")
    private String assignedBy;  // E.g., "HOD (Dr. Robert Vance)"
    private String deadline;    // E.g., "15 Sep 2026"
    private String status;      // PENDING, IN_PROGRESS, COMPLETED
    private String priority;    // HIGH, MEDIUM, LOW

    public Task() {
    }

    public Task(String taskId, String taskTitle, String description, String assignedTo,
                String assignedBy, String deadline, String status, String priority) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.description = description;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.deadline = deadline;
        this.status = status;
        this.priority = priority;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}
