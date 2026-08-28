package com.example.deptflow.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.communication.ChatActivity;
import com.example.deptflow.models.Faculty;

import java.util.List;

public class FacultyAdapter
        extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    private List<Faculty> facultyList;
    private Context context;

    public FacultyAdapter(Context context,
                          List<Faculty> facultyList) {

        this.context = context;
        this.facultyList = facultyList;
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faculty, parent, false);

        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FacultyViewHolder holder,
            int position) {

        Faculty faculty = facultyList.get(position);

        holder.tvFacultyName.setText(faculty.getName());
        holder.tvDepartment.setText(faculty.getDepartment());
        holder.tvFacultyEmail.setText(faculty.getEmail());

        if (faculty.getName() != null &&
                !faculty.getName().isEmpty()) {

            holder.tvInitial.setText(
                    String.valueOf(
                            faculty.getName().charAt(0)
                    )
            );
        }

        // Open Chat
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    context,
                    ChatActivity.class
            );

            intent.putExtra(
                    "facultyName",
                    faculty.getName()
            );

            intent.putExtra(
                    "department",
                    faculty.getDepartment()
            );

            intent.putExtra(
                    "facultyId",
                    faculty.getUserId()
            );

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return facultyList.size();
    }

    public static class FacultyViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvInitial;
        TextView tvFacultyName;
        TextView tvDepartment;
        TextView tvFacultyEmail;

        public FacultyViewHolder(
                @NonNull View itemView) {

            super(itemView);

            tvInitial =
                    itemView.findViewById(R.id.tvInitial);

            tvFacultyName =
                    itemView.findViewById(R.id.tvFacultyName);

            tvDepartment =
                    itemView.findViewById(R.id.tvDepartment);

            tvFacultyEmail =
                    itemView.findViewById(R.id.tvFacultyEmail);
        }
    }
}