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
    public static final String TYPE_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String TYPE_CHAT = "CHAT_MESSAGE";

    private String id;
    private String type;
    private String title;
    private String subtitle;
    private String message;
    private String deadline;
    private String priority;
    private long timestamp;
    private String sender;
    private String senderId;
    private String recipientId;
    private String chatId;
    private String messageId;
    private String taskId;
    private boolean isRead;

    public AppNotification() {
    }

    /**
     * Constructor for Task assignment and Announcement notifications.
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
        this.timestamp = timestamp;
        this.sender = sender != null ? sender : "HOD";
        this.taskId = taskId;
        this.isRead = isRead;
        this.senderId = "";
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
