package com.example.azimbalife.Activity;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.HealthWorkerAppointmentsAdapter;
import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.Domain.Doctor;
import com.example.azimbalife.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ManageAppointmentsActivity extends AppCompatActivity implements HealthWorkerAppointmentsAdapter.OnAppointmentActionListener {

    private RecyclerView recyclerAppointments;
    private ProgressBar progressBar;
    private TextView tvNoAppointments, tvAppointmentsCount;
    private ChipGroup chipGroupFilters;
    private Chip chipAll, chipPending, chipConfirmed, chipScheduled, chipEmergency;

    private HealthWorkerAppointmentsAdapter appointmentsAdapter;
    private List<Appointment> allAppointments;
    private List<Appointment> filteredAppointments;
    private List<Doctor> availableDoctors;

    private String healthWorkerId;
    private String healthWorkerName;
    private String department;
    private String autoFilter;

    private DatabaseReference appointmentsRef;
    private DatabaseReference patientNotificationsRef;
    private DatabaseReference doctorsRef;

    // Filter counts
    private int allCount = 0;
    private int pendingCount = 0;
    private int confirmedCount = 0;
    private int scheduledCount = 0;
    private int emergencyCount = 0;

    // Current filter
    private String currentFilter = "all";

    // Random generator for token numbers
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointments);

        // Get health worker data from intent
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");
        department = getIntent().getStringExtra("department");
        autoFilter = getIntent().getStringExtra("autoFilter");

        if (healthWorkerId == null) {
            showToast("Health worker not identified");
            finish();
            return;
        }

        Log.d("HEALTH_WORKER_DEBUG", "Health Worker: " + healthWorkerName + ", Department: " + department);
        Log.d("AUTO_FILTER", "Auto filter: " + autoFilter);

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupChipListeners();
        loadAllAppointments(); // Load ALL appointments without department filter
        loadAvailableDoctors();

        // Auto-apply emergency filter if specified
        if ("emergency".equals(autoFilter)) {
            new Handler().postDelayed(() -> {
                Log.d("AUTO_FILTER", "Applying emergency filter automatically");
                onFilterEmergencyClicked();

                // Check if we found any emergencies
                new Handler().postDelayed(() -> {
                    if (filteredAppointments.isEmpty()) {
                        showToast("No emergency appointments found. Showing all appointments instead.");
                        onFilterAllClicked();
                    } else {
                        showToast("Found " + filteredAppointments.size() + " emergency appointments");
                        if (recyclerAppointments.getAdapter() != null && recyclerAppointments.getAdapter().getItemCount() > 0) {
                            recyclerAppointments.scrollToPosition(0);
                        }
                    }
                }, 1000);
            }, 1500);
        }
    }

    private void initializeViews() {
        recyclerAppointments = findViewById(R.id.recyclerAppointments);
        progressBar = findViewById(R.id.progressBar);
        tvNoAppointments = findViewById(R.id.tvNoAppointments);
        tvAppointmentsCount = findViewById(R.id.tvAppointmentsCount);

        // Initialize chips
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipConfirmed = findViewById(R.id.chipConfirmed);
        chipScheduled = findViewById(R.id.chipScheduled);
        chipEmergency = findViewById(R.id.chipEmergency);

        allAppointments = new ArrayList<>();
        filteredAppointments = new ArrayList<>();
        availableDoctors = new ArrayList<>();
    }

    private void setupFirebase() {
        appointmentsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/AllAppointments");
        patientNotificationsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/PatientNotifications");
        doctorsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Doctors");
    }

    private void setupRecyclerView() {
        appointmentsAdapter = new HealthWorkerAppointmentsAdapter(filteredAppointments, this);
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(this));
        recyclerAppointments.setAdapter(appointmentsAdapter);
        recyclerAppointments.setHasFixedSize(true);

        Log.d("RECYCLER_VIEW", "RecyclerView setup completed");
    }

    private void setupChipListeners() {
        chipAll.setOnClickListener(v -> onFilterAllClicked());
        chipPending.setOnClickListener(v -> onFilterPendingClicked());
        chipConfirmed.setOnClickListener(v -> onFilterConfirmedClicked());
        chipScheduled.setOnClickListener(v -> onFilterScheduledClicked());
        chipEmergency.setOnClickListener(v -> onFilterEmergencyClicked());
    }

    private void loadAvailableDoctors() {
        Log.d("DOCTOR_LOAD", "Starting to load doctors from Firebase...");

        doctorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                availableDoctors.clear();
                Log.d("DOCTOR_LOAD", "Firebase snapshot exists: " + snapshot.exists());
                Log.d("DOCTOR_LOAD", "Firebase children count: " + snapshot.getChildrenCount());

                if (snapshot.exists()) {
                    for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                        try {
                            Doctor doctor = doctorSnapshot.getValue(Doctor.class);
                            if (doctor != null) {
                                // Set doctor ID if not set
                                if (doctor.getDoctorId() == null || doctor.getDoctorId().isEmpty()) {
                                    doctor.setDoctorId(doctorSnapshot.getKey());
                                }

                                availableDoctors.add(doctor);
                                Log.d("DOCTOR_LOAD_DETAIL", "✅ Loaded doctor: " + doctor.getName() +
                                        " - " + doctor.getSpecialty() +
                                        " - Dept: " + doctor.getDepartment() +
                                        " - Available: " + doctor.isAvailable() +
                                        " - Patients: " + doctor.getCurrentPatientCount() + "/" + doctor.getMaxPatientsPerDay());
                            } else {
                                Log.d("DOCTOR_LOAD", "❌ Doctor object is null for snapshot: " + doctorSnapshot.getKey());
                            }
                        } catch (Exception e) {
                            Log.e("DOCTOR_LOAD", "Error parsing doctor: " + e.getMessage(), e);
                        }
                    }
                    Log.d("DOCTOR_LOAD", "✅ Total doctors loaded: " + availableDoctors.size());
                } else {
                    Log.d("DOCTOR_LOAD", "❌ No doctors found in database");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DOCTOR_LOAD", "❌ Error loading doctors: " + error.getMessage());
            }
        });
    }

    private void loadAllAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoAppointments.setVisibility(View.GONE);

        // REMOVED DEPARTMENT FILTER - Load ALL appointments
        Query appointmentsQuery = appointmentsRef.orderByChild("createdAt");
        Log.d("APPOINTMENT_LOAD", "Loading ALL appointments without department filter");

        appointmentsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allAppointments.clear();
                int loadedCount = 0;
                int emergencyFound = 0;

                if (snapshot.exists()) {
                    for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                        try {
                            Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                            if (appointment != null) {
                                // Set appointment ID if not set
                                if (appointment.getAppointmentId() == null || appointment.getAppointmentId().isEmpty()) {
                                    appointment.setAppointmentId(appointmentSnapshot.getKey());
                                }

                                // Enhanced emergency detection
                                boolean isEmergency = isEmergencyAppointment(appointment);
                                if (isEmergency && !appointment.isEmergency()) {
                                    appointment.setEmergency(true);
                                    Log.d("EMERGENCY_FIX", "Fixed emergency flag for: " + appointment.getPatientName());
                                }

                                // Include all valid status appointments
                                if (isValidAppointmentStatus(appointment.getStatus())) {
                                    allAppointments.add(appointment);
                                    loadedCount++;

                                    if (isEmergency) {
                                        emergencyFound++;
                                        Log.d("EMERGENCY_LOAD", "🚨 EMERGENCY: " + appointment.getPatientName() +
                                                " - Dept: " + appointment.getDepartment() +
                                                " - Urgency: " + appointment.getUrgencyLevel() +
                                                " - Status: " + appointment.getStatus());
                                    } else {
                                        Log.d("APPOINTMENT_LOAD", "✅ Loaded: " + appointment.getDepartment() +
                                                " - " + appointment.getPatientName() +
                                                " - Status: " + appointment.getStatus());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e("APPOINTMENT_ERROR", "Error parsing appointment: " + e.getMessage(), e);
                        }
                    }

                    // Sort appointments by priority (Emergency first)
                    sortAppointmentsByPriority();
                } else {
                    Log.d("APPOINTMENT_LOAD", "No appointments found in database");
                }

                progressBar.setVisibility(View.GONE);

                // Update chip counts and apply current filter
                updateChipCounts();
                applyCurrentFilter();

                Log.d("APPOINTMENT_LOAD", "✅ Successfully loaded " + loadedCount + " appointments from ALL departments");
                Log.d("EMERGENCY_STATS", "🚨 Found " + emergencyFound + " emergency appointments");

                // Show success message
                if (loadedCount > 0) {
                    showToast("Loaded " + loadedCount + " appointments from all departments");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvNoAppointments.setVisibility(View.VISIBLE);
                tvNoAppointments.setText("Failed to load appointments");
                showToast("Error loading appointments: " + error.getMessage());
                Log.e("APPOINTMENT_ERROR", "Database error: " + error.getMessage());
            }
        });
    }

    // Enhanced emergency detection method
    private boolean isEmergencyAppointment(Appointment appointment) {
        if (appointment == null) return false;

        boolean isEmergency = appointment.isEmergency() ||
                (appointment.getStatus() != null && "emergency".equalsIgnoreCase(appointment.getStatus())) ||
                (appointment.getUrgencyLevel() != null && "emergency".equalsIgnoreCase(appointment.getUrgencyLevel())) ||
                (appointment.getVisitReason() != null &&
                        (appointment.getVisitReason().toLowerCase().contains("emergency") ||
                                appointment.getVisitReason().toLowerCase().contains("critical") ||
                                appointment.getVisitReason().toLowerCase().contains("urgent")));

        return isEmergency;
    }

    private void sortAppointmentsByPriority() {
        Collections.sort(allAppointments, (a1, a2) -> {
            // First sort by emergency status
            boolean a1Emergency = isEmergencyAppointment(a1);
            boolean a2Emergency = isEmergencyAppointment(a2);

            if (a1Emergency && !a2Emergency) return -1;
            if (!a1Emergency && a2Emergency) return 1;

            // Then sort by priority weight
            int priorityCompare = Integer.compare(a2.getPriorityWeight(), a1.getPriorityWeight());

            if (priorityCompare != 0) {
                return priorityCompare;
            }

            // For same priority, sort by creation time (newest first)
            try {
                long time1 = a1.getCreatedAt() != null ? Long.parseLong(a1.getCreatedAt()) : 0;
                long time2 = a2.getCreatedAt() != null ? Long.parseLong(a2.getCreatedAt()) : 0;
                return Long.compare(time2, time1);
            } catch (NumberFormatException e) {
                return 0;
            }
        });
    }

    private void updateChipCounts() {
        pendingCount = 0;
        confirmedCount = 0;
        scheduledCount = 0;
        emergencyCount = 0;

        for (Appointment appointment : allAppointments) {
            String status = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "";

            switch (status) {
                case "pending":
                    pendingCount++;
                    break;
                case "confirmed":
                    confirmedCount++;
                    break;
                case "scheduled":
                    scheduledCount++;
                    break;
                case "emergency":
                    emergencyCount++;
                    break;
            }

            // Count emergency appointments separately
            if (isEmergencyAppointment(appointment)) {
                emergencyCount++;
            }
        }

        allCount = allAppointments.size();

        // Update chip texts
        String allText = "All (" + allCount + ")";
        if (emergencyCount > 0) {
            allText = "All (" + allCount + " 🚨" + emergencyCount + ")";
        }
        chipAll.setText(allText);

        chipPending.setText("Pending (" + pendingCount + ")");
        chipConfirmed.setText("Confirmed (" + confirmedCount + ")");
        chipScheduled.setText("Scheduled (" + scheduledCount + ")");
        chipEmergency.setText("Emergency (" + emergencyCount + ")");

        Log.d("CHIP_COUNTS", "All: " + allCount + ", Pending: " + pendingCount +
                ", Confirmed: " + confirmedCount + ", Scheduled: " + scheduledCount +
                ", Emergency: " + emergencyCount);
    }

    private void applyCurrentFilter() {
        filteredAppointments.clear();

        switch (currentFilter) {
            case "all":
                filteredAppointments.addAll(allAppointments);
                break;
            case "pending":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null && "pending".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                break;
            case "confirmed":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null && "confirmed".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                break;
            case "scheduled":
                for (Appointment appointment : allAppointments) {
                    if (appointment.getStatus() != null && "scheduled".equalsIgnoreCase(appointment.getStatus())) {
                        filteredAppointments.add(appointment);
                    }
                }
                break;
            case "emergency":
                for (Appointment appointment : allAppointments) {
                    if (isEmergencyAppointment(appointment)) {
                        filteredAppointments.add(appointment);
                        Log.d("EMERGENCY_FILTER", "Added emergency: " + appointment.getPatientName() +
                                " - Dept: " + appointment.getDepartment());
                    }
                }
                Log.d("EMERGENCY_FILTER", "Total emergencies found: " + filteredAppointments.size());
                break;
        }

        updateAppointmentsCount();
        appointmentsAdapter.updateAppointments(filteredAppointments);

        if (filteredAppointments.isEmpty()) {
            tvNoAppointments.setVisibility(View.VISIBLE);
            String message = getNoAppointmentsMessage();
            tvNoAppointments.setText(message);
        } else {
            tvNoAppointments.setVisibility(View.GONE);
        }

        Log.d("FILTER_APPLIED", "Applied filter: " + currentFilter + ", showing " + filteredAppointments.size() + " appointments");
    }

    private String getNoAppointmentsMessage() {
        switch (currentFilter) {
            case "pending":
                return "No pending appointments found";
            case "confirmed":
                return "No confirmed appointments found";
            case "scheduled":
                return "No scheduled appointments found";
            case "emergency":
                return "No emergency appointments found\n\nAll appointments from all departments are visible to health workers.";
            default:
                return "No appointments found";
        }
    }

    private boolean isValidAppointmentStatus(String status) {
        if (status == null) return false;
        String statusLower = status.toLowerCase();
        return statusLower.equals("pending") ||
                statusLower.equals("confirmed") ||
                statusLower.equals("scheduled") ||
                statusLower.equals("emergency") ||
                statusLower.equals("approved");
    }

    private void updateAppointmentsCount() {
        String countText;
        int emergencyInFilter = countEmergencyInFilter();

        switch (currentFilter) {
            case "pending":
                countText = pendingCount + " pending appointment" + (pendingCount != 1 ? "s" : "");
                break;
            case "confirmed":
                countText = confirmedCount + " confirmed appointment" + (confirmedCount != 1 ? "s" : "");
                break;
            case "scheduled":
                countText = scheduledCount + " scheduled appointment" + (scheduledCount != 1 ? "s" : "");
                break;
            case "emergency":
                countText = emergencyCount + " emergency appointment" + (emergencyCount != 1 ? "s" : "");
                break;
            default:
                countText = allCount + " appointment" + (allCount != 1 ? "s" : "") + " from all departments";
                break;
        }

        // Add emergency count to display
        if (emergencyInFilter > 0 && !currentFilter.equals("emergency")) {
            countText += " (🚨 " + emergencyInFilter + " emergency)";
        }

        tvAppointmentsCount.setText(countText);
    }

    private int countEmergencyInFilter() {
        int count = 0;
        for (Appointment appointment : filteredAppointments) {
            if (isEmergencyAppointment(appointment)) {
                count++;
            }
        }
        return count;
    }

    // Filter methods
    public void onFilterAllClicked() {
        currentFilter = "all";
        updateChipAppearance();
        applyCurrentFilter();
        Log.d("FILTER", "Applied filter: All");
    }

    public void onFilterPendingClicked() {
        currentFilter = "pending";
        updateChipAppearance();
        applyCurrentFilter();
        Log.d("FILTER", "Applied filter: Pending");
    }

    public void onFilterConfirmedClicked() {
        currentFilter = "confirmed";
        updateChipAppearance();
        applyCurrentFilter();
        Log.d("FILTER", "Applied filter: Confirmed");
    }

    public void onFilterScheduledClicked() {
        currentFilter = "scheduled";
        updateChipAppearance();
        applyCurrentFilter();
        Log.d("FILTER", "Applied filter: Scheduled");
    }

    public void onFilterEmergencyClicked() {
        currentFilter = "emergency";
        updateChipAppearance();
        applyCurrentFilter();
        Log.d("FILTER", "Applied filter: Emergency");

        // Show debug info
        Log.d("EMERGENCY_DEBUG", "Emergency filter clicked - Total appointments: " + allAppointments.size());
        for (Appointment app : allAppointments) {
            if (isEmergencyAppointment(app)) {
                Log.d("EMERGENCY_DEBUG", "Emergency found: " + app.getPatientName() +
                        " - Dept: " + app.getDepartment() +
                        " - Status: " + app.getStatus());
            }
        }
    }

    private void updateChipAppearance() {
        // Reset all chips to default appearance
        resetChipAppearance(chipAll);
        resetChipAppearance(chipPending);
        resetChipAppearance(chipConfirmed);
        resetChipAppearance(chipScheduled);
        resetChipAppearance(chipEmergency);

        // Set selected chip appearance
        switch (currentFilter) {
            case "all":
                setSelectedChipAppearance(chipAll);
                break;
            case "pending":
                setSelectedChipAppearance(chipPending);
                break;
            case "confirmed":
                setSelectedChipAppearance(chipConfirmed);
                break;
            case "scheduled":
                setSelectedChipAppearance(chipScheduled);
                break;
            case "emergency":
                setSelectedChipAppearance(chipEmergency);
                break;
        }
    }

    private void resetChipAppearance(Chip chip) {
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setChipStrokeColorResource(R.color.chip_stroke);
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void setSelectedChipAppearance(Chip chip) {
        chip.setChipBackgroundColorResource(R.color.chip_selected);
        chip.setChipStrokeColorResource(R.color.my_primary);
        chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text_selected));
    }

    // Enhanced method to load doctors with better filtering
    private List<Doctor> getFilteredDoctors(String department, boolean isEmergency) {
        List<Doctor> filtered = new ArrayList<>();

        Log.d("DOCTOR_FILTER_DEBUG", "Filtering doctors for appointment department: " + department + ", emergency: " + isEmergency);

        if (availableDoctors.isEmpty()) {
            Log.d("DOCTOR_FILTER_DEBUG", "No available doctors to filter");
            return filtered;
        }

        for (Doctor doctor : availableDoctors) {
            if (doctor == null || doctor.getName() == null) {
                continue;
            }

            boolean isAvailable = doctor.isAvailable();
            boolean hasCapacity = doctor.getCurrentPatientCount() < doctor.getMaxPatientsPerDay();

            // For health workers, show doctors from ALL departments
            boolean departmentMatches = true; // Health workers can assign to any department

            // For emergencies, prioritize emergency department doctors
            boolean suitableForEmergency = isEmergency &&
                    doctor.getDepartment() != null &&
                    ("Emergency Department".equals(doctor.getDepartment()) ||
                            "General Medicine".equals(doctor.getDepartment()) ||
                            "Emergency".equals(doctor.getDepartment()));

            Log.d("DOCTOR_FILTER_DEBUG", "Doctor: " + doctor.getName() +
                    " | Available: " + isAvailable +
                    " | Capacity: " + hasCapacity +
                    " | Dept: " + doctor.getDepartment() +
                    " | Emergency Suitable: " + suitableForEmergency);

            if (departmentMatches || suitableForEmergency) {
                filtered.add(doctor);
                Log.d("DOCTOR_FILTER_DEBUG", "✅ ADDED DOCTOR: " + doctor.getName());
            }
        }

        Log.d("DOCTOR_FILTER_DEBUG", "Filtered " + filtered.size() + " doctors");
        return filtered;
    }

    // Helper method to check if emergency dialog layout exists
    private boolean isEmergencyDialogLayoutAvailable() {
        try {
            int layoutId = R.layout.dialog_emergency_assignment;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onApproveAppointment(Appointment appointment) {
        Log.d("APPOINTMENT_ACTION", "Approve clicked for: " + appointment.getPatientName());

        // For emergency appointments, handle differently
        if (isEmergencyAppointment(appointment)) {
            Log.d("APPOINTMENT_ACTION", "This is an EMERGENCY appointment");

            if (isEmergencyDialogLayoutAvailable()) {
                showEnhancedEmergencyAssignmentDialog(appointment);
            } else {
                showTokenAndDoctorAssignmentDialog(appointment);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Approve Appointment")
                    .setMessage("Approve appointment for " + appointment.getPatientName() + "?")
                    .setPositiveButton("APPROVE", (dialog, which) -> {
                        approveAppointmentInFirebase(appointment);
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        }
    }

    private void showEnhancedEmergencyAssignmentDialog(Appointment appointment) {
        Log.d("EMERGENCY_DIALOG", "Showing emergency assignment dialog for: " + appointment.getPatientName());

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("🚨 Emergency Case Assignment");

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_emergency_assignment, null);
            builder.setView(dialogView);

            TextView tvPatientInfo = dialogView.findViewById(R.id.tvPatientInfo);
            TextView tvEmergencyDetails = dialogView.findViewById(R.id.tvEmergencyDetails);
            ProgressBar progressBarDoctors = dialogView.findViewById(R.id.progressBarDoctors);
            Spinner spinnerEmergencyDoctors = dialogView.findViewById(R.id.spinnerEmergencyDoctors);
            Button btnAssignImmediately = dialogView.findViewById(R.id.btnAssignImmediately);
            Button btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);

            // Set patient information
            tvPatientInfo.setText("Patient: " + appointment.getPatientName() +
                    "\nDepartment: " + appointment.getDepartment());

            String urgencyLevel = appointment.getUrgencyLevel() != null ? appointment.getUrgencyLevel() : "Unknown";
            String visitReason = appointment.getVisitReason() != null ? appointment.getVisitReason() : "Not specified";

            tvEmergencyDetails.setText("Emergency Reason: " + visitReason +
                    "\nUrgency Level: " + urgencyLevel +
                    "\nPriority: " + appointment.getPriorityWeight());

            // Load available emergency doctors
            loadEmergencyDoctors(spinnerEmergencyDoctors, progressBarDoctors, appointment);

            AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);

            btnAssignImmediately.setOnClickListener(v -> {
                if (spinnerEmergencyDoctors.getSelectedItem() instanceof Doctor) {
                    Doctor selectedDoctor = (Doctor) spinnerEmergencyDoctors.getSelectedItem();
                    if (selectedDoctor != null && !"Select Emergency Doctor".equals(selectedDoctor.getName())) {
                        assignEmergencyToDoctor(appointment, selectedDoctor);
                        dialog.dismiss();
                    } else {
                        showToast("Please select an emergency doctor");
                    }
                } else {
                    showToast("Please select a valid emergency doctor");
                }
            });

            btnViewDetails.setOnClickListener(v -> {
                dialog.dismiss();
                showEmergencyDetails(appointment);
            });

            dialog.show();

        } catch (Exception e) {
            Log.e("EMERGENCY_DIALOG", "Error showing emergency dialog: " + e.getMessage(), e);
            showToast("Error showing emergency dialog: " + e.getMessage());
            showTokenAndDoctorAssignmentDialog(appointment);
        }
    }

    private void loadEmergencyDoctors(Spinner spinner, ProgressBar progressBar, Appointment appointment) {
        progressBar.setVisibility(View.VISIBLE);

        List<Doctor> emergencyDoctors = new ArrayList<>();

        // Add default option
        Doctor defaultDoctor = new Doctor();
        defaultDoctor.setName("Select Emergency Doctor");
        defaultDoctor.setSpecialty("");
        emergencyDoctors.add(defaultDoctor);

        // Get filtered emergency doctors
        List<Doctor> filteredDoctors = getFilteredDoctors(appointment.getDepartment(), true);
        emergencyDoctors.addAll(filteredDoctors);

        Log.d("EMERGENCY_DOCTORS", "Loading " + filteredDoctors.size() + " emergency doctors");

        // Create adapter
        ArrayAdapter<Doctor> adapter = new ArrayAdapter<Doctor>(this,
                android.R.layout.simple_spinner_item, emergencyDoctors) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setText("Select Emergency Doctor");
                    textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    Doctor doctor = emergencyDoctors.get(position);
                    String displayText = "🚨 Dr. " + doctor.getName() + " - " + doctor.getSpecialty();
                    textView.setText(displayText);
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setText("Select Emergency Doctor");
                    textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    Doctor doctor = emergencyDoctors.get(position);
                    String displayText = "🚨 Dr. " + doctor.getName() + " - " + doctor.getSpecialty();
                    if (doctor.getDepartment() != null) {
                        displayText += " (" + doctor.getDepartment() + ")";
                    }
                    if (doctor.getCurrentPatientCount() > 0) {
                        displayText += " [" + doctor.getCurrentPatientCount() + "/" + doctor.getMaxPatientsPerDay() + "]";
                    }
                    textView.setText(displayText);
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        progressBar.setVisibility(View.GONE);

        if (emergencyDoctors.size() <= 1) {
            showToast("No emergency doctors available");
        }
    }

    private void assignEmergencyToDoctor(Appointment appointment, Doctor doctor) {
        Log.d("EMERGENCY_ASSIGN", "Assigning emergency to Dr. " + doctor.getName());

        // Generate emergency token
        String emergencyToken = "EMG-" + (1000 + random.nextInt(9000));
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        assignDoctorAndTokenToAppointment(appointment, doctor, emergencyToken, currentTime);
    }

    private void showEmergencyDetails(Appointment appointment) {
        new AlertDialog.Builder(this)
                .setTitle("🚨 Emergency Details")
                .setMessage("Patient: " + appointment.getPatientName() + "\n" +
                        "Department: " + appointment.getDepartment() + "\n" +
                        "Reason: " + (appointment.getVisitReason() != null ? appointment.getVisitReason() : "Not specified") + "\n" +
                        "Urgency: " + (appointment.getUrgencyLevel() != null ? appointment.getUrgencyLevel() : "Unknown") + "\n" +
                        "Priority Weight: " + appointment.getPriorityWeight() + "\n" +
                        "Registered: " + formatTimestamp(appointment.getCreatedAt()))
                .setPositiveButton("ASSIGN DOCTOR", (dialog, which) -> {
                    showTokenAndDoctorAssignmentDialog(appointment);
                })
                .setNegativeButton("CLOSE", null)
                .show();
    }

    private String formatTimestamp(String timestamp) {
        try {
            long time = Long.parseLong(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(time));
        } catch (NumberFormatException e) {
            return "Unknown";
        }
    }

    @Override
    public void onRejectAppointment(Appointment appointment) {
        Log.d("APPOINTMENT_ACTION", "Reject clicked for: " + appointment.getPatientName());

        if (isEmergencyAppointment(appointment)) {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Reject Emergency?")
                    .setMessage("This is an emergency case. Are you sure you want to reject it?")
                    .setPositiveButton("REJECT EMERGENCY", (dialog, which) -> {
                        rejectAppointmentInFirebase(appointment);
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Reject Appointment")
                    .setMessage("Reject appointment for " + appointment.getPatientName() + "?")
                    .setPositiveButton("REJECT", (dialog, which) -> {
                        rejectAppointmentInFirebase(appointment);
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        }
    }

    @Override
    public void onAssignToken(Appointment appointment, String tokenNumber, String allocatedTime) {
        Log.d("APPOINTMENT_ACTION", "Assign token clicked for: " + appointment.getPatientName());
        showTokenAndDoctorAssignmentDialog(appointment);
    }

    private void approveAppointmentInFirebase(Appointment appointment) {
        DatabaseReference appointmentRef = appointmentsRef.child(appointment.getAppointmentId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "confirmed");
        updates.put("approvedBy", healthWorkerName);
        updates.put("approvedAt", String.valueOf(System.currentTimeMillis()));

        appointmentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    showToast("Appointment approved successfully!");
                    loadAllAppointments();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to approve appointment");
                });
    }

    private void rejectAppointmentInFirebase(Appointment appointment) {
        DatabaseReference appointmentRef = appointmentsRef.child(appointment.getAppointmentId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("approvedBy", healthWorkerName);
        updates.put("approvedAt", String.valueOf(System.currentTimeMillis()));

        appointmentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    showToast("Appointment rejected");
                    loadAllAppointments();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to reject appointment");
                });
    }

    private void showTokenAndDoctorAssignmentDialog(Appointment appointment) {
        Log.d("TOKEN_DIALOG", "Showing token assignment dialog for: " + appointment.getPatientName());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String title = isEmergencyAppointment(appointment) ?
                "🚨 Assign Emergency Doctor & Token" :
                "Assign Doctor, Token & Schedule";

        builder.setTitle(title);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_assign_token, null);
        builder.setView(dialogView);

        Spinner spinnerDoctors = dialogView.findViewById(R.id.spinnerDoctors);
        ProgressBar progressBarDoctors = dialogView.findViewById(R.id.progressBarDoctors);
        EditText etTokenNumber = dialogView.findViewById(R.id.etTokenNumber);
        EditText etAllocatedTime = dialogView.findViewById(R.id.etAllocatedTime);
        TextView tvTokenHint = dialogView.findViewById(R.id.tvTokenHint);

        // Generate and set appropriate token
        String token;
        boolean isEmergency = isEmergencyAppointment(appointment);

        if (isEmergency) {
            token = "EMG-" + (1000 + random.nextInt(9000));
            tvTokenHint.setText("Emergency token automatically generated");
            tvTokenHint.setTextColor(ContextCompat.getColor(this, R.color.emergency_red));
        } else {
            token = "UG-" + (1000 + random.nextInt(9000));
            tvTokenHint.setText("Token is automatically generated with UG- prefix");
        }
        etTokenNumber.setText(token);

        // Set current time as default
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        etAllocatedTime.setText(currentTime);

        // Setup time picker
        etAllocatedTime.setOnClickListener(v -> showTimePicker(etAllocatedTime));

        // Load doctors into spinner
        loadDoctorsIntoSpinner(spinnerDoctors, progressBarDoctors, appointment.getDepartment(), isEmergency);

        builder.setPositiveButton("ASSIGN", (dialog, which) -> {
            String tokenNumber = etTokenNumber.getText().toString().trim();
            String allocatedTime = etAllocatedTime.getText().toString().trim();

            if (spinnerDoctors.getSelectedItem() instanceof Doctor) {
                Doctor selectedDoctor = (Doctor) spinnerDoctors.getSelectedItem();

                if (selectedDoctor == null || "Select Doctor".equals(selectedDoctor.getName())) {
                    showToast("Please select a doctor");
                    return;
                }

                if (TextUtils.isEmpty(tokenNumber)) {
                    showToast("Please enter token number");
                    return;
                }

                if (TextUtils.isEmpty(allocatedTime)) {
                    showToast("Please enter allocated time");
                    return;
                }

                assignDoctorAndTokenToAppointment(appointment, selectedDoctor, tokenNumber, allocatedTime);
            } else {
                showToast("Please select a valid doctor");
            }
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void loadDoctorsIntoSpinner(Spinner spinnerDoctors, ProgressBar progressBar, String department, boolean isEmergency) {
        progressBar.setVisibility(View.VISIBLE);

        List<Doctor> filteredDoctors = new ArrayList<>();

        // Add a default option
        Doctor defaultDoctor = new Doctor();
        defaultDoctor.setName("Select Doctor");
        defaultDoctor.setSpecialty("");
        defaultDoctor.setDepartment("");
        filteredDoctors.add(defaultDoctor);

        // Get filtered doctors - Health workers can see ALL doctors
        List<Doctor> suitableDoctors = getFilteredDoctors(department, isEmergency);
        filteredDoctors.addAll(suitableDoctors);

        Log.d("DOCTOR_SPINNER", "Loaded " + (filteredDoctors.size() - 1) + " doctors for assignment");

        // Create adapter for spinner
        ArrayAdapter<Doctor> doctorAdapter = new ArrayAdapter<Doctor>(this,
                android.R.layout.simple_spinner_item, filteredDoctors) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setText("Select Doctor");
                    textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    Doctor doctor = filteredDoctors.get(position);
                    String displayText = doctor.getName() + " - " + doctor.getSpecialty();
                    if (doctor.getDepartment() != null && !doctor.getDepartment().isEmpty()) {
                        displayText += " (" + doctor.getDepartment() + ")";
                    }
                    textView.setText(displayText);
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setText("Select Doctor");
                    textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                } else {
                    Doctor doctor = filteredDoctors.get(position);
                    String displayText = doctor.getName() + " - " + doctor.getSpecialty();
                    if (doctor.getDepartment() != null && !doctor.getDepartment().isEmpty()) {
                        displayText += " (" + doctor.getDepartment() + ")";
                    }
                    if (doctor.getCurrentPatientCount() > 0) {
                        displayText += " [" + doctor.getCurrentPatientCount() + "/" + doctor.getMaxPatientsPerDay() + "]";
                    }
                    textView.setText(displayText);
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };

        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDoctors.setAdapter(doctorAdapter);
        progressBar.setVisibility(View.GONE);

        if (filteredDoctors.size() <= 1) {
            showToast("No doctors available");
        }
    }

    private void showTimePicker(EditText etAllocatedTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        etAllocatedTime.setText(selectedTime);
                    }
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void assignDoctorAndTokenToAppointment(Appointment appointment, Doctor doctor,
                                                   String tokenNumber, String allocatedTime) {
        DatabaseReference appointmentRef = appointmentsRef.child(appointment.getAppointmentId());

        // Calculate consultation duration based on urgency
        int consultationDuration = calculateConsultationDuration(appointment);

        // Update appointment with doctor, token and time
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "scheduled");
        updates.put("assignedDoctorId", doctor.getDoctorId());
        updates.put("assignedDoctorName", doctor.getName());
        updates.put("assignedDoctorSpecialization", doctor.getSpecialty());
        updates.put("doctorAssigned", true);
        updates.put("doctorAssignmentTime", String.valueOf(System.currentTimeMillis()));
        updates.put("tokenNumber", tokenNumber);
        updates.put("allocatedTime", allocatedTime);
        updates.put("scheduledAt", String.valueOf(System.currentTimeMillis()));
        updates.put("consultationDuration", consultationDuration);

        boolean isEmergency = isEmergencyAppointment(appointment);

        // For emergencies, mark as handled
        if (isEmergency) {
            updates.put("emergencyHandled", true);
            updates.put("emergencyHandledAt", System.currentTimeMillis());
        }

        appointmentRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    String message = isEmergency ?
                            "🚨 Emergency assigned to Dr. " + doctor.getName() + " successfully!" :
                            "Doctor and token assigned successfully!";

                    showToast(message);
                    Log.d("DOCTOR_TOKEN_ASSIGN", "✅ Assigned Dr. " + doctor.getName() +
                            " and token " + tokenNumber + " to appointment: " + appointment.getAppointmentId());

                    // Update doctor's patient count
                    updateDoctorPatientCount(doctor);

                    // Send notification to patient
                    sendPatientNotification(appointment, doctor, tokenNumber, allocatedTime);

                    loadAllAppointments(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to assign doctor and token");
                    Log.e("DOCTOR_TOKEN_ASSIGN", "❌ Error: " + e.getMessage());
                });
    }

    private int calculateConsultationDuration(Appointment appointment) {
        boolean isEmergency = isEmergencyAppointment(appointment);

        if (isEmergency) {
            return 45; // 45 minutes for emergencies
        }

        String urgencyLevel = appointment.getUrgencyLevel() != null ? appointment.getUrgencyLevel().toLowerCase() : "normal";

        switch (urgencyLevel) {
            case "emergency":
                return 45;
            case "urgent":
                return 30;
            case "normal":
            default:
                return 20;
        }
    }

    private void updateDoctorPatientCount(Doctor doctor) {
        if (doctor.getDoctorId() != null) {
            DatabaseReference doctorRef = doctorsRef.child(doctor.getDoctorId());

            // Increment patient count
            int currentCount = doctor.getCurrentPatientCount();
            int newPatientCount = currentCount + 1;

            doctorRef.child("currentPatientCount").setValue(newPatientCount)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("DOCTOR_UPDATE", "✅ Updated patient count for Dr. " + doctor.getName() + " to " + newPatientCount);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DOCTOR_UPDATE", "❌ Failed to update patient count: " + e.getMessage());
                    });
        }
    }

    private void sendPatientNotification(Appointment appointment, Doctor doctor,
                                         String tokenNumber, String allocatedTime) {
        String notificationId = patientNotificationsRef.push().getKey();
        if (notificationId == null) return;

        String title, message;

        boolean isEmergency = isEmergencyAppointment(appointment);

        if (isEmergency) {
            title = "🚨 Emergency Appointment Scheduled";
            message = "Your emergency has been assigned to Dr. " + doctor.getName() +
                    ". Please proceed immediately. Token: " + tokenNumber + ", Time: " + allocatedTime;
        } else {
            title = "Appointment Scheduled";
            message = "Your appointment with Dr. " + doctor.getName() +
                    " has been scheduled. Token: " + tokenNumber + ", Time: " + allocatedTime;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("patientUsername", appointment.getPatientUsername());
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        notification.put("type", isEmergency ? "emergency_scheduled" : "appointment_scheduled");

        patientNotificationsRef.child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d("NOTIFICATION", "✅ Notification sent to patient: " + appointment.getPatientUsername());
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTIFICATION", "❌ Failed to send notification: " + e.getMessage());
                });
    }

    // Helper method to show toast with error handling
    private void showToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("TOAST_ERROR", "Failed to show toast: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        Log.d("MANAGE_APPOINTMENTS", "Refreshing appointments on resume");
        loadAllAppointments();
        loadAvailableDoctors();
    }

    public void onBackClicked(View view) {
        onBackPressed();
    }

    public void onRefreshClicked(View view) {
        loadAllAppointments();
        loadAvailableDoctors();
        showToast("Refreshing appointments...");
    }
}