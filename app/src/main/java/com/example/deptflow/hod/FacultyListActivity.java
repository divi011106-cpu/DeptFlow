package com.example.deptflow.hod;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

public class FacultyListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_list2);

        ListView listFaculty = findViewById(R.id.listFaculty);

        String[] faculty = {
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                faculty
        );

        listFaculty.setAdapter(adapter);
    }
}