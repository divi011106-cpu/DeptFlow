package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing a notification in the Communication module.
 * Represents task assignment alerts and department announcements.
 */
public class AppNotification implements Serializable {

    public static final String TYPE_TASK = "TASK_ASSIGNED";
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";

    private String id;
    private String type;
    private String title;
    private String subtitle;
    private String message;
    private String deadline;
    private String priority;
    private long timestamp;
    private String sender;
    private String taskId;
    private boolean isRead;

    public AppNotification() {
    }

    public AppNotification(String id, String type, String title, String subtitle,
                           String message, String deadline, String priority,
                           long timestamp, String sender, String taskId, boolean isRead) {
        this.id = id;
        this.type = type != null ? type : TYPE_TASK;
        this.title = title;
        this.subtitle = subtitle;
        this.message = message;
        this.deadline = deadline;
        this.priority = priority;
        this.timestamp = timestamp;
        this.sender = sender != null ? sender : "HOD";
        this.taskId = taskId;
        this.isRead = isRead;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type != null ? type : TYPE_TASK;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle != null ? subtitle : "";
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender != null ? sender : "HOD";
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTaskId() {
        return taskId != null ? taskId : "";
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * Formats the timestamp into a human-friendly string:
     * - "Today, 10:45 AM"
     * - "Yesterday, 04:20 PM"
     * - "25 Sep 2026, 09:15 AM"
     */
    public String getFormattedDate() {
        if (timestamp <= 0) {
            return "Recent";
        }

        Calendar now = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = timeFormat.format(new Date(timestamp));

        if (now.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
            if (now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR)) {
                return "Today, " + timeStr;
            } else if (now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1) {
                return "Yesterday, " + timeStr;
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                return dateFormat.format(new Date(timestamp));
            }
        } else {
            SimpleDateFormat fullFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return fullFormat.format(new Date(timestamp));
        }
    }
}
