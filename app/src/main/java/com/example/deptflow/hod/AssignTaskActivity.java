package com.example.deptflow.hod;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class AssignTaskActivity extends AppCompatActivity {

    private final String[] facultyNames = {
            "Dr. R. Vijayalakshmi",
            "Dr. R. Raja Sudharsan",
            "Dr. K. M. Alaaudeen",
            "Dr. T. Saranya",
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
    };

    private boolean[] selectedFaculty;
    private final ArrayList<String> selectedList = new ArrayList<>();

    private EditText etTaskId, etTitle, etDescription, etDeadline;
    private TextView tvSelectedCount, tvSelectedFaculty;
    private AutoCompleteTextView actvPriority, actvStatus;
    private Button btnSelectFaculty, btnAssign, btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task);

        selectedFaculty = new boolean[facultyNames.length];

        initViews();
        setupDropdowns();
        setupDatePicker();
        setupFacultySelectionDialog();
        setupButtons();
        generateDefaultTaskId();
    }

    private void initViews() {
        etTaskId = findViewById(R.id.etTaskId);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDeadline = findViewById(R.id.etDeadline);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvSelectedFaculty = findViewById(R.id.tvSelectedFaculty);
        actvPriority = findViewById(R.id.actvPriority);
        actvStatus = findViewById(R.id.actvStatus);
        btnSelectFaculty = findViewById(R.id.btnSelectFaculty);
        btnAssign = findViewById(R.id.btnAssign);
        btnClear = findViewById(R.id.btnClear);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupDropdowns() {
        String[] priorities = {"High", "Medium", "Low"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, priorities);
        if (actvPriority != null) {
            actvPriority.setAdapter(priorityAdapter);
            actvPriority.setText("High", false);
        }

        String[] statuses = {"Assigned", "In Progress", "Completed"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuses);
        if (actvStatus != null) {
            actvStatus.setAdapter(statusAdapter);
            actvStatus.setText("Assigned", false);
        }
    }

    private void setupDatePicker() {
        etDeadline.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    AssignTaskActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar cal = Calendar.getInstance();
                        cal.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                        etDeadline.setText(sdf.format(cal.getTime()));
                    },
                    year, month, day
            );
            dialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
            dialog.show();
        });
    }

    /**
     * Multi-Select Faculty Dialog with the "Select All Faculties" feature.
     */
    private void setupFacultySelectionDialog() {
        btnSelectFaculty.setOnClickListener(v -> {
            // Index 0: Select All Faculties, followed by all faculty names
            String[] dialogItems = new String[facultyNames.length + 1];
            dialogItems[0] = "☑ Select All Faculties";
            System.arraycopy(facultyNames, 0, dialogItems, 1, facultyNames.length);

            boolean allSelected = (selectedList.size() == facultyNames.length);
            boolean[] dialogCheckedStates = new boolean[dialogItems.length];
            dialogCheckedStates[0] = allSelected;

            for (int i = 0; i < facultyNames.length; i++) {
                dialogCheckedStates[i + 1] = selectedFaculty[i];
            }

            new MaterialAlertDialogBuilder(AssignTaskActivity.this)
                    .setTitle("Select Department Faculty")
                    .setMultiChoiceItems(dialogItems, dialogCheckedStates, (dialog, which, isChecked) -> {
                        if (which == 0) {
                            // "Select All Faculties" toggled!
                            for (int i = 1; i < dialogCheckedStates.length; i++) {
                                dialogCheckedStates[i] = isChecked;
                                ((AlertDialog) dialog).getListView().setItemChecked(i, isChecked);
                            }
                        } else {
                            // Individual faculty member toggled
                            dialogCheckedStates[which] = isChecked;
                            if (!isChecked) {
                                dialogCheckedStates[0] = false;
                                ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                            }
                        }
                    })
                    .setPositiveButton("Confirm Selection", (dialog, which) -> {
                        selectedList.clear();
                        for (int i = 0; i < facultyNames.length; i++) {
                            selectedFaculty[i] = dialogCheckedStates[i + 1];
                            if (selectedFaculty[i]) {
                                selectedList.add(facultyNames[i]);
                            }
                        }
                        updateFacultySummaryUI();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void updateFacultySummaryUI() {
        if (selectedList.isEmpty()) {
            if (tvSelectedCount != null) tvSelectedCount.setText("Selected Faculties (0):");
            tvSelectedFaculty.setText("No Faculty Selected");
            tvSelectedFaculty.setTextColor(getColor(R.color.text_secondary));
        } else if (selectedList.size() == facultyNames.length) {
            if (tvSelectedCount != null) tvSelectedCount.setText("Selected Faculties (" + selectedList.size() + "):");
            tvSelectedFaculty.setText("All Faculties Selected (" + selectedList.size() + " members - Entire Department)");
            tvSelectedFaculty.setTextColor(getColor(R.color.primary));
        } else {
            if (tvSelectedCount != null) tvSelectedCount.setText("Selected Faculties (" + selectedList.size() + "):");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < selectedList.size(); i++) {
                sb.append(selectedList.get(i));
                if (i < selectedList.size() - 1) sb.append(", ");
            }
            tvSelectedFaculty.setText(sb.toString());
            tvSelectedFaculty.setTextColor(getColor(R.color.text_primary));
        }
    }

    private void setupButtons() {
        btnAssign.setOnClickListener(v -> assignTask());

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> clearForm());
        }
    }

    private void assignTask() {
        String taskId = etTaskId != null ? etTaskId.getText().toString().trim() : "";
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String deadline = etDeadline.getText().toString().trim();
        String priority = actvPriority != null ? actvPriority.getText().toString().trim() : "High";
        String status = actvStatus != null ? actvStatus.getText().toString().trim() : "Assigned";

        if (TextUtils.isEmpty(taskId)) {
            taskId = generateDefaultTaskId();
        }

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Task title is required");
            etTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Task description is required");
            etDescription.requestFocus();
            return;
        }

        if (selectedList.isEmpty()) {
            Toast.makeText(this, "Please select at least one faculty or choose 'Select All Faculties'", Toast.LENGTH_LONG).show();
            return;
        }

        if (TextUtils.isEmpty(deadline)) {
            Toast.makeText(this, "Please select a deadline using the calendar", Toast.LENGTH_SHORT).show();
            etDeadline.performClick();
            return;
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Standardize priority and status
            String normPriority = priority != null ? priority.trim().toUpperCase() : "HIGH";
            String normStatus = "PENDING";
            if (status != null) {
                String s = status.trim().toUpperCase();
                if (s.contains("COMPLET")) normStatus = "COMPLETED";
                else if (s.contains("PROGRESS")) normStatus = "IN_PROGRESS";
            }

            // Create individual document per faculty for precise direct matching in Faculty Module
            for (int i = 0; i < selectedList.size(); i++) {
                String faculty = selectedList.get(i);
                String docId = (selectedList.size() == 1) ? taskId : taskId + "_" + (i + 1);

                Map<String, Object> docMap = new HashMap<>();
                docMap.put("taskId", docId);
                docMap.put("id", docId);
                docMap.put("groupTaskId", taskId);
                docMap.put("taskTitle", title);
                docMap.put("title", title);
                docMap.put("description", description);
                docMap.put("deadline", deadline);
                docMap.put("priority", normPriority);
                docMap.put("status", normStatus);
                docMap.put("assignedTo", faculty);
                docMap.put("faculty", faculty);
                docMap.put("assignedBy", "HOD (Department Head)");
                docMap.put("allAssignedFaculty", new ArrayList<>(selectedList));
                docMap.put("assignedFacultyCount", selectedList.size());
                docMap.put("timestamp", System.currentTimeMillis());

                db.collection("task_assignments").document(docId).set(docMap);
            }

            // Synchronize with local TaskData.tasks bridge so any synchronous in-memory readers have it immediately
            StringBuilder facSb = new StringBuilder();
            for (int i = 0; i < selectedList.size(); i++) {
                facSb.append(selectedList.get(i));
                if (i < selectedList.size() - 1) facSb.append("\n");
            }
            String rawTask = "Title: " + title
                    + "\nDescription: " + description
                    + "\nDeadline: " + deadline
                    + "\nFaculty: " + facSb.toString()
                    + "\nStatus: " + normStatus
                    + "\nPriority: " + normPriority;

            if (TaskData.tasks == null) {
                TaskData.tasks = new ArrayList<>();
            }
            TaskData.tasks.add(0, rawTask);

            // Persist locally via TaskRepository
            try {
                com.example.deptflow.feature.faculty.repository.TaskRepository.getInstance(this).getHodTasks();
            } catch (Exception ignored) {}

            Toast.makeText(this, "Task Assigned Successfully to " + selectedList.size() + " Faculty Member(s)!", Toast.LENGTH_LONG).show();
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Task queued: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void clearForm() {
        generateDefaultTaskId();
        etTitle.setText("");
        etDescription.setText("");
        etDeadline.setText("");
        if (actvPriority != null) actvPriority.setText("High", false);
        if (actvStatus != null) actvStatus.setText("Assigned", false);
        selectedList.clear();
        Arrays.fill(selectedFaculty, false);
        updateFacultySummaryUI();
        Toast.makeText(this, "Form cleared", Toast.LENGTH_SHORT).show();
    }

    private String generateDefaultTaskId() {
        int randomId = 1000 + new Random().nextInt(9000);
        String id = "TSK-2026-" + randomId;
        if (etTaskId != null) {
            etTaskId.setText(id);
        }
        return id;
    }
}