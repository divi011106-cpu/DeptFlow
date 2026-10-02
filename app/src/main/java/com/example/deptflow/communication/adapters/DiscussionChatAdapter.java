package com.example.deptflow.communication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.communication.models.DiscussionMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for rendering real-time chat messages in a Task Discussion.
 * Automatically splits between sent bubbles (right) and received bubbles (left).
 */
public class DiscussionChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final Context context;
    private final List<DiscussionMessage> messageList;
    private final Set<String> userIdentifiers = new HashSet<>();

    public DiscussionChatAdapter(Context context, List<DiscussionMessage> messageList, String currentUserId) {
        this.context = context;
        this.messageList = new ArrayList<>();
        if (messageList != null) {
            this.messageList.addAll(messageList);
        }
        if (currentUserId != null && !currentUserId.trim().isEmpty()) {
            this.userIdentifiers.add(currentUserId.trim());
            this.userIdentifiers.add(currentUserId.trim().toLowerCase());
        }
    }

    public DiscussionChatAdapter(Context context, List<DiscussionMessage> messageList, Set<String> identifiers) {
        this.context = context;
        this.messageList = new ArrayList<>();
        if (messageList != null) {
            this.messageList.addAll(messageList);
        }
        if (identifiers != null) {
            this.userIdentifiers.addAll(identifiers);
        }
    }

    public void setUserIdentifiers(Set<String> identifiers) {
        this.userIdentifiers.clear();
        if (identifiers != null) {
            this.userIdentifiers.addAll(identifiers);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        DiscussionMessage message = messageList.get(position);
        if (message.isSentBy(userIdentifiers)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_discussion_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_discussion_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DiscussionMessage message = messageList.get(position);

        if (holder instanceof SentMessageViewHolder) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            sentHolder.tvMessageText.setText(message.getMessage());
            sentHolder.tvMessageTime.setText(message.getFormattedTime());

            int tickState = message.getTickStatus(userIdentifiers);
            if (tickState == 2) {
                // READ: ✓✓ blue
                sentHolder.tvMessageStatus.setText("✓✓");
                sentHolder.tvMessageStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_tick_read));
            } else if (tickState == 1) {
                // DELIVERED: ✓✓ grey
                sentHolder.tvMessageStatus.setText("✓✓");
                sentHolder.tvMessageStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_tick_delivered));
            } else {
                // SENT: ✓ grey
                sentHolder.tvMessageStatus.setText("✓");
                sentHolder.tvMessageStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_tick_delivered));
            }
            sentHolder.tvMessageStatus.setVisibility(View.VISIBLE);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ReceivedMessageViewHolder recvHolder = (ReceivedMessageViewHolder) holder;
            recvHolder.tvMessageText.setText(message.getMessage());
            recvHolder.tvMessageTime.setText(message.getFormattedTime());
            recvHolder.tvSenderName.setText(message.getSenderName());

            String role = message.getSenderRole();
            if (role != null && !role.trim().isEmpty()) {
                recvHolder.tvSenderRole.setVisibility(View.VISIBLE);
                recvHolder.tvSenderRole.setText(role.toUpperCase());
            } else {
                recvHolder.tvSenderRole.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessages(List<DiscussionMessage> newMessages) {
        this.messageList.clear();
        if (newMessages != null) {
            this.messageList.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(DiscussionMessage message) {
        if (message != null) {
            this.messageList.add(message);
            notifyItemInserted(this.messageList.size() - 1);
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText;
        TextView tvMessageTime;
        TextView tvMessageStatus;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvMessageStatus = itemView.findViewById(R.id.tvMessageStatus);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName;
        TextView tvSenderRole;
        TextView tvMessageText;
        TextView tvMessageTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvSenderRole = itemView.findViewById(R.id.tvSenderRole);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
