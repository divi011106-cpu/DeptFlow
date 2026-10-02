package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing a notification in the Communication module.
 * Represents task assignment alerts, department announcements, and faculty chat messages.
 */
public class AppNotification implements Serializable {

    public static final String TYPE_TASK = "TASK_ASSIGNED";
    public static final String TYPE_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TYPE_TASK_ASSIGNMENT = "TASK_ASSIGNMENT";
    public static final String TYPE_TEAM_TASK = "TEAM_TASK";
    public static final String TYPE_TEAM_TASK_ASSIGNED = "TEAM_TASK_ASSIGNED";
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String TYPE_CHAT = "CHAT_MESSAGE";
    public static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";

    private String id;
    private String type;
    private String title;
    private String subtitle;
    private String message;
    private String deadline;
    private String priority;
    private String status;
    private long timestamp;
    private String sender;
    private String senderId;
    private String senderRole;
    private String recipientId;
    private String chatId;
    private String messageId;
    private String taskId;
    private boolean isRead;
    private java.util.List<String> assignedMembers = new java.util.ArrayList<>();
    private int assignedMemberCount = 0;
    private boolean isAll = false;

    public AppNotification() {
    }

    /**
     * Constructor for Task assignment, Team Task, and Announcement notifications.
     */
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
        this.status = "PENDING";
        this.timestamp = timestamp;
        this.sender = sender != null ? sender : "HOD";
        this.taskId = taskId;
        this.isRead = isRead;
        this.senderId = "";
        this.senderRole = "";
        this.recipientId = "";
        this.chatId = "";
        this.messageId = "";
    }

    /**
     * Comprehensive constructor for Team Tasks and HOD assignments with member lists.
     */
    public AppNotification(String id, String type, String title, String subtitle,
                           String message, String deadline, String priority, String status,
                           long timestamp, String sender, String taskId,
                           java.util.List<String> assignedMembers, boolean isAll, boolean isRead) {
        this.id = id;
        this.type = type != null ? type : (assignedMembers != null && assignedMembers.size() > 1 ? TYPE_TEAM_TASK : TYPE_TASK_ASSIGNMENT);
        this.title = title;
        this.subtitle = subtitle;
        this.message = message;
        this.deadline = deadline;
        this.priority = priority;
        this.status = status != null ? status : "PENDING";
        this.timestamp = timestamp;
        this.sender = sender != null ? sender : "HOD";
        this.taskId = taskId;
        this.assignedMembers = assignedMembers != null ? assignedMembers : new java.util.ArrayList<>();
        this.assignedMemberCount = this.assignedMembers.size();
        this.isAll = isAll;
        this.isRead = isRead;
        this.senderId = "";
        this.senderRole = "";
        this.recipientId = "";
        this.chatId = "";
        this.messageId = "";
    }

    /**
     * Constructor for Faculty Chat notifications.
     */
    public AppNotification(String id, String type, String title, String subtitle,
                           String message, long timestamp, String sender,
                           String senderId, String recipientId, String chatId,
                           String messageId, boolean isRead) {
        this.id = id;
        this.type = type != null ? type : TYPE_CHAT;
        this.title = title;
        this.subtitle = subtitle;
        this.message = message;
        this.timestamp = timestamp;
        this.sender = sender != null ? sender : "Faculty";
        this.senderId = senderId != null ? senderId : "";
        this.recipientId = recipientId != null ? recipientId : "";
        this.chatId = chatId != null ? chatId : "";
        this.messageId = messageId != null ? messageId : "";
        this.isRead = isRead;
        this.deadline = "";
        this.priority = "";
        this.taskId = "";
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

    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId != null ? recipientId : "";
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getChatId() {
        return chatId != null ? chatId : "";
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessageId() {
        return messageId != null ? messageId : "";
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getStatus() {
        return status != null ? status : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSenderRole() {
        return senderRole != null ? senderRole : "";
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public java.util.List<String> getAssignedMembers() {
        return assignedMembers != null ? assignedMembers : new java.util.ArrayList<>();
    }

    public void setAssignedMembers(java.util.List<String> assignedMembers) {
        this.assignedMembers = assignedMembers != null ? assignedMembers : new java.util.ArrayList<>();
        this.assignedMemberCount = this.assignedMembers.size();
    }

    public int getAssignedMemberCount() {
        if (isAll) return 23;
        return assignedMembers != null ? Math.max(assignedMemberCount, assignedMembers.size()) : assignedMemberCount;
    }

    public void setAssignedMemberCount(int assignedMemberCount) {
        this.assignedMemberCount = assignedMemberCount;
    }

    public boolean isAll() {
        return isAll;
    }

    public void setAll(boolean all) {
        isAll = all;
    }

    public boolean isChat() {
        if (type == null) return false;
        String t = type.toUpperCase(Locale.ROOT);
        return t.contains("CHAT");
    }

    public boolean isTeamTask() {
        if (type == null) return false;
        String t = type.toUpperCase(Locale.ROOT);
        if (t.contains("TEAM")) return true;
        if (isAll) return true;
        return (assignedMembers != null && assignedMembers.size() > 1);
    }

    public boolean isTaskAssignment() {
        if (isChat()) return false;
        if (isTeamTask()) return false;
        if (type == null) return true;
        String t = type.toUpperCase(Locale.ROOT);
        return t.contains("TASK") || t.contains("ASSIGN");
    }

    public String getTeamMembersSummary() {
        if (isAll) {
            return "Team: All Department Faculty";
        }
        if (assignedMembers != null && !assignedMembers.isEmpty()) {
            StringBuilder sb = new StringBuilder("Team: ");
            for (int i = 0; i < assignedMembers.size(); i++) {
                sb.append(assignedMembers.get(i));
                if (i < assignedMembers.size() - 1) sb.append(" • ");
            }
            return sb.toString();
        }
        return "";
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
