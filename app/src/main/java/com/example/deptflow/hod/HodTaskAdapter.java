package com.example.deptflow.hod;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.Task;

import java.util.ArrayList;
import java.util.List;

public class HodTaskAdapter extends RecyclerView.Adapter<HodTaskAdapter.HodTaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final Context context;
    private List<Task> tasks;
    private final OnTaskClickListener listener;

    public HodTaskAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.tasks = (tasks != null) ? tasks : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Task> newTasks) {
        this.tasks = (newTasks != null) ? newTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HodTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hod_task, parent, false);
        return new HodTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HodTaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class HodTaskViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardTask;
        private final TextView tvPriority;
        private final TextView tvStatus;
        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvFaculty;
        private final TextView tvDeadline;

        HodTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.card_hod_task);
            tvPriority = itemView.findViewById(R.id.tv_hod_task_priority);
            tvStatus = itemView.findViewById(R.id.tv_hod_task_status);
            tvTitle = itemView.findViewById(R.id.tv_hod_task_title);
            tvDesc = itemView.findViewById(R.id.tv_hod_task_desc);
            tvFaculty = itemView.findViewById(R.id.tv_hod_task_faculty);
            tvDeadline = itemView.findViewById(R.id.tv_hod_task_deadline);
        }

        void bind(Task task) {
            if (task == null) return;

            tvTitle.setText(task.getTaskTitle());
            tvDesc.setText(task.getDescription());

            String faculty = task.getAssignedTo();
            if (faculty == null || faculty.trim().isEmpty()) {
                faculty = "Department Faculty";
            }
            tvFaculty.setText("Assigned to: " + faculty);

            String deadline = task.getDeadline();
            if (deadline == null || deadline.trim().isEmpty()) {
                deadline = "No Deadline";
            }
            tvDeadline.setText("Deadline: " + deadline);

            // Priority badge
            String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : "MEDIUM";
            tvPriority.setText(priority);
            if (Task.PRIORITY_HIGH.equalsIgnoreCase(priority)) {
                tvPriority.setBackgroundResource(R.drawable.bg_priority_high);
                tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high_text));
            } else if (Task.PRIORITY_LOW.equalsIgnoreCase(priority)) {
                tvPriority.setBackgroundResource(R.drawable.bg_priority_low);
                tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low_text));
            } else {
                tvPriority.setBackgroundResource(R.drawable.bg_priority_medium);
                tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium_text));
            }

            // Status badge
            String status = task.getStatus() != null ? task.getStatus().toUpperCase() : Task.STATUS_PENDING;
            if (Task.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                tvStatus.setText("COMPLETED");
                tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text));
            } else if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                tvStatus.setText("IN PROGRESS");
                tvStatus.setBackgroundResource(R.drawable.bg_status_inprogress);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_inprogress_text));
            } else {
                tvStatus.setText("ASSIGNED");
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text));
            }

            cardTask.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }
    }
}
