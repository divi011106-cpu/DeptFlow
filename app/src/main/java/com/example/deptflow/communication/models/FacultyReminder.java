package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing a persistent faculty reminder created in the Communication module.
 * Stored in Firestore collection "faculty_reminders" and scheduled via Android AlarmManager.
 */
public class FacultyReminder implements Serializable {

    private String reminderId;
    private String facultyId;
    private String facultyUid;
    private String facultyName;
    private String title;
    private String description;
    private long scheduledDateTime; // Milliseconds since epoch
    private String scheduledDateStr;
    private String scheduledTimeStr;
    private long createdAt;
    private boolean isCompleted;
    private String status; // "PENDING", "COMPLETED"

    public FacultyReminder() {
    }

    public FacultyReminder(String reminderId, String facultyId, String facultyUid,
                           String facultyName, String title, String description,
                           long scheduledDateTime, String scheduledDateStr,
                           String scheduledTimeStr, long createdAt, boolean isCompleted,
                           String status) {
        this.reminderId = reminderId;
        this.facultyId = facultyId != null ? facultyId : "";
        this.facultyUid = facultyUid != null ? facultyUid : "";
        this.facultyName = facultyName != null ? facultyName : "";
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.scheduledDateTime = scheduledDateTime;
        this.scheduledDateStr = scheduledDateStr != null ? scheduledDateStr : "";
        this.scheduledTimeStr = scheduledTimeStr != null ? scheduledTimeStr : "";
        this.createdAt = createdAt;
        this.isCompleted = isCompleted;
        this.status = status != null ? status : (isCompleted ? "COMPLETED" : "PENDING");
    }

    public String getReminderId() {
        return reminderId != null ? reminderId : "";
    }

    public void setReminderId(String reminderId) {
        this.reminderId = reminderId;
    }

    public String getFacultyId() {
        return facultyId != null ? facultyId : "";
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyUid() {
        return facultyUid != null ? facultyUid : "";
    }

    public void setFacultyUid(String facultyUid) {
        this.facultyUid = facultyUid;
    }

    public String getFacultyName() {
        return facultyName != null ? facultyName : "";
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
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

    public long getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(long scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public String getScheduledDateStr() {
        return scheduledDateStr != null ? scheduledDateStr : "";
    }

    public void setScheduledDateStr(String scheduledDateStr) {
        this.scheduledDateStr = scheduledDateStr;
    }

    public String getScheduledTimeStr() {
        return scheduledTimeStr != null ? scheduledTimeStr : "";
    }

    public void setScheduledTimeStr(String scheduledTimeStr) {
        this.scheduledTimeStr = scheduledTimeStr;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
        this.status = completed ? "COMPLETED" : "PENDING";
    }

    public String getStatus() {
        return status != null ? status : (isCompleted ? "COMPLETED" : "PENDING");
    }

    public void setStatus(String status) {
        this.status = status;
        this.isCompleted = "COMPLETED".equalsIgnoreCase(status);
    }

    /**
     * Formats the scheduled date and time into a user-friendly string:
     * e.g., "Today at 04:30 PM", "Tomorrow at 10:00 AM", or "24 Sep 2026, 02:15 PM"
     */
    public String getFormattedScheduledText() {
        if (scheduledDateTime <= 0) {
            if (!getScheduledDateStr().isEmpty()) {
                return getScheduledDateStr() + (!getScheduledTimeStr().isEmpty() ? " at " + getScheduledTimeStr() : "");
            }
            return "No schedule";
        }

        Calendar now = Calendar.getInstance();
        Calendar scheduled = Calendar.getInstance();
        scheduled.setTimeInMillis(scheduledDateTime);

        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = timeFmt.format(new Date(scheduledDateTime));

        if (now.get(Calendar.YEAR) == scheduled.get(Calendar.YEAR)) {
            int nowDay = now.get(Calendar.DAY_OF_YEAR);
            int schedDay = scheduled.get(Calendar.DAY_OF_YEAR);

            if (nowDay == schedDay) {
                return "Today at " + timeStr;
            } else if (schedDay - nowDay == 1) {
                return "Tomorrow at " + timeStr;
            } else if (nowDay - schedDay == 1) {
                return "Yesterday at " + timeStr;
            } else {
                SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                return dateFmt.format(new Date(scheduledDateTime));
            }
        } else {
            SimpleDateFormat fullFmt = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return fullFmt.format(new Date(scheduledDateTime));
        }
    }

    /**
     * Returns true if the scheduled time is in the past.
     */
    public boolean isPastDue() {
        return scheduledDateTime > 0 && scheduledDateTime < System.currentTimeMillis();
    }
}
