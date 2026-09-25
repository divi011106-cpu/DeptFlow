package com.example.deptflow.communication.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.communication.models.FacultyReminder;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering personal Faculty Reminders with completion toggles,
 * edit actions, delete actions, and live schedule formatting.
 */
public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderActionListener {
        void onToggleCompleted(FacultyReminder reminder, boolean isCompleted);
        void onEditReminder(FacultyReminder reminder);
        void onDeleteReminder(FacultyReminder reminder);
    }

    private final Context context;
    private final List<FacultyReminder> reminderList = new ArrayList<>();
    private final OnReminderActionListener listener;

    public ReminderAdapter(Context context, OnReminderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateReminders(List<FacultyReminder> reminders) {
        reminderList.clear();
        if (reminders != null) {
            reminderList.addAll(reminders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        FacultyReminder reminder = reminderList.get(position);

        holder.tvReminderTitle.setText(reminder.getTitle());

        String desc = reminder.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            holder.tvReminderDescription.setVisibility(View.VISIBLE);
            holder.tvReminderDescription.setText(desc.trim());
        } else {
            holder.tvReminderDescription.setVisibility(View.GONE);
        }

        // Scheduled Time text
        holder.tvReminderDeadline.setText("⏰ Scheduled: " + reminder.getFormattedScheduledText());

        // Completed checkbox & strike-through styling
        holder.cbReminderCompleted.setOnCheckedChangeListener(null);
        holder.cbReminderCompleted.setChecked(reminder.isCompleted());

        if (reminder.isCompleted()) {
            holder.tvReminderStatus.setText("COMPLETED");
            holder.tvReminderStatus.setBackgroundResource(R.drawable.bg_stat_completed);
            holder.tvReminderStatus.setTextColor(ContextCompat.getColor(context, R.color.stat_completed_text));
            holder.tvCountdownBadge.setVisibility(View.GONE);

            // Strike through title
            holder.tvReminderTitle.setPaintFlags(
                    holder.tvReminderTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvReminderTitle.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.cardReminder.setAlpha(0.75f);
        } else {
            holder.tvReminderTitle.setPaintFlags(
                    holder.tvReminderTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvReminderTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.cardReminder.setAlpha(1.0f);

            if (reminder.isPastDue()) {
                holder.tvReminderStatus.setText("PENDING");
                holder.tvReminderStatus.setBackgroundResource(R.drawable.bg_stat_pending);
                holder.tvReminderStatus.setTextColor(ContextCompat.getColor(context, R.color.stat_pending_text));

                holder.tvCountdownBadge.setVisibility(View.VISIBLE);
                holder.tvCountdownBadge.setText("⚠️ OVERDUE");
            } else {
                holder.tvReminderStatus.setText("UPCOMING");
                holder.tvReminderStatus.setBackgroundResource(R.drawable.bg_stat_inprogress);
                holder.tvReminderStatus.setTextColor(ContextCompat.getColor(context, R.color.primary));

                holder.tvCountdownBadge.setVisibility(View.GONE);
            }
        }

        // Checkbox listener
        holder.cbReminderCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleCompleted(reminder, isChecked);
            }
        });

        // Edit listener
        holder.btnEditReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditReminder(reminder);
            }
        });

        // Delete listener
        holder.btnDeleteReminder.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteReminder(reminder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardReminder;
        CheckBox cbReminderCompleted;
        TextView tvReminderStatus;
        TextView tvCountdownBadge;
        TextView tvReminderTitle;
        TextView tvReminderDescription;
        TextView tvReminderDeadline;
        ImageButton btnEditReminder;
        ImageButton btnDeleteReminder;

        ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardReminder = itemView.findViewById(R.id.cardReminder);
            cbReminderCompleted = itemView.findViewById(R.id.cbReminderCompleted);
            tvReminderStatus = itemView.findViewById(R.id.tvReminderStatus);
            tvCountdownBadge = itemView.findViewById(R.id.tvCountdownBadge);
            tvReminderTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvReminderDescription = itemView.findViewById(R.id.tvReminderDescription);
            tvReminderDeadline = itemView.findViewById(R.id.tvReminderDeadline);
            btnEditReminder = itemView.findViewById(R.id.btnEditReminder);
            btnDeleteReminder = itemView.findViewById(R.id.btnDeleteReminder);
        }
    }
}
