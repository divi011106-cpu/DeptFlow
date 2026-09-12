package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class FacultyListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_faculty_list);


        Button btnFaculty1 =
                findViewById(R.id.btnFaculty1);

        Button btnFaculty2 =
                findViewById(R.id.btnFaculty2);

        Button btnFaculty3 =
                findViewById(R.id.btnFaculty3);


        btnFaculty1.setOnClickListener(v ->
                openChat("Priya")
        );


        btnFaculty2.setOnClickListener(v ->
                openChat("Kumar")
        );


        btnFaculty3.setOnClickListener(v ->
                openChat("Anitha")
        );

    }


    private void openChat(String facultyName) {

        Intent intent =
                new Intent(
                        FacultyListActivity.this,
                        ChatActivity.class
                );

        intent.putExtra(
                "facultyName",
                facultyName
        );

        startActivity(intent);

    }

}