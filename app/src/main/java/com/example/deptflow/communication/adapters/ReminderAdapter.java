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
import com.example.deptflow.communication.models.TaskReminder;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering task deadline reminders with urgency-colored countdown badges
 * and direct shortcuts into Task Discussion.
 */
public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderClickListener {
        void onDiscussClick(TaskReminder reminder);
    }

    private final Context context;
    private final List<TaskReminder> reminderList;
    private final OnReminderClickListener listener;

    public ReminderAdapter(Context context, List<TaskReminder> reminderList, OnReminderClickListener listener) {
        this.context = context;
        this.reminderList = reminderList != null ? reminderList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        TaskReminder reminder = reminderList.get(position);

        // Title
        holder.tvReminderTitle.setText(reminder.getTitle());

        // Description
        String desc = reminder.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            holder.tvReminderDescription.setVisibility(View.GONE);
        } else {
            holder.tvReminderDescription.setVisibility(View.VISIBLE);
            holder.tvReminderDescription.setText(desc.trim());
        }

        // Deadline
        holder.tvReminderDeadline.setText("Deadline: " + reminder.getDeadline());

        // Priority
        String priority = reminder.getPriority();
        holder.tvReminderPriority.setText(priority.toUpperCase());

        // Status
        String status = reminder.getStatus();
        holder.tvReminderStatus.setText(status.toUpperCase());
        if ("COMPLETED".equalsIgnoreCase(status)) {
            holder.tvReminderStatus.setBackgroundResource(R.drawable.bg_status_completed);
            holder.tvReminderStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text));
        } else if ("IN PROGRESS".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
            holder.tvReminderStatus.setBackgroundResource(R.drawable.bg_status_inprogress);
            holder.tvReminderStatus.setTextColor(ContextCompat.getColor(context, R.color.status_inprogress_text));
        } else {
            holder.tvReminderStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvReminderStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text));
        }

        // Countdown Badge & Card Stroke Urgency Styling
        TaskReminder.UrgencyCategory category = reminder.getCategory();
        holder.tvCountdownBadge.setText(reminder.getCountdownText());

        switch (category) {
            case OVERDUE:
                holder.tvCountdownBadge.setBackgroundResource(R.drawable.bg_priority_high);
                holder.tvCountdownBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_high_text));
                holder.cardReminder.setStrokeColor(ContextCompat.getColor(context, R.color.priority_high));
                holder.cardReminder.setStrokeWidth(3);
                break;

            case DUE_TODAY:
                holder.tvCountdownBadge.setBackgroundResource(R.drawable.bg_priority_medium);
                holder.tvCountdownBadge.setTextColor(ContextCompat.getColor(context, R.color.priority_medium_text));
                holder.cardReminder.setStrokeColor(ContextCompat.getColor(context, R.color.gold_accent));
                holder.cardReminder.setStrokeWidth(3);
                break;

            case DUE_TOMORROW:
                holder.tvCountdownBadge.setBackgroundResource(R.drawable.bg_stat_pending);
                holder.tvCountdownBadge.setTextColor(ContextCompat.getColor(context, R.color.stat_pending_text));
                holder.cardReminder.setStrokeColor(ContextCompat.getColor(context, R.color.gold_accent));
                holder.cardReminder.setStrokeWidth(2);
                break;

            case COMPLETED:
                holder.tvCountdownBadge.setBackgroundResource(R.drawable.bg_status_completed);
                holder.tvCountdownBadge.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text));
                holder.cardReminder.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
                holder.cardReminder.setStrokeWidth(1);
                break;

            case UPCOMING:
            default:
                holder.tvCountdownBadge.setBackgroundResource(R.drawable.bg_stat_inprogress);
                holder.tvCountdownBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
                holder.cardReminder.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
                holder.cardReminder.setStrokeWidth(1);
                break;
        }

        // Click on Discuss button
        holder.btnDiscussReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDiscussClick(reminder);
            }
        });

        // Click on card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDiscussClick(reminder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public void updateReminders(List<TaskReminder> newReminders) {
        this.reminderList.clear();
        if (newReminders != null) {
            this.reminderList.addAll(newReminders);
        }
        notifyDataSetChanged();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardReminder;
        TextView tvCountdownBadge;
        TextView tvReminderPriority;
        TextView tvReminderStatus;
        TextView tvReminderTitle;
        TextView tvReminderDescription;
        TextView tvReminderDeadline;
        View btnDiscussReminder;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardReminder = itemView.findViewById(R.id.cardReminder);
            tvCountdownBadge = itemView.findViewById(R.id.tvCountdownBadge);
            tvReminderPriority = itemView.findViewById(R.id.tvReminderPriority);
            tvReminderStatus = itemView.findViewById(R.id.tvReminderStatus);
            tvReminderTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvReminderDescription = itemView.findViewById(R.id.tvReminderDescription);
            tvReminderDeadline = itemView.findViewById(R.id.tvReminderDeadline);
            btnDiscussReminder = itemView.findViewById(R.id.btnDiscussReminder);
        }
    }
}
