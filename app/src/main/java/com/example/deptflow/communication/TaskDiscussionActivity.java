package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class TaskDiscussionActivity extends AppCompatActivity {

    Button btnTask1;
    Button btnTask2;
    Button btnTask3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_discussion);

        btnTask1 = findViewById(R.id.btnTask1);
        btnTask2 = findViewById(R.id.btnTask2);
        btnTask3 = findViewById(R.id.btnTask3);

        btnTask1.setOnClickListener(v ->
                openDiscussion("Result Analysis")
        );

        btnTask2.setOnClickListener(v ->
                openDiscussion("Attendance Report")
        );

        btnTask3.setOnClickListener(v ->
                openDiscussion("Department Event")
        );
    }

    private void openDiscussion(String taskName) {

        Intent intent = new Intent(
                TaskDiscussionActivity.this,
                TaskDiscussionChatActivity.class
        );

        intent.putExtra("taskName", taskName);

        startActivity(intent);
    }
}