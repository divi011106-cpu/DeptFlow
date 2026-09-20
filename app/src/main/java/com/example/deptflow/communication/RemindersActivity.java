
package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.ReminderAdapter;
import com.example.deptflow.communication.models.DiscussionTask;
import com.example.deptflow.communication.models.TaskReminder;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RemindersActivity extends AppCompatActivity
        implements ReminderAdapter.OnReminderClickListener {

    private static final String TAG = "RemindersActivity";

    private enum FilterMode {
        ALL, OVERDUE, TOMORROW, UPCOMING
    }

    private static final List<String> DEPARTMENT_FACULTY =
            Arrays.asList(
                    "Dr. R. Vijayalakshmi",
                    "Dr. R. Raja Sudharsan",
                    "Dr. K. M. Alaaudeen",
                    "Dr. T. Sarnya",
                    "Mrs. M. Prabha",
                    "Mrs. P. Saraswathi",
                    "Mr. S. Jegadeesan",
                    "Mrs. A. Meena",
                    "Dr. T. Venkatesh Kanna",
                    "Mrs. M. Ishvarya",
                    "Mrs. R. Nancy Deborah",
                    "Mrs. C. Manjula Devi",
                    "Mrs. A. Vinora",
                    "Mr. A. Srinivasan",
                    "Mr. P. KalyanaKumar",
                    "Ms. G. Sivakarthi",
                    "Mrs. M. Soundarya",
                    "Mrs. J. John Shiny",
                    "Mr. R. Umesh",
                    "Mrs. A. Periya Nayaki",
                    "Mrs. A. Elavarasi",
                    "Dr. S. Esakki Muthu",
                    "Mr. K. Loganathan"
            );

    private RecyclerView rvReminders;
    private ProgressBar pbLoadingReminders;
    private View layoutEmptyReminders;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private ImageButton ibBackReminders;

    private TextView chipAll;
    private TextView chipOverdue;
    private TextView chipTomorrow;
    private TextView chipUpcoming;

    private ReminderAdapter adapter;

    private final List<TaskReminder> displayedReminders =
            new ArrayList<>();

    private final List<TaskReminder> allReminders =
            new ArrayList<>();

    private FilterMode currentFilter = FilterMode.ALL;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration tasksListener;

    private String currentUid = "";
    private String currentName = "";
    private String currentUserRole = "FACULTY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        initViews();
        loadCurrentUserInfo();
        setupRecyclerView();
        setupFilterChips();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        listenToFirestoreTasks();
    }

    private void initViews() {
        rvReminders = findViewById(R.id.rvReminders);
        pbLoadingReminders = findViewById(R.id.pbLoadingReminders);
        layoutEmptyReminders = findViewById(R.id.layoutEmptyReminders);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        ibBackReminders = findViewById(R.id.ibBackReminders);

        chipAll = findViewById(R.id.chipAll);
        chipOverdue = findViewById(R.id.chipOverdue);
        chipTomorrow = findViewById(R.id.chipTomorrow);
        chipUpcoming = findViewById(R.id.chipUpcoming);

        if (ibBackReminders != null) {
            ibBackReminders.setOnClickListener(v -> finish());
        }
    }

    private void loadCurrentUserInfo() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();

            if (firebaseUser.getDisplayName() != null
                    && !firebaseUser.getDisplayName()
                    .trim().isEmpty()) {

                currentName =
                        firebaseUser.getDisplayName().trim();
            }
        }

        FacultyUser cachedUser =
                AuthManager.getInstance(this).getCurrentUser();

        if (cachedUser != null) {
            if (cachedUser.getName() != null
                    && !cachedUser.getName().trim().isEmpty()) {

                currentName = cachedUser.getName().trim();
            }

            if (cachedUser.getRole() != null
                    && !cachedUser.getRole().trim().isEmpty()) {

                currentUserRole =
                        cachedUser.getRole().trim().toUpperCase();
            }
        }

        Log.d(TAG,
                "User UID=" + currentUid
                        + ", Name=" + currentName
                        + ", Role=" + currentUserRole);
    }

    private void setupRecyclerView() {
        adapter = new ReminderAdapter(
                this,
                displayedReminders,
                this
        );

        rvReminders.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvReminders.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(
                v -> setFilter(FilterMode.ALL)
        );

        chipOverdue.setOnClickListener(
                v -> setFilter(FilterMode.OVERDUE)
        );

        chipTomorrow.setOnClickListener(
                v -> setFilter(FilterMode.TOMORROW)
        );

        chipUpcoming.setOnClickListener(
                v -> setFilter(FilterMode.UPCOMING)
        );

        updateFilterChipStyles();
    }

    private void setFilter(FilterMode mode) {
        currentFilter = mode;
        updateFilterChipStyles();
        applyFilter();
    }

    private void updateFilterChipStyles() {
        resetChip(chipAll);
        resetChip(chipOverdue);
        resetChip(chipTomorrow);
        resetChip(chipUpcoming);

        TextView activeChip;

        switch (currentFilter) {
            case OVERDUE:
                activeChip = chipOverdue;
                break;

            case TOMORROW:
                activeChip = chipTomorrow;
                break;

            case UPCOMING:
                activeChip = chipUpcoming;
                break;

            case ALL:
            default:
                activeChip = chipAll;
                break;
        }

        activeChip.setBackgroundResource(
                R.drawable.bg_chip_selected
        );

        activeChip.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_on_primary
                )
        );
    }

    private void resetChip(TextView chip) {
        chip.setBackgroundResource(
                R.drawable.bg_chip_unselected
        );

        chip.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_secondary
                )
        );
    }

    private void listenToFirestoreTasks() {
        pbLoadingReminders.setVisibility(View.VISIBLE);

        tasksListener = db.collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    pbLoadingReminders.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG,
                                "Error listening to task_assignments",
                                error);

                        Toast.makeText(
                                RemindersActivity.this,
                                "Error loading reminders: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    boolean isHod =
                            "HOD".equalsIgnoreCase(currentUserRole);

                    Map<String, DiscussionTask> uniqueTasksMap =
                            new LinkedHashMap<>();

                    for (DocumentSnapshot doc :
                            snapshots.getDocuments()) {

                        try {
                            DiscussionTask task =
                                    parseTaskDocument(doc);

                            if (task == null) {
                                continue;
                            }

                            String groupId = task.getId();

                            if (groupId == null
                                    || groupId.trim().isEmpty()) {
                                groupId = doc.getId();
                            }

                            /*
                             * Multiple documents may represent
                             * one task assigned to several faculty.
                             * Keep one reminder per group task.
                             */
                            if (!uniqueTasksMap.containsKey(groupId)) {
                                uniqueTasksMap.put(groupId, task);
                            }

                        } catch (Exception e) {
                            Log.e(TAG,
                                    "Failed parsing task: "
                                            + doc.getId(),
                                    e);
                        }
                    }

                    allReminders.clear();

                    int overdueCount = 0;
                    int tomorrowCount = 0;
                    int upcomingCount = 0;

                    for (DiscussionTask task :
                            uniqueTasksMap.values()) {

                        boolean assignedToUser =
                                task.isAssignedToUser(
                                        currentUid,
                                        currentName,
                                        DEPARTMENT_FACULTY
                                );

                        if (!isHod && !assignedToUser) {
                            continue;
                        }

                        TaskReminder reminder =
                                TaskReminder.fromTask(task);

                        if (reminder.getCategory()
                                == TaskReminder.UrgencyCategory.COMPLETED) {
                            continue;
                        }

                        allReminders.add(reminder);

                        switch (reminder.getCategory()) {
                            case OVERDUE:
                                overdueCount++;
                                break;

                            case DUE_TODAY:
                            case DUE_TOMORROW:
                                tomorrowCount++;
                                break;

                            case UPCOMING:
                                upcomingCount++;
                                break;

                            case COMPLETED:
                            default:
                                break;
                        }
                    }

                    chipAll.setText(
                            "All (" + allReminders.size() + ")"
                    );

                    chipOverdue.setText(
                            "⚠️ Overdue (" + overdueCount + ")"
                    );

                    chipTomorrow.setText(
                            "⏳ Due Tomorrow (" + tomorrowCount + ")"
                    );

                    chipUpcoming.setText(
                            "📅 Upcoming (" + upcomingCount + ")"
                    );

                    applyFilter();
                });
    }

    private DiscussionTask parseTaskDocument(
            DocumentSnapshot doc) {

        String taskId = firstNonEmpty(
                doc.getString("groupTaskId"),
                doc.getString("taskId"),
                doc.getString("id"),
                doc.getId()
        );

        String title = firstNonEmpty(
                doc.getString("taskTitle"),
                doc.getString("title")
        );

        if (title.isEmpty()) {
            Log.w(TAG,
                    "Skipping task without title: "
                            + doc.getId());
            return null;
        }

        String description =
                safeString(doc.getString("description"));

        String deadline =
                safeString(doc.getString("deadline"));

        String priority = firstNonEmpty(
                doc.getString("priority"),
                "Medium"
        );

        String status = firstNonEmpty(
                doc.getString("status"),
                "Pending"
        );

        String assignedTo =
                safeString(doc.getString("assignedTo"));

        String faculty =
                safeString(doc.getString("faculty"));

        List<String> assignedFaculty =
                new ArrayList<>();

        /*
         * HOD field: allAssignedFaculty
         */
        addStringList(
                assignedFaculty,
                doc.get("allAssignedFaculty")
        );

        /*
         * Alternative fields used by other versions.
         */
        addStringList(
                assignedFaculty,
                doc.get("assignedFaculty")
        );

        /*
         * HOD per-faculty field.
         */
        addIfMissing(assignedFaculty, assignedTo);
        addIfMissing(assignedFaculty, faculty);

        List<String> assignedUids =
                new ArrayList<>();

        addStringList(
                assignedUids,
                doc.get("assignedFacultyUids")
        );

        /*
         * Some task documents may contain a single UID.
         */
        addIfMissing(
                assignedUids,
                safeString(doc.getString("assignedToUid"))
        );

        long timestamp = 0L;

        Long timestampValue = doc.getLong("timestamp");

        if (timestampValue != null) {
            timestamp = timestampValue;
        }

        return new DiscussionTask(
                taskId,
                title,
                description,
                deadline,
                priority,
                status,
                assignedFaculty,
                assignedUids,
                assignedFaculty.size(),
                firstNonEmpty(faculty, assignedTo),
                timestamp
        );
    }

    private void addStringList(
            List<String> destination,
            Object value) {

        if (!(value instanceof List<?>)) {
            return;
        }

        for (Object item : (List<?>) value) {
            if (item != null) {
                addIfMissing(
                        destination,
                        item.toString().trim()
                );
            }
        }
    }

    private void addIfMissing(
            List<String> list,
            String value) {

        if (value == null || value.trim().isEmpty()) {
            return;
        }

        String cleanValue = value.trim();

        if (!list.contains(cleanValue)) {
            list.add(cleanValue);
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null
                    && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return "";
    }

    private void applyFilter() {
        List<TaskReminder> filtered =
                new ArrayList<>();

        for (TaskReminder reminder : allReminders) {
            switch (currentFilter) {
                case OVERDUE:
                    if (reminder.getCategory()
                            == TaskReminder.UrgencyCategory.OVERDUE) {
                        filtered.add(reminder);
                    }
                    break;

                case TOMORROW:
                    if (reminder.getCategory()
                            == TaskReminder.UrgencyCategory.DUE_TOMORROW
                            || reminder.getCategory()
                            == TaskReminder.UrgencyCategory.DUE_TODAY) {

                        filtered.add(reminder);
                    }
                    break;

                case UPCOMING:
                    if (reminder.getCategory()
                            == TaskReminder.UrgencyCategory.UPCOMING) {
                        filtered.add(reminder);
                    }
                    break;

                case ALL:
                default:
                    filtered.add(reminder);
                    break;
            }
        }

        Collections.sort(filtered, (r1, r2) -> {
            int rank1 = getCategoryRank(r1.getCategory());
            int rank2 = getCategoryRank(r2.getCategory());

            if (rank1 != rank2) {
                return Integer.compare(rank1, rank2);
            }

            return Long.compare(
                    r1.getDeadlineTimestamp(),
                    r2.getDeadlineTimestamp()
            );
        });

        displayedReminders.clear();
        displayedReminders.addAll(filtered);

        adapter.updateReminders(displayedReminders);

        if (displayedReminders.isEmpty()) {
            layoutEmptyReminders.setVisibility(View.VISIBLE);
            rvReminders.setVisibility(View.GONE);

            switch (currentFilter) {
                case OVERDUE:
                    tvEmptyTitle.setText(
                            "No Overdue Tasks! 🎉"
                    );
                    tvEmptySubtitle.setText(
                            "Great job! None of your assigned tasks are past deadline."
                    );
                    break;

                case TOMORROW:
                    tvEmptyTitle.setText(
                            "No Tasks Due Tomorrow"
                    );
                    tvEmptySubtitle.setText(
                            "You have no deadlines approaching within the next 24-48 hours."
                    );
                    break;

                case UPCOMING:
                    tvEmptyTitle.setText(
                            "No Upcoming Deadlines"
                    );
                    tvEmptySubtitle.setText(
                            "No future task deadlines found."
                    );
                    break;

                case ALL:
                default:
                    tvEmptyTitle.setText(
                            "No Active Reminders"
                    );
                    tvEmptySubtitle.setText(
                            "Tasks assigned to you will appear here with live deadline countdowns."
                    );
                    break;
            }

        } else {
            layoutEmptyReminders.setVisibility(View.GONE);
            rvReminders.setVisibility(View.VISIBLE);
        }
    }

    private int getCategoryRank(
            TaskReminder.UrgencyCategory category) {

        switch (category) {
            case OVERDUE:
                return 1;

            case DUE_TODAY:
                return 2;

            case DUE_TOMORROW:
                return 3;

            case UPCOMING:
                return 4;

            case COMPLETED:
            default:
                return 5;
        }
    }

    @Override
    public void onDiscussClick(TaskReminder reminder) {
        Intent intent = new Intent(
                RemindersActivity.this,
                TaskDiscussionChatActivity.class
        );

        intent.putExtra(
                "EXTRA_TASK_ID",
                reminder.getTaskId()
        );

        intent.putExtra(
                "EXTRA_TASK_TITLE",
                reminder.getTitle()
        );

        intent.putExtra(
                "EXTRA_TASK_DEADLINE",
                reminder.getDeadline()
        );

        intent.putExtra(
                "EXTRA_TASK_PRIORITY",
                reminder.getPriority()
        );

        intent.putExtra(
                "taskName",
                reminder.getTitle()
        );

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
    }
}