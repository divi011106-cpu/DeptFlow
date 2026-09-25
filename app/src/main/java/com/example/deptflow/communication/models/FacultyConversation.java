package com.example.deptflow.communication.models;

import com.example.deptflow.feature.faculty.models.FacultyUser;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing a conversation item in the Faculty Chat list.
 * Holds the other faculty's user profile together with real-time last message metadata and unread count.
 */
public class FacultyConversation implements Serializable {

    private FacultyUser faculty;
    private String chatId;
    private String lastMessage;
    private long lastMessageTimestamp;
    private String lastSenderId;
    private String lastSenderName;
    private int unreadCount;
    private boolean isLastMessageMine;

    public FacultyConversation() {
    }

    public FacultyConversation(FacultyUser faculty, String chatId) {
        this.faculty = faculty;
        this.chatId = chatId != null ? chatId : "";
        this.lastMessage = "";
        this.lastMessageTimestamp = 0;
        this.lastSenderId = "";
        this.lastSenderName = "";
        this.unreadCount = 0;
        this.isLastMessageMine = false;
    }

    public FacultyUser getFaculty() {
        return faculty;
    }

    public void setFaculty(FacultyUser faculty) {
        this.faculty = faculty;
    }

    public String getChatId() {
        return chatId != null ? chatId : "";
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastSenderId() {
        return lastSenderId != null ? lastSenderId : "";
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public String getLastSenderName() {
        return lastSenderName != null ? lastSenderName : "";
    }

    public void setLastSenderName(String lastSenderName) {
        this.lastSenderName = lastSenderName;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = Math.max(0, unreadCount);
    }

    public boolean isLastMessageMine() {
        return isLastMessageMine;
    }

    public void setLastMessageMine(boolean lastMessageMine) {
        isLastMessageMine = lastMessageMine;
    }

    /**
     * Formatted string representation of the last message timestamp.
     * Examples: "10:45 AM", "Yesterday", "24 Sep"
     */
    public String getFormattedTime() {
        if (lastMessageTimestamp <= 0) {
            return "";
        }

        Calendar now = Calendar.getInstance();
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(lastMessageTimestamp);

        if (now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) {
            int nowDay = now.get(Calendar.DAY_OF_YEAR);
            int msgDay = messageTime.get(Calendar.DAY_OF_YEAR);

            if (nowDay == msgDay) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return timeFormat.format(new Date(lastMessageTimestamp));
            } else if (nowDay - msgDay == 1) {
                return "Yesterday";
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
                return dateFormat.format(new Date(lastMessageTimestamp));
            }
        } else {
            SimpleDateFormat fullFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            return fullFormat.format(new Date(lastMessageTimestamp));
        }
    }
}
