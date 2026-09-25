package com.example.deptflow.communication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.auth.AuthManager;
import com.example.deptflow.communication.adapters.FacultyChatAdapter;
import com.example.deptflow.communication.models.FacultyConversation;
import com.example.deptflow.communication.models.FacultyDirectory;
import com.example.deptflow.communication.services.FcmTokenManager;
import com.example.deptflow.feature.faculty.models.FacultyUser;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * FacultyListActivity — WhatsApp-style messaging inbox with:
 * 1. "All Chats" section: Active conversations sorted by latest message & unread badge.
 * 2. "Contacts" section: Complete 23 canonical department faculty members from HOD module.
 */
public class FacultyListActivity extends AppCompatActivity
        implements FacultyChatAdapter.OnFacultyChatClickListener {

    private static final String TAG = "FacultyListActivity";

    private enum TabMode {
        ALL_CHATS, CONTACTS
    }

    private RecyclerView rvFacultyList;
    private ProgressBar pbLoadingFaculty;
    private View layoutEmptyFaculty;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private TextView tvStatus;
    private ImageButton ibBackFacultyList;
    private EditText etSearchFaculty;
    private TextView tabAllChats;
    private TextView tabContacts;

    private FacultyChatAdapter adapter;
    private final List<FacultyConversation> activeChatsList = new ArrayList<>();
    private final List<FacultyConversation> contactsList = new ArrayList<>();
    private final Map<String, FacultyConversation> allFacultyMap = new HashMap<>(); // key: canonical ID, name (lowercase), UID

    private TabMode currentTab = TabMode.ALL_CHATS;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration chatsListener;

    private String currentUid = "";
    private String currentUserId = "";
    private String currentCanonicalId = "";
    private String currentUserName = "";
    private final Set<String> myIdentifiers = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_list);

        initViews();
        loadCurrentUserInfo();
        setupRecyclerView();
        setupTabs();

        if (currentUid.isEmpty() && currentUserId.isEmpty() && currentCanonicalId.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Register FCM Push token for this device
        FcmTokenManager.registerDeviceToken(this);

        initCanonicalFacultyContacts();
        listenToChats();
    }

    private void initViews() {
        rvFacultyList = findViewById(R.id.rvFacultyList);
        pbLoadingFaculty = findViewById(R.id.pbLoadingFaculty);
        layoutEmptyFaculty = findViewById(R.id.layoutEmptyFaculty);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        tvStatus = findViewById(R.id.tvStatus);
        ibBackFacultyList = findViewById(R.id.ibBackFacultyList);
        etSearchFaculty = findViewById(R.id.etSearchFaculty);
        tabAllChats = findViewById(R.id.tabAllChats);
        tabContacts = findViewById(R.id.tabContacts);

        if (ibBackFacultyList != null) {
            ibBackFacultyList.setOnClickListener(v -> finish());
        }

        if (etSearchFaculty != null) {
            etSearchFaculty.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (adapter != null) {
                        adapter.filter(s != null ? s.toString() : "");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupTabs() {
        tabAllChats.setOnClickListener(v -> switchTab(TabMode.ALL_CHATS));
        tabContacts.setOnClickListener(v -> switchTab(TabMode.CONTACTS));
        updateTabStyles();
    }

    private void switchTab(TabMode mode) {
        currentTab = mode;
        updateTabStyles();
        if (etSearchFaculty != null) {
            etSearchFaculty.setText("");
        }
        displayCurrentTabContent();
    }

    private void updateTabStyles() {
        if (currentTab == TabMode.ALL_CHATS) {
            tabAllChats.setBackgroundResource(R.drawable.bg_chip_selected);
            tabAllChats.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
            tabContacts.setBackgroundResource(R.drawable.bg_chip_unselected);
            tabContacts.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            if (adapter != null) adapter.setContactsMode(false);
        } else {
            tabContacts.setBackgroundResource(R.drawable.bg_chip_selected);
            tabContacts.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary));
            tabAllChats.setBackgroundResource(R.drawable.bg_chip_unselected);
            tabAllChats.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            if (adapter != null) adapter.setContactsMode(true);
        }
    }

    private void loadCurrentUserInfo() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        myIdentifiers.clear();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            addIdentifier(currentUid);
            if (firebaseUser.getEmail() != null) {
                addIdentifier(firebaseUser.getEmail());
            }
            if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                currentUserName = firebaseUser.getDisplayName().trim();
                addIdentifier(currentUserName);
            }
        }

        FacultyUser cached = AuthManager.getInstance(this).getCurrentUser();
        if (cached != null) {
            if (cached.getUserId() != null && !cached.getUserId().trim().isEmpty()) {
                currentUserId = cached.getUserId().trim();
                addIdentifier(currentUserId);
            }
            if (cached.getName() != null && !cached.getName().trim().isEmpty()) {
                currentUserName = cached.getName().trim();
                addIdentifier(currentUserName);
            }
            if (cached.getEmail() != null && !cached.getEmail().trim().isEmpty()) {
                addIdentifier(cached.getEmail());
            }

            FacultyUser canonical = FacultyDirectory.resolveCanonicalFaculty(cached);
            if (canonical != null) {
                currentCanonicalId = canonical.getUserId();
                addIdentifier(currentCanonicalId);
                addIdentifier(canonical.getName());
                addIdentifier(canonical.getEmail());
            }
        }

        if (currentCanonicalId.isEmpty() && !currentUserName.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserName);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }

        if (currentCanonicalId.isEmpty() && !currentUserId.isEmpty()) {
            currentCanonicalId = FacultyDirectory.resolveCanonicalId(currentUserId);
            if (!currentCanonicalId.isEmpty()) addIdentifier(currentCanonicalId);
        }

        if (currentCanonicalId.isEmpty() && !currentUid.isEmpty()) {
            currentCanonicalId = currentUid;
            addIdentifier(currentCanonicalId);
        }

        Log.d(TAG, "Current logged-in faculty: UID=" + currentUid
                + ", FacultyId=" + currentUserId
                + ", CanonicalId=" + currentCanonicalId
                + ", Name=" + currentUserName
                + ", Identifiers=" + myIdentifiers);
    }

    private void addIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return;
        String trimmed = val.trim();
        myIdentifiers.add(trimmed.toLowerCase(Locale.ROOT));
        myIdentifiers.add(trimmed);
        String clean = trimmed.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (!clean.isEmpty()) {
            myIdentifiers.add(clean);
        }
    }

    private void setupRecyclerView() {
        adapter = new FacultyChatAdapter(this, this);
        rvFacultyList.setLayoutManager(new LinearLayoutManager(this));
        rvFacultyList.setAdapter(adapter);
    }

    /**
     * Initializes the complete canonical 23 faculty members list from HOD module.
     */
    private void initCanonicalFacultyContacts() {
        contactsList.clear();
        allFacultyMap.clear();

        List<FacultyUser> allFaculty = FacultyDirectory.getAllFacultyContacts();

        for (FacultyUser faculty : allFaculty) {
            String fId = faculty.getUserId();
            String fName = faculty.getName();

            // Symmetrical deterministic Chat ID
            String chatId = FacultyDirectory.getDeterministicChatId(currentCanonicalId, fId);
            FacultyConversation conv = new FacultyConversation(faculty, chatId);

            allFacultyMap.put(fId.toLowerCase(Locale.ROOT), conv);
            allFacultyMap.put(fName.toLowerCase(Locale.ROOT), conv);

            // Don't show currently logged in faculty as a contact to chat with themselves
            boolean isSelf = fId.equalsIgnoreCase(currentCanonicalId)
                    || fName.equalsIgnoreCase(currentUserName)
                    || isMyIdentifier(fId)
                    || isMyIdentifier(fName);

            if (!isSelf) {
                contactsList.add(conv);
            }
        }
    }

    private void listenToChats() {
        if (chatsListener != null) {
            chatsListener.remove();
        }

        pbLoadingFaculty.setVisibility(View.VISIBLE);

        chatsListener = db.collection("chats")
                .addSnapshotListener((snapshots, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    pbLoadingFaculty.setVisibility(View.GONE);

                    if (error != null) {
                        String code = "UNKNOWN";
                        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                            code = error.getCode().name();
                        }
                        Log.e(TAG, "Operation failed. code=" + code + " message=" + error.getMessage(), error);
                        displayCurrentTabContent();
                        return;
                    }

                    if (snapshots != null) {
                        Log.d(TAG, "Chats snapshot received with " + snapshots.size() + " total conversations in Firestore.");
                        for (DocumentSnapshot chatDoc : snapshots.getDocuments()) {
                            String chatId = chatDoc.getId();
                            List<?> participants = (List<?>) chatDoc.get("participants");
                            List<?> participantIds = (List<?>) chatDoc.get("participantIds");
                            List<?> participantUids = (List<?>) chatDoc.get("participantUids");
                            List<?> participantNames = (List<?>) chatDoc.get("participantNames");
                            String faculty1Id = getString(chatDoc, "faculty1Id");
                            String faculty2Id = getString(chatDoc, "faculty2Id");
                            String senderId = getString(chatDoc, "senderId");
                            String receiverId = getString(chatDoc, "receiverId");
                            String recipientId = getString(chatDoc, "recipientId");

                            boolean isParticipant = false;
                            String otherPartyIdentifier = null;

                            // 1. Check chatId parts (e.g. FAC-101_FAC-104)
                            if (chatId.contains("_")) {
                                String[] parts = chatId.split("_");
                                if (parts.length == 2) {
                                    if (isMyIdentifier(parts[0])) {
                                        isParticipant = true;
                                        otherPartyIdentifier = parts[1];
                                    } else if (isMyIdentifier(parts[1])) {
                                        isParticipant = true;
                                        otherPartyIdentifier = parts[0];
                                    }
                                }
                            }

                            // 2. Check participantIds array
                            if (participantIds != null) {
                                for (Object p : participantIds) {
                                    if (p != null) {
                                        String pStr = p.toString().trim();
                                        if (isMyIdentifier(pStr)) {
                                            isParticipant = true;
                                        } else if (otherPartyIdentifier == null) {
                                            otherPartyIdentifier = pStr;
                                        }
                                    }
                                }
                            }

                            // 3. Check participants array
                            if (participants != null) {
                                for (Object p : participants) {
                                    if (p != null) {
                                        String pStr = p.toString().trim();
                                        if (isMyIdentifier(pStr)) {
                                            isParticipant = true;
                                        } else if (otherPartyIdentifier == null) {
                                            otherPartyIdentifier = pStr;
                                        }
                                    }
                                }
                            }

                            // 4. Check explicit paired ID fields
                            if (!faculty1Id.isEmpty() || !faculty2Id.isEmpty()) {
                                if (isMyIdentifier(faculty1Id)) {
                                    isParticipant = true;
                                    if (!faculty2Id.isEmpty()) otherPartyIdentifier = faculty2Id;
                                } else if (isMyIdentifier(faculty2Id)) {
                                    isParticipant = true;
                                    if (!faculty1Id.isEmpty()) otherPartyIdentifier = faculty1Id;
                                }
                            }

                            // 5. Check sender/receiver
                            if (isMyIdentifier(senderId)) {
                                isParticipant = true;
                                if (!receiverId.isEmpty()) otherPartyIdentifier = receiverId;
                                else if (!recipientId.isEmpty()) otherPartyIdentifier = recipientId;
                            } else if (isMyIdentifier(receiverId) || isMyIdentifier(recipientId)) {
                                isParticipant = true;
                                if (!senderId.isEmpty()) otherPartyIdentifier = senderId;
                            }

                            // 6. Check participantUids array
                            if (participantUids != null) {
                                for (Object u : participantUids) {
                                    if (u != null) {
                                        String uStr = u.toString().trim();
                                        if (isMyIdentifier(uStr)) {
                                            isParticipant = true;
                                        } else if (otherPartyIdentifier == null) {
                                            otherPartyIdentifier = uStr;
                                        }
                                    }
                                }
                            }

                            // 7. Check unread count fields for self
                            int unread = extractUnreadCount(chatDoc);
                            if (unread > 0) {
                                isParticipant = true;
                            }

                            if (isParticipant) {
                                String lastMsg = getFirstNonEmptyString(chatDoc, "lastMessage", "last_message", "message", "latestMessage");
                                long ts = getTimestamp(chatDoc, "lastMessageTime");
                                if (ts == 0) ts = getTimestamp(chatDoc, "lastTimestamp");
                                if (ts == 0) ts = getTimestamp(chatDoc, "timestamp");
                                if (ts == 0) ts = getTimestamp(chatDoc, "lastMessageTimestamp");
                                if (ts == 0) ts = getTimestamp(chatDoc, "updatedAt");

                                String lastSenderId = getFirstNonEmptyString(chatDoc, "lastSenderId", "senderId", "faculty1Id");
                                String lastSenderName = getFirstNonEmptyString(chatDoc, "lastSenderName", "senderName", "faculty1Name");

                                // If other party identifier is still missing, fallback to lastSenderName if not mine
                                if (otherPartyIdentifier == null || isMyIdentifier(otherPartyIdentifier)) {
                                    if (!lastSenderId.isEmpty() && !isMyIdentifier(lastSenderId)) {
                                        otherPartyIdentifier = lastSenderId;
                                    } else if (!lastSenderName.isEmpty() && !isMyIdentifier(lastSenderName)) {
                                        otherPartyIdentifier = lastSenderName;
                                    }
                                }

                                // Resolve target FacultyUser
                                FacultyUser resolvedFaculty = null;
                                if (otherPartyIdentifier != null && !otherPartyIdentifier.trim().isEmpty()) {
                                    resolvedFaculty = FacultyDirectory.resolveByNameOrId(otherPartyIdentifier);
                                }
                                if (resolvedFaculty == null && !lastSenderName.isEmpty() && !isMyIdentifier(lastSenderName)) {
                                    resolvedFaculty = FacultyDirectory.resolveByNameOrId(lastSenderName);
                                }
                                if (resolvedFaculty == null && otherPartyIdentifier != null && !otherPartyIdentifier.trim().isEmpty()) {
                                    // Dynamically build user profile for non-directory faculty or HOD
                                    String fallbackName = FacultyDirectory.resolveCanonicalName(otherPartyIdentifier);
                                    resolvedFaculty = new FacultyUser(otherPartyIdentifier, fallbackName, "", "Information Technology", "FACULTY");
                                }

                                if (resolvedFaculty != null) {
                                    String targetId = resolvedFaculty.getUserId();
                                    String targetName = resolvedFaculty.getName();

                                    // Don't show self conversation
                                    if (isMyIdentifier(targetId) || isMyIdentifier(targetName)) {
                                        continue;
                                    }

                                    FacultyConversation targetConv = allFacultyMap.get(targetId.toLowerCase(Locale.ROOT));
                                    if (targetConv == null) {
                                        targetConv = allFacultyMap.get(targetName.toLowerCase(Locale.ROOT));
                                    }
                                    if (targetConv == null) {
                                        targetConv = new FacultyConversation(resolvedFaculty, chatId);
                                        allFacultyMap.put(targetId.toLowerCase(Locale.ROOT), targetConv);
                                        allFacultyMap.put(targetName.toLowerCase(Locale.ROOT), targetConv);
                                    }

                                    boolean isMine = isMyIdentifier(lastSenderId) || isMyIdentifier(lastSenderName);

                                    targetConv.setChatId(chatId);
                                    targetConv.setLastMessage(lastMsg);
                                    targetConv.setLastMessageTimestamp(ts);
                                    targetConv.setLastSenderId(lastSenderId);
                                    targetConv.setLastSenderName(lastSenderName);
                                    targetConv.setLastMessageMine(isMine);
                                    targetConv.setUnreadCount(unread);

                                    Log.d(TAG, "Detected conversation partner: " + targetName
                                            + " (ID: " + targetId + ")"
                                            + " | ChatId: " + chatId
                                            + " | Latest message: '" + lastMsg + "'"
                                            + " | Timestamp: " + ts
                                            + " | IsMine: " + isMine
                                            + " | Unread count: " + unread);
                                }
                            }
                        }
                    }

                    displayCurrentTabContent();
                });
    }

    private boolean isMyIdentifier(String val) {
        if (val == null || val.trim().isEmpty()) return false;
        String raw = val.trim();
        String normal = raw.toLowerCase(Locale.ROOT);
        String clean = normal.replaceAll("[^a-zA-Z0-9]", "");

        if (myIdentifiers.contains(normal) || myIdentifiers.contains(raw)) {
            return true;
        }
        if (!clean.isEmpty() && myIdentifiers.contains(clean)) {
            return true;
        }

        if (!currentCanonicalId.isEmpty() && (normal.equalsIgnoreCase(currentCanonicalId) || clean.equalsIgnoreCase(currentCanonicalId.replaceAll("[^a-zA-Z0-9]", "")))) {
            return true;
        }
        if (!currentUserId.isEmpty() && (normal.equalsIgnoreCase(currentUserId) || clean.equalsIgnoreCase(currentUserId.replaceAll("[^a-zA-Z0-9]", "")))) {
            return true;
        }
        if (!currentUid.isEmpty() && normal.equalsIgnoreCase(currentUid)) {
            return true;
        }
        if (!currentUserName.isEmpty() && (normal.equalsIgnoreCase(currentUserName) || clean.equalsIgnoreCase(currentUserName.replaceAll("[^a-zA-Z0-9]", "")))) {
            return true;
        }

        return false;
    }

    private int extractUnreadCount(DocumentSnapshot chatDoc) {
        List<String> candidateKeys = new ArrayList<>();
        if (!currentCanonicalId.isEmpty()) candidateKeys.add("unreadCount_" + currentCanonicalId);
        if (!currentUserId.isEmpty()) candidateKeys.add("unreadCount_" + currentUserId);
        if (!currentUid.isEmpty()) candidateKeys.add("unreadCount_" + currentUid);
        if (!currentUserName.isEmpty()) candidateKeys.add("unreadCount_" + currentUserName.toLowerCase(Locale.ROOT));

        for (String key : candidateKeys) {
            Object val = chatDoc.get(key);
            int count = parseNumber(val);
            if (count > 0) return count;
        }

        Object unreadCountsObj = chatDoc.get("unreadCounts");
        if (unreadCountsObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) unreadCountsObj;
            for (String key : candidateKeys) {
                String subKey = key.replace("unreadCount_", "");
                if (map.containsKey(subKey)) {
                    int count = parseNumber(map.get(subKey));
                    if (count > 0) return count;
                }
            }
        }

        return 0;
    }

    private int parseNumber(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt(((String) val).trim());
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private void displayCurrentTabContent() {
        if (currentTab == TabMode.ALL_CHATS) {
            // Collect all unique conversations that have a last message, timestamp, or unread messages
            Map<String, FacultyConversation> unique = new HashMap<>();
            for (FacultyConversation c : allFacultyMap.values()) {
                if (c != null && c.getFaculty() != null) {
                    String fId = c.getFaculty().getUserId();
                    String fName = c.getFaculty().getName();
                    String key = (fId != null && !fId.isEmpty()) ? fId.toLowerCase(Locale.ROOT) : fName.toLowerCase(Locale.ROOT);

                    // Skip self
                    if (isMyIdentifier(fId) || isMyIdentifier(fName)) continue;

                    boolean hasMessage = (c.getLastMessage() != null && !c.getLastMessage().trim().isEmpty());
                    boolean hasTimestamp = (c.getLastMessageTimestamp() > 0);
                    boolean hasUnread = (c.getUnreadCount() > 0);

                    if (!unique.containsKey(key) && (hasMessage || hasTimestamp || hasUnread)) {
                        unique.put(key, c);
                    }
                }
            }

            activeChatsList.clear();
            activeChatsList.addAll(unique.values());

            // WhatsApp-style sorting: conversations sorted strictly by latest message timestamp descending
            Collections.sort(activeChatsList, (a, b) ->
                    Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp())
            );

            adapter.updateList(activeChatsList);

            if (activeChatsList.isEmpty()) {
                layoutEmptyFaculty.setVisibility(View.VISIBLE);
                rvFacultyList.setVisibility(View.GONE);
                tvEmptyTitle.setText("No Conversations Yet");
                tvEmptySubtitle.setText("Tap 'Contacts (23)' above to start a private conversation with any faculty member.");
                tvStatus.setText("Inbox is empty");
            } else {
                layoutEmptyFaculty.setVisibility(View.GONE);
                rvFacultyList.setVisibility(View.VISIBLE);
                tvStatus.setText(activeChatsList.size() + " Active Conversation" + (activeChatsList.size() > 1 ? "s" : ""));
            }

            Log.d(TAG, "All Chats updated: " + activeChatsList.size() + " conversation(s) displayed.");

        } else {
            // Contacts tab: Display full 23 canonical faculty list (excluding self)
            Map<String, FacultyConversation> unique = new HashMap<>();
            for (FacultyConversation c : contactsList) {
                if (c != null && c.getFaculty() != null) {
                    String fId = c.getFaculty().getUserId();
                    if (!unique.containsKey(fId)) {
                        unique.put(fId, c);
                    }
                }
            }

            List<FacultyConversation> sortedContacts = new ArrayList<>(unique.values());
            Collections.sort(sortedContacts, (a, b) -> {
                String nameA = a.getFaculty() != null ? a.getFaculty().getName() : "";
                String nameB = b.getFaculty() != null ? b.getFaculty().getName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });

            adapter.updateList(sortedContacts);

            layoutEmptyFaculty.setVisibility(View.GONE);
            rvFacultyList.setVisibility(View.VISIBLE);
            tvStatus.setText(sortedContacts.size() + " Department Faculty Members");
        }
    }

    private String getFirstNonEmptyString(DocumentSnapshot doc, String... fields) {
        for (String field : fields) {
            String val = getString(doc, field);
            if (!val.isEmpty()) return val;
        }
        return "";
    }

    private String getString(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        return val != null ? val.toString().trim() : "";
    }

    private long getTimestamp(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        if (val instanceof Timestamp) {
            return ((Timestamp) val).toDate().getTime();
        }
        if (val instanceof Long) {
            return (Long) val;
        }
        if (val instanceof Double) {
            return ((Double) val).longValue();
        }
        return 0L;
    }

    @Override
    public void onFacultyChatClick(FacultyConversation conversation) {
        if (conversation == null || conversation.getFaculty() == null) return;

        FacultyUser faculty = conversation.getFaculty();
        String targetFacultyId = faculty.getUserId();
        String targetFacultyName = faculty.getName();
        String chatId = FacultyDirectory.getDeterministicChatId(currentCanonicalId, targetFacultyId);

        Log.d(TAG, "Opening conversation with " + targetFacultyName + " (ID=" + targetFacultyId + ", ChatId=" + chatId + ")");

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("facultyName", targetFacultyName);
        intent.putExtra("facultyId", targetFacultyId);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && etSearchFaculty != null) {
            adapter.filter(etSearchFaculty.getText().toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) chatsListener.remove();
    }
}