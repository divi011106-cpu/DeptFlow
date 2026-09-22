package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing a chat message in a task discussion group.
 */
public class DiscussionMessage implements Serializable {

    private String messageId;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String message;
    private long timestamp;

    public DiscussionMessage() {
        // Required for Firestore deserialization
    }

    public DiscussionMessage(String messageId, String senderId, String senderName,
                             String senderRole, String message, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId != null ? messageId : "";
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId != null ? senderId : "";
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName != null ? senderName : "Faculty";
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderRole() {
        return senderRole != null ? senderRole : "Faculty";
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSentBy(String currentUserId) {
        return currentUserId != null && !currentUserId.isEmpty() && currentUserId.equals(senderId);
    }

    public String getFormattedTime() {
        if (timestamp <= 0) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
