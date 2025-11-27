package com.example.azimbalife.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.azimbalife.Adapter.AppointmentAdapter;
import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyAppointmentsActivity extends AppCompatActivity {

    private RecyclerView recyclerAppointments;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyState;
    private Button btnBookFirstAppointment;

    private TextView filterAll, filterPending, filterConfirmed, filterCompleted, filterCancelled;

    private AppointmentAdapter adapter;
    private List<Appointment> allAppointments = new ArrayList<>();
    private List<Appointment> filteredAppointments = new ArrayList<>();

    private String username;
    private String currentFilter = "all";

    private DatabaseReference allAppointmentsRef;
    private DatabaseReference userAppointmentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_appointments);

        // Get username from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("APPOINTMENTS_DEBUG", "Loading appointments for user: " + username);

        // Initialize Firebase references
        allAppointmentsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/AllAppointments");
        userAppointmentsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Appointments").child(username);

        initializeViews();
        setupRecyclerView();
        setupFilterListeners();
        setupSwipeRefresh();
        loadAppointments();

        // Listen for real-time updates from AllAppointments
        listenForAppointmentUpdates();
    }

    private void initializeViews() {
        recyclerAppointments = findViewById(R.id.recyclerAppointments);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyState = findViewById(R.id.emptyState);
        btnBookFirstAppointment = findViewById(R.id.btnBookFirstAppointment);

        filterAll = findViewById(R.id.filterAll);
        filterPending = findViewById(R.id.filterPending);
        filterConfirmed = findViewById(R.id.filterConfirmed);
        filterCompleted = findViewById(R.id.filterCompleted);
        filterCancelled = findViewById(R.id.filterCancelled);

        // Set initial filter state
        updateFilterUI(filterAll, filterPending, filterConfirmed, filterCompleted, filterCancelled);

        btnBookFirstAppointment.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookAppointmentActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Add back button functionality
        TextView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        adapter = new AppointmentAdapter(filteredAppointments, new AppointmentAdapter.OnAppointmentClickListener() {
            @Override
            public void onViewDetailsClick(Appointment appointment) {
                showAppointmentDetails(appointment);
            }

            @Override
            public void onCancelClick(Appointment appointment) {
                cancelAppointment(appointment);
            }
        });

        recyclerAppointments.setLayoutManager(new LinearLayoutManager(this));
        recyclerAppointments.setAdapter(adapter);

        // Add item decoration for spacing between items
        recyclerAppointments.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                this, LinearLayoutManager.VERTICAL));
    }

    private void setupFilterListeners() {
        filterAll.setOnClickListener(v -> applyFilter("all"));
        filterPending.setOnClickListener(v -> applyFilter("pending"));
        filterConfirmed.setOnClickListener(v -> applyFilter("confirmed"));
        filterCompleted.setOnClickListener(v -> applyFilter("completed"));
        filterCancelled.setOnClickListener(v -> applyFilter("cancelled"));
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAppointments();
        });

        // Configure refresh colors
        swipeRefreshLayout.setColorSchemeResources(
                R.color.my_primary,
                R.color.green,
                R.color.blueCat
        );
    }

    private void loadAppointments() {
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not identified", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Log.d("APPOINTMENTS_DEBUG", "Loading from AllAppointments for user: " + username);

        // Query AllAppointments for this user's appointments
        Query userAppointmentsQuery = allAppointmentsRef.orderByChild("patientUsername").equalTo(username);

        userAppointmentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAppointments.clear();
                filteredAppointments.clear();

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                        try {
                            Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                            if (appointment != null) {
                                // Ensure appointment has an ID from Firebase key
                                if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
                                    appointment.setAppointmentId(appointmentSnapshot.getKey());
                                }
                                // Ensure username is set
                                if (appointment.getPatientUsername() == null || appointment.getPatientUsername().isEmpty()) {
                                    appointment.setPatientUsername(username);
                                }
                                allAppointments.add(appointment);
                                Log.d("APPOINTMENTS_DEBUG", "Loaded appointment: " + appointment.getDepartment() + " - " + appointment.getStatus());

                                // Sync to user's personal appointments node
                                syncAppointmentToUserNode(appointment);
                            }
                        } catch (Exception e) {
                            Log.e("APPOINTMENTS_ERROR", "Error parsing appointment: " + e.getMessage(), e);
                        }
                    }

                    // Sort by creation date (newest first)
                    Collections.sort(allAppointments, (a1, a2) -> {
                        try {
                            long time1 = a1.getCreatedAt() != null ? Long.parseLong(a1.getCreatedAt()) : 0;
                            long time2 = a2.getCreatedAt() != null ? Long.parseLong(a2.getCreatedAt()) : 0;
                            return Long.compare(time2, time1); // Descending order
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    });

                    applyFilter(currentFilter);
                    showEmptyState(false);
                    Log.d("APPOINTMENTS_DEBUG", "Successfully loaded " + allAppointments.size() + " appointments from AllAppointments");
                } else {
                    showEmptyState(true);
                    Log.d("APPOINTMENTS_DEBUG", "No appointments found for user in AllAppointments");
                }

                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyAppointmentsActivity.this,
                        "Failed to load appointments: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState(true);
                Log.e("APPOINTMENTS_ERROR", "Database error: " + error.getMessage() + ", Code: " + error.getCode());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void syncAppointmentToUserNode(Appointment appointment) {
        if (appointment.getAppointmentId() != null) {
            userAppointmentsRef.child(appointment.getAppointmentId()).setValue(appointment)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("SYNC_DEBUG", "Appointment synced to user node: " + appointment.getAppointmentId());
                        } else {
                            Log.e("SYNC_DEBUG", "Failed to sync appointment to user node: " + task.getException());
                        }
                    });
        }
    }

    private void listenForAppointmentUpdates() {
        // Listen for real-time updates in AllAppointments for this user
        Query userAppointmentsQuery = allAppointmentsRef.orderByChild("patientUsername").equalTo(username);

        userAppointmentsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("REALTIME_UPDATE", "Real-time update received for user appointments");
                // Refresh the list when updates occur
                loadAppointments();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("REALTIME_UPDATE", "Error listening for real-time updates: " + error.getMessage());
            }
        });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredAppointments.clear();

        switch (filter.toLowerCase()) {
            case "all":
                filteredAppointments.addAll(allAppointments);
                updateFilterUI(filterAll, filterPending, filterConfirmed, filterCompleted, filterCancelled);
                break;
            case "pending":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null &&
                            "pending".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                updateFilterUI(filterPending, filterAll, filterConfirmed, filterCompleted, filterCancelled);
                break;
            case "confirmed":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null &&
                            "confirmed".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                updateFilterUI(filterConfirmed, filterAll, filterPending, filterCompleted, filterCancelled);
                break;
            case "scheduled":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null &&
                            "scheduled".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                updateFilterUI(filterConfirmed, filterAll, filterPending, filterCompleted, filterCancelled);
                break;
            case "completed":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null &&
                            "completed".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                updateFilterUI(filterCompleted, filterAll, filterPending, filterConfirmed, filterCancelled);
                break;
            case "cancelled":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null &&
                            "cancelled".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                updateFilterUI(filterCancelled, filterAll, filterPending, filterConfirmed, filterCompleted);
                break;
        }

        if (adapter != null) {
            adapter.updateAppointments(filteredAppointments);
        }
        showEmptyState(filteredAppointments.isEmpty());

        Log.d("FILTER_DEBUG", "Applied filter: " + filter + ", showing " + filteredAppointments.size() + " appointments");
    }

    private void updateFilterUI(TextView selected, TextView... others) {
        // Set selected filter style
        selected.setBackgroundResource(R.drawable.filter_selected_background);
        selected.setTextColor(getColor(R.color.white));

        // Set unselected filter style
        for (TextView other : others) {
            other.setBackgroundResource(R.drawable.filter_unselected_background);
            other.setTextColor(getColor(R.color.my_primary));
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerAppointments.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerAppointments.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showAppointmentDetails(Appointment appointment) {
        StringBuilder details = new StringBuilder();
        details.append("Department: ").append(appointment.getDepartment()).append("\n\n");
        details.append("Date: ").append(appointment.getPreferredDate()).append("\n");

        if (appointment.getAllocatedTime() != null && !appointment.getAllocatedTime().isEmpty()) {
            details.append("⏰ Time: ").append(appointment.getAllocatedTime()).append("\n");
        } else {
            details.append("Time: Not allocated yet\n");
        }

        if (appointment.getTokenNumber() != null && !appointment.getTokenNumber().isEmpty()) {
            details.append("🎫 Token: ").append(appointment.getTokenNumber()).append("\n");
        } else {
            details.append("Token: Not assigned yet\n");
        }

        details.append("\n");
        details.append("Reason: ").append(appointment.getVisitReason()).append("\n\n");

        // Enhanced status display
        String status = appointment.getStatus();
        details.append("Status: ");
        switch (status != null ? status.toLowerCase() : "") {
            case "scheduled":
                details.append("✅ Scheduled");
                break;
            case "confirmed":
                details.append("✅ Confirmed");
                break;
            case "pending":
                details.append("⏳ Pending");
                break;
            case "cancelled":
                details.append("❌ Cancelled");
                break;
            case "completed":
                details.append("🏁 Completed");
                break;
            default:
                details.append(status);
        }
        details.append("\n");

        if (appointment.getApprovedBy() != null && !appointment.getApprovedBy().isEmpty()) {
            details.append("Approved by: ").append(appointment.getApprovedBy()).append("\n");
        }

        if (appointment.getUrgencyLevel() != null && !appointment.getUrgencyLevel().isEmpty()) {
            details.append("Urgency: ").append(appointment.getUrgencyLevel()).append("\n");
        }

        if (appointment.getCreatedAt() != null && !appointment.getCreatedAt().isEmpty()) {
            try {
                long timestamp = Long.parseLong(appointment.getCreatedAt());
                String date = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a")
                        .format(new java.util.Date(timestamp));
                details.append("Booked on: ").append(date);
            } catch (NumberFormatException e) {
                details.append("Booked on: ").append(appointment.getCreatedAt());
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Appointment Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Refresh", (dialog, which) -> loadAppointments());

        // Add option to view notifications if scheduled
        if ("scheduled".equalsIgnoreCase(appointment.getStatus())) {
            builder.setNegativeButton("View Notifications", (dialog, which) -> {
                // This would open the notifications dialog
                Toast.makeText(this, "Check your notifications for confirmation details", Toast.LENGTH_SHORT).show();
            });
        }

        builder.show();
    }

    private void cancelAppointment(Appointment appointment) {
        // Check if appointment can be cancelled
        if ("completed".equalsIgnoreCase(appointment.getStatus()) ||
                "cancelled".equalsIgnoreCase(appointment.getStatus()) ||
                "scheduled".equalsIgnoreCase(appointment.getStatus())) {
            Toast.makeText(this, "This appointment cannot be cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?\n\n" +
                        "Department: " + appointment.getDepartment() + "\n" +
                        "Date: " + appointment.getPreferredDate())
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    updateAppointmentStatus(appointment, "cancelled");
                })
                .setNegativeButton("No, Keep It", null)
                .show();
    }

    private void updateAppointmentStatus(Appointment appointment, String newStatus) {
        if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
            Toast.makeText(this, "Error: Appointment ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update in both locations
        DatabaseReference allAppointmentsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/AllAppointments")
                .child(appointment.getAppointmentId());

        DatabaseReference userAppointmentRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/Appointments")
                .child(username)
                .child(appointment.getAppointmentId());

        // Create a map of updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        // Update in AllAppointments
        allAppointmentsRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update in user's appointments
                    userAppointmentRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "Appointment " + newStatus + " successfully", Toast.LENGTH_SHORT).show();
                                Log.d("APPOINTMENT_UPDATE", "Appointment " + appointment.getAppointmentId() + " updated to: " + newStatus);

                                // Update local appointment and refresh
                                appointment.setStatus(newStatus);
                                loadAppointments(); // Refresh the list
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update appointment in user node: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("APPOINTMENT_UPDATE", "Error updating appointment: " + e.getMessage());
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        Log.d("APPOINTMENTS_DEBUG", "onResume - refreshing appointments");
        loadAppointments();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}