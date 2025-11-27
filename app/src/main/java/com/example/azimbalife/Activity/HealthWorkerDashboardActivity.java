package com.example.azimbalife.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.HealthWorkerAppointmentsAdapter;
import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.Domain.Doctor;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthWorkerDashboardActivity extends AppCompatActivity {

    private CardView cardManageAppointments, cardPatientMetrics, cardTestResults,
            cardMedicalRecords, cardExaminations, cardSettings;
    private CardView cardEmergencyQuickAction, cardEmergencyCases, cardAvailableDoctors, cardEmergencyDoctors;
    private TextView tvWelcome, tvDepartment, tvPendingCount, tvTodayCount, tvCompletedCount, tvEmergencyCount;
    private TextView tvCurrentDate, tvCurrentTime, tvEmergencyAction, tvAvailableDoctorsCount, tvEmergencyDoctorsCount;
    private Button btnHandleEmergency;
    private RecyclerView recyclerRecentAppointments;

    private HealthWorkerAppointmentsAdapter recentAppointmentsAdapter;
    private List<Appointment> recentAppointments = new ArrayList<>();

    private String healthWorkerId;
    private String healthWorkerName;
    private String userType;
    private String department;
    private String specialization;

    private DatabaseReference appointmentsRef;
    private DatabaseReference completedExaminationsRef;
    private DatabaseReference doctorsRef;

    private Handler timeHandler;
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_worker_dashboard);

        // Get health worker data from intent
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");
        userType = getIntent().getStringExtra("userType");
        department = getIntent().getStringExtra("department");
        specialization = getIntent().getStringExtra("specialization");

        if (healthWorkerId == null) {
            Toast.makeText(this, "Health worker not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // DEBUG: Log all intent data
        Log.d("INTENT_DEBUG", "=== INTENT EXTRAS DEBUG ===");
        Log.d("INTENT_DEBUG", "healthWorkerId: " + healthWorkerId);
        Log.d("INTENT_DEBUG", "healthWorkerName: " + healthWorkerName);
        Log.d("INTENT_DEBUG", "userType: " + userType);
        Log.d("INTENT_DEBUG", "department: " + department);
        Log.d("INTENT_DEBUG", "specialization: " + specialization);
        Log.d("INTENT_DEBUG", "=== END INTENT DEBUG ===");

        // Force user type to health_worker
        userType = "health_worker";
        Log.d("FORCE_FIX", "User type forced to: health_worker");

        // Initialize Firebase references
        appointmentsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/AllAppointments");
        completedExaminationsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/CompletedExaminations");
        doctorsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Doctors");

        initializeViews();
        setupEmergencyQuickAction();
        setupClickListeners();
        setupRecentAppointments();
        startTimeUpdates();
        loadDashboardData();
    }

    private void initializeViews() {
        cardManageAppointments = findViewById(R.id.cardManageAppointments);
        cardPatientMetrics = findViewById(R.id.cardPatientMetrics);
        cardTestResults = findViewById(R.id.cardTestResults);
        cardMedicalRecords = findViewById(R.id.cardMedicalRecords);
        cardExaminations = findViewById(R.id.cardExaminations);
        cardSettings = findViewById(R.id.cardSettings);

        // Emergency and doctor cards
        cardEmergencyQuickAction = findViewById(R.id.cardEmergencyQuickAction);
        cardEmergencyCases = findViewById(R.id.cardEmergencyCases);
        cardAvailableDoctors = findViewById(R.id.cardAvailableDoctors);
        cardEmergencyDoctors = findViewById(R.id.cardEmergencyDoctors);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvTodayCount = findViewById(R.id.tvTodayCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvEmergencyCount = findViewById(R.id.tvEmergencyCount);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvEmergencyAction = findViewById(R.id.tvEmergencyAction);
        tvAvailableDoctorsCount = findViewById(R.id.tvAvailableDoctorsCount);
        tvEmergencyDoctorsCount = findViewById(R.id.tvEmergencyDoctorsCount);

        btnHandleEmergency = findViewById(R.id.btnHandleEmergency);
        recyclerRecentAppointments = findViewById(R.id.recyclerRecentAppointments);

        // Set welcome message and department
        String welcomeText = "Welcome, " + (healthWorkerName != null ? healthWorkerName : "Health Worker") + "";
        tvWelcome.setText(welcomeText);

        String departmentText = "";
        if (department != null) departmentText += department;
        if (specialization != null) departmentText += " • " + specialization;
        tvDepartment.setText(departmentText);

        // Initialize time display
        updateDateTime();
    }

    private void setupEmergencyQuickAction() {
        btnHandleEmergency.setOnClickListener(v -> {
            // Navigate directly to emergency appointments in ManageAppointmentsActivity
            Intent intent = new Intent(this, ManageAppointmentsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            intent.putExtra("department", department);
            intent.putExtra("autoFilter", "emergency");
            startActivity(intent);
        });

        // Emergency cases card click
        cardEmergencyCases.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageAppointmentsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            intent.putExtra("department", department);
            intent.putExtra("autoFilter", "emergency");
            startActivity(intent);
        });

        // Available doctors card click
        cardAvailableDoctors.setOnClickListener(v -> {
            showAvailableDoctorsDialog();
        });

        // Emergency doctors card click
        cardEmergencyDoctors.setOnClickListener(v -> {
            showEmergencyDoctorsDialog();
        });
    }

    private void showAvailableDoctorsDialog() {
        // Load and show available doctors
        doctorsRef.orderByChild("available").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Doctor> availableDoctors = new ArrayList<>();
                        for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                            Doctor doctor = doctorSnapshot.getValue(Doctor.class);
                            if (doctor != null && doctor.isAvailable()) {
                                availableDoctors.add(doctor);
                            }
                        }

                        if (availableDoctors.isEmpty()) {
                            Toast.makeText(HealthWorkerDashboardActivity.this,
                                    "No doctors available at the moment", Toast.LENGTH_SHORT).show();
                        } else {
                            showDoctorsListDialog("Available Doctors", availableDoctors);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HealthWorkerDashboardActivity.this,
                                "Error loading doctors", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEmergencyDoctorsDialog() {
        // Load and show emergency doctors
        doctorsRef.orderByChild("available").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Doctor> emergencyDoctors = new ArrayList<>();
                        for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                            Doctor doctor = doctorSnapshot.getValue(Doctor.class);
                            if (doctor != null && doctor.isAvailable() &&
                                    ("Emergency Department".equals(doctor.getDepartment()) ||
                                            "General Medicine".equals(doctor.getDepartment()))) {
                                emergencyDoctors.add(doctor);
                            }
                        }

                        if (emergencyDoctors.isEmpty()) {
                            Toast.makeText(HealthWorkerDashboardActivity.this,
                                    "No emergency doctors available", Toast.LENGTH_SHORT).show();
                        } else {
                            showDoctorsListDialog("Emergency Doctors", emergencyDoctors);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HealthWorkerDashboardActivity.this,
                                "Error loading emergency doctors", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDoctorsListDialog(String title, List<Doctor> doctors) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(title);

        StringBuilder doctorList = new StringBuilder();
        for (Doctor doctor : doctors) {
            doctorList.append("• Dr. ").append(doctor.getName())
                    .append(" - ").append(doctor.getSpecialty())
                    .append(" (").append(doctor.getDepartment()).append(")\n");

            if (doctor.getCurrentPatientCount() > 0) {
                doctorList.append("  Patients: ").append(doctor.getCurrentPatientCount())
                        .append("/").append(doctor.getMaxPatientsPerDay()).append("\n");
            }
            doctorList.append("\n");
        }

        builder.setMessage(doctorList.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void startTimeUpdates() {
        timeHandler = new Handler();
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                // Update every minute (60000 milliseconds)
                timeHandler.postDelayed(this, 60000);
            }
        };
        // Start the time updates
        timeHandler.post(timeRunnable);
    }

    private void updateDateTime() {
        // Get current date and time
        Date currentDate = new Date();

        // Format date (e.g., "Nov 2, 2024")
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(currentDate);
        tvCurrentDate.setText(formattedDate);

        // Format time (e.g., "02:34 PM")
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(currentDate);
        tvCurrentTime.setText(formattedTime);

        Log.d("TIME_UPDATE", "Updated time: " + formattedDate + " " + formattedTime);
    }

    private void setupClickListeners() {
        // Manage Appointments - Approve appointments and assign tokens
        cardManageAppointments.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageAppointmentsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            intent.putExtra("department", department);
            startActivity(intent);
        });

        // Patient Metrics - Enter patient vital signs and health data
        cardPatientMetrics.setOnClickListener(v -> {
            Intent intent = new Intent(this, PatientMetricsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            startActivity(intent);
        });

        // Test Results - Upload and manage lab results
        cardTestResults.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageTestResultsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            startActivity(intent);
        });

        // Medical Records - View and update patient records
        cardMedicalRecords.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageMedicalRecordsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            startActivity(intent);
        });

        // Completed Examinations - DIRECT ACCESS FOR ALL HEALTH WORKERS
        cardExaminations.setOnClickListener(v -> {
            Log.d("CLICK_DEBUG", "MAIN EXAMINATIONS CARD clicked - Starting activity directly");
            Intent intent = new Intent(this, ManageCompletedExaminationsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            intent.putExtra("userType", userType);
            intent.putExtra("department", department);
            startActivity(intent);
        });

        // Settings - Fixed to go to actual SettingsActivity
        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("healthWorkerId", healthWorkerId);
            intent.putExtra("healthWorkerName", healthWorkerName);
            startActivity(intent);
        });

        // View All Recent Appointments
        TextView tvViewAll = findViewById(R.id.tvViewAll);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(this, ManageAppointmentsActivity.class);
                intent.putExtra("healthWorkerId", healthWorkerId);
                intent.putExtra("healthWorkerName", healthWorkerName);
                intent.putExtra("department", department);
                startActivity(intent);
            });
        }
    }

    private void setupRecentAppointments() {
        // Create a dummy listener that navigates to full management screen when actions are clicked
        HealthWorkerAppointmentsAdapter.OnAppointmentActionListener dashboardListener =
                new HealthWorkerAppointmentsAdapter.OnAppointmentActionListener() {
                    @Override
                    public void onApproveAppointment(Appointment appointment) {
                        // Navigate to full management screen for approval
                        navigateToManageAppointments();
                    }

                    @Override
                    public void onRejectAppointment(Appointment appointment) {
                        // Navigate to full management screen for rejection
                        navigateToManageAppointments();
                    }

                    @Override
                    public void onAssignToken(Appointment appointment, String tokenNumber, String allocatedTime) {
                        // Navigate to full management screen for token assignment
                        navigateToManageAppointments();
                    }
                };

        recentAppointmentsAdapter = new HealthWorkerAppointmentsAdapter(recentAppointments, dashboardListener);
        recyclerRecentAppointments.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentAppointments.setAdapter(recentAppointmentsAdapter);
    }

    private void navigateToManageAppointments() {
        Intent intent = new Intent(this, ManageAppointmentsActivity.class);
        intent.putExtra("healthWorkerId", healthWorkerId);
        intent.putExtra("healthWorkerName", healthWorkerName);
        intent.putExtra("department", department);
        startActivity(intent);
    }

    private void loadDashboardData() {
        loadPendingAppointmentsCount();
        loadTodayAppointmentsCount();
        loadEmergencyAppointmentsCount();
        loadRecentAppointments();
        loadCompletedExaminationsCount();
        loadAvailableDoctorsCount();
        loadEmergencyDoctorsCount();
        checkEmergencyAppointments();
    }

    private void checkEmergencyAppointments() {
        Query emergencyQuery = appointmentsRef.orderByChild("emergency").equalTo(true);

        emergencyQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int activeEmergencies = 0;
                List<String> emergencyPatients = new ArrayList<>();

                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null && appointment.isEmergency() &&
                            ("pending".equalsIgnoreCase(appointment.getStatus()) ||
                                    "emergency".equalsIgnoreCase(appointment.getStatus()))) {
                        activeEmergencies++;
                        emergencyPatients.add(appointment.getPatientName());
                    }
                }

                // Show/hide emergency quick action based on active emergencies
                if (activeEmergencies > 0) {
                    cardEmergencyQuickAction.setVisibility(View.VISIBLE);

                    // Update the emergency action text
                    String actionText;
                    if (activeEmergencies == 1) {
                        actionText = "1 emergency patient needs immediate doctor assignment";
                    } else {
                        actionText = activeEmergencies + " emergency patients need immediate doctor assignment";
                    }
                    tvEmergencyAction.setText(actionText);

                    // Show notification if this is a new emergency
                    showEmergencyNotification(activeEmergencies);
                } else {
                    cardEmergencyQuickAction.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EMERGENCY_CHECK", "Error checking emergency appointments: " + error.getMessage());
            }
        });
    }

    private void showEmergencyNotification(int emergencyCount) {
        // Create a notification for new emergencies
        if (emergencyCount > 0) {
            Toast.makeText(this, "🚨 " + emergencyCount + " emergency case(s) need attention!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadPendingAppointmentsCount() {
        Query pendingQuery;

        // Filter by department if specified and not "All"
        if (department != null && !department.equals("All") && !department.isEmpty()) {
            pendingQuery = appointmentsRef.orderByChild("department").equalTo(department);
            Log.d("PENDING_COUNT", "Counting pending appointments for department: " + department);
        } else {
            pendingQuery = appointmentsRef.orderByChild("status").equalTo("pending");
            Log.d("PENDING_COUNT", "Counting all pending appointments");
        }

        pendingQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long pendingCount = 0;
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null && "pending".equalsIgnoreCase(appointment.getStatus())) {
                        pendingCount++;
                    }
                }
                tvPendingCount.setText(String.valueOf(pendingCount));
                Log.d("PENDING_COUNT", "Final pending count: " + pendingCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvPendingCount.setText("0");
                Log.e("PENDING_COUNT", "Error counting pending: " + error.getMessage());
            }
        });
    }

    private void loadTodayAppointmentsCount() {
        String todayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        Log.d("TODAY_COUNT", "Looking for appointments on: " + todayDate);

        Query todayQuery;

        if (department != null && !department.equals("All") && !department.isEmpty()) {
            // First filter by department, then we'll check date and status manually
            todayQuery = appointmentsRef.orderByChild("department").equalTo(department);
        } else {
            // For "All" departments, we need to check all appointments
            todayQuery = appointmentsRef;
        }

        todayQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long todayCount = 0;
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null) {
                        boolean isToday = todayDate.equals(appointment.getPreferredDate());
                        boolean isValidStatus = "confirmed".equalsIgnoreCase(appointment.getStatus()) ||
                                "scheduled".equalsIgnoreCase(appointment.getStatus());

                        if (isToday && isValidStatus) {
                            todayCount++;
                        }
                    }
                }
                tvTodayCount.setText(String.valueOf(todayCount));
                Log.d("TODAY_COUNT", "Final today count: " + todayCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvTodayCount.setText("0");
                Log.e("TODAY_COUNT", "Error counting today's appointments: " + error.getMessage());
            }
        });
    }

    private void loadEmergencyAppointmentsCount() {
        Query emergencyQuery = appointmentsRef.orderByChild("emergency").equalTo(true);

        emergencyQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long emergencyCount = 0;
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null && appointment.isEmergency() &&
                            ("pending".equalsIgnoreCase(appointment.getStatus()) ||
                                    "emergency".equalsIgnoreCase(appointment.getStatus()))) {
                        emergencyCount++;
                    }
                }

                if (tvEmergencyCount != null) {
                    tvEmergencyCount.setText(String.valueOf(emergencyCount));
                }

                Log.d("EMERGENCY_COUNT", "Active emergency appointments: " + emergencyCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (tvEmergencyCount != null) {
                    tvEmergencyCount.setText("0");
                }
                Log.e("EMERGENCY_COUNT", "Error counting emergency appointments: " + error.getMessage());
            }
        });
    }

    private void loadCompletedExaminationsCount() {
        Query completedQuery = completedExaminationsRef.orderByChild("notificationStatus").equalTo("ready_for_patient");

        completedQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long completedCount = snapshot.getChildrenCount();
                tvCompletedCount.setText(String.valueOf(completedCount));
                Log.d("COMPLETED_COUNT", "Found " + completedCount + " completed examinations ready for patient notification");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvCompletedCount.setText("0");
                Log.e("COMPLETED_COUNT", "Error counting completed examinations: " + error.getMessage());
            }
        });
    }

    private void loadAvailableDoctorsCount() {
        doctorsRef.orderByChild("available").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long availableCount = 0;
                        for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                            Doctor doctor = doctorSnapshot.getValue(Doctor.class);
                            if (doctor != null && doctor.isAvailable()) {
                                availableCount++;
                            }
                        }
                        tvAvailableDoctorsCount.setText(String.valueOf(availableCount));
                        Log.d("DOCTORS_COUNT", "Available doctors: " + availableCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvAvailableDoctorsCount.setText("0");
                        Log.e("DOCTORS_COUNT", "Error counting available doctors: " + error.getMessage());
                    }
                });
    }

    private void loadEmergencyDoctorsCount() {
        doctorsRef.orderByChild("available").equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long emergencyDoctorsCount = 0;
                        for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                            Doctor doctor = doctorSnapshot.getValue(Doctor.class);
                            if (doctor != null && doctor.isAvailable() &&
                                    ("Emergency Department".equals(doctor.getDepartment()) ||
                                            "General Medicine".equals(doctor.getDepartment()))) {
                                emergencyDoctorsCount++;
                            }
                        }
                        tvEmergencyDoctorsCount.setText(String.valueOf(emergencyDoctorsCount));
                        Log.d("EMERGENCY_DOCTORS", "Emergency doctors available: " + emergencyDoctorsCount);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvEmergencyDoctorsCount.setText("0");
                        Log.e("EMERGENCY_DOCTORS", "Error counting emergency doctors: " + error.getMessage());
                    }
                });
    }

    private void loadRecentAppointments() {
        Query recentQuery;

        if (department != null && !department.equals("All") && !department.isEmpty()) {
            // Get recent appointments for this department
            recentQuery = appointmentsRef.orderByChild("department").equalTo(department);
        } else {
            // Get all recent appointments
            recentQuery = appointmentsRef.orderByChild("createdAt").limitToLast(10);
        }

        recentQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentAppointments.clear();
                int loadedCount = 0;

                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null) {
                        // Set appointment ID if not set
                        if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
                            appointment.setAppointmentId(appointmentSnapshot.getKey());
                        }

                        // Show valid status appointments including emergency
                        if (isValidAppointmentStatus(appointment.getStatus())) {
                            recentAppointments.add(0, appointment); // Add to beginning for reverse chronological order
                            loadedCount++;
                        }
                    }
                }

                // Sort by priority (emergency first)
                sortRecentAppointmentsByPriority();

                // Keep only last 5 for dashboard display
                if (recentAppointments.size() > 5) {
                    recentAppointments = new ArrayList<>(recentAppointments.subList(0, 5));
                }

                recentAppointmentsAdapter.updateAppointments(recentAppointments);
                Log.d("RECENT_APPOINTMENTS", "Loaded " + loadedCount + " recent appointments, showing " + recentAppointments.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HealthWorkerDashboardActivity.this,
                        "Failed to load recent appointments", Toast.LENGTH_SHORT).show();
                Log.e("RECENT_APPOINTMENTS", "Error loading recent appointments: " + error.getMessage());
            }
        });
    }

    private void sortRecentAppointmentsByPriority() {
        recentAppointments.sort((a1, a2) -> {
            // First sort by emergency status
            boolean a1Emergency = a1.isEmergency() || "emergency".equalsIgnoreCase(a1.getStatus());
            boolean a2Emergency = a2.isEmergency() || "emergency".equalsIgnoreCase(a2.getStatus());

            if (a1Emergency && !a2Emergency) return -1;
            if (!a1Emergency && a2Emergency) return 1;

            // Then sort by priority weight
            int priorityCompare = Integer.compare(
                    a2.getPriorityWeight(),
                    a1.getPriorityWeight()
            );

            if (priorityCompare != 0) {
                return priorityCompare;
            }

            // For same priority, sort by creation time (newest first)
            try {
                long time1 = Long.parseLong(a1.getCreatedAt());
                long time2 = Long.parseLong(a2.getCreatedAt());
                return Long.compare(time2, time1);
            } catch (NumberFormatException e) {
                return 0;
            }
        });
    }

    private boolean isValidAppointmentStatus(String status) {
        if (status == null) return false;
        String statusLower = status.toLowerCase();
        return statusLower.equals("pending") ||
                statusLower.equals("confirmed") ||
                statusLower.equals("scheduled") ||
                statusLower.equals("emergency");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to dashboard
        Log.d("DASHBOARD", "Refreshing dashboard data on resume");
        loadDashboardData();
        // Update time immediately when resuming
        updateDateTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop time updates when activity is not visible
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handlers to prevent memory leaks
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }
}