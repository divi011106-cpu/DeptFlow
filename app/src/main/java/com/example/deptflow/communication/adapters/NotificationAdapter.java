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
import com.example.deptflow.communication.models.AppNotification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern interactive Notification Adapter.
 * Supports tap actions, dedicated action buttons (Open Chat / View Task), Mark Read, and Dismiss.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
        void onNotificationMarkRead(AppNotification notification);
        void onNotificationDismiss(AppNotification notification);
    }

    private final Context context;
    private final List<AppNotification> notificationList;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(Context context, List<AppNotification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notificationList.get(position);

        // Subtitle (Task title or sender name)
        String subtitle = notification.getSubtitle();
        if (subtitle == null || subtitle.trim().isEmpty()) {
            subtitle = notification.getTitle();
        }
        holder.tvNotificationSubtitle.setText(subtitle);

        // Timestamp
        holder.tvNotificationTime.setText(notification.getFormattedDate());

        // Message snippet
        String message = notification.getMessage();

        // Bind based on exact notification type: CHAT_MESSAGE vs TEAM_TASK vs TASK_ASSIGNMENT
        if (notification.isChat()) {
            holder.tvNotificationType.setText("💬 NEW MESSAGE");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_inprogress);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.stat_inprogress_text));

            // Sender as title/subtitle
            String sender = notification.getSender();
            if (sender != null && !sender.trim().isEmpty()) {
                holder.tvNotificationSubtitle.setText(sender);
            }

            // Chat message snippet with quotes
            if (message != null && !message.trim().isEmpty()) {
                holder.tvNotificationMessage.setVisibility(View.VISIBLE);
                String formattedMsg = message.trim();
                if (!formattedMsg.startsWith("\"")) {
                    formattedMsg = "\"" + formattedMsg + "\"";
                }
                holder.tvNotificationMessage.setText(formattedMsg);
            } else {
                holder.tvNotificationMessage.setVisibility(View.GONE);
            }

            holder.tvNotificationAssignedBy.setVisibility(View.GONE);
            holder.tvNotificationTeamMembers.setVisibility(View.GONE);
            holder.tvNotificationStatus.setVisibility(View.GONE);
            holder.tvNotificationDeadline.setVisibility(View.GONE);
            holder.tvNotificationPriority.setVisibility(View.GONE);
            holder.btnPrimaryAction.setText("OPEN CHAT");
            holder.btnPrimaryAction.setIcon(null);

        } else if (notification.isTeamTask()) {
            holder.tvNotificationType.setText("👥 TEAM TASK");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_total);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.primary));

            // Subtitle: Task title
            holder.tvNotificationSubtitle.setText(subtitle);

            // Assigned by
            holder.tvNotificationAssignedBy.setVisibility(View.VISIBLE);
            String sender = notification.getSender();
            holder.tvNotificationAssignedBy.setText("Assigned by: " + (sender != null && !sender.trim().isEmpty() ? sender.trim() : "HOD"));

            // Team members list
            String teamSummary = notification.getTeamMembersSummary();
            if (teamSummary != null && !teamSummary.trim().isEmpty()) {
                holder.tvNotificationTeamMembers.setVisibility(View.VISIBLE);
                holder.tvNotificationTeamMembers.setText(teamSummary);
            } else {
                holder.tvNotificationTeamMembers.setVisibility(View.GONE);
            }

            // Message / Description
            if (message != null && !message.trim().isEmpty()) {
                holder.tvNotificationMessage.setVisibility(View.VISIBLE);
                holder.tvNotificationMessage.setText(message.trim());
            } else {
                holder.tvNotificationMessage.setVisibility(View.GONE);
            }

            holder.tvNotificationStatus.setVisibility(View.GONE);

            // Deadline
            bindDeadline(holder, notification.getDeadline());

            // Priority
            bindPriority(holder, notification.getPriority());

            holder.btnPrimaryAction.setText("OPEN DISCUSSION");
            holder.btnPrimaryAction.setIcon(null);

        } else if (AppNotification.TYPE_ANNOUNCEMENT.equalsIgnoreCase(notification.getType())) {
            holder.tvNotificationType.setText("📢 ANNOUNCEMENT");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_pending);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.stat_pending_text));

            if (message != null && !message.trim().isEmpty()) {
                holder.tvNotificationMessage.setVisibility(View.VISIBLE);
                holder.tvNotificationMessage.setText(message.trim());
            } else {
                holder.tvNotificationMessage.setVisibility(View.GONE);
            }

            holder.tvNotificationAssignedBy.setVisibility(View.GONE);
            holder.tvNotificationTeamMembers.setVisibility(View.GONE);
            holder.tvNotificationStatus.setVisibility(View.GONE);
            holder.tvNotificationDeadline.setVisibility(View.GONE);
            holder.tvNotificationPriority.setVisibility(View.GONE);
            holder.btnPrimaryAction.setText("VIEW DETAILS");
            holder.btnPrimaryAction.setIcon(null);

        } else {
            // Individual TASK ASSIGNMENT
            holder.tvNotificationType.setText("📋 TASK ASSIGNMENT");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_total);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.primary));

            holder.tvNotificationSubtitle.setText(subtitle);

            // Message / Description
            if (message != null && !message.trim().isEmpty()) {
                holder.tvNotificationMessage.setVisibility(View.VISIBLE);
                holder.tvNotificationMessage.setText(message.trim());
            } else {
                holder.tvNotificationMessage.setVisibility(View.GONE);
            }

            holder.tvNotificationAssignedBy.setVisibility(View.GONE);
            holder.tvNotificationTeamMembers.setVisibility(View.GONE);

            // Status
            String status = notification.getStatus();
            if (status != null && !status.trim().isEmpty()) {
                holder.tvNotificationStatus.setVisibility(View.VISIBLE);
                holder.tvNotificationStatus.setText("Status: " + status.trim().toUpperCase());
            } else {
                holder.tvNotificationStatus.setVisibility(View.GONE);
            }

            // Deadline
            bindDeadline(holder, notification.getDeadline());

            // Priority
            bindPriority(holder, notification.getPriority());

            holder.btnPrimaryAction.setText("OPEN TASK");
            holder.btnPrimaryAction.setIcon(null);
        }

        // Unread styling
        if (!notification.isRead()) {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(context, R.color.primary_light));
            holder.cardNotification.setStrokeWidth(2);
            holder.cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
            holder.btnMarkRead.setVisibility(View.VISIBLE);
        } else {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
            holder.cardNotification.setStrokeWidth(1);
            holder.cardNotification.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background));
            holder.btnMarkRead.setVisibility(View.GONE);
        }

        // Card tap action
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });

        // Button action: Open Chat / Open Discussion / Open Task
        holder.btnPrimaryAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });

        // Button action: Mark Read
        holder.btnMarkRead.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationMarkRead(notification);
            }
        });

        // Button action: Dismiss
        holder.btnDismiss.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationDismiss(notification);
            }
        });
    }

    private void bindDeadline(NotificationViewHolder holder, String deadline) {
        if (deadline != null && !deadline.trim().isEmpty()) {
            holder.tvNotificationDeadline.setVisibility(View.VISIBLE);
            holder.tvNotificationDeadline.setText("📅 Due: " + deadline.trim());
        } else {
            holder.tvNotificationDeadline.setVisibility(View.GONE);
        }
    }

    private void bindPriority(NotificationViewHolder holder, String priority) {
        if (priority != null && !priority.trim().isEmpty()) {
            holder.tvNotificationPriority.setVisibility(View.VISIBLE);
            holder.tvNotificationPriority.setText(priority.trim().toUpperCase());
            if ("HIGH".equalsIgnoreCase(priority)) {
                holder.tvNotificationPriority.setBackgroundResource(R.drawable.bg_priority_high);
                holder.tvNotificationPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high_text));
            } else if ("LOW".equalsIgnoreCase(priority)) {
                holder.tvNotificationPriority.setBackgroundResource(R.drawable.bg_priority_low);
                holder.tvNotificationPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low_text));
            } else {
                holder.tvNotificationPriority.setBackgroundResource(R.drawable.bg_priority_medium);
                holder.tvNotificationPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium_text));
            }
        } else {
            holder.tvNotificationPriority.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void setNotifications(List<AppNotification> newNotifications) {
        this.notificationList.clear();
        if (newNotifications != null) {
            this.notificationList.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    public void markAllAsRead() {
        for (AppNotification n : notificationList) {
            n.setRead(true);
        }
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotification;
        TextView tvNotificationType;
        View viewUnreadDot;
        TextView tvNotificationTime;
        TextView tvNotificationSubtitle;
        TextView tvNotificationAssignedBy;
        TextView tvNotificationTeamMembers;
        TextView tvNotificationMessage;
        TextView tvNotificationStatus;
        TextView tvNotificationPriority;
        TextView tvNotificationDeadline;
        MaterialButton btnPrimaryAction;
        MaterialButton btnMarkRead;
        MaterialButton btnDismiss;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            tvNotificationType = itemView.findViewById(R.id.tvNotificationType);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            tvNotificationSubtitle = itemView.findViewById(R.id.tvNotificationSubtitle);
            tvNotificationAssignedBy = itemView.findViewById(R.id.tvNotificationAssignedBy);
            tvNotificationTeamMembers = itemView.findViewById(R.id.tvNotificationTeamMembers);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationStatus = itemView.findViewById(R.id.tvNotificationStatus);
            tvNotificationPriority = itemView.findViewById(R.id.tvNotificationPriority);
            tvNotificationDeadline = itemView.findViewById(R.id.tvNotificationDeadline);
            btnPrimaryAction = itemView.findViewById(R.id.btnPrimaryAction);
            btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
        }
    }
}
