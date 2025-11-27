package com.example.azimbalife.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.Domain.HealthMetrics;
import com.example.azimbalife.Domain.PatientMetrics;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PatientMetricsActivity extends AppCompatActivity {

    private EditText etTemperature, etBloodPressure, etHeartRate,
            etRespiratoryRate, etOxygenSaturation, etHeight, etWeight,
            etBloodSugar, etNotes;
    private Spinner spinnerScheduledPatients;
    private Button btnSubmit, btnClear, btnViewHistory, btnRefresh;
    private ProgressBar progressBar;
    private TextView tvPatientCount, tvSelectedPatientInfo;

    private String healthWorkerId;
    private String healthWorkerName;

    private DatabaseReference metricsRef;
    private DatabaseReference appointmentsRef;
    private DatabaseReference patientsRef;
    private DatabaseReference usersRef;

    private List<Appointment> scheduledAppointments;
    private List<String> displayList;
    private Map<String, String> patientNameMap;
    private String selectedPatientId;
    private String selectedPatientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_metrics);

        // Get health worker data from intent
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");

        if (healthWorkerId == null) {
            Toast.makeText(this, "Health worker not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupFirebase();
        setupClickListeners();
        loadScheduledPatients();
    }

    private void initializeViews() {
        etTemperature = findViewById(R.id.etTemperature);
        etBloodPressure = findViewById(R.id.etBloodPressure);
        etHeartRate = findViewById(R.id.etHeartRate);
        etRespiratoryRate = findViewById(R.id.etRespiratoryRate);
        etOxygenSaturation = findViewById(R.id.etOxygenSaturation);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        etBloodSugar = findViewById(R.id.etBloodSugar);
        etNotes = findViewById(R.id.etNotes);

        spinnerScheduledPatients = findViewById(R.id.spinnerPatients);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnClear = findViewById(R.id.btnClear);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnRefresh = findViewById(R.id.btnRefresh);
        progressBar = findViewById(R.id.progressBar);
        tvPatientCount = findViewById(R.id.tvPatientCount);
        tvSelectedPatientInfo = findViewById(R.id.tvSelectedPatientInfo);

        scheduledAppointments = new ArrayList<>();
        displayList = new ArrayList<>();
        patientNameMap = new HashMap<>();

        // Initialize with default values
        displayList.add("Select Scheduled Patient");
        setupSpinnerWithDefault();
    }

    private void setupSpinnerWithDefault() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, displayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerScheduledPatients.setAdapter(adapter);
    }

    private void setupFirebase() {
        patientsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Patients");
        metricsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/PatientMetrics");
        appointmentsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/AllAppointments");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        Log.d("FIREBASE", "Database references initialized");
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> submitPatientMetrics());
        btnClear.setOnClickListener(v -> clearForm());
        btnViewHistory.setOnClickListener(v -> viewPatientHistory());
        btnRefresh.setOnClickListener(v -> refreshScheduledPatients());

        // FIXED: Safe spinner item selection
        spinnerScheduledPatients.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && position - 1 < scheduledAppointments.size()) {
                    try {
                        Appointment selectedAppointment = scheduledAppointments.get(position - 1);
                        selectedPatientId = selectedAppointment.getPatientUsername();
                        selectedPatientName = patientNameMap.get(selectedPatientId);

                        if (selectedPatientName == null) {
                            selectedPatientName = "Unknown Patient";
                        }

                        updateSelectedPatientInfo(selectedAppointment);
                        Log.d("PATIENT_SELECT", "Selected patient: " + selectedPatientName + " (" + selectedPatientId + ")");
                    } catch (Exception e) {
                        Log.e("SPINNER_ERROR", "Error selecting patient: " + e.getMessage());
                        Toast.makeText(PatientMetricsActivity.this, "Error selecting patient", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    selectedPatientId = null;
                    selectedPatientName = null;
                    tvSelectedPatientInfo.setText("No patient selected");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedPatientId = null;
                selectedPatientName = null;
                tvSelectedPatientInfo.setText("No patient selected");
            }
        });
    }

    private void updateSelectedPatientInfo(Appointment appointment) {
        if (appointment == null) return;

        String department = appointment.getDepartment() != null ? appointment.getDepartment() : "Unknown Dept";
        String time = appointment.getAllocatedTime() != null ? appointment.getAllocatedTime() : "Time TBD";
        String token = appointment.getTokenNumber() != null ? "Token: " + appointment.getTokenNumber() : "No token";

        String info = "Patient: " + selectedPatientName +
                "\nDepartment: " + department +
                "\nTime: " + time +
                "\n" + token;

        tvSelectedPatientInfo.setText(info);
    }

    private void refreshScheduledPatients() {
        loadScheduledPatients();
        Toast.makeText(this, "Refreshing scheduled patients...", Toast.LENGTH_SHORT).show();
    }

    private void loadScheduledPatients() {
        progressBar.setVisibility(View.VISIBLE);
        scheduledAppointments.clear();
        displayList.clear();
        patientNameMap.clear();

        // Reset to default
        displayList.add("Select Scheduled Patient");
        setupSpinnerWithDefault();

        Log.d("SCHEDULED_LOAD", "Loading scheduled patients...");

        // Load appointments with status "scheduled"
        Query scheduledQuery = appointmentsRef.orderByChild("status").equalTo("scheduled");

        scheduledQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("SCHEDULED_LOAD", "Found " + snapshot.getChildrenCount() + " scheduled appointments");

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    showNoScheduledPatients();
                    return;
                }

                // First, collect all scheduled appointments
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    try {
                        Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                        if (appointment != null &&
                                appointment.getPatientUsername() != null &&
                                !appointment.getPatientUsername().isEmpty()) {

                            scheduledAppointments.add(appointment);
                            Log.d("APPOINTMENT", "Scheduled: " + appointment.getPatientUsername() +
                                    " - " + appointment.getDepartment());
                        }
                    } catch (Exception e) {
                        Log.e("APPOINTMENT_ERROR", "Error parsing appointment: " + e.getMessage());
                    }
                }

                if (scheduledAppointments.isEmpty()) {
                    showNoScheduledPatients();
                    return;
                }

                Log.d("SCHEDULED_LOAD", "Processing " + scheduledAppointments.size() + " scheduled appointments");

                // Now load patient names for these appointments
                loadPatientNames();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("SCHEDULED_LOAD", "Error loading appointments: " + error.getMessage());
                Toast.makeText(PatientMetricsActivity.this,
                        "Error loading scheduled patients: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPatientNames() {
        final int totalPatients = scheduledAppointments.size();
        final int[] loadedCount = {0};

        Log.d("NAME_LOAD", "Loading names for " + totalPatients + " patients");

        for (Appointment appointment : scheduledAppointments) {
            String patientId = appointment.getPatientUsername();

            if (patientId == null || patientId.isEmpty()) {
                loadedCount[0]++;
                continue;
            }

            // Try MbararaHospital/Patients first
            patientsRef.child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String patientName = "Unknown Patient";
                    if (snapshot.exists()) {
                        patientName = getPatientNameFromSnapshot(snapshot);
                    } else {
                        // Fallback to Users collection
                        loadPatientNameFromUsers(patientId, loadedCount, totalPatients);
                        return;
                    }

                    patientNameMap.put(patientId, patientName);
                    loadedCount[0]++;

                    Log.d("PATIENT_NAME", "Loaded name for " + patientId + ": " + patientName);

                    checkAllNamesLoaded(loadedCount[0], totalPatients);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("NAME_LOAD", "Error loading patient name from Patients: " + error.getMessage());
                    loadPatientNameFromUsers(patientId, loadedCount, totalPatients);
                }
            });
        }
    }

    private void loadPatientNameFromUsers(String patientId, int[] loadedCount, int totalPatients) {
        usersRef.child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String patientName = "Unknown Patient";
                if (snapshot.exists()) {
                    patientName = getPatientNameFromSnapshot(snapshot);
                }

                patientNameMap.put(patientId, patientName);
                loadedCount[0]++;

                Log.d("PATIENT_NAME", "Loaded name from Users for " + patientId + ": " + patientName);
                checkAllNamesLoaded(loadedCount[0], totalPatients);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NAME_LOAD", "Error loading patient name from Users: " + error.getMessage());
                patientNameMap.put(patientId, "Unknown Patient");
                loadedCount[0]++;
                checkAllNamesLoaded(loadedCount[0], totalPatients);
            }
        });
    }

    private void checkAllNamesLoaded(int loadedCount, int totalPatients) {
        Log.d("NAME_LOAD", "Progress: " + loadedCount + "/" + totalPatients + " names loaded");

        if (loadedCount >= totalPatients) {
            Log.d("NAME_LOAD", "All names loaded, setting up spinner");
            setupPatientSpinner();
        }
    }

    private String getPatientNameFromSnapshot(DataSnapshot snapshot) {
        try {
            // Try multiple possible field names for patient name
            if (snapshot.child("name").exists()) {
                return snapshot.child("name").getValue(String.class);
            } else if (snapshot.child("patientName").exists()) {
                return snapshot.child("patientName").getValue(String.class);
            } else if (snapshot.child("username").exists()) {
                return snapshot.child("username").getValue(String.class);
            } else if (snapshot.child("fullName").exists()) {
                return snapshot.child("fullName").getValue(String.class);
            } else if (snapshot.child("firstName").exists()) {
                String firstName = snapshot.child("firstName").getValue(String.class);
                String lastName = snapshot.child("lastName").getValue(String.class);
                if (firstName != null && lastName != null) {
                    return firstName + " " + lastName;
                }
                return firstName != null ? firstName : "Unknown";
            }
        } catch (Exception e) {
            Log.e("NAME_ERROR", "Error getting patient name: " + e.getMessage());
        }
        return "Unknown Patient";
    }

    private void setupPatientSpinner() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);

            displayList.clear();
            displayList.add("Select Scheduled Patient");

            for (Appointment appointment : scheduledAppointments) {
                String patientId = appointment.getPatientUsername();
                String patientName = patientNameMap.get(patientId);
                String department = appointment.getDepartment() != null ? appointment.getDepartment() : "Unknown";
                String time = appointment.getAllocatedTime() != null ?
                        appointment.getAllocatedTime() : "Time TBD";
                String token = appointment.getTokenNumber() != null ?
                        appointment.getTokenNumber() : "";

                String displayText = patientName + " - " + department + " (" + time + ")";
                if (!token.isEmpty()) {
                    displayText += " - Token: " + token;
                }

                displayList.add(displayText);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, displayList) {
                @Override
                public boolean isEnabled(int position) {
                    return position != 0; // Disable the first item (hint)
                }

                @Override
                public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView textView = (TextView) view;

                    if (position == 0) {
                        textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    } else {
                        textView.setTextColor(getResources().getColor(android.R.color.black));
                    }
                    return view;
                }
            };

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerScheduledPatients.setAdapter(adapter);

            String summary = "Loaded " + scheduledAppointments.size() + " scheduled patients";
            tvPatientCount.setText(summary);

            if (scheduledAppointments.size() > 0) {
                Toast.makeText(this, summary, Toast.LENGTH_SHORT).show();
            }

            Log.d("SPINNER", "Spinner setup with " + scheduledAppointments.size() + " scheduled patients");
        });
    }

    private void showNoScheduledPatients() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);

            displayList.clear();
            displayList.add("No Scheduled Patients Available");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, displayList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerScheduledPatients.setAdapter(adapter);

            tvPatientCount.setText("No scheduled patients found");
            tvSelectedPatientInfo.setText("Please check back later for scheduled appointments");

            Toast.makeText(this,
                    "No patients are currently scheduled. Please check the appointments system.",
                    Toast.LENGTH_LONG).show();
        });
    }

    private void submitPatientMetrics() {
        // Validate patient selection
        if (selectedPatientId == null) {
            Toast.makeText(this, "Please select a patient from the scheduled list", Toast.LENGTH_SHORT).show();
            spinnerScheduledPatients.requestFocus();
            return;
        }

        String temperature = etTemperature.getText().toString().trim();
        String bloodPressure = etBloodPressure.getText().toString().trim();
        String heartRate = etHeartRate.getText().toString().trim();
        String respiratoryRate = etRespiratoryRate.getText().toString().trim();
        String oxygenSaturation = etOxygenSaturation.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
        String bloodSugar = etBloodSugar.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Validate that at least one metric is provided
        if (TextUtils.isEmpty(temperature) && TextUtils.isEmpty(bloodPressure) &&
                TextUtils.isEmpty(heartRate) && TextUtils.isEmpty(respiratoryRate) &&
                TextUtils.isEmpty(oxygenSaturation) && TextUtils.isEmpty(height) &&
                TextUtils.isEmpty(weight) && TextUtils.isEmpty(bloodSugar)) {
            Toast.makeText(this, "Please enter at least one patient metric", Toast.LENGTH_SHORT).show();
            etTemperature.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        savePatientMetrics(selectedPatientId, selectedPatientName, temperature, bloodPressure,
                heartRate, respiratoryRate, oxygenSaturation, height, weight,
                bloodSugar, notes);
    }

    private void savePatientMetrics(String patientId, String patientName, String temperature,
                                    String bloodPressure, String heartRate, String respiratoryRate,
                                    String oxygenSaturation, String height, String weight,
                                    String bloodSugar, String notes) {

        try {
            String timestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            String recordId = metricsRef.push().getKey();

            if (recordId == null) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error generating record ID", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("SAVE_METRICS", "Saving metrics for scheduled patient: " + patientId);

            PatientMetrics metrics = new PatientMetrics();
            metrics.setRecordId(recordId);
            metrics.setPatientId(patientId);
            metrics.setPatientName(patientName != null ? patientName : "Unknown Patient");
            metrics.setHealthWorkerId(healthWorkerId);
            metrics.setHealthWorkerName(healthWorkerName != null ? healthWorkerName : "Unknown Health Worker");
            metrics.setTemperature(temperature);
            metrics.setBloodPressure(bloodPressure);
            metrics.setHeartRate(heartRate);
            metrics.setRespiratoryRate(respiratoryRate);
            metrics.setOxygenSaturation(oxygenSaturation);
            metrics.setHeight(height);
            metrics.setWeight(weight);
            metrics.setBloodSugar(bloodSugar);
            metrics.setNotes(notes);
            metrics.setTimestamp(timestamp);
            metrics.setDate(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));

            // Calculate BMI if height and weight are provided
            if (!TextUtils.isEmpty(height) && !TextUtils.isEmpty(weight)) {
                try {
                    double heightM = Double.parseDouble(height) / 100;
                    double weightKg = Double.parseDouble(weight);
                    double bmi = weightKg / (heightM * heightM);
                    metrics.setBmi(String.format(Locale.getDefault(), "%.2f", bmi));
                } catch (NumberFormatException e) {
                    metrics.setBmi("N/A");
                    Log.e("BMI_CALC", "Error calculating BMI: " + e.getMessage());
                }
            }

            metricsRef.child(recordId).setValue(metrics)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            Log.d("SAVE_METRICS", "Metrics saved successfully for scheduled patient: " + patientId);
                            Toast.makeText(PatientMetricsActivity.this,
                                    "✓ Vital signs recorded for " + patientName, Toast.LENGTH_SHORT).show();

                            // Update the patient's latest metrics reference
                            updatePatientLatestMetrics(patientId, metrics);
                            clearForm();
                        } else {
                            Log.e("SAVE_METRICS", "Failed to save metrics: " + task.getException());
                            Toast.makeText(PatientMetricsActivity.this,
                                    "✗ Failed to save patient metrics: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Log.e("SAVE_METRICS", "Exception saving metrics: " + e.getMessage());
            Toast.makeText(this, "Error saving metrics: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updatePatientLatestMetrics(String patientId, PatientMetrics metrics) {
        try {
            DatabaseReference patientLatestRef = FirebaseDatabase.getInstance()
                    .getReference("MbararaHospital/Patients/" + patientId + "/latestMetrics");

            HealthMetrics healthMetrics = new HealthMetrics();
            healthMetrics.setBloodPressure(metrics.getBloodPressure());
            healthMetrics.setHeartRate(metrics.getHeartRate());
            healthMetrics.setBloodSugar(metrics.getBloodSugar());
            healthMetrics.setWeight(metrics.getWeight());
            healthMetrics.setTemperature(metrics.getTemperature());
            healthMetrics.setOxygenSaturation(metrics.getOxygenSaturation());
            healthMetrics.setBmi(metrics.getBmi());
            healthMetrics.setTimestamp(metrics.getTimestamp());
            healthMetrics.setLastUpdated(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));

            patientLatestRef.setValue(healthMetrics)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("METRICS", "Latest metrics updated for: " + patientId);
                        updatePatientSummary(patientId, healthMetrics);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("METRICS", "Failed to update latest metrics: " + e.getMessage());
                    });
        } catch (Exception e) {
            Log.e("UPDATE_METRICS", "Error updating latest metrics: " + e.getMessage());
        }
    }

    private void updatePatientSummary(String patientId, HealthMetrics metrics) {
        try {
            DatabaseReference patientSummaryRef = FirebaseDatabase.getInstance()
                    .getReference("MbararaHospital/Patients/" + patientId + "/summary");

            Map<String, Object> summaryUpdates = new HashMap<>();
            summaryUpdates.put("lastCheckup", metrics.getLastUpdated());

            if (metrics.getBloodPressure() != null && !metrics.getBloodPressure().isEmpty()) {
                summaryUpdates.put("bloodPressure", metrics.getBloodPressure());
            }

            if (metrics.getHeartRate() != null && !metrics.getHeartRate().isEmpty()) {
                summaryUpdates.put("heartRate", metrics.getHeartRate());
            }

            patientSummaryRef.updateChildren(summaryUpdates)
                    .addOnSuccessListener(aVoid -> Log.d("SUMMARY", "Patient summary updated"))
                    .addOnFailureListener(e -> Log.e("SUMMARY", "Failed to update summary: " + e.getMessage()));
        } catch (Exception e) {
            Log.e("UPDATE_SUMMARY", "Error updating patient summary: " + e.getMessage());
        }
    }

    private void clearForm() {
        etTemperature.setText("");
        etBloodPressure.setText("");
        etHeartRate.setText("");
        etRespiratoryRate.setText("");
        etOxygenSaturation.setText("");
        etHeight.setText("");
        etWeight.setText("");
        etBloodSugar.setText("");
        etNotes.setText("");

        // Reset spinner to first position
        if (spinnerScheduledPatients.getAdapter() != null && spinnerScheduledPatients.getAdapter().getCount() > 0) {
            spinnerScheduledPatients.setSelection(0);
        }

        tvSelectedPatientInfo.setText("No patient selected");
        selectedPatientId = null;
        selectedPatientName = null;
    }

    private void viewPatientHistory() {
        if (selectedPatientId == null) {
            Toast.makeText(this, "Please select a patient first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PatientMetricsHistoryActivity.class);
        intent.putExtra("patientId", selectedPatientId);
        intent.putExtra("healthWorkerId", healthWorkerId);
        intent.putExtra("healthWorkerName", healthWorkerName);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up to prevent memory leaks
        if (scheduledAppointments != null) {
            scheduledAppointments.clear();
        }
        if (displayList != null) {
            displayList.clear();
        }
        if (patientNameMap != null) {
            patientNameMap.clear();
        }
    }
}