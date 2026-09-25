package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.BroadcastFacultyAdapter;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * BroadcastSelectActivity — Allows a faculty member to select multiple faculty recipients
 * from the department roster to send a WhatsApp-style broadcast message.
 */
public class BroadcastSelectActivity extends AppCompatActivity
        implements BroadcastFacultyAdapter.OnSelectionChangeListener {

    private ImageButton ibBackBroadcastSelect;
    private TextView tvSelectedCounter;
    private EditText etSearchFaculty;
    private ImageButton ibClearSearch;
    private TextView btnSelectAll;
    private TextView btnClearSelection;
    private TextView tvTotalFacultyCount;
    private RecyclerView rvBroadcastFaculty;
    private View layoutEmptyBroadcastList;
    private ExtendedFloatingActionButton fabProceedCompose;

    private BroadcastFacultyAdapter adapter;
    private final List<FacultyUser> facultyList = new ArrayList<>();
    private final List<FacultyUser> selectedFaculty = new ArrayList<>();

    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_select);

        initViews();
        loadCurrentUserInfo();
        setupRecyclerView();
        setupSearchAndActions();
    }

    private void initViews() {
        ibBackBroadcastSelect = findViewById(R.id.ibBackBroadcastSelect);
        tvSelectedCounter = findViewById(R.id.tvSelectedCounter);
        etSearchFaculty = findViewById(R.id.etSearchFaculty);
        ibClearSearch = findViewById(R.id.ibClearSearch);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnClearSelection = findViewById(R.id.btnClearSelection);
        tvTotalFacultyCount = findViewById(R.id.tvTotalFacultyCount);
        rvBroadcastFaculty = findViewById(R.id.rvBroadcastFaculty);
        layoutEmptyBroadcastList = findViewById(R.id.layoutEmptyBroadcastList);
        fabProceedCompose = findViewById(R.id.fabProceedCompose);

        if (ibBackBroadcastSelect != null) {
            ibBackBroadcastSelect.setOnClickListener(v -> finish());
        }
    }

    private void loadCurrentUserInfo() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null && fbUser.getDisplayName() != null) {
            currentName = fbUser.getDisplayName().trim();
        }

        FacultyUser sessionUser = AuthManager.getInstance(this).getCurrentUser();
        if (sessionUser != null) {
            if (sessionUser.getUserId() != null) currentUserId = sessionUser.getUserId().trim();
            if (sessionUser.getName() != null) currentName = sessionUser.getName().trim();

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(sessionUser);
            if (canonical != null) {
                currentCanonicalId = canonical.getUserId();
                currentName = canonical.getName();
            }
        }

        if (currentCanonicalId.isEmpty() && !currentName.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentName);
        }
    }

    private void setupRecyclerView() {
        facultyList.clear();
        facultyList.addAll(FacultyDirectory.getAllFacultyContacts());

        adapter = new BroadcastFacultyAdapter(
                this,
                facultyList,
                currentUserId,
                currentCanonicalId,
                this
        );

        rvBroadcastFaculty.setLayoutManager(new LinearLayoutManager(this));
        rvBroadcastFaculty.setAdapter(adapter);

        tvTotalFacultyCount.setText(adapter.getTotalCount() + " Contacts");
        updateCounter(0);
    }

    private void setupSearchAndActions() {
        etSearchFaculty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                if (ibClearSearch != null) {
                    ibClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }
                if (adapter != null) {
                    adapter.filter(query);
                    if (layoutEmptyBroadcastList != null) {
                        layoutEmptyBroadcastList.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (ibClearSearch != null) {
            ibClearSearch.setOnClickListener(v -> etSearchFaculty.setText(""));
        }

        if (btnSelectAll != null) {
            btnSelectAll.setOnClickListener(v -> {
                if (adapter != null) adapter.selectAll();
            });
        }

        if (btnClearSelection != null) {
            btnClearSelection.setOnClickListener(v -> {
                if (adapter != null) adapter.clearSelection();
            });
        }

        if (fabProceedCompose != null) {
            fabProceedCompose.setOnClickListener(v -> {
                if (selectedFaculty.isEmpty()) {
                    Toast.makeText(this, "Please select at least one faculty member", Toast.LENGTH_SHORT).show();
                    return;
                }

                ArrayList<String> selectedIds = new ArrayList<>();
                ArrayList<String> selectedNames = new ArrayList<>();

                for (FacultyUser f : selectedFaculty) {
                    selectedIds.add(f.getUserId());
                    selectedNames.add(f.getName());
                }

                Intent intent = new Intent(this, BroadcastComposeActivity.class);
                intent.putStringArrayListExtra("EXTRA_SELECTED_IDS", selectedIds);
                intent.putStringArrayListExtra("EXTRA_SELECTED_NAMES", selectedNames);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onSelectionChanged(int selectedCount, List<FacultyUser> selectedList) {
        this.selectedFaculty.clear();
        if (selectedList != null) {
            this.selectedFaculty.addAll(selectedList);
        }
        updateCounter(selectedCount);
    }

    private void updateCounter(int count) {
        if (tvSelectedCounter != null) {
            tvSelectedCounter.setText(count + " selected");
        }
        if (fabProceedCompose != null) {
            fabProceedCompose.setText("Next (" + count + ")");
            if (count > 0) {
                fabProceedCompose.show();
            }
        }
    }
}
