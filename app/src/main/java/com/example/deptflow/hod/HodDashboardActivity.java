package com.example.deptflow.hod;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deptflow.R;

/**
 * Placeholder for Member 2 (HOD Module).
 */
public class HodDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText(R.string.hod_module_title);
        title.setTextSize(22);
        title.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);

        TextView desc = new TextView(this);
        desc.setText("This module is developed by Member 2 (HOD Module).");
        desc.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        desc.setPadding(0, 16, 0, 32);

        Button back = new Button(this);
        back.setText(R.string.back);
        back.setOnClickListener(v -> finish());

        layout.addView(title);
        layout.addView(desc);
        layout.addView(back);

        setContentView(layout);
    }
}
