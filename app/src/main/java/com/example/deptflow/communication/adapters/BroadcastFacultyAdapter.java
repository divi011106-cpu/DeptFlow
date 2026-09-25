package com.example.deptflow.communication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * BroadcastFacultyAdapter — Displays the department faculty roster with multi-select checkboxes.
 */
public class BroadcastFacultyAdapter extends RecyclerView.Adapter<BroadcastFacultyAdapter.ViewHolder> {

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount, List<FacultyUser> selectedList);
    }

    private final Context context;
    private final List<FacultyUser> fullList = new ArrayList<>();
    private final List<FacultyUser> filteredList = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final String currentUserId;
    private final String currentCanonicalId;
    private final OnSelectionChangeListener listener;

    public BroadcastFacultyAdapter(Context context,
                                   List<FacultyUser> facultyList,
                                   String currentUserId,
                                   String currentCanonicalId,
                                   OnSelectionChangeListener listener) {
        this.context = context;
        this.currentUserId = currentUserId != null ? currentUserId : "";
        this.currentCanonicalId = currentCanonicalId != null ? currentCanonicalId : "";
        this.listener = listener;

        if (facultyList != null) {
            for (FacultyUser f : facultyList) {
                if (f != null && !isSelf(f)) {
                    this.fullList.add(f);
                }
            }
        }
        this.filteredList.addAll(this.fullList);
    }

    private boolean isSelf(FacultyUser f) {
        if (f == null) return false;
        String id = f.getUserId();
        return id.equalsIgnoreCase(currentUserId) || id.equalsIgnoreCase(currentCanonicalId);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_broadcast_faculty, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FacultyUser faculty = filteredList.get(position);

        holder.tvName.setText(faculty.getName());
        holder.tvDetails.setText(faculty.getUserId() + " • " + (faculty.getDepartment() != null ? faculty.getDepartment() : "IT"));

        // Avatar Initials
        String initials = getInitials(faculty.getName());
        holder.tvAvatar.setText(initials);

        boolean isSelected = selectedIds.contains(faculty.getUserId());
        holder.cbSelect.setChecked(isSelected);

        if (isSelected) {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.primary));
            holder.cardView.setStrokeWidth(2);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
        } else {
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.card_stroke));
            holder.cardView.setStrokeWidth(1);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
        }

        holder.itemView.setOnClickListener(v -> toggleSelection(faculty.getUserId()));
    }

    private void toggleSelection(String facultyId) {
        if (selectedIds.contains(facultyId)) {
            selectedIds.remove(facultyId);
        } else {
            selectedIds.add(facultyId);
        }
        notifyDataSetChanged();
        notifyListener();
    }

    public void selectAll() {
        selectedIds.clear();
        for (FacultyUser f : fullList) {
            selectedIds.add(f.getUserId());
        }
        notifyDataSetChanged();
        notifyListener();
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        notifyListener();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String lower = query.toLowerCase(Locale.ROOT).trim();
            for (FacultyUser f : fullList) {
                if (f.getName().toLowerCase(Locale.ROOT).contains(lower)
                        || f.getUserId().toLowerCase(Locale.ROOT).contains(lower)
                        || (f.getDepartment() != null && f.getDepartment().toLowerCase(Locale.ROOT).contains(lower))) {
                    filteredList.add(f);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<FacultyUser> getSelectedFaculty() {
        List<FacultyUser> result = new ArrayList<>();
        for (FacultyUser f : fullList) {
            if (selectedIds.contains(f.getUserId())) {
                result.add(f);
            }
        }
        return result;
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public int getTotalCount() {
        return fullList.size();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onSelectionChanged(selectedIds.size(), getSelectedFaculty());
        }
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "F";
        String[] parts = name.trim().replace("Dr.", "").replace("Mr.", "").replace("Mrs.", "").replace("Ms.", "").trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(Locale.ROOT);
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvAvatar;
        TextView tvName;
        TextView tvDetails;
        CheckBox cbSelect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardBroadcastItem);
            tvAvatar = itemView.findViewById(R.id.tvBroadcastAvatarInitial);
            tvName = itemView.findViewById(R.id.tvBroadcastFacultyName);
            tvDetails = itemView.findViewById(R.id.tvBroadcastFacultyDetails);
            cbSelect = itemView.findViewById(R.id.cbBroadcastSelect);
        }
    }
}
