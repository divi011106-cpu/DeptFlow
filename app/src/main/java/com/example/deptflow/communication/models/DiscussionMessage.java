package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Model representing a chat message in a task discussion group.
 */
public class DiscussionMessage implements Serializable {

    private String messageId;
    private String senderId;
    private String senderUid;
    private String senderCanonicalId;
    private String senderName;
    private String senderRole;
    private String message;
    private long timestamp;

    private String status = "sent";
    private List<String> deliveredTo = new ArrayList<>();
    private List<String> readBy = new ArrayList<>();

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
        this.status = "sent";
        this.deliveredTo = new ArrayList<>();
        this.readBy = new ArrayList<>();
    }

    public DiscussionMessage(String messageId, String senderId, String senderUid,
                             String senderCanonicalId, String senderName,
                             String senderRole, String message, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderUid = senderUid;
        this.senderCanonicalId = senderCanonicalId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
        this.status = "sent";
        this.deliveredTo = new ArrayList<>();
        this.readBy = new ArrayList<>();
    }

    public DiscussionMessage(String messageId, String senderId, String senderUid,
                             String senderCanonicalId, String senderName,
                             String senderRole, String message, long timestamp,
                             String status, List<String> deliveredTo, List<String> readBy) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderUid = senderUid;
        this.senderCanonicalId = senderCanonicalId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.message = message;
        this.timestamp = timestamp;
        this.status = (status != null && !status.trim().isEmpty()) ? status : "sent";
        this.deliveredTo = deliveredTo != null ? deliveredTo : new ArrayList<>();
        this.readBy = readBy != null ? readBy : new ArrayList<>();
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

    public String getSenderUid() {
        return senderUid != null ? senderUid : "";
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderCanonicalId() {
        return senderCanonicalId != null ? senderCanonicalId : "";
    }

    public void setSenderCanonicalId(String senderCanonicalId) {
        this.senderCanonicalId = senderCanonicalId;
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

    public String getStatus() {
        return status != null && !status.trim().isEmpty() ? status : "sent";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDeliveredTo() {
        return deliveredTo != null ? deliveredTo : new ArrayList<>();
    }

    public void setDeliveredTo(List<String> deliveredTo) {
        this.deliveredTo = deliveredTo;
    }

    public List<String> getReadBy() {
        return readBy != null ? readBy : new ArrayList<>();
    }

    public void setReadBy(List<String> readBy) {
        this.readBy = readBy;
    }

    public boolean isSentBy(String currentUserId) {
        if (currentUserId == null || currentUserId.trim().isEmpty()) return false;
        String cur = currentUserId.trim();
        return cur.equalsIgnoreCase(senderId)
                || cur.equalsIgnoreCase(senderUid)
                || cur.equalsIgnoreCase(senderCanonicalId)
                || cur.equalsIgnoreCase(senderName);
    }

    public boolean isSentBy(Set<String> myIdentifiers) {
        if (myIdentifiers == null || myIdentifiers.isEmpty()) return false;
        if (senderId != null && (myIdentifiers.contains(senderId) || myIdentifiers.contains(senderId.toLowerCase(Locale.ROOT)))) return true;
        if (senderUid != null && (myIdentifiers.contains(senderUid) || myIdentifiers.contains(senderUid.toLowerCase(Locale.ROOT)))) return true;
        if (senderCanonicalId != null && (myIdentifiers.contains(senderCanonicalId) || myIdentifiers.contains(senderCanonicalId.toLowerCase(Locale.ROOT)))) return true;
        if (senderName != null && (myIdentifiers.contains(senderName) || myIdentifiers.contains(senderName.toLowerCase(Locale.ROOT)))) return true;
        return false;
    }

    /**
     * Checks if this message has been read by any recipient (excluding the sender).
     */
    public boolean isReadByOthers(Set<String> senderIdentifiers) {
        if ("read".equalsIgnoreCase(status)) return true;
        if (readBy != null && !readBy.isEmpty()) {
            for (String r : readBy) {
                if (r != null && !r.trim().isEmpty()) {
                    String rTrim = r.trim().toLowerCase(Locale.ROOT);
                    if (senderIdentifiers != null && senderIdentifiers.contains(rTrim)) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if this message has been delivered to any recipient (excluding the sender).
     */
    public boolean isDeliveredToOthers(Set<String> senderIdentifiers) {
        if ("delivered".equalsIgnoreCase(status)) return true;
        if (deliveredTo != null && !deliveredTo.isEmpty()) {
            for (String d : deliveredTo) {
                if (d != null && !d.trim().isEmpty()) {
                    String dTrim = d.trim().toLowerCase(Locale.ROOT);
                    if (senderIdentifiers != null && senderIdentifiers.contains(dTrim)) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns 0 for Sent (✓), 1 for Delivered (✓✓ grey), 2 for Read (✓✓ blue).
     */
    public int getTickStatus(Set<String> myIdentifiers) {
        if (isReadByOthers(myIdentifiers)) {
            return 2;
        }
        if (isDeliveredToOthers(myIdentifiers)) {
            return 1;
        }
        return 0;
    }

    public String getFormattedTime() {
        if (timestamp <= 0) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
