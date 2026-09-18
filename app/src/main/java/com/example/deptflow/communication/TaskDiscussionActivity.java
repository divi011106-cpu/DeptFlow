package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

import java.util.ArrayList;

public class TaskDiscussionActivity extends AppCompatActivity {

    private ListView listTasks;
    private TextView tvNoTasks;

    private final ArrayList<String> taskNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_discussion);

        listTasks = findViewById(R.id.listTasks);
        tvNoTasks = findViewById(R.id.tvNoTasks);

        loadTasks();
    }

    private void loadTasks() {

        taskNames.clear();

        // Temporary communication-module task list.
        // HOD integration can be added later without changing HOD code.
        taskNames.add("Result Analysis");
        taskNames.add("Attendance Report");
        taskNames.add("Department Event");

        if (taskNames.isEmpty()) {

            listTasks.setVisibility(ListView.GONE);
            tvNoTasks.setVisibility(TextView.VISIBLE);

            return;
        }

        listTasks.setVisibility(ListView.VISIBLE);
        tvNoTasks.setVisibility(TextView.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                taskNames
        );

        listTasks.setAdapter(adapter);

        listTasks.setOnItemClickListener(
                (parent, view, position, id) -> {

                    String selectedTask =
                            taskNames.get(position);

                    Intent intent = new Intent(
                            TaskDiscussionActivity.this,
                            TaskDiscussionChatActivity.class
                    );

                    intent.putExtra(
                            "taskName",
                            selectedTask
                    );

                    startActivity(intent);
                }
        );
    }
}