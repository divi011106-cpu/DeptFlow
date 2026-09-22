package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FacultyListActivity extends AppCompatActivity {

    private ListView listFaculty;
    private TextView tvStatus;

    private FirebaseFirestore db;

    private ArrayList<String> facultyNames;
    private ArrayList<String> facultyIds;

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_list);

        listFaculty = findViewById(R.id.listFaculty);
        tvStatus = findViewById(R.id.tvStatus);

        db = FirebaseFirestore.getInstance();

        facultyNames = new ArrayList<>();
        facultyIds = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                facultyNames
        );

        listFaculty.setAdapter(adapter);

        // Load faculty members from Firestore
        loadFaculty();

        // Open chat when a faculty member is selected
        listFaculty.setOnItemClickListener((parent, view, position, id) -> {

            String name = facultyNames.get(position);
            String facultyId = facultyIds.get(position);

            if (facultyId == null || facultyId.trim().isEmpty()) {
                Toast.makeText(
                        FacultyListActivity.this,
                        "Faculty ID is missing",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(
                    FacultyListActivity.this,
                    ChatActivity.class
            );

            intent.putExtra("facultyName", name);
            intent.putExtra("facultyId", facultyId);

            startActivity(intent);
        });
    }

    private void loadFaculty() {

        tvStatus.setText("Loading faculty...");

        db.collection("users")
                .whereEqualTo("role", "FACULTY")
                .get()
                .addOnSuccessListener(snapshot -> {

                    facultyNames.clear();
                    facultyIds.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        String name = document.getString("name");

                        // First try userId then userID
                        String facultyId = document.getString("userId");
                        if (facultyId == null || facultyId.trim().isEmpty()) {
                            facultyId = document.getString("userID");
                        }

                        // If still empty, use the Firestore document ID
                        if (facultyId == null || facultyId.trim().isEmpty()) {
                            facultyId = document.getId();
                        }

                        if (name != null && !name.trim().isEmpty()) {
                            facultyNames.add(name);
                            facultyIds.add(facultyId);
                        }
                    }

                    // Sort names and IDs together
                    ArrayList<Faculty> facultyList = new ArrayList<>();

                    for (int i = 0; i < facultyNames.size(); i++) {
                        facultyList.add(
                                new Faculty(
                                        facultyNames.get(i),
                                        facultyIds.get(i)
                                )
                        );
                    }

                    Collections.sort(
                            facultyList,
                            Comparator.comparing(
                                    Faculty::getName,
                                    String.CASE_INSENSITIVE_ORDER
                            )
                    );

                    facultyNames.clear();
                    facultyIds.clear();

                    for (Faculty faculty : facultyList) {
                        facultyNames.add(faculty.getName());
                        facultyIds.add(faculty.getId());
                    }

                    adapter.notifyDataSetChanged();

                    if (facultyNames.isEmpty()) {
                        tvStatus.setText("No faculty found.");
                    } else {
                        tvStatus.setText(
                                "Faculty members: " + facultyNames.size()
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    tvStatus.setText("Failed to load faculty.");

                    Toast.makeText(
                            FacultyListActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Small helper class to keep each name and ID together
    private static class Faculty {

        private final String name;
        private final String id;

        Faculty(String name, String id) {
            this.name = name;
            this.id = id;
        }

        String getName() {
            return name;
        }

        String getId() {
            return id;
        }
    }
}