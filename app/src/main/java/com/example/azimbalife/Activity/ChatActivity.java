package com.example.azimbalife.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.MessageAdapter;
import com.example.azimbalife.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.*;

public class ChatActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ChatActivity";
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;

    private EditText editTextMessage;
    private ImageView sendButton;
    private ImageView voiceButton;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private ArrayList<MessageModel> messageList;
    private OkHttpClient client;

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private DatabaseReference chatRef;
    private final String userId = "guest";

    private boolean waitingForSymptomInput = false;
    private boolean historyLoaded = false;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.blue));
        }

        toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        editTextMessage = findViewById(R.id.editTextMessage);
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton); // Voice button

        recyclerView = findViewById(R.id.recyclerViewMessages);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        client = new OkHttpClient();
        chatRef = FirebaseDatabase.getInstance().getReference("ChatHistory").child(userId).child("messages");

        // Show initial welcome options ON START (no history loading)
        showInitialOptions();

        sendButton.setOnClickListener(view -> {
            String userMessage = editTextMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(userMessage)) {
                addMessage(userMessage, true);
                saveMessageToFirebase(userMessage, true);
                processUserInput(userMessage);
                editTextMessage.setText("");
            }
        });

        voiceButton.setOnClickListener(view -> startVoiceInput());
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Speech input not supported on your device", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Speech input error: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spokenText = results.get(0);
                    editTextMessage.setText(spokenText);
                    // Automatically send the message
                    sendButton.performClick();
                }
            }
        }
    }


    private void loadChatHistory() {
        if (historyLoaded) {
            Log.d(TAG, "History already loaded. Skipping.");
            return;
        }
        historyLoaded = true;

        Log.d(TAG, "Loading chat history from Firebase...");
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        MessageModel message = messageSnapshot.getValue(MessageModel.class);
                        if (message != null) {
                            messageList.add(message);
                        }
                    }
                    if (messageList.isEmpty()) {
                        addMessage("No chats yet.", false);
                    }
                } else {
                    addMessage("No chats yet.", false);
                }
                messageAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
                Log.d(TAG, "Chat history loaded: " + messageList.size() + " messages.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat history: " + error.getMessage());
                addMessage("Failed to load chat history.", false);
            }
        });
    }

    private void saveMessageToFirebase(String messageText, boolean isUser) {
        String key = chatRef.push().getKey();
        if (key != null) {
            MessageModel message = new MessageModel(messageText, isUser);
            chatRef.child(key).setValue(message);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        drawerLayout.closeDrawer(GravityCompat.START);

        if (id == R.id.nav_history) {
            Log.d(TAG, "Drawer nav_history clicked.");
            messageList.clear();
            messageAdapter.notifyDataSetChanged();
            historyLoaded = false;
            loadChatHistory();
            return true;
        } else if (id == R.id.nav_logout) {
            Log.d(TAG, "Drawer nav_logout clicked.");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else {
            Log.w(TAG, "Unknown drawer item clicked: " + id);
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void showInitialOptions() {
        messageList.clear();
        messageAdapter.notifyDataSetChanged();

        String options = "Welcome to AzimbaLife 👋\nPlease select an option:\n\n" +
                "1. Medical Research\n" +
                "2. Do you have signs or symptoms?\n" +
                "3. Do you want to see the doctor?\n" +
                "4. Help\n" +
                "5. Exit";
        addMessage(options, false);
        saveMessageToFirebase(options, false);
    }

    private void addMessage(String message, boolean isUser) {
        messageList.add(new MessageModel(message, isUser));
        runOnUiThread(() -> {
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
        });
    }

    private void processUserInput(String input) {
        if (waitingForSymptomInput) {
            waitingForSymptomInput = false;
            sendMessageToBackend(input);
            return;
        }

        switch (input.trim()) {
            case "1":
                addMessage("Medical Research is under development.", false);
                saveMessageToFirebase("Medical Research is under development.", false);
                break;
            case "2":
                waitingForSymptomInput = true;
                String prompt = "Please describe how you're feeling (e.g. chest pain, fatigue).";
                addMessage(prompt, false);
                saveMessageToFirebase(prompt, false);
                break;
            case "3":
                String connecting = "Connecting you to available doctors near you...";
                addMessage(connecting, false);
                saveMessageToFirebase(connecting, false);
                startActivity(new Intent(ChatActivity.this, AllDoctorsActivity.class));
                break;
            case "4":
                String helpMsg = "Type the number of the service you want. Example: Type '2' to report symptoms.";
                addMessage(helpMsg, false);
                saveMessageToFirebase(helpMsg, false);
                break;
            case "5":
                String goodbye = "Goodbye! Stay healthy.";
                addMessage(goodbye, false);
                saveMessageToFirebase(goodbye, false);
                new android.os.Handler().postDelayed(() -> {
                    startActivity(new Intent(ChatActivity.this, MainActivity.class));
                    finish();
                }, 1500);
                break;
            default:
                String invalid = "Invalid input. Please enter 1 - 5 to continue.";
                addMessage(invalid, false);
                saveMessageToFirebase(invalid, false);
        }
    }

    private void sendMessageToBackend(String userMessage) {
        JSONObject json = new JSONObject();
        try {
            json.put("message", userMessage);
        } catch (JSONException e) {
            addMessage("JSON Error: " + e.getMessage(), false);
            saveMessageToFirebase("JSON Error: " + e.getMessage(), false);
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://192.168.43.13:8080/predict")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String userFriendlyError = "Error: Server currently down. Please try again later.";
                addMessage(userFriendlyError, false);
                saveMessageToFirebase(userFriendlyError, false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    addMessage("Server Error: " + response.message(), false);
                    saveMessageToFirebase("Server Error: " + response.message(), false);
                    return;
                }

                String resStr = response.body() != null ? response.body().string() : null;
                if (resStr == null) {
                    addMessage("Empty response from AI", false);
                    saveMessageToFirebase("Empty response from AI", false);
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(resStr);
                    StringBuilder resultsText = new StringBuilder();

                    if (jsonResponse.has("message")) {
                        resultsText.append(jsonResponse.getString("message")).append("\n\n");
                    }

                    if (jsonResponse.has("results")) {
                        JSONArray results = jsonResponse.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String disease = obj.getString("disease");
                            double confidence = obj.optDouble("confidence", 0.0);
                            resultsText.append(String.format(Locale.US, "- %s (Confidence: %.1f%%)\n", disease, confidence));
                        }
                    }

                    if (jsonResponse.has("note")) {
                        resultsText.append("\n").append(jsonResponse.getString("note"));
                    }

                    String aiResponse = resultsText.toString().trim();
                    addMessage(aiResponse, false);
                    saveMessageToFirebase(aiResponse, false);

                } catch (JSONException e) {
                    addMessage("Invalid AI response: " + e.getMessage(), false);
                    saveMessageToFirebase("Invalid AI response: " + e.getMessage(), false);
                }
            }
        });
    }
}
