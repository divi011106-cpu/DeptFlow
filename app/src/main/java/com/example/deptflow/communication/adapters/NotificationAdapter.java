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

        // Subtitle (Task title or sender subject)
        holder.tvNotificationSubtitle.setText(notification.getSubtitle());

        // Message snippet
        String message = notification.getMessage();
        if (message == null || message.trim().isEmpty()) {
            holder.tvNotificationMessage.setVisibility(View.GONE);
        } else {
            holder.tvNotificationMessage.setVisibility(View.VISIBLE);
            holder.tvNotificationMessage.setText(message.trim());
        }

        // Timestamp
        holder.tvNotificationTime.setText(notification.getFormattedDate());

        // Type badge & Action button text
        String type = notification.getType();
        if (AppNotification.TYPE_CHAT.equalsIgnoreCase(type)) {
            holder.tvNotificationType.setText("💬 FACULTY CHAT");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_inprogress);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.stat_inprogress_text));
            holder.tvNotificationDeadline.setVisibility(View.GONE);
            holder.tvNotificationPriority.setVisibility(View.GONE);
            holder.btnPrimaryAction.setText("Open Chat");
            holder.btnPrimaryAction.setIcon(null);
        } else if (AppNotification.TYPE_ANNOUNCEMENT.equalsIgnoreCase(type)) {
            holder.tvNotificationType.setText("🔔 ANNOUNCEMENT");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_pending);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.stat_pending_text));
            holder.tvNotificationDeadline.setVisibility(View.GONE);
            holder.tvNotificationPriority.setVisibility(View.GONE);
            holder.btnPrimaryAction.setText("View Details");
        } else {
            holder.tvNotificationType.setText("📢 TASK ASSIGNMENT");
            holder.tvNotificationType.setBackgroundResource(R.drawable.bg_stat_total);
            holder.tvNotificationType.setTextColor(ContextCompat.getColor(context, R.color.primary));
            holder.btnPrimaryAction.setText("View Task");

            // Deadline
            String deadline = notification.getDeadline();
            if (deadline != null && !deadline.trim().isEmpty()) {
                holder.tvNotificationDeadline.setVisibility(View.VISIBLE);
                holder.tvNotificationDeadline.setText("Due: " + deadline.trim());
            } else {
                holder.tvNotificationDeadline.setVisibility(View.GONE);
            }

            // Priority
            String priority = notification.getPriority();
            if (priority != null && !priority.trim().isEmpty()) {
                holder.tvNotificationPriority.setVisibility(View.VISIBLE);
                holder.tvNotificationPriority.setText(priority.toUpperCase());
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

        // Button action: Open Chat / View Task
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
        TextView tvNotificationMessage;
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
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvNotificationPriority = itemView.findViewById(R.id.tvNotificationPriority);
            tvNotificationDeadline = itemView.findViewById(R.id.tvNotificationDeadline);
            btnPrimaryAction = itemView.findViewById(R.id.btnPrimaryAction);
            btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
        }
    }
}
