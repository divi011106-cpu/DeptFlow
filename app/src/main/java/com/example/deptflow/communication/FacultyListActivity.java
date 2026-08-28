package com.example.deptflow.communication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.adapters.FacultyAdapter;
import com.example.deptflow.models.Faculty;

import java.util.ArrayList;
import java.util.List;

public class FacultyListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFaculty;
    private FacultyAdapter facultyAdapter;
    private List<Faculty> facultyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_list);

        recyclerViewFaculty = findViewById(R.id.recyclerViewFaculty);

        recyclerViewFaculty.setLayoutManager(
                new LinearLayoutManager(this)
        );

        facultyList = new ArrayList<>();

        // Sample Faculty Data
        facultyList.add(new Faculty(
                "F001",
                "Divya",
                "divya@gmail.com",
                "Computer Science",
                "Faculty"
        ));

        facultyList.add(new Faculty(
                "F002",
                "Priya",
                "priya@gmail.com",
                "Information Technology",
                "Faculty"
        ));

        facultyList.add(new Faculty(
                "F003",
                "Kumar",
                "kumar@gmail.com",
                "Computer Science",
                "Faculty"
        ));

        facultyList.add(new Faculty(
                "F004",
                "Anitha",
                "anitha@gmail.com",
                "Artificial Intelligence",
                "Faculty"
        ));

        facultyAdapter = new FacultyAdapter(
                this,
                facultyList
        );

        recyclerViewFaculty.setAdapter(facultyAdapter);
    }
}