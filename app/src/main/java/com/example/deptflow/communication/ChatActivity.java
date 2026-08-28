package com.example.deptflow.communication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deptflow.R;
import com.example.deptflow.adapters.ChatAdapter;
import com.example.deptflow.models.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText etMessage;
    private Button btnSend;

    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        TextView tvChatName = findViewById(R.id.tvChatName);
        TextView tvChatDepartment = findViewById(R.id.tvChatDepartment);

        recyclerViewMessages =
                findViewById(R.id.recyclerViewMessages);

        etMessage =
                findViewById(R.id.etMessage);

        btnSend =
                findViewById(R.id.btnSend);

        // Get faculty details from FacultyListActivity
        String facultyName =
                getIntent().getStringExtra("facultyName");

        String department =
                getIntent().getStringExtra("department");

        if (facultyName != null) {
            tvChatName.setText(facultyName);
        }

        if (department != null) {
            tvChatDepartment.setText(department);
        }

        // RecyclerView
        recyclerViewMessages.setLayoutManager(
                new LinearLayoutManager(this)
        );

        messageList = new ArrayList<>();

        // Sample messages
        messageList.add(new Message(
                "M001",
                "F002",
                "F001",
                "Hi, can you send me the result analysis?",
                System.currentTimeMillis()
        ));

        messageList.add(new Message(
                "M002",
                "F001",
                "F002",
                "Sure, I will send it shortly.",
                System.currentTimeMillis()
        ));

        chatAdapter = new ChatAdapter(messageList);

        recyclerViewMessages.setAdapter(chatAdapter);

        // Send button
        btnSend.setOnClickListener(v -> {

            String text =
                    etMessage.getText().toString().trim();

            if (!text.isEmpty()) {

                Message newMessage = new Message(
                        "M" + System.currentTimeMillis(),
                        "F001",
                        "F002",
                        text,
                        System.currentTimeMillis()
                );

                messageList.add(newMessage);

                chatAdapter.notifyItemInserted(
                        messageList.size() - 1
                );

                recyclerViewMessages.scrollToPosition(
                        messageList.size() - 1
                );

                etMessage.setText("");
            }
        });
    }
}