package com.example.deptflow.feature.faculty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter to display department tasks assigned to the faculty member.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final Context context;
    private List<Task> tasks;
    private final OnTaskClickListener listener;

    public TaskAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
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
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, context, listener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvDescription;
        private final TextView tvDeadline;
        private final TextView tvStatusBadge;
        private final TextView tvPriority;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvDeadline = itemView.findViewById(R.id.tv_task_deadline);
            tvStatusBadge = itemView.findViewById(R.id.tv_task_status_badge);
            tvPriority = itemView.findViewById(R.id.tv_task_priority);
        }

        public void bind(final Task task, Context context, final OnTaskClickListener listener) {
            tvTitle.setText(task.getTaskTitle());
            tvDescription.setText(task.getDescription());
            tvDeadline.setText("Deadline: " + task.getDeadline());

            // Status Styling
            String status = task.getStatus() != null ? task.getStatus().toUpperCase() : Task.STATUS_PENDING;
            tvStatusBadge.setText(status);

            if (Task.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text));
            } else if (Task.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_inprogress);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_inprogress_text));
            } else {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text));
            }

            // Priority Styling
            String priority = task.getPriority() != null ? task.getPriority().toUpperCase() : Task.PRIORITY_MEDIUM;
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

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }
    }
}
