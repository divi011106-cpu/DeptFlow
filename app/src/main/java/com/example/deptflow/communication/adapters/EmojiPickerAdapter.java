package com.example.deptflow.communication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;

import java.util.List;

/**
 * Adapter for displaying emojis in a grid for the task discussion chat.
 */
public class EmojiPickerAdapter extends RecyclerView.Adapter<EmojiPickerAdapter.EmojiViewHolder> {

    public interface OnEmojiClickListener {
        void onEmojiClick(String emoji);
    }

    private final List<String> emojiList;
    private final OnEmojiClickListener listener;

    public EmojiPickerAdapter(List<String> emojiList, OnEmojiClickListener listener) {
        this.emojiList = emojiList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emoji_picker, parent, false);
        return new EmojiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        String emoji = emojiList.get(position);
        holder.tvEmoji.setText(emoji);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmojiClick(emoji);
            }
        });
    }

    @Override
    public int getItemCount() {
        return emojiList != null ? emojiList.size() : 0;
    }

    static class EmojiViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji;

        EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
        }
    }
}
