package com.example.deptflow.communication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.communication.models.DiscussionTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering task discussion cards in the TaskDiscussionActivity.
 */
public class TaskDiscussionAdapter extends RecyclerView.Adapter<TaskDiscussionAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(DiscussionTask task);
    }

    private final Context context;
    private final List<DiscussionTask> taskList;
    private final OnTaskClickListener listener;

    public TaskDiscussionAdapter(Context context, List<DiscussionTask> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList != null ? taskList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_discussion, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        DiscussionTask task = taskList.get(position);

        // Task ID
        String taskId = task.getId();
        if (taskId == null || taskId.isEmpty()) {
            taskId = "TASK";
        }
        holder.tvTaskId.setText(taskId);

        // Task Title with icon
        String rawTitle = task.getTitle();
        if (rawTitle == null || rawTitle.isEmpty()) {
            rawTitle = "Task Discussion";
        }
        if (!rawTitle.startsWith("📋")) {
            holder.tvTaskTitle.setText("📋 " + rawTitle);
        } else {
            holder.tvTaskTitle.setText(rawTitle);
        }

        // Assigned by HOD
        String assignedBy = task.getAssignedBy();
        if (assignedBy == null || assignedBy.trim().isEmpty()) {
            assignedBy = "HOD";
        }
        holder.tvAssignedBy.setText("Assigned by: " + (assignedBy.toUpperCase().contains("HOD") ? "HOD" : assignedBy));

        // Task Description
        String desc = task.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            holder.tvTaskDescription.setVisibility(View.GONE);
        } else {
            holder.tvTaskDescription.setVisibility(View.VISIBLE);
            holder.tvTaskDescription.setText(desc.trim());
        }

        // Deadline
        String deadline = task.getDeadline();
        if (deadline == null || deadline.trim().isEmpty()) {
            holder.tvTaskDeadline.setText("Deadline: No deadline");
        } else {
            holder.tvTaskDeadline.setText("Deadline: " + deadline.trim());
        }

        // Priority Badge
        String priority = task.getPriority();
        if (priority == null) priority = "MEDIUM";
        holder.tvTaskPriority.setText(priority.toUpperCase());

        if ("HIGH".equalsIgnoreCase(priority)) {
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_priority_high);
            holder.tvTaskPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high_text));
        } else if ("LOW".equalsIgnoreCase(priority)) {
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_priority_low);
            holder.tvTaskPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low_text));
        } else {
            holder.tvTaskPriority.setBackgroundResource(R.drawable.bg_priority_medium);
            holder.tvTaskPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium_text));
        }

        // Status Badge
        String status = task.getStatus();
        if (status == null || status.isEmpty()) status = "Assigned";
        holder.tvTaskStatus.setText(status.toUpperCase());

        if ("COMPLETED".equalsIgnoreCase(status)) {
            holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_completed);
            holder.tvTaskStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text));
        } else if ("IN PROGRESS".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
            holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_inprogress);
            holder.tvTaskStatus.setTextColor(ContextCompat.getColor(context, R.color.status_inprogress_text));
        } else {
            holder.tvTaskStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvTaskStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text));
        }

        // Participants / Team Members Section
        List<String> assignedList = task.getAssignedFaculty();
        int count = task.getAssignedFacultyCount();

        if (task.isAll()) {
            if (holder.tvTaskType != null) {
                holder.tvTaskType.setText("Team Task");
                holder.tvTaskType.setVisibility(View.VISIBLE);
            }
            holder.tvParticipantsHeader.setText("Team Task: All Faculty");
            holder.tvAssignedFaculty.setVisibility(View.VISIBLE);
            holder.tvAssignedFaculty.setText("All Department Faculty Members");
        } else if (count > 1 || (assignedList != null && assignedList.size() > 1)) {
            if (holder.tvTaskType != null) {
                holder.tvTaskType.setText("Team Task");
                holder.tvTaskType.setVisibility(View.VISIBLE);
            }
            holder.tvParticipantsHeader.setText("Team Members (" + count + "):");
            if (assignedList != null && !assignedList.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < assignedList.size(); i++) {
                    sb.append("• ").append(assignedList.get(i));
                    if (i < assignedList.size() - 1) sb.append("\n");
                }
                holder.tvAssignedFaculty.setVisibility(View.VISIBLE);
                holder.tvAssignedFaculty.setText(sb.toString());
            } else {
                holder.tvAssignedFaculty.setVisibility(View.GONE);
            }
        } else {
            if (holder.tvTaskType != null) {
                holder.tvTaskType.setText("Individual Task");
                holder.tvTaskType.setVisibility(View.VISIBLE);
            }
            holder.tvParticipantsHeader.setText("Individual Task");
            if (assignedList != null && !assignedList.isEmpty()) {
                holder.tvAssignedFaculty.setVisibility(View.VISIBLE);
                holder.tvAssignedFaculty.setText("• " + assignedList.get(0));
            } else if (!task.getFaculty().isEmpty()) {
                holder.tvAssignedFaculty.setVisibility(View.VISIBLE);
                holder.tvAssignedFaculty.setText("• " + task.getFaculty());
            } else {
                holder.tvAssignedFaculty.setVisibility(View.GONE);
            }
        }

        // Card Click Listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<DiscussionTask> newTasks) {
        this.taskList.clear();
        if (newTasks != null) {
            this.taskList.addAll(newTasks);
        }
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskId;
        TextView tvTaskType;
        TextView tvTaskPriority;
        TextView tvTaskStatus;
        TextView tvTaskTitle;
        TextView tvAssignedBy;
        TextView tvTaskDescription;
        TextView tvParticipantsHeader;
        TextView tvAssignedFaculty;
        TextView tvTaskDeadline;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskId = itemView.findViewById(R.id.tvTaskId);
            tvTaskType = itemView.findViewById(R.id.tvTaskType);
            tvTaskPriority = itemView.findViewById(R.id.tvTaskPriority);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvAssignedBy = itemView.findViewById(R.id.tvAssignedBy);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvParticipantsHeader = itemView.findViewById(R.id.tvParticipantsHeader);
            tvAssignedFaculty = itemView.findViewById(R.id.tvAssignedFaculty);
            tvTaskDeadline = itemView.findViewById(R.id.tvTaskDeadline);
        }
    }
}
