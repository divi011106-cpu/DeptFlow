package com.example.deptflow.communication.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing a task deadline reminder in the Communication module.
 * Parses dates, dynamically computes countdowns, and categorizes tasks into
 * OVERDUE, DUE_TODAY, DUE_TOMORROW, UPCOMING, or COMPLETED.
 */
public class TaskReminder implements Serializable {

    public enum UrgencyCategory {
        OVERDUE,
        DUE_TODAY,
        DUE_TOMORROW,
        UPCOMING,
        COMPLETED
    }

    private final DiscussionTask task;
    private final UrgencyCategory category;
    private final String countdownText;
    private final long daysRemaining;
    private final long deadlineTimestamp;

    public TaskReminder(DiscussionTask task, UrgencyCategory category, String countdownText,
                        long daysRemaining, long deadlineTimestamp) {
        this.task = task;
        this.category = category;
        this.countdownText = countdownText;
        this.daysRemaining = daysRemaining;
        this.deadlineTimestamp = deadlineTimestamp;
    }

    public DiscussionTask getTask() {
        return task;
    }

    public UrgencyCategory getCategory() {
        return category;
    }

    public String getCountdownText() {
        return countdownText;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public long getDeadlineTimestamp() {
        return deadlineTimestamp;
    }

    public String getTaskId() {
        return task != null ? task.getId() : "";
    }

    public String getTitle() {
        return task != null ? task.getTitle() : "";
    }

    public String getDescription() {
        return task != null ? task.getDescription() : "";
    }

    public String getDeadline() {
        return task != null ? task.getDeadline() : "";
    }

    public String getPriority() {
        return task != null ? task.getPriority() : "MEDIUM";
    }

    public String getStatus() {
        return task != null ? task.getStatus() : "Assigned";
    }

    /**
     * Factory method that parses task deadline and computes the live countdown.
     */
    public static TaskReminder fromTask(DiscussionTask task) {
        if (task == null) {
            return new TaskReminder(null, UrgencyCategory.UPCOMING, "No Task", 0, 0);
        }

        // 1. Check if completed
        if ("COMPLETED".equalsIgnoreCase(task.getStatus())) {
            return new TaskReminder(task, UrgencyCategory.COMPLETED, "Completed", 0, 0);
        }

        String rawDeadline = task.getDeadline();
        if (rawDeadline == null || rawDeadline.trim().isEmpty()) {
            return new TaskReminder(task, UrgencyCategory.UPCOMING, "No deadline set", 999, 0);
        }

        // 2. Multi-format date parsing
        Date parsedDate = parseDate(rawDeadline.trim());
        if (parsedDate == null) {
            return new TaskReminder(task, UrgencyCategory.UPCOMING, "Due: " + rawDeadline.trim(), 999, 0);
        }

        // 3. Set target deadline to end of that day (23:59:59.999)
        Calendar deadlineCal = Calendar.getInstance();
        deadlineCal.setTime(parsedDate);
        deadlineCal.set(Calendar.HOUR_OF_DAY, 23);
        deadlineCal.set(Calendar.MINUTE, 59);
        deadlineCal.set(Calendar.SECOND, 59);
        deadlineCal.set(Calendar.MILLISECOND, 999);
        long deadlineTime = deadlineCal.getTimeInMillis();

        // 4. Calculate day difference relative to start of today
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        long todayStart = todayCal.getTimeInMillis();

        long now = System.currentTimeMillis();
        long diffDays = (deadlineTime - todayStart) / (24L * 60L * 60L * 1000L);

        UrgencyCategory cat;
        String countdown;

        if (deadlineTime < now) {
            cat = UrgencyCategory.OVERDUE;
            long pastDays = Math.max(1, (todayStart - deadlineTime) / (24L * 60L * 60L * 1000L) + 1);
            countdown = "Overdue by " + pastDays + (pastDays == 1 ? " day" : " days");
        } else if (diffDays == 0) {
            cat = UrgencyCategory.DUE_TODAY;
            countdown = "Due Today";
        } else if (diffDays == 1) {
            cat = UrgencyCategory.DUE_TOMORROW;
            countdown = "Due Tomorrow";
        } else {
            cat = UrgencyCategory.UPCOMING;
            countdown = "Due in " + diffDays + " days";
        }

        return new TaskReminder(task, cat, countdown, diffDays, deadlineTime);
    }

    private static Date parseDate(String dateStr) {
        String[] formats = new String[]{
                "dd MMM yyyy", // Primary HOD format (e.g. "28 Sep 2026")
                "d MMM yyyy",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(dateStr);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
