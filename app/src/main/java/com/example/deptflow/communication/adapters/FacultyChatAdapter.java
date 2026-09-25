package com.example.deptflow.communication.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.communication.models.FacultyConversation;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern WhatsApp-style Faculty Chat Adapter.
 * Displays active conversations with live message previews, formatted timestamps,
 * and green unread count badges, or department directory contacts.
 */
public class FacultyChatAdapter extends RecyclerView.Adapter<FacultyChatAdapter.FacultyChatViewHolder> {

    public interface OnFacultyChatClickListener {
        void onFacultyChatClick(FacultyConversation conversation);
    }

    private final Context context;
    private final List<FacultyConversation> conversationList = new ArrayList<>();
    private final List<FacultyConversation> fullList = new ArrayList<>();
    private final OnFacultyChatClickListener listener;
    private boolean isContactsMode = false;

    public FacultyChatAdapter(Context context, OnFacultyChatClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setContactsMode(boolean contactsMode) {
        this.isContactsMode = contactsMode;
    }

    public void updateList(List<FacultyConversation> conversations) {
        fullList.clear();
        conversationList.clear();
        if (conversations != null) {
            fullList.addAll(conversations);
            conversationList.addAll(conversations);
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        String filterPattern = query != null ? query.toLowerCase(Locale.ROOT).trim() : "";
        conversationList.clear();

        if (filterPattern.isEmpty()) {
            conversationList.addAll(fullList);
        } else {
            for (FacultyConversation item : fullList) {
                FacultyUser faculty = item.getFaculty();
                String name = faculty != null && faculty.getName() != null ? faculty.getName() : "";
                String dept = faculty != null && faculty.getDepartment() != null ? faculty.getDepartment() : "";
                String role = faculty != null && faculty.getRole() != null ? faculty.getRole() : "";
                String lastMsg = item.getLastMessage() != null ? item.getLastMessage() : "";
                String combined = (name + " " + dept + " " + role + " " + lastMsg).toLowerCase(Locale.ROOT);

                if (combined.contains(filterPattern)) {
                    conversationList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FacultyChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_faculty_chat, parent, false);
        return new FacultyChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyChatViewHolder holder, int position) {
        FacultyConversation item = conversationList.get(position);
        FacultyUser faculty = item.getFaculty();

        String name = faculty != null && faculty.getName() != null && !faculty.getName().trim().isEmpty()
                ? faculty.getName().trim() : "Faculty Member";

        holder.tvFacultyName.setText(name);

        String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "F";
        holder.tvFacultyInitial.setText(initial);

        String role = faculty != null && faculty.getRole() != null ? faculty.getRole().trim() : "";
        String dept = faculty != null && faculty.getDepartment() != null ? faculty.getDepartment().trim() : "";
        String subtitle = "Faculty • " + (!dept.isEmpty() ? dept : "Information Technology");
        holder.tvFacultySubtitle.setText(subtitle);

        // Last Message & Time
        String lastMsg = item.getLastMessage();
        int unread = item.getUnreadCount();

        if (isContactsMode) {
            holder.tvLastMessage.setText("Tap to start conversation");
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvLastMessageTime.setVisibility(View.GONE);
            holder.tvUnreadBadge.setVisibility(View.GONE);
            holder.cardFacultyChat.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
            holder.cardFacultyChat.setStrokeWidth(1);
        } else {
            if (lastMsg != null && !lastMsg.trim().isEmpty()) {
                String previewText = item.isLastMessageMine() ? "You: " + lastMsg.trim() : lastMsg.trim();
                holder.tvLastMessage.setText(previewText);
                holder.tvLastMessageTime.setText(item.getFormattedTime());
                holder.tvLastMessageTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvLastMessage.setText("Tap to send message");
                holder.tvLastMessageTime.setVisibility(View.GONE);
            }

            // WhatsApp-style green unread badge
            if (unread > 0) {
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
                holder.tvUnreadBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
                holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                holder.tvLastMessageTime.setTextColor(ContextCompat.getColor(context, R.color.stat_completed_text));
                holder.cardFacultyChat.setStrokeColor(ContextCompat.getColor(context, R.color.primary_light));
                holder.cardFacultyChat.setStrokeWidth(2);
            } else {
                holder.tvUnreadBadge.setVisibility(View.GONE);
                holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                holder.tvLastMessageTime.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                holder.cardFacultyChat.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
                holder.cardFacultyChat.setStrokeWidth(1);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFacultyChatClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    static class FacultyChatViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardFacultyChat;
        TextView tvFacultyInitial;
        TextView tvFacultyName;
        TextView tvFacultySubtitle;
        TextView tvLastMessage;
        TextView tvLastMessageTime;
        TextView tvUnreadBadge;

        FacultyChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFacultyChat = itemView.findViewById(R.id.cardFacultyChat);
            tvFacultyInitial = itemView.findViewById(R.id.tvFacultyInitial);
            tvFacultyName = itemView.findViewById(R.id.tvFacultyName);
            tvFacultySubtitle = itemView.findViewById(R.id.tvFacultySubtitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}
