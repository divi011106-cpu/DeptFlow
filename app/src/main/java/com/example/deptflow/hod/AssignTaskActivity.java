package com.example.deptflow.hod;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

import java.util.ArrayList;
import java.util.Calendar;

public class AssignTaskActivity extends AppCompatActivity {

    private String[] facultyNames = {
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
    };

    private boolean[] selectedFaculty =
            new boolean[facultyNames.length];

    private ArrayList<String> selectedList =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task);

        EditText etTitle = findViewById(R.id.etTitle);
        EditText etDescription = findViewById(R.id.etDescription);
        EditText etDeadline = findViewById(R.id.etDeadline);

        Button btnSelectFaculty =
                findViewById(R.id.btnSelectFaculty);

        TextView tvSelectedFaculty =
                findViewById(R.id.tvSelectedFaculty);

        Button btnAssign =
                findViewById(R.id.btnAssign);

        // Date Picker
        etDeadline.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog =
                    new DatePickerDialog(
                            AssignTaskActivity.this,
                            (view, year, month, dayOfMonth) ->
                                    etDeadline.setText(
                                            dayOfMonth + "/" +
                                                    (month + 1) + "/" +
                                                    year),
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH));

            dialog.show();
        });

        // Faculty Selection Dialog
        btnSelectFaculty.setOnClickListener(v -> {

            AlertDialog.Builder builder =
                    new AlertDialog.Builder(
                            AssignTaskActivity.this);

            builder.setTitle("Select Faculty");

            builder.setMultiChoiceItems(
                    facultyNames,
                    selectedFaculty,
                    (dialog, which, isChecked) -> {

                        if (isChecked) {
                            selectedList.add(
                                    facultyNames[which]);
                        } else {
                            selectedList.remove(
                                    facultyNames[which]);
                        }
                    });

            builder.setPositiveButton(
                    "OK",
                    (dialog, which) -> {

                        StringBuilder names =
                                new StringBuilder();

                        for (String faculty :
                                selectedList) {

                            names.append(faculty)
                                    .append("\n");
                        }

                        if (selectedList.isEmpty()) {
                            tvSelectedFaculty.setText(
                                    "No Faculty Selected");
                        } else {
                            tvSelectedFaculty.setText(
                                    names.toString());
                        }
                    });

            builder.setNegativeButton(
                    "Cancel", null);

            builder.show();
        });

        // Assign Task
        btnAssign.setOnClickListener(v -> {

            if (selectedList.isEmpty()) {

                Toast.makeText(
                        AssignTaskActivity.this,
                        "Please select at least one faculty",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String task =
                    "Title: " + etTitle.getText().toString() +
                            "\nDescription: " + etDescription.getText().toString() +
                            "\nDeadline: " + etDeadline.getText().toString() +
                            "\nFaculty: " + tvSelectedFaculty.getText().toString();

            TaskData.tasks.add(task);

            Toast.makeText(
                    AssignTaskActivity.this,
                    "Task Assigned Successfully",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });
    }
}