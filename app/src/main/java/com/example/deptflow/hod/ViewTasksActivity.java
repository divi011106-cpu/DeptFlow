package com.example.deptflow.hod;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class ViewTasksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tasks);

        ListView listTasks = findViewById(R.id.listTasks);
        Button btnBack = findViewById(R.id.btnBack);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        TaskData.tasks
                );

        listTasks.setAdapter(adapter);

        // Show task details in popup
        listTasks.setOnItemClickListener((parent, view, position, id) -> {

            String taskDetails = TaskData.tasks.get(position);

            new AlertDialog.Builder(ViewTasksActivity.this)
                    .setTitle("Task Details")
                    .setMessage(taskDetails)
                    .setPositiveButton("OK", null)
                    .show();
        });

        // Go back to HOD Dashboard
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }
}