package com.example.deptflow.communication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.FacultyUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacultyAdapter
        extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {

    public interface OnFacultyClickListener {
        void onFacultyClick(FacultyUser faculty);
    }

    private final List<FacultyUser> facultyList = new ArrayList<>();
    private final List<FacultyUser> allFacultyList = new ArrayList<>();
    private final OnFacultyClickListener listener;

    public FacultyAdapter(
            List<FacultyUser> faculty,
            OnFacultyClickListener listener) {

        this.listener = listener;
        updateList(faculty);
    }

    public void updateList(List<FacultyUser> faculty) {
        allFacultyList.clear();
        facultyList.clear();

        if (faculty != null) {
            allFacultyList.addAll(faculty);
            facultyList.addAll(faculty);
        }

        notifyDataSetChanged();
    }

    public void filter(String text) {
        String query = text == null
                ? ""
                : text.toLowerCase(Locale.ROOT).trim();

        facultyList.clear();

        if (query.isEmpty()) {
            facultyList.addAll(allFacultyList);
        } else {
            for (FacultyUser faculty : allFacultyList) {
                String name = faculty.getName() == null
                        ? "" : faculty.getName();

                String role = faculty.getRole() == null
                        ? "" : faculty.getRole();

                String department = faculty.getDepartment() == null
                        ? "" : faculty.getDepartment();

                String searchable = (
                        name + " " + role + " " + department
                ).toLowerCase(Locale.ROOT);

                if (searchable.contains(query)) {
                    facultyList.add(faculty);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faculty, parent, false);

        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FacultyViewHolder holder, int position) {

        FacultyUser faculty = facultyList.get(position);

        String name = faculty.getName();

        if (name == null || name.trim().isEmpty()) {
            name = "Faculty";
        }

        holder.tvFacultyName.setText(name);

        String role = faculty.getRole();
        String department = faculty.getDepartment();

        if (role == null) role = "";
        if (department == null) department = "";

        String subtitle = role;

        if (!department.trim().isEmpty()) {
            subtitle = subtitle.isEmpty()
                    ? department
                    : subtitle + " • " + department;
        }

        holder.tvFacultyRole.setText(subtitle);

        String initial = name.trim().isEmpty()
                ? "F"
                : name.trim().substring(0, 1).toUpperCase(Locale.ROOT);

        holder.tvFacultyInitial.setText(initial);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFacultyClick(faculty);
            }
        });
    }

    @Override
    public int getItemCount() {
        return facultyList.size();
    }

    static class FacultyViewHolder extends RecyclerView.ViewHolder {

        TextView tvFacultyInitial;
        TextView tvFacultyName;
        TextView tvFacultyRole;

        FacultyViewHolder(@NonNull View itemView) {
            super(itemView);

            tvFacultyInitial =
                    itemView.findViewById(R.id.tvFacultyInitial);

            tvFacultyName =
                    itemView.findViewById(R.id.tvFacultyName);

            tvFacultyRole =
                    itemView.findViewById(R.id.tvFacultyRole);
        }
    }
}