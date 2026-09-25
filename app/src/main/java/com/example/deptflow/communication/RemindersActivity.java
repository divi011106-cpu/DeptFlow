package com.example.deptflow.communication;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.ReminderAdapter;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.communication.models.FacultyReminder;
import com.example.deptflow.communication.reminders.ReminderScheduler;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * RemindersActivity — Full management of personal & task-assigned Faculty Reminders:
 * Creation, Editing, Deletion, Completion toggling, Real-time Firestore sync,
 * and Android AlarmManager system notification scheduling.
 */
public class RemindersActivity extends AppCompatActivity
        implements ReminderAdapter.OnReminderActionListener {

    private static final String TAG = "RemindersActivity";

    private enum FilterMode {
        ALL, UPCOMING, COMPLETED
    }

    private RecyclerView rvReminders;
    private ProgressBar pbLoadingReminders;
    private View layoutEmptyReminders;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private ImageButton ibBackReminders;
    private ExtendedFloatingActionButton fabAddReminder;

    private TextView chipAll;
    private TextView chipUpcoming;
    private TextView chipCompleted;

    private ReminderAdapter adapter;

    private final List<FacultyReminder> allReminders = new ArrayList<>();
    private final List<FacultyReminder> displayedReminders = new ArrayList<>();

    private FilterMode currentFilter = FilterMode.ALL;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration remindersListener;

    private String currentUid = "";
    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentName = "Faculty";
    private final Set<String> myIdentifiers = new HashSet<>();

    // Permission launcher for Android 13+ Notification permission
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted.");
                } else {
                    Toast.makeText(this,
                            "Notification permission denied. Reminders will be saved, but audio alerts won't pop up.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        initViews();
        loadCurrentUserInfo();
        setupRecyclerView();
        setupFilterChips();
        checkNotificationPermissions();

        if (currentUid.isEmpty() && currentUserId.isEmpty() && currentCanonicalId.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listenToFacultyReminders();
    }

    private void initViews() {
        rvReminders = findViewById(R.id.rvReminders);
        pbLoadingReminders = findViewById(R.id.pbLoadingReminders);
        layoutEmptyReminders = findViewById(R.id.layoutEmptyReminders);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        ibBackReminders = findViewById(R.id.ibBackReminders);
        fabAddReminder = findViewById(R.id.fabAddReminder);

        chipAll = findViewById(R.id.chipAll);
        chipUpcoming = findViewById(R.id.chipUpcoming);
        chipCompleted = findViewById(R.id.chipCompleted);

        if (ibBackReminders != null) {
            ibBackReminders.setOnClickListener(v -> finish());
        }

        if (fabAddReminder != null) {
            fabAddReminder.setOnClickListener(v -> showAddEditReminderDialog(null));
        }
    }

    private void loadCurrentUserInfo() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        myIdentifiers.clear();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            addIdentifier(currentUid);
            if (firebaseUser.getEmail() != null) {
                addIdentifier(firebaseUser.getEmail());
            }
            if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentName = firebaseUser.getDisplayName().trim();
                addIdentifier(currentName);
            }
        }

        FacultyUser cachedUser = AuthManager.getInstance(this).getCurrentUser();
        if (cachedUser != null) {
            if (cachedUser.getUserId() != null && !cachedUser.getUserId().trim().isEmpty()) {
                currentUserId = cachedUser.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if (cachedUser.getName() != null && !cachedUser.getName().trim().isEmpty()) {
                currentName = cachedUser.getName().trim();
                addIdentifier(currentName);
            }
            if (cachedUser.getEmail() != null && !cachedUser.getEmail().trim().isEmpty()) {
                addIdentifier(cachedUser.getEmail());
            }

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(cachedUser);
            if (canonical != null) {
                currentCanonicalId = canonical.getUserId();
                addIdentifier(currentCanonicalId);
                addIdentifier(canonical.getName());
                addIdentifier(canonical.getEmail());
            }
        }

        if (currentCanonicalId.isEmpty() && !currentName.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentName);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }

        if (currentCanonicalId.isEmpty() && !currentUserId.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserId);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }

        if (currentCanonicalId.isEmpty() && !currentUid.isEmpty()) {
            currentCanonicalId = currentUid;
            addIdentifier(currentCanonicalId);
        }

        Log.d(TAG, "Current faculty info: UID=" + currentUid
                + ", CanonicalId=" + currentCanonicalId
                + ", Name=" + currentName
                + ", Identifiers=" + myIdentifiers);
    }

    private void addIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return;
        String trimmed = val.trim();
        myIdentifiers.add(trimmed.toLowerCase(Locale.ROOT));
        myIdentifiers.add(trimmed);
        String clean = trimmed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (!clean.isEmpty()) {
            myIdentifiers.add(clean);
        }
    }

    private void setupRecyclerView() {
        adapter = new ReminderAdapter(this, this);
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        rvReminders.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> setFilter(FilterMode.ALL));
        chipUpcoming.setOnClickListener(v -> setFilter(FilterMode.UPCOMING));
        chipCompleted.setOnClickListener(v -> setFilter(FilterMode.COMPLETED));

        updateFilterChipStyles();
    }

    private void setFilter(FilterMode mode) {
        currentFilter = mode;
        updateFilterChipStyles();
        applyFilter();
    }

    private void updateFilterChipStyles() {
        resetChip(chipAll);
        resetChip(chipUpcoming);
        resetChip(chipCompleted);

        TextView activeChip;
        switch (currentFilter) {
            case UPCOMING:
                activeChip = chipUpcoming;
                break;
            case COMPLETED:
                activeChip = chipCompleted;
                break;
            case ALL:
            default:
                activeChip = chipAll;
                break;
        }

        activeChip.setBackgroundResource(R.drawable.bg_chip_selected);
        activeChip.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
    }

    private void resetChip(TextView chip) {
        if (chip != null) {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void listenToFacultyReminders() {
        pbLoadingReminders.setVisibility(View.VISIBLE);

        remindersListener = db.collection("faculty_reminders")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    pbLoadingReminders.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e(TAG, "Error listening to faculty_reminders: " + error.getMessage(), error);
                        if (error.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            if (layoutEmptyReminders != null && tvEmptyTitle != null && tvEmptySubtitle != null) {
                                layoutEmptyReminders.setVisibility(View.VISIBLE);
                                rvReminders.setVisibility(View.GONE);
                                tvEmptyTitle.setText("Permission Denied");
                                tvEmptySubtitle.setText("Missing or insufficient Firestore permissions for 'faculty_reminders'. Please check your Firebase Auth session or Firestore security rules.");
                            }
                            Toast.makeText(RemindersActivity.this, "PERMISSION_DENIED: Check Firestore rules or login session", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RemindersActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (snapshots == null) return;

                    allReminders.clear();
                    int upcomingCount = 0;
                    int completedCount = 0;

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String docFacultyId = doc.getString("facultyId");
                        String docFacultyUid = doc.getString("facultyUid");
                        String docFacultyName = doc.getString("facultyName");

                        boolean belongsToMe = isBelongsToMe(docFacultyId, docFacultyUid, docFacultyName);
                        if (!belongsToMe) continue;

                        try {
                            String reminderId = doc.getId();
                            String title = doc.getString("title");
                            String description = doc.getString("description");

                            Long scheduledTime = doc.getLong("scheduledDateTime");
                            String scheduledDateStr = doc.getString("scheduledDateStr");
                            String scheduledTimeStr = doc.getString("scheduledTimeStr");
                            Long createdAt = doc.getLong("createdAt");
                            Boolean isCompleted = doc.getBoolean("isCompleted");
                            String status = doc.getString("status");

                            FacultyReminder reminder = new FacultyReminder(
                                    reminderId,
                                    docFacultyId != null ? docFacultyId : currentCanonicalId,
                                    docFacultyUid != null ? docFacultyUid : currentUid,
                                    docFacultyName != null ? docFacultyName : currentName,
                                    title != null ? title : "Reminder",
                                    description != null ? description : "",
                                    scheduledTime != null ? scheduledTime : 0L,
                                    scheduledDateStr != null ? scheduledDateStr : "",
                                    scheduledTimeStr != null ? scheduledTimeStr : "",
                                    createdAt != null ? createdAt : System.currentTimeMillis(),
                                    isCompleted != null ? isCompleted : false,
                                    status != null ? status : (Boolean.TRUE.equals(isCompleted) ? "COMPLETED" : "PENDING")
                            );

                            allReminders.add(reminder);

                            if (reminder.isCompleted()) {
                                completedCount++;
                            } else {
                                upcomingCount++;
                                // Auto-schedule alarm if upcoming and future
                                if (reminder.getScheduledDateTime() > System.currentTimeMillis()) {
                                    ReminderScheduler.scheduleReminder(this, reminder);
                                }
                            }

                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing reminder doc " + doc.getId(), e);
                        }
                    }

                    chipAll.setText("All (" + allReminders.size() + ")");
                    chipUpcoming.setText("Upcoming (" + upcomingCount + ")");
                    chipCompleted.setText("Completed (" + completedCount + ")");

                    applyFilter();
                });
    }

    private boolean isBelongsToMe(String facultyId, String facultyUid, String facultyName) {
        if (facultyId != null && (myIdentifiers.contains(facultyId.trim().toLowerCase(Locale.ROOT))
                || facultyId.equalsIgnoreCase(currentCanonicalId)
                || facultyId.equalsIgnoreCase(currentUserId))) {
            return true;
        }
        if (facultyUid != null && !facultyUid.isEmpty() && facultyUid.equals(currentUid)) {
            return true;
        }
        if (facultyName != null) {
            String clean = facultyName.trim().toLowerCase(Locale.ROOT);
            if (myIdentifiers.contains(clean) || clean.equalsIgnoreCase(currentName)) return true;
            FacultyUser resolved = FacultyDirectory.resolveByNameOrId(facultyName);
            if (resolved != null && myIdentifiers.contains(resolved.getUserId().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void applyFilter() {
        displayedReminders.clear();

        for (FacultyReminder r : allReminders) {
            switch (currentFilter) {
                case UPCOMING:
                    if (!r.isCompleted()) displayedReminders.add(r);
                    break;
                case COMPLETED:
                    if (r.isCompleted()) displayedReminders.add(r);
                    break;
                case ALL:
                default:
                    displayedReminders.add(r);
                    break;
            }
        }

        // Sort: upcoming by scheduled date ascending, completed by created date descending
        Collections.sort(displayedReminders, (a, b) -> {
            if (!a.isCompleted() && !b.isCompleted()) {
                return Long.compare(a.getScheduledDateTime(), b.getScheduledDateTime());
            }
            if (a.isCompleted() && b.isCompleted()) {
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            }
            return a.isCompleted() ? 1 : -1;
        });

        adapter.updateReminders(displayedReminders);

        if (displayedReminders.isEmpty()) {
            layoutEmptyReminders.setVisibility(View.VISIBLE);
            rvReminders.setVisibility(View.GONE);

            switch (currentFilter) {
                case UPCOMING:
                    tvEmptyTitle.setText("No Upcoming Reminders");
                    tvEmptySubtitle.setText("You're all caught up! Tap '+' to set a new deadline reminder.");
                    break;
                case COMPLETED:
                    tvEmptyTitle.setText("No Completed Reminders");
                    tvEmptySubtitle.setText("Check off upcoming reminders as tasks are finished.");
                    break;
                case ALL:
                default:
                    tvEmptyTitle.setText("No Reminders Found");
                    tvEmptySubtitle.setText("Never miss an HOD task deadline. Tap '+' to create your first reminder.");
                    break;
            }
        } else {
            layoutEmptyReminders.setVisibility(View.GONE);
            rvReminders.setVisibility(View.VISIBLE);
        }
    }

    private void showAddEditReminderDialog(FacultyReminder existingReminder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_reminder, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputLayout tilTitle = dialogView.findViewById(R.id.tilReminderTitle);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etReminderTitle);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etReminderDesc);
        MaterialButton btnPickDate = dialogView.findViewById(R.id.btnPickDate);
        MaterialButton btnPickTime = dialogView.findViewById(R.id.btnPickTime);
        LinearLayout layoutSchedulePreview = dialogView.findViewById(R.id.layoutSchedulePreview);
        TextView tvSchedulePreview = dialogView.findViewById(R.id.tvSchedulePreview);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelReminder);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveReminder);

        final Calendar selectedCalendar = Calendar.getInstance();

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (existingReminder != null) {
            tvDialogTitle.setText("Edit Reminder");
            etTitle.setText(existingReminder.getTitle());
            etDesc.setText(existingReminder.getDescription());
            if (existingReminder.getScheduledDateTime() > 0) {
                selectedCalendar.setTimeInMillis(existingReminder.getScheduledDateTime());
            }
        } else {
            tvDialogTitle.setText("New Faculty Reminder");
            selectedCalendar.add(Calendar.HOUR_OF_DAY, 2);
        }

        btnPickDate.setText(dateFmt.format(selectedCalendar.getTime()));
        btnPickTime.setText(timeFmt.format(selectedCalendar.getTime()));

        if (layoutSchedulePreview != null && tvSchedulePreview != null) {
            layoutSchedulePreview.setVisibility(View.VISIBLE);
            tvSchedulePreview.setText("Scheduled for: " + dateFmt.format(selectedCalendar.getTime()) + " at " + timeFmt.format(selectedCalendar.getTime()));
        }

        btnPickDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedCalendar.set(Calendar.YEAR, year);
                        selectedCalendar.set(Calendar.MONTH, month);
                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        btnPickDate.setText(dateFmt.format(selectedCalendar.getTime()));
                        if (tvSchedulePreview != null) {
                            tvSchedulePreview.setText("Scheduled for: " + dateFmt.format(selectedCalendar.getTime()) + " at " + timeFmt.format(selectedCalendar.getTime()));
                        }
                    },
                    selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)
            );
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dpd.show();
        });

        btnPickTime.setOnClickListener(v -> {
            TimePickerDialog tpd = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedCalendar.set(Calendar.MINUTE, minute);
                        selectedCalendar.set(Calendar.SECOND, 0);
                        btnPickTime.setText(timeFmt.format(selectedCalendar.getTime()));
                        if (tvSchedulePreview != null) {
                            tvSchedulePreview.setText("Scheduled for: " + dateFmt.format(selectedCalendar.getTime()) + " at " + timeFmt.format(selectedCalendar.getTime()));
                        }
                    },
                    selectedCalendar.get(Calendar.HOUR_OF_DAY),
                    selectedCalendar.get(Calendar.MINUTE),
                    false
            );
            tpd.show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            String dateStr = dateFmt.format(selectedCalendar.getTime());
            String timeStr = timeFmt.format(selectedCalendar.getTime());

            if (title.isEmpty()) {
                tilTitle.setError("Title is required");
                return;
            }
            tilTitle.setError(null);

            long scheduledMillis = selectedCalendar.getTimeInMillis();
            String reminderId = (existingReminder != null)
                    ? existingReminder.getReminderId()
                    : "rem_" + System.currentTimeMillis();

            Map<String, Object> reminderMap = new HashMap<>();
            reminderMap.put("reminderId", reminderId);
            reminderMap.put("facultyId", !currentCanonicalId.isEmpty() ? currentCanonicalId : currentUserId);
            reminderMap.put("facultyUid", currentUid);
            reminderMap.put("facultyName", currentName);
            reminderMap.put("title", title);
            reminderMap.put("description", desc);
            reminderMap.put("scheduledDateTime", scheduledMillis);
            reminderMap.put("scheduledDateStr", dateStr);
            reminderMap.put("scheduledTimeStr", timeStr);
            reminderMap.put("createdAt", (existingReminder != null) ? existingReminder.getCreatedAt() : System.currentTimeMillis());
            reminderMap.put("isCompleted", (existingReminder != null) && existingReminder.isCompleted());
            reminderMap.put("status", (existingReminder != null && existingReminder.isCompleted()) ? "COMPLETED" : "PENDING");

            db.collection("faculty_reminders").document(reminderId)
                    .set(reminderMap, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(RemindersActivity.this, "Reminder saved successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(RemindersActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    @Override
    public void onToggleCompleted(FacultyReminder reminder, boolean isCompleted) {
        if (reminder == null) return;

        Map<String, Object> update = new HashMap<>();
        update.put("isCompleted", isCompleted);
        update.put("status", isCompleted ? "COMPLETED" : "PENDING");

        db.collection("faculty_reminders").document(reminder.getReminderId())
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    if (isCompleted) {
                        ReminderScheduler.cancelReminder(this, reminder.getReminderId());
                        // Play a short pleasant confirmation tone
                        try {
                            android.media.ToneGenerator toneGen = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80);
                            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 200);
                        } catch (Exception ignored) {}
                    } else if (reminder.getScheduledDateTime() > System.currentTimeMillis()) {
                        ReminderScheduler.scheduleReminder(this, reminder);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Could not update status", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onEditReminder(FacultyReminder reminder) {
        showAddEditReminderDialog(reminder);
    }

    @Override
    public void onDeleteReminder(FacultyReminder reminder) {
        if (reminder == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete '" + reminder.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    ReminderScheduler.cancelReminder(this, reminder.getReminderId());
                    db.collection("faculty_reminders").document(reminder.getReminderId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remindersListener != null) {
            remindersListener.remove();
        }
    }
}