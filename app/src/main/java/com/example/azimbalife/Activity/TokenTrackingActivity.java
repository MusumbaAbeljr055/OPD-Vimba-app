package com.example.azimbalife.Activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.StageAdapter;
import com.example.azimbalife.Domain.TokenTracking;
import com.example.azimbalife.R;
import com.example.azimbalife.databinding.ActivityTokenTrackingBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TokenTrackingActivity extends AppCompatActivity {

    private ActivityTokenTrackingBinding binding;
    private String username;
    private String currentTokenId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTokenTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get data from intent
        username = getIntent().getStringExtra("username");
        currentTokenId = getIntent().getStringExtra("tokenId");

        Log.d("TOKEN_TRACKING", "Starting TokenTrackingActivity");
        Log.d("TOKEN_TRACKING", "Username: " + username);
        Log.d("TOKEN_TRACKING", "Token ID: " + currentTokenId);

        if (currentTokenId == null || currentTokenId.isEmpty()) {
            Toast.makeText(this, "Invalid token ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViews();
        loadTokenTracking();
    }

    private void setupViews() {
        binding.tvTokenId.setText("Token: " + currentTokenId);
        binding.tvPatientName.setText("Patient: " + (username != null ? username : "Unknown"));

        binding.btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            loadTokenTracking();
        });

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void loadTokenTracking() {
        Log.d("TOKEN_TRACKING", "Loading token data for: " + currentTokenId);

        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/TokenTracking")
                .child(currentTokenId);

        tokenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("TOKEN_TRACKING", "Firebase data received: " + snapshot.exists());

                if (snapshot.exists()) {
                    try {
                        TokenTracking tokenTracking = snapshot.getValue(TokenTracking.class);
                        if (tokenTracking != null) {
                            Log.d("TOKEN_TRACKING", "Token data loaded successfully");
                            updateTokenUI(tokenTracking);
                        } else {
                            Log.e("TOKEN_TRACKING", "Failed to parse token data");
                            Toast.makeText(TokenTrackingActivity.this, "Failed to load token data", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("TOKEN_TRACKING", "Error parsing token data: " + e.getMessage());
                        Toast.makeText(TokenTrackingActivity.this, "Error loading token data", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("TOKEN_TRACKING", "Token not found in database");
                    Toast.makeText(TokenTrackingActivity.this, "Token not found: " + currentTokenId, Toast.LENGTH_LONG).show();
                    binding.tvNoTokenFound.setVisibility(TextView.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TOKEN_TRACKING", "Firebase error: " + error.getMessage());
                Toast.makeText(TokenTrackingActivity.this, "Error loading token data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTokenUI(TokenTracking tokenTracking) {
        try {
            // Update basic info
            binding.tvTokenId.setText("Token: " + (tokenTracking.getTokenId() != null ? tokenTracking.getTokenId() : "Unknown"));
            binding.tvPatientName.setText("Patient: " + (username != null ? username : "Unknown"));
            binding.tvDoctor.setText("Doctor: " + (tokenTracking.getDoctor() != null ? tokenTracking.getDoctor() : "Not assigned"));
            binding.tvSpecialty.setText("Specialty: " + (tokenTracking.getSpecialty() != null ? tokenTracking.getSpecialty() : "General"));
            binding.tvCurrentStage.setText("Current Stage: " + getStageDisplayName(tokenTracking.getCurrentStage()));
            binding.tvOverallStatus.setText("Status: " + getStatusDisplayName(tokenTracking.getOverallStatus()));

            // NEW: Update queue information
            updateQueueUI(tokenTracking);

            // Setup stages RecyclerView
            if (tokenTracking.getStages() != null) {
                StageAdapter stageAdapter = new StageAdapter(tokenTracking.getStages());
                binding.stagesRecycler.setLayoutManager(new LinearLayoutManager(this));
                binding.stagesRecycler.setAdapter(stageAdapter);
            } else {
                binding.stagesRecycler.setAdapter(null);
            }

            // Update total estimated time
            updateTotalEstimatedTime(tokenTracking);

        } catch (Exception e) {
            Log.e("TOKEN_TRACKING", "Error updating UI: " + e.getMessage());
            Toast.makeText(this, "Error displaying token data", Toast.LENGTH_SHORT).show();
        }
    }

    // NEW: Method to update queue information
    private void updateQueueUI(TokenTracking tokenTracking) {
        // Show queue position if available
        if (tokenTracking.getQueuePosition() > 0) {
            binding.cardQueueInfo.setVisibility(View.VISIBLE);

            binding.tvQueuePosition.setText(String.valueOf(tokenTracking.getQueuePosition()));
            binding.tvTotalInQueue.setText(String.valueOf(tokenTracking.getTotalInQueue()));
            binding.tvEstimatedWait.setText("Estimated wait: " +
                    (tokenTracking.getEstimatedWaitTime() != null ?
                            tokenTracking.getEstimatedWaitTime() : "Calculating..."));
            binding.tvNextCallEstimate.setText("Next call: " +
                    (tokenTracking.getNextCallEstimate() != null ?
                            tokenTracking.getNextCallEstimate() : "Not available"));

            // NEW: Update queue status message
            if (tokenTracking.getQueuePosition() == 1) {
                binding.tvQueueStatusMessage.setText("🎉 You are NEXT to be served!");
            } else {
                binding.tvQueueStatusMessage.setText("Patients ahead of you: " + (tokenTracking.getQueuePosition() - 1));
            }

            // Show/hide urgent "You're Next" banner
            if (tokenTracking.isNextInQueue()) {
                binding.cardUrgentNext.setVisibility(View.VISIBLE);
                binding.tvUrgentTitle.setText("🚨 YOU ARE NEXT IN LINE! 🚨");
                binding.tvUrgentMessage.setText("Please proceed to consultation room immediately");
                binding.tvYourStatus.setText("✅ YOU'RE NEXT - GET READY!");
                binding.tvYourStatus.setTextColor(getColor(R.color.green));
                binding.tvQueueStatus.setText("🔊 Calling your token NOW: " + tokenTracking.getTokenId());

                // Send notification
                sendNextInQueueNotification(tokenTracking.getTokenId(), tokenTracking.getPatient());
            } else {
                binding.cardUrgentNext.setVisibility(View.GONE);
                binding.tvYourStatus.setText("⏳ Please wait for your turn");
                binding.tvYourStatus.setTextColor(getColor(R.color.my_primary));
                binding.tvQueueStatus.setText("Now serving token " + (tokenTracking.getQueuePosition() - 1) + " ahead of you");
            }
        } else {
            binding.cardQueueInfo.setVisibility(View.GONE);
            binding.cardUrgentNext.setVisibility(View.GONE);
            binding.tvQueueStatus.setText("Queue position not available");
            binding.tvYourStatus.setText("Please check back later");
        }
    }

    // NEW: Method to calculate total estimated time
    private void updateTotalEstimatedTime(TokenTracking tokenTracking) {
        String estimatedTime = "Calculating...";

        if (tokenTracking.getQueuePosition() > 0 && tokenTracking.getEstimatedWaitTime() != null) {
            // Extract minutes from estimated wait time
            try {
                String waitTime = tokenTracking.getEstimatedWaitTime();
                if (waitTime.contains("minutes")) {
                    String[] parts = waitTime.split(" ");
                    if (parts.length > 0) {
                        int minutes = Integer.parseInt(parts[0]);
                        // Add time for remaining stages
                        int totalMinutes = minutes + 30; // Add 30 mins for consultation + pharmacy
                        estimatedTime = totalMinutes + " minutes";
                    }
                } else if (waitTime.contains("1-5")) {
                    estimatedTime = "30-45 minutes";
                } else if (waitTime.contains("5-10")) {
                    estimatedTime = "35-50 minutes";
                } else {
                    estimatedTime = "45-60 minutes";
                }
            } catch (Exception e) {
                estimatedTime = "45-60 minutes";
            }
        }

        binding.tvTotalEstimatedTime.setText(estimatedTime);
    }

    // NEW: Notification method for when user is next
    private void sendNextInQueueNotification(String tokenId, String username) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "urgent_queue_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Urgent Queue Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for when you're next in queue");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_token)
                .setContentTitle("🚨 YOU ARE NEXT IN QUEUE!")
                .setContentText("Token " + tokenId + " - Please proceed to consultation room")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(Color.RED);

        notificationManager.notify(1001, builder.build());

        // Play sound
        playUrgentNotificationSound();
    }

    private void playUrgentNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (notification == null) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStageDisplayName(String stage) {
        if (stage == null) return "Registration";
        switch (stage) {
            case "registration": return "Registration";
            case "triage": return "Triage";
            case "consultation": return "Doctor Consultation";
            case "pharmacy": return "Pharmacy";
            default: return stage;
        }
    }

    private String getStatusDisplayName(String status) {
        if (status == null) return "⏳ Waiting";
        switch (status) {
            case "waiting": return "⏳ Waiting";
            case "in_progress": return "🔄 In Progress";
            case "completed": return "✅ Completed";
            default: return status;
        }
    }
}