package com.example.azimbalife.Activity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.azimbalife.Adapter.MedicalTipsAdapter;
import com.example.azimbalife.Adapter.QuickActionsAdapter;
import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.Domain.MedicalTip;
import com.example.azimbalife.Domain.Notification;
import com.example.azimbalife.Domain.QuickAction;
import com.example.azimbalife.R;
import com.example.azimbalife.ViewModel.MainViewModel;
import com.example.azimbalife.databinding.ActivityMainBinding;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private String username;
    private static final String PREFS_NAME = "NotificationPrefs";
    private boolean initialLoadDone = false;
    private int notificationCount = 0;
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;

    private DatabaseReference patientNotificationsRef;
    private DatabaseReference allAppointmentsRef;

    // Track processed appointments to avoid duplicate notifications
    private Map<String, String> processedAppointmentStatus = new HashMap<>();

    // Track current notification dialog
    private AlertDialog currentNotificationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Debug: Check what's in session
        SharedPreferences prefs = getSharedPreferences("LoginSession", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String savedUsername = prefs.getString("username", "");
        Log.d("MAIN_DEBUG", "Session check - isLoggedIn: " + isLoggedIn + ", username: " + savedUsername);

        // Validate session first - IMPORTANT SECURITY CHECK
        validateSession();

        // Get username from intent or SharedPreferences
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = prefs.getString("username", "");
        }

        // Final security check - if username is still empty, redirect to login
        if (username == null || username.isEmpty()) {
            Log.d("MAIN_DEBUG", "Username is empty, redirecting to login");
            redirectToLogin();
            return;
        }

        username = username.trim();
        Log.d("MAIN_DEBUG", "Final username: " + username);

        // Initialize Firebase references
        patientNotificationsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/PatientNotifications");
        allAppointmentsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/AllAppointments");

        // Clean up any existing duplicate notifications
        cleanupDuplicateNotifications();

        // Initialize components
        viewModel = new MainViewModel();

        // Load notification count from Firebase instead of local storage
        refreshNotificationCount();

        setupDashboard();
        listenForRealtimeNotifications();
        listenForPatientNotifications();
        listenForAppointmentConfirmations(); // Listen for appointment confirmations
        loadUserProfile(username);
        loadMedicalTips();
        loadQuickActions();
        loadPatientSummary();
        loadUpcomingAppointments();

        // Add click listener for "View All Appointments"
        setupViewAllAppointmentsListener();
    }

    /** -------------------- SESSION VALIDATION -------------------- **/
    private void validateSession() {
        SharedPreferences prefs = getSharedPreferences("LoginSession", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String savedUsername = prefs.getString("username", "");

        Log.d("SESSION_DEBUG", "Validating session - isLoggedIn: " + isLoggedIn + ", username: " + savedUsername);

        if (!isLoggedIn || savedUsername.isEmpty()) {
            // Session invalid, redirect to login
            Log.d("SESSION_DEBUG", "Session invalid, redirecting to login");
            redirectToLogin();
        } else {
            Log.d("SESSION_DEBUG", "Session valid, proceeding to MainActivity");
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** -------------------- DASHBOARD SETUP -------------------- **/
    private void setupDashboard() {
        // Update greeting based on XML layout
        binding.tvGreeting.setText("Welcome to Your Health Dashboard");

        // Commented out - this TextView doesn't exist in current XML
        // binding.tvHowAreYou.setText("Manage your health journey in one place");

        // Setup click listeners
        setupFeatureCards();
        setupBottomNavigation();
        setupEmergencyFeatures();
    }

    private void setupFeatureCards() {
        // Book Appointment
        binding.cardBookAppointment.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BookAppointmentActivity.class)
                    .putExtra("username", username));
        });

        // My Appointments
        binding.cardMyAppointments.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MyAppointmentsActivity.class)
                    .putExtra("username", username));
        });

        // Test Results
        binding.cardTestResults.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TestResultsActivity.class)
                    .putExtra("username", username));
        });

        // Medical Records
        binding.cardMedicalRecords.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MedicalRecordsActivity.class)
                    .putExtra("username", username));
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                // Home - Refresh dashboard
                Toast.makeText(MainActivity.this, "Refreshing Dashboard...", Toast.LENGTH_SHORT).show();
                if (binding.scrollView2 != null) {
                    binding.scrollView2.post(() -> binding.scrollView2.fullScroll(View.FOCUS_UP));
                }
                loadUserProfile(username);
                loadMedicalTips();
                loadQuickActions();
                loadPatientSummary();
                loadUpcomingAppointments();
                refreshNotificationCount();
                return true;

            } else if (itemId == R.id.myTokens) {
                // Health Reports
                startActivity(new Intent(MainActivity.this, HealthReportsActivity.class)
                        .putExtra("username", username));
                return true;

            } else if (itemId == R.id.profileEdit) {
                // Profile edit
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
                return true;

            } else if (itemId == R.id.settings) {
                // Settings
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }

            return false;
        });

        // Keep only notification and logout click listeners
        binding.notificationIcon.setOnClickListener(v -> {
            playNotificationSound();
            showEnhancedNotificationDialog();
        });

        binding.logoutIcon.setOnClickListener(v -> showLogoutConfirmation());
    }
    private void setupEmergencyFeatures() {
        // Emergency call FAB
        binding.fabEmergency.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:+256701995356"));
            startActivity(callIntent);
        });
    }

    private void setupViewAllAppointmentsListener() {
        // Add click listener for "View All Appointments"
        binding.tvViewAllAppointments.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MyAppointmentsActivity.class)
                    .putExtra("username", username));
        });
    }

    /** -------------------- MEDICAL COMPANION FEATURES -------------------- **/
    private void loadMedicalTips() {
        List<MedicalTip> tips = new ArrayList<>();
        tips.add(new MedicalTip("Stay Hydrated", "Drink at least 8 glasses of water daily", R.drawable.ic_token));
        tips.add(new MedicalTip("Regular Exercise", "30 minutes of exercise improves health", R.drawable.ic_token));
        tips.add(new MedicalTip("Balanced Diet", "Eat fruits and vegetables daily", R.drawable.ic_token));
        tips.add(new MedicalTip("Adequate Sleep", "7-9 hours of sleep is essential", R.drawable.ic_token));
        tips.add(new MedicalTip("Stress Management", "Practice meditation and relaxation", R.drawable.ic_token));

        MedicalTipsAdapter adapter = new MedicalTipsAdapter(tips);
        binding.categoryRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.categoryRecycler.setAdapter(adapter);

        // Update section title to match XML
        binding.tvCategories.setText("Health Tips & Wellness");
    }

    private void loadQuickActions() {
        List<QuickAction> actions = new ArrayList<>();
        actions.add(new QuickAction("Book Appointment", "Schedule hospital visit", R.drawable.ic_calendar,
                v -> startActivity(new Intent(this, BookAppointmentActivity.class)
                        .putExtra("username", username))));
        actions.add(new QuickAction("Test Results", "View lab reports", R.drawable.ic_token,
                v -> startActivity(new Intent(this, TestResultsActivity.class)
                        .putExtra("username", username))));
        actions.add(new QuickAction("Medical History", "Your health records", R.drawable.ic_calendar,
                v -> startActivity(new Intent(this, MedicalRecordsActivity.class)
                        .putExtra("username", username))));
        actions.add(new QuickAction("Health Reports", "Generate analytics", R.drawable.ic_token,
                v -> startActivity(new Intent(this, HealthReportsActivity.class)
                        .putExtra("username", username))));

        QuickActionsAdapter adapter = new QuickActionsAdapter(actions);

        // Replace doctors recycler with quick actions


    }

    private void loadPatientSummary() {
        // Load patient summary from Firebase
        DatabaseReference patientRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/Patients")
                .child(username)
                .child("summary");

        patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nextAppointment = snapshot.child("nextAppointment").getValue(String.class);
                    String recentResults = snapshot.child("recentResults").getValue(String.class);
                    String healthScore = snapshot.child("healthScore").getValue(String.class);

                    // Update UI with patient summary data
                    if (nextAppointment != null && !nextAppointment.isEmpty()) {
                        binding.tvNextAppointment.setText(nextAppointment);
                    }

                    if (recentResults != null && !recentResults.isEmpty()) {
                        binding.tvRecentResults.setText(recentResults);
                    }

                    if (healthScore != null && !healthScore.isEmpty()) {
                        binding.tvHealthScore.setText(healthScore);
                        // Update color based on health score
                        try {
                            int score = Integer.parseInt(healthScore.replace("%", ""));
                            if (score >= 80) {
                                binding.tvHealthScore.setTextColor(getColor(R.color.green));
                            } else if (score >= 60) {
                                binding.tvHealthScore.setTextColor(getColor(R.color.my_primary));
                            } else {
                                binding.tvHealthScore.setTextColor(getColor(R.color.red));
                            }
                        } catch (NumberFormatException e) {
                            binding.tvHealthScore.setTextColor(getColor(R.color.green));
                        }
                    }

                    Log.d("PATIENT_SUMMARY", "Patient summary loaded successfully");
                } else {
                    // Default summary - already set in XML
                    Log.d("PATIENT_SUMMARY", "No patient summary found, using defaults");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PATIENT_SUMMARY", "Error loading patient summary: " + error.getMessage());
            }
        });
    }

    /** -------------------- UPCOMING APPOINTMENTS -------------------- **/
    private void loadUpcomingAppointments() {
        Query upcomingQuery = allAppointmentsRef.orderByChild("patientUsername").equalTo(username);

        upcomingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int scheduledCount = 0;
                StringBuilder upcomingInfo = new StringBuilder();

                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null && "scheduled".equalsIgnoreCase(appointment.getStatus())) {
                        scheduledCount++;
                        if (upcomingInfo.length() == 0) {
                            // Show details of the first scheduled appointment
                            String time = appointment.getAllocatedTime() != null ?
                                    appointment.getAllocatedTime() : "Time TBD";
                            String token = appointment.getTokenNumber() != null ?
                                    "Token: " + appointment.getTokenNumber() : "";

                            upcomingInfo.append(appointment.getDepartment())
                                    .append("\n")
                                    .append(time);

                            if (!token.isEmpty()) {
                                upcomingInfo.append(" | ").append(token);
                            }
                        }
                    }
                }

                if (scheduledCount > 0) {
                    binding.tvUpcomingAppointments.setText(upcomingInfo.toString());
                    binding.tvUpcomingAppointments.setTextColor(getColor(R.color.green));
                    binding.tvAppointmentCount.setVisibility(View.VISIBLE);
                    binding.tvAppointmentCount.setText(String.valueOf(scheduledCount));
                    Log.d("UPCOMING_APPOINTMENTS", "Found " + scheduledCount + " scheduled appointments");
                } else {
                    binding.tvUpcomingAppointments.setText("No upcoming appointments");
                    binding.tvUpcomingAppointments.setTextColor(getColor(R.color.grey));
                    binding.tvAppointmentCount.setVisibility(View.GONE);
                    Log.d("UPCOMING_APPOINTMENTS", "No scheduled appointments found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.tvUpcomingAppointments.setText("Error loading appointments");
                binding.tvUpcomingAppointments.setTextColor(getColor(R.color.red));
                binding.tvAppointmentCount.setVisibility(View.GONE);
                Log.e("UPCOMING_APPOINTMENTS", "Error loading upcoming appointments: " + error.getMessage());
            }
        });
    }

    /** -------------------- APPOINTMENT CONFIRMATION NOTIFICATIONS -------------------- **/
    private void listenForAppointmentConfirmations() {
        Query appointmentQuery = allAppointmentsRef.orderByChild("patientUsername").equalTo(username);

        appointmentQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Don't trigger notifications for existing appointments
                Appointment appointment = snapshot.getValue(Appointment.class);
                if (appointment != null) {
                    // Initialize tracking for this appointment
                    processedAppointmentStatus.put(appointment.getAppointmentId(), appointment.getStatus());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Appointment appointment = snapshot.getValue(Appointment.class);
                if (appointment != null) {
                    String appointmentId = appointment.getAppointmentId();
                    String currentStatus = appointment.getStatus();
                    String previousStatus = processedAppointmentStatus.get(appointmentId);

                    Log.d("APPOINTMENT_STATUS", "Appointment " + appointmentId +
                            " changed from " + previousStatus + " to " + currentStatus);

                    // ONLY trigger notification when status changes TO "scheduled"
                    // AND only if it's a recent change (within last 2 minutes)
                    if ("scheduled".equalsIgnoreCase(currentStatus) &&
                            !"scheduled".equalsIgnoreCase(previousStatus)) {

                        long currentTime = System.currentTimeMillis();
                        long scheduledAt = appointment.getScheduledAt() != null ?
                                Long.parseLong(appointment.getScheduledAt()) : 0;

                        // Only process if scheduled within last 2 minutes (to catch real-time changes)
                        if (scheduledAt == 0 || (currentTime - scheduledAt) < 120000) {
                            // Check if we already created a notification for this appointment
                            checkAndCreateAppointmentNotification(appointment);
                        } else {
                            Log.d("APPOINTMENT_CONFIRMATION", "Skipping old appointment change: " + appointmentId);
                        }
                    }

                    // Update the processed status
                    processedAppointmentStatus.put(appointmentId, currentStatus);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("APPOINTMENT_CONFIRMATION", "Error listening for appointment updates: " + error.getMessage());
            }
        });
    }

    private void checkAndCreateAppointmentNotification(Appointment appointment) {
        // Check if notification already exists for this appointment from health worker
        String expectedNotificationIdPattern = "hw_scheduled_" + appointment.getAppointmentId();

        Query existingQuery = patientNotificationsRef.orderByChild("appointmentId").equalTo(appointment.getAppointmentId());
        existingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean healthWorkerNotificationExists = false;
                for (DataSnapshot existingSnapshot : snapshot.getChildren()) {
                    Notification existingNotification = existingSnapshot.getValue(Notification.class);
                    if (existingNotification != null &&
                            "appointment_scheduled".equals(existingNotification.getType()) &&
                            existingNotification.getNotificationId().startsWith("hw_scheduled_")) {
                        healthWorkerNotificationExists = true;
                        break;
                    }
                }

                if (!healthWorkerNotificationExists) {
                    // Only create if health worker hasn't already created one
                    Log.d("APPOINTMENT_CONFIRMATION", "Creating auto-notification for scheduled appointment: " + appointment.getAppointmentId());
                    showAppointmentConfirmationNotification(appointment);
                } else {
                    Log.d("APPOINTMENT_CONFIRMATION", "Health worker notification already exists for appointment: " + appointment.getAppointmentId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("APPOINTMENT_CONFIRMATION", "Error checking existing notifications: " + error.getMessage());
            }
        });
    }

    private void showAppointmentConfirmationNotification(Appointment appointment) {
        String title = "Appointment Scheduled! 🎉";
        String message = buildAppointmentConfirmationMessage(appointment);

        // Create system notification
        showSystemNotification(title, message);

        // Create in-app notification
        createInAppAppointmentNotification(appointment, title, message);

        // Play notification sound
        playNotificationSound();

        // Refresh notification count from Firebase
        refreshNotificationCount();

        // Refresh upcoming appointments
        loadUpcomingAppointments();

        Log.d("APPOINTMENT_CONFIRMATION", "Appointment confirmation notification shown for: " + appointment.getAppointmentId());
    }

    private String buildAppointmentConfirmationMessage(Appointment appointment) {
        StringBuilder message = new StringBuilder();
        message.append("Your appointment with ").append(appointment.getDepartment()).append(" has been scheduled!\n\n");

        if (appointment.getAllocatedTime() != null && !appointment.getAllocatedTime().isEmpty()) {
            message.append("🕐 Time: ").append(appointment.getAllocatedTime()).append("\n");
        }

        if (appointment.getTokenNumber() != null && !appointment.getTokenNumber().isEmpty()) {
            message.append("🎫 Token: ").append(appointment.getTokenNumber()).append("\n");
        }

        if (appointment.getPreferredDate() != null && !appointment.getPreferredDate().isEmpty()) {
            message.append("📅 Date: ").append(appointment.getPreferredDate()).append("\n");
        }

        if (appointment.getApprovedBy() != null && !appointment.getApprovedBy().isEmpty()) {
            message.append("👨‍⚕️ Approved by: ").append(appointment.getApprovedBy());
        }

        return message.toString();
    }

    private void createInAppAppointmentNotification(Appointment appointment, String title, String message) {
        String notificationId = "auto_scheduled_" + appointment.getAppointmentId() + "_" + System.currentTimeMillis();

        // First check if notification already exists
        patientNotificationsRef.child(notificationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Only create if it doesn't exist
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("notificationId", notificationId);
                    notificationData.put("patientId", username);
                    notificationData.put("appointmentId", appointment.getAppointmentId());
                    notificationData.put("title", title);
                    notificationData.put("message", message);
                    notificationData.put("read", false); // CHANGED FROM "isRead" to "read"
                    notificationData.put("timestamp", System.currentTimeMillis());
                    notificationData.put("type", "appointment_scheduled");
                    notificationData.put("department", appointment.getDepartment());
                    notificationData.put("tokenNumber", appointment.getTokenNumber());
                    notificationData.put("allocatedTime", appointment.getAllocatedTime());

                    patientNotificationsRef.child(notificationId).setValue(notificationData)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("NOTIFICATION", "Auto scheduled notification saved: " + notificationId);
                                } else {
                                    Log.e("NOTIFICATION", "Failed to save auto scheduled notification: " + task.getException());
                                }
                            });
                } else {
                    Log.d("NOTIFICATION", "Auto notification already exists: " + notificationId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NOTIFICATION", "Error checking auto notification existence: " + error.getMessage());
            }
        });
    }

    /** -------------------- CLEANUP DUPLICATE NOTIFICATIONS -------------------- **/
    private void cleanupDuplicateNotifications() {
        Query patientQuery = patientNotificationsRef.orderByChild("patientId").equalTo(username);

        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<String>> appointmentNotifications = new HashMap<>();

                // Group notifications by appointment ID
                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    Notification notification = notificationSnapshot.getValue(Notification.class);
                    if (notification != null && notification.getAppointmentId() != null) {
                        String appointmentId = notification.getAppointmentId();
                        if (!appointmentNotifications.containsKey(appointmentId)) {
                            appointmentNotifications.put(appointmentId, new ArrayList<>());
                        }
                        appointmentNotifications.get(appointmentId).add(notification.getNotificationId());
                    }
                }

                // Keep only the most recent scheduled notification for each appointment
                for (Map.Entry<String, List<String>> entry : appointmentNotifications.entrySet()) {
                    String appointmentId = entry.getKey();
                    List<String> notificationIds = entry.getValue();

                    if (notificationIds.size() > 1) {
                        Log.d("CLEANUP", "Found " + notificationIds.size() + " notifications for appointment: " + appointmentId);

                        // Find the most recent scheduled notification
                        String keepNotificationId = null;
                        long latestTimestamp = 0;

                        for (String notificationId : notificationIds) {
                            DataSnapshot notificationSnapshot = snapshot.child(notificationId);
                            if (notificationSnapshot.exists()) {
                                Notification notification = notificationSnapshot.getValue(Notification.class);
                                if (notification != null &&
                                        "appointment_scheduled".equals(notification.getType()) &&
                                        notification.getTimestamp() > latestTimestamp) {
                                    latestTimestamp = notification.getTimestamp();
                                    keepNotificationId = notificationId;
                                }
                            }
                        }

                        // If we found a scheduled notification, delete all others for this appointment
                        if (keepNotificationId != null) {
                            for (String notificationId : notificationIds) {
                                if (!notificationId.equals(keepNotificationId)) {
                                    patientNotificationsRef.child(notificationId).removeValue()
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Log.d("CLEANUP", "Deleted duplicate notification: " + notificationId);
                                                }
                                            });
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CLEANUP", "Error cleaning up duplicate notifications: " + error.getMessage());
            }
        });
    }

    /** -------------------- ENHANCED NOTIFICATION SYSTEM -------------------- **/
    private void listenForPatientNotifications() {
        Query patientQuery = patientNotificationsRef.orderByChild("patientId").equalTo(username);

        patientQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists() && initialLoadDone) {
                    Notification notification = snapshot.getValue(Notification.class);
                    if (notification != null && !notification.isRead()) {
                        // Only process if notification is not read
                        notificationCount++;
                        saveNotificationCount(notificationCount);
                        updateBadge();
                        playNotificationSound();
                        showSystemNotification(notification.getTitle(), notification.getMessage());

                        Log.d("PATIENT_NOTIFICATION", "New patient notification: " + notification.getTitle());
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle when notification is marked as read
                Notification notification = snapshot.getValue(Notification.class);
                if (notification != null && notification.isRead()) {
                    // If notification was marked as read, update local count
                    refreshNotificationCount();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // If notification is removed, update count
                refreshNotificationCount();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PATIENT_NOTIFICATION", "Error listening for patient notifications: " + error.getMessage());
            }
        });

        initialLoadDone = true;
        Log.d("PATIENT_NOTIFICATION", "Started listening for patient notifications for user: " + username);
    }

    /** -------------------- REFRESH NOTIFICATION COUNT -------------------- **/
    private void refreshNotificationCount() {
        Query patientQuery = patientNotificationsRef.orderByChild("patientId").equalTo(username);

        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;
                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    Notification notification = notificationSnapshot.getValue(Notification.class);
                    if (notification != null && !notification.isRead()) {
                        unreadCount++;
                    }
                }

                notificationCount = unreadCount;
                saveNotificationCount(notificationCount);
                updateBadge();
                Log.d("NOTIFICATION_COUNT", "Refreshed notification count: " + notificationCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NOTIFICATION_COUNT", "Error refreshing notification count: " + error.getMessage());
            }
        });
    }

    private void markNotificationAsRead(String notificationId) {
        DatabaseReference notificationRef = patientNotificationsRef.child(notificationId);

        // Use direct setValue for the specific field with explicit field name
        notificationRef.child("read").setValue(true) // Use "read" instead of "isRead"
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("NOTIFICATION", "Notification marked as read: " + notificationId);
                        // Force immediate UI update
                        refreshNotificationCount();
                        updateBadge();

                        // Show immediate feedback
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Notification marked as read", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e("NOTIFICATION", "Failed to mark notification as read: " + notificationId);
                        if (task.getException() != null) {
                            Log.e("NOTIFICATION", "Error: " + task.getException().getMessage());
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Failed to mark as read", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void showEnhancedNotificationDialog() {
        Query patientQuery = patientNotificationsRef.orderByChild("patientId").equalTo(username)
                .limitToLast(20); // Show last 20 notifications

        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Notification> notifications = new ArrayList<>();
                int unreadCount = 0;

                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    Notification notification = notificationSnapshot.getValue(Notification.class);
                    if (notification != null) {
                        notifications.add(notification);
                        if (!notification.isRead()) {
                            unreadCount++;
                        }
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(notifications, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                if (notifications.isEmpty()) {
                    showSimpleNotificationDialog("No Notifications", "You don't have any notifications yet.");
                } else {
                    showNotificationsListDialog(notifications, unreadCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showSimpleNotificationDialog("Error", "Failed to load notifications. Please try again.");
            }
        });
    }

    private void showNotificationsListDialog(List<Notification> notifications, int unreadCount) {
        // Close existing dialog if open
        if (currentNotificationDialog != null && currentNotificationDialog.isShowing()) {
            currentNotificationDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set title with unread count - create final copy for use in lambda
        final int finalUnreadCount = unreadCount;
        String title;
        if (finalUnreadCount > 0) {
            title = "Your Notifications (" + finalUnreadCount + " new)";
        } else {
            title = "Your Notifications (" + notifications.size() + ")";
        }
        builder.setTitle(title);

        // Create a simple list of notification messages
        String[] notificationItems = new String[notifications.size()];
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    .format(new java.util.Date(notification.getTimestamp()));

            String readIndicator = notification.isRead() ? "✅ " : "🔔 ";
            notificationItems[i] = String.format("%s[%s] %s\n%s",
                    readIndicator, date, notification.getTitle(), notification.getMessage());
        }

        // Create final copies of variables for use in lambda
        final List<Notification> finalNotifications = new ArrayList<>(notifications);
        final String[] finalNotificationItems = notificationItems;

        builder.setItems(finalNotificationItems, (dialog, which) -> {
            // Show full notification details when clicked
            Notification selectedNotification = finalNotifications.get(which);

            // MARK AS READ WHEN VIEWED (only if not already read)
            if (!selectedNotification.isRead()) {
                markNotificationAsRead(selectedNotification.getNotificationId());
                // Update local object immediately for better UX
                selectedNotification.setRead(true);
                // Update the displayed item immediately
                finalNotificationItems[which] = finalNotificationItems[which].replace("🔔", "✅");

                // Update dialog title to reflect the change
                if (currentNotificationDialog != null && currentNotificationDialog.isShowing()) {
                    int updatedUnreadCount = finalUnreadCount - 1;
                    String updatedTitle;
                    if (updatedUnreadCount > 0) {
                        updatedTitle = "Your Notifications (" + updatedUnreadCount + " new)";
                    } else {
                        updatedTitle = "Your Notifications (" + finalNotifications.size() + ")";
                    }
                    currentNotificationDialog.setTitle(updatedTitle);
                }
            }

            showNotificationDetails(selectedNotification);
        });

        builder.setPositiveButton("Mark All as Read", (dialog, which) -> {
            markAllNotificationsAsRead(finalNotifications);
        });

        builder.setNegativeButton("Close", (dialog, which) -> {
            // Refresh count when dialog closes
            refreshNotificationCount();
        });

        // Add refresh button
        builder.setNeutralButton("Refresh", (dialog, which) -> {
            showEnhancedNotificationDialog();
        });

        currentNotificationDialog = builder.create();

        // Set up dismiss listener to refresh count when dialog is dismissed
        currentNotificationDialog.setOnDismissListener(dialogInterface -> {
            refreshNotificationCount();
            currentNotificationDialog = null;
        });

        currentNotificationDialog.show();
    }

    private void showNotificationDetails(Notification notification) {
        String date = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                .format(new java.util.Date(notification.getTimestamp()));

        String message = "Date: " + date + "\n\n" + notification.getMessage();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(notification.getTitle())
                .setMessage(message)
                .setPositiveButton("OK", null);

        // Add calendar option for appointment notifications
        if ("appointment_scheduled".equals(notification.getType())) {
            builder.setNeutralButton("Add to Calendar", (dialog, which) -> {
                addAppointmentToCalendar(notification);
            });
        }

        builder.show();
    }

    private void addAppointmentToCalendar(Notification notification) {
        // Implement calendar integration here
        Toast.makeText(this, "Calendar feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void markAllNotificationsAsRead(List<Notification> notifications) {
        int markedCount = 0;
        List<String> notificationIdsToMark = new ArrayList<>();

        // First, collect all notification IDs that need to be marked as read
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notificationIdsToMark.add(notification.getNotificationId());
                markedCount++;
            }
        }

        if (markedCount > 0) {
            // Show immediate feedback
            Toast.makeText(this, "Marking " + markedCount + " notifications as read...", Toast.LENGTH_SHORT).show();

            // Mark all notifications as read in Firebase - USE "read" CONSISTENTLY
            for (String notificationId : notificationIdsToMark) {
                patientNotificationsRef.child(notificationId).child("read").setValue(true) // CHANGED FROM "isRead" to "read"
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("NOTIFICATION", "Notification marked as read: " + notificationId);
                            } else {
                                Log.e("NOTIFICATION", "Failed to mark notification as read: " + notificationId);
                            }
                        });
            }

            // Update local state immediately
            for (Notification notification : notifications) {
                notification.setRead(true);
            }

            // Update badge immediately
            notificationCount = Math.max(0, notificationCount - markedCount);
            saveNotificationCount(notificationCount);
            updateBadge();

            // Simply dismiss the dialog instead of recreating it
            if (currentNotificationDialog != null && currentNotificationDialog.isShowing()) {
                currentNotificationDialog.dismiss();
            }

            // Show success message
            Toast.makeText(this, markedCount + " notifications marked as read", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, "All notifications are already read", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSimpleNotificationDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /** -------------------- EXISTING FUNCTIONALITY -------------------- **/
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Clear login session completely
        SharedPreferences prefs = getSharedPreferences("LoginSession", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Also clear notification preferences
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();

        Log.d("LOGOUT_DEBUG", "All sessions cleared, redirecting to login");

        // Navigate to LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** -------------------- Load Profile -------------------- **/
    private void loadUserProfile(String username) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d("PROFILE_DEBUG", "No user data found for: " + username);
                    return;
                }

                String displayName = snapshot.child("name").getValue(String.class);
                if (displayName == null || displayName.isEmpty())
                    displayName = snapshot.child("username").getValue(String.class);
                if (displayName == null || displayName.isEmpty())
                    displayName = "User";

                // Update greeting with personalized name
                binding.tvGreeting.setText("Hi " + displayName + " 👋");

                String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(MainActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.person_sharp_icon)
                            .circleCrop()
                            .into(binding.userProfileIcon);
                } else {
                    binding.userProfileIcon.setImageResource(R.drawable.person_sharp_icon);
                }

                Log.d("PROFILE_DEBUG", "Profile loaded for: " + displayName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PROFILE_DEBUG", "Error loading profile: " + error.getMessage());
            }
        });
    }

    /** -------------------- Notifications -------------------- **/
    private void listenForRealtimeNotifications() {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("Notifications")
                .child(username);

        notificationsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists() && initialLoadDone) {
                    // Use the main notification system instead
                    refreshNotificationCount();
                    playNotificationSound();
                    showSystemNotification("New Notification", "You have a new notification");
                    Log.d("NOTIFICATION_DEBUG", "New notification received");
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NOTIFICATION_DEBUG", "Error listening for notifications: " + error.getMessage());
            }
        });
    }

    private void updateBadge() {
        if (notificationCount > 0) {
            binding.notificationBadge.setVisibility(View.VISIBLE);
            binding.notificationBadge.setText(String.valueOf(notificationCount));
        } else {
            binding.notificationBadge.setVisibility(View.GONE);
        }
    }

    private int loadNotificationCount() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt("notification_count", 0);
    }

    private void saveNotificationCount(int count) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt("notification_count", count).apply();
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSystemNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "default_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Default channel for app notifications");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notifications_24dp_999999_fill0_wght400_grad0_opsz24)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Additional session check when activity resumes
        Log.d("MAIN_DEBUG", "onResume - validating session");
        validateSession();

        // Refresh data when returning to activity
        loadPatientSummary();
        loadUpcomingAppointments();

        // Refresh notification count from Firebase (not from local storage)
        refreshNotificationCount();

        Log.d("MAIN_DEBUG", "Activity resumed, data refreshed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        // Clean up dialog reference
        if (currentNotificationDialog != null && currentNotificationDialog.isShowing()) {
            currentNotificationDialog.dismiss();
        }
        Log.d("MAIN_DEBUG", "MainActivity destroyed");
    }
}