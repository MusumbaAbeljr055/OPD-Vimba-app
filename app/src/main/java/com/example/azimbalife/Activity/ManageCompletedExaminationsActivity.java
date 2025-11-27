package com.example.azimbalife.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.CompletedExaminationsAdapter;
import com.example.azimbalife.Domain.CompletedExamination;
import com.example.azimbalife.Domain.MedicalRecord;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManageCompletedExaminationsActivity extends AppCompatActivity implements CompletedExaminationsAdapter.OnExaminationClickListener {

    private RecyclerView recyclerCompletedExaminations;
    private LinearLayout emptyState;
    private TextView tvExaminationsCount, tvReadyCount, tvNotifiedCount;

    private CompletedExaminationsAdapter completedExaminationsAdapter;
    private List<CompletedExamination> completedExaminations = new ArrayList<>();

    private DatabaseReference examinationsRef;
    private DatabaseReference notificationsRef;
    private DatabaseReference medicalRecordsRef;
    private DatabaseReference patientsRef;
    private DatabaseReference appointmentsRef;

    private String healthWorkerId;
    private String healthWorkerName;
    private String userType;

    private static final String TAG = "ManageCompletedExams";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_completed_examinations);

        // Get health worker data from intent
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");
        userType = getIntent().getStringExtra("userType");

        Log.d(TAG, "User accessing completed examinations - ID: " + healthWorkerId +
                ", Name: " + healthWorkerName + ", Type: " + userType);

        // Firebase references
        examinationsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Examinations");
        notificationsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Notifications");
        medicalRecordsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/MedicalRecords");
        patientsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Patients");
        appointmentsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/AllAppointments");

        initializeViews();
        setupRecyclerView();
        loadCompletedExaminations();
    }

    private void initializeViews() {
        recyclerCompletedExaminations = findViewById(R.id.recyclerCompletedExaminations);
        emptyState = findViewById(R.id.emptyState);
        tvExaminationsCount = findViewById(R.id.tvExaminationsCount);
        tvReadyCount = findViewById(R.id.tvReadyCount);
        tvNotifiedCount = findViewById(R.id.tvNotifiedCount);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Completed Examinations");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        completedExaminationsAdapter = new CompletedExaminationsAdapter(completedExaminations, this);
        recyclerCompletedExaminations.setLayoutManager(new LinearLayoutManager(this));
        recyclerCompletedExaminations.setAdapter(completedExaminationsAdapter);
    }

    private void loadCompletedExaminations() {
        Log.d(TAG, "Starting to load examinations from: " + examinationsRef.toString());

        examinationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                completedExaminations.clear();
                int readyCount = 0;
                int notifiedCount = 0;
                int totalExamined = 0;

                Log.d(TAG, "=== LOADING EXAMINATIONS ===");
                Log.d(TAG, "Total examinations found: " + snapshot.getChildrenCount());

                if (!snapshot.exists()) {
                    Log.e(TAG, "No examinations found at all");
                    updateUI();
                    return;
                }

                for (DataSnapshot examSnapshot : snapshot.getChildren()) {
                    totalExamined++;
                    try {
                        Log.d(TAG, "🔍 Processing examination " + totalExamined + ": " + examSnapshot.getKey());

                        // Check if examination is completed
                        String status = examSnapshot.hasChild("status") ?
                                examSnapshot.child("status").getValue(String.class) : null;

                        Log.d(TAG, "   Status: " + status);

                        if (!"completed".equals(status)) {
                            Log.d(TAG, "   ✗ SKIPPED - Not completed: " + status);
                            continue;
                        }

                        // Parse the examination
                        CompletedExamination examination = parseExamination(examSnapshot);
                        if (examination != null) {
                            completedExaminations.add(examination);

                            // Count by notification status
                            String notificationStatus = examination.getNotificationStatus();
                            if ("ready_for_patient".equals(notificationStatus)) {
                                readyCount++;
                            } else if ("patient_notified".equals(notificationStatus)) {
                                notifiedCount++;
                            }

                            Log.d(TAG, "   ✓ ADDED - Patient: " + examination.getPatientName() +
                                    ", Status: " + notificationStatus);
                        } else {
                            Log.d(TAG, "   ✗ FAILED - Could not parse examination");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error processing examination " + examSnapshot.getKey() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "=== LOADING SUMMARY ===");
                Log.d(TAG, "Total examined: " + totalExamined);
                Log.d(TAG, "Ready for patient: " + readyCount);
                Log.d(TAG, "Already notified: " + notifiedCount);
                Log.d(TAG, "Final list size: " + completedExaminations.size());

                // Update counts
                tvReadyCount.setText(String.valueOf(readyCount));
                tvNotifiedCount.setText(String.valueOf(notifiedCount));

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageCompletedExaminationsActivity.this,
                        "Failed to load examinations: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading examinations: " + error.getMessage());
            }
        });
    }

    private CompletedExamination parseExamination(DataSnapshot examSnapshot) {
        try {
            CompletedExamination examination = new CompletedExamination();
            examination.setId(examSnapshot.getKey());

            Log.d(TAG, "=== PARSING EXAMINATION: " + examSnapshot.getKey() + " ===");

            // Extract patient information - FIRST try to get from examination data
            String patientName = extractPatientName(examSnapshot);
            String patientId = extractPatientId(examSnapshot);

            // If patient info not found in examination, try to find it from appointments
            if (patientName == null || "Unknown Patient".equals(patientName) ||
                    patientId == null || "Unknown".equals(patientId)) {
                Log.d(TAG, "   ℹ Patient info not found in examination, looking up from appointments...");
                lookupPatientInfoFromAppointments(examSnapshot.getKey(), examination);
            } else {
                examination.setPatientName(patientName);
                examination.setPatientId(patientId);
            }

            // Extract doctor information
            String doctorName = extractDoctorName(examSnapshot);
            examination.setDoctor(doctorName != null ? doctorName : "Unknown Doctor");

            String doctorId = extractDoctorId(examSnapshot);
            examination.setDoctorId(doctorId != null ? doctorId : "Unknown");

            // Extract timestamp
            String timestamp = extractTimestamp(examSnapshot);
            examination.setCompletedAt(timestamp != null ? timestamp : "Unknown date");

            // Extract notification status
            String notificationStatus = extractNotificationStatus(examSnapshot);
            examination.setNotificationStatus(notificationStatus);

            // Extract diagnosis information
            String diagnosis = extractDiagnosis(examSnapshot);
            examination.setDiagnosis(diagnosis);

            // Extract recommendations and treatment
            String recommendations = extractRecommendations(examSnapshot);
            examination.setRecommendations(recommendations);

            // Extract follow-up date
            String followUpDate = extractFollowUpDate(examSnapshot);
            examination.setFollowUpDate(followUpDate);

            Log.d(TAG, "=== FINAL PARSED EXAMINATION ===");
            Log.d(TAG, "Patient: " + examination.getPatientName());
            Log.d(TAG, "Patient ID: " + examination.getPatientId());
            Log.d(TAG, "Doctor: " + examination.getDoctor());
            Log.d(TAG, "Timestamp: " + examination.getCompletedAt());
            Log.d(TAG, "Notification Status: " + examination.getNotificationStatus());
            Log.d(TAG, "Diagnosis: " + examination.getDiagnosis());
            Log.d(TAG, "Recommendations: " + examination.getRecommendations());
            Log.d(TAG, "Follow-up Date: " + examination.getFollowUpDate());
            Log.d(TAG, "=== END PARSING ===");

            return examination;

        } catch (Exception e) {
            Log.e(TAG, "❌ Parsing failed for " + examSnapshot.getKey() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void lookupPatientInfoFromAppointments(String examinationId, CompletedExamination examination) {
        Log.d(TAG, "   🔍 Looking up patient info for examination: " + examinationId);

        // Search through appointments to find one that matches this examination
        appointmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;

                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    try {
                        // Check if this appointment has the same ID as the examination
                        // OR if it has an examination reference that matches
                        String appointmentId = appointmentSnapshot.getKey();

                        // Look for patient information in the appointment
                        String patientName = appointmentSnapshot.child("patientName").getValue(String.class);
                        String patientUsername = appointmentSnapshot.child("patientUsername").getValue(String.class);
                        String patientId = appointmentSnapshot.child("patientId").getValue(String.class);

                        // Also check if this appointment has an examination reference
                        String examRef = appointmentSnapshot.child("examinationId").getValue(String.class);
                        String assignedDoctor = appointmentSnapshot.child("assignedDoctorName").getValue(String.class);
                        String examDoctor = examination.getDoctor();

                        // If we found patient info and the doctors match, use this appointment
                        if (patientName != null && !patientName.isEmpty() &&
                                assignedDoctor != null && examDoctor != null &&
                                assignedDoctor.equals(examDoctor)) {

                            examination.setPatientName(patientName);
                            examination.setPatientId(patientUsername != null ? patientUsername : patientId);
                            examination.setAppointmentId(appointmentId);

                            Log.d(TAG, "   ✓ Found patient from appointment: " + patientName + " (ID: " + examination.getPatientId() + ")");
                            found = true;

                            // Update the examination in Firebase with patient info
                            updateExaminationWithPatientInfo(examinationId, patientName, examination.getPatientId(), appointmentId);
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "   ✗ Error processing appointment: " + e.getMessage());
                    }
                }

                if (!found) {
                    Log.d(TAG, "   ✗ No matching appointment found for examination");
                    // Set default values
                    examination.setPatientName("Patient (Info Missing)");
                    examination.setPatientId("unknown");
                }

                // Refresh the adapter to show updated patient info
                completedExaminationsAdapter.updateExaminations(completedExaminations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "   ✗ Failed to lookup patient info from appointments: " + error.getMessage());
                examination.setPatientName("Patient (Lookup Failed)");
                examination.setPatientId("unknown");
                completedExaminationsAdapter.updateExaminations(completedExaminations);
            }
        });
    }

    private void updateExaminationWithPatientInfo(String examinationId, String patientName, String patientId, String appointmentId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("patientName", patientName);
        updates.put("patientId", patientId);
        updates.put("appointmentId", appointmentId);

        examinationsRef.child(examinationId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Updated examination with patient info: " + patientName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to update examination with patient info: " + e.getMessage());
                });
    }

    private String extractPatientName(DataSnapshot examSnapshot) {
        // Try multiple possible field names for patient name
        String[] possibleFields = {"patientName", "patient", "patient_name", "name", "patientName",
                "patientName", "patient_name", "patientName"};

        for (String field : possibleFields) {
            if (examSnapshot.hasChild(field)) {
                String patientName = examSnapshot.child(field).getValue(String.class);
                if (patientName != null && !patientName.isEmpty()) {
                    Log.d(TAG, "✓ Found patient name in field '" + field + "': " + patientName);
                    return patientName;
                }
            }
        }

        Log.d(TAG, "✗ No patient name field found in examination");
        return null;
    }

    private String extractPatientId(DataSnapshot examSnapshot) {
        String[] possibleFields = {"patientId", "patientID", "patient_id", "patientId",
                "patientUsername", "username", "patient_username"};

        for (String field : possibleFields) {
            if (examSnapshot.hasChild(field)) {
                String patientId = examSnapshot.child(field).getValue(String.class);
                if (patientId != null && !patientId.isEmpty()) {
                    Log.d(TAG, "✓ Found patient ID in field '" + field + "': " + patientId);
                    return patientId;
                }
            }
        }

        Log.d(TAG, "✗ No patient ID field found in examination");
        return null;
    }

    private String extractDoctorName(DataSnapshot examSnapshot) {
        String[] possibleFields = {"doctor", "doctorName", "doctor_name", "doctorName", "examiner"};

        for (String field : possibleFields) {
            if (examSnapshot.hasChild(field)) {
                String doctorName = examSnapshot.child(field).getValue(String.class);
                if (doctorName != null && !doctorName.isEmpty()) {
                    Log.d(TAG, "✓ Found doctor name in field '" + field + "': " + doctorName);
                    return doctorName;
                }
            }
        }

        Log.d(TAG, "✗ No doctor name field found");
        return null;
    }

    private String extractDoctorId(DataSnapshot examSnapshot) {
        String[] possibleFields = {"doctorId", "doctorID", "doctor_id", "doctorId"};

        for (String field : possibleFields) {
            if (examSnapshot.hasChild(field)) {
                String doctorId = examSnapshot.child(field).getValue(String.class);
                if (doctorId != null && !doctorId.isEmpty()) {
                    Log.d(TAG, "✓ Found doctor ID in field '" + field + "': " + doctorId);
                    return doctorId;
                }
            }
        }

        Log.d(TAG, "✗ No doctor ID field found");
        return null;
    }

    private String extractTimestamp(DataSnapshot examSnapshot) {
        String[] possibleFields = {"timestamp", "completedAt", "examDate", "date", "time"};

        for (String field : possibleFields) {
            if (examSnapshot.hasChild(field)) {
                String timestamp = examSnapshot.child(field).getValue(String.class);
                if (timestamp != null && !timestamp.isEmpty()) {
                    Log.d(TAG, "✓ Found timestamp in field '" + field + "': " + timestamp);

                    // Try to format the timestamp if it's in ISO format
                    try {
                        if (timestamp.contains("T")) {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
                            Date date = inputFormat.parse(timestamp);
                            return outputFormat.format(date);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not parse timestamp, using raw value");
                    }

                    return timestamp;
                }
            }
        }

        Log.d(TAG, "✗ No timestamp field found, using current time");
        return new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(new Date());
    }

    private String extractNotificationStatus(DataSnapshot examSnapshot) {
        if (examSnapshot.hasChild("notificationStatus")) {
            String notificationStatus = examSnapshot.child("notificationStatus").getValue(String.class);
            if (notificationStatus != null && !notificationStatus.isEmpty()) {
                Log.d(TAG, "✓ Found notificationStatus: " + notificationStatus);
                return notificationStatus;
            }
        }

        Log.d(TAG, "✓ Set default notificationStatus: ready_for_patient");
        return "ready_for_patient";
    }

    private String extractDiagnosis(DataSnapshot examSnapshot) {
        StringBuilder diagnosisBuilder = new StringBuilder();

        // Extract from diagnosis field
        if (examSnapshot.hasChild("diagnosis")) {
            DataSnapshot diagnosisSnapshot = examSnapshot.child("diagnosis");
            if (diagnosisSnapshot.hasChild("final")) {
                String finalDiagnosis = diagnosisSnapshot.child("final").getValue(String.class);
                if (finalDiagnosis != null && !finalDiagnosis.isEmpty()) {
                    diagnosisBuilder.append("Diagnosis: ").append(finalDiagnosis).append("\n");
                    Log.d(TAG, "✓ Found final diagnosis: " + finalDiagnosis);
                }
            } else {
                // Handle case where diagnosis is a direct string
                String diagnosis = diagnosisSnapshot.getValue(String.class);
                if (diagnosis != null && !diagnosis.isEmpty()) {
                    diagnosisBuilder.append("Diagnosis: ").append(diagnosis).append("\n");
                    Log.d(TAG, "✓ Found diagnosis: " + diagnosis);
                }
            }
        }

        // Extract from findings
        if (examSnapshot.hasChild("findings")) {
            DataSnapshot findings = examSnapshot.child("findings");
            if (findings.hasChild("chiefComplaints")) {
                String chiefComplaints = findings.child("chiefComplaints").getValue(String.class);
                if (chiefComplaints != null && !chiefComplaints.isEmpty()) {
                    diagnosisBuilder.append("Chief Complaints: ").append(chiefComplaints).append("\n");
                    Log.d(TAG, "✓ Found chiefComplaints: " + chiefComplaints);
                }
            }
            if (findings.hasChild("clinicalObservations")) {
                String clinicalObservations = findings.child("clinicalObservations").getValue(String.class);
                if (clinicalObservations != null && !clinicalObservations.isEmpty()) {
                    diagnosisBuilder.append("Observations: ").append(clinicalObservations).append("\n");
                    Log.d(TAG, "✓ Found clinicalObservations: " + clinicalObservations);
                }
            }
        }

        // Extract vital signs
        if (examSnapshot.hasChild("vitalSigns")) {
            DataSnapshot vitalSigns = examSnapshot.child("vitalSigns");
            StringBuilder vitalSignsBuilder = new StringBuilder("Vital Signs: ");

            boolean hasVitalSigns = false;
            if (vitalSigns.hasChild("bloodPressure")) {
                String bp = vitalSigns.child("bloodPressure").getValue(String.class);
                if (bp != null && !bp.isEmpty()) {
                    vitalSignsBuilder.append("BP: ").append(bp).append(", ");
                    hasVitalSigns = true;
                    Log.d(TAG, "✓ Found bloodPressure: " + bp);
                }
            }
            if (vitalSigns.hasChild("pulse")) {
                String pulse = vitalSigns.child("pulse").getValue(String.class);
                if (pulse != null && !pulse.isEmpty()) {
                    vitalSignsBuilder.append("Pulse: ").append(pulse).append(", ");
                    hasVitalSigns = true;
                    Log.d(TAG, "✓ Found pulse: " + pulse);
                }
            }
            if (vitalSigns.hasChild("temperature")) {
                String temp = vitalSigns.child("temperature").getValue(String.class);
                if (temp != null && !temp.isEmpty()) {
                    vitalSignsBuilder.append("Temp: ").append(temp).append("°C");
                    hasVitalSigns = true;
                    Log.d(TAG, "✓ Found temperature: " + temp);
                }
            }

            if (hasVitalSigns) {
                String vitalSignsText = vitalSignsBuilder.toString();
                if (vitalSignsText.endsWith(", ")) {
                    vitalSignsText = vitalSignsText.substring(0, vitalSignsText.length() - 2);
                }
                diagnosisBuilder.append(vitalSignsText).append("\n");
            }
        }

        if (diagnosisBuilder.length() > 0) {
            return diagnosisBuilder.toString().trim();
        } else {
            Log.d(TAG, "✗ No diagnosis information found");
            return "No diagnosis recorded";
        }
    }

    private String extractRecommendations(DataSnapshot examSnapshot) {
        StringBuilder recommendationsBuilder = new StringBuilder();

        // Extract treatment information
        if (examSnapshot.hasChild("treatment")) {
            DataSnapshot treatment = examSnapshot.child("treatment");
            if (treatment.hasChild("prescription")) {
                String prescription = treatment.child("prescription").getValue(String.class);
                if (prescription != null && !prescription.isEmpty()) {
                    recommendationsBuilder.append("Prescription: ").append(prescription).append("\n");
                    Log.d(TAG, "✓ Found prescription: " + prescription);
                }
            }
            if (treatment.hasChild("recommendations")) {
                String treatmentRecs = treatment.child("recommendations").getValue(String.class);
                if (treatmentRecs != null && !treatmentRecs.isEmpty()) {
                    recommendationsBuilder.append("Treatment: ").append(treatmentRecs).append("\n");
                    Log.d(TAG, "✓ Found treatment recommendations: " + treatmentRecs);
                }
            }
        }

        // Extract follow-up recommendations
        if (examSnapshot.hasChild("followUp")) {
            DataSnapshot followUp = examSnapshot.child("followUp");
            if (followUp.hasChild("recommendations")) {
                String followUpRecs = followUp.child("recommendations").getValue(String.class);
                if (followUpRecs != null && !followUpRecs.isEmpty()) {
                    recommendationsBuilder.append("Follow-up: ").append(followUpRecs).append("\n");
                    Log.d(TAG, "✓ Found followUp recommendations: " + followUpRecs);
                }
            }
        }

        if (recommendationsBuilder.length() > 0) {
            return recommendationsBuilder.toString().trim();
        } else {
            Log.d(TAG, "✗ No recommendations found");
            return "No recommendations recorded";
        }
    }

    private String extractFollowUpDate(DataSnapshot examSnapshot) {
        if (examSnapshot.hasChild("followUp")) {
            DataSnapshot followUp = examSnapshot.child("followUp");
            if (followUp.hasChild("date")) {
                String followUpDate = followUp.child("date").getValue(String.class);
                if (followUpDate != null && !followUpDate.isEmpty()) {
                    Log.d(TAG, "✓ Found followUp date: " + followUpDate);

                    // Try to format the date
                    try {
                        if (followUpDate.contains("T")) {
                            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            Date date = inputFormat.parse(followUpDate);
                            return outputFormat.format(date);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Could not parse follow-up date, using raw value");
                    }

                    return followUpDate;
                }
            }
        }

        Log.d(TAG, "✗ No follow-up date found");
        return null;
    }

    private void updateUI() {
        if (completedExaminations.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerCompletedExaminations.setVisibility(View.GONE);
            tvExaminationsCount.setText("No completed examinations found");
            Log.d(TAG, "UI Updated: No examinations found - Showing empty state");
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerCompletedExaminations.setVisibility(View.VISIBLE);
            completedExaminationsAdapter.updateExaminations(completedExaminations);
            tvExaminationsCount.setText(completedExaminations.size() + " completed examinations");
            Log.d(TAG, "UI Updated: Showing " + completedExaminations.size() + " examinations");
        }
    }

    @Override
    public void onNotifyPatientClick(CompletedExamination examination) {
        Log.d(TAG, "Notifying patient for examination: " + examination.getId());

        // Update notification status to "patient_notified"
        Map<String, Object> updates = new HashMap<>();
        updates.put("notificationStatus", "patient_notified");
        updates.put("patientNotifiedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date()));
        updates.put("notifiedBy", healthWorkerName);
        updates.put("notifiedById", healthWorkerId);

        examinationsRef.child(examination.getId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Examination notification status updated");

                    // Create medical record for patient
                    createMedicalRecordForPatient(examination);

                    // Create notification record
                    createPatientNotification(examination);

                    Toast.makeText(this, "Patient " + examination.getPatientName() + " notified successfully!", Toast.LENGTH_SHORT).show();

                    // Refresh the list
                    loadCompletedExaminations();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating examination: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating examination: " + e.getMessage());
                });
    }

    private void createMedicalRecordForPatient(CompletedExamination examination) {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setRecordId("EXAM_" + examination.getId());
        medicalRecord.setPatientUsername(examination.getPatientId());
        medicalRecord.setTitle("Medical Examination Report");
        medicalRecord.setRecordType("examination");
        medicalRecord.setDate(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
        medicalRecord.setTimestamp(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
        medicalRecord.setDoctorName(examination.getDoctor());
        medicalRecord.setHospitalName("Mbarara Hospital");

        // FIXED: Use setDiagnosisText() instead of setDiagnosis()
        medicalRecord.setDiagnosisText(examination.getDiagnosis());

        medicalRecord.setTreatment(examination.getRecommendations());
        medicalRecord.setFollowUpDate(examination.getFollowUpDate());
        medicalRecord.setStatus("Completed");
        medicalRecord.setUrgency("Normal");

        // Set boolean fields for proper type checking
        medicalRecord.setIsDiagnosis(true);
        medicalRecord.setRecordTypeWithUpdate("examination");

        // Build comprehensive description
        StringBuilder description = new StringBuilder();
        description.append("Medical Examination Report\n\n");
        description.append("Patient: ").append(examination.getPatientName()).append("\n");
        description.append("Doctor: ").append(examination.getDoctor()).append("\n");
        description.append("Date: ").append(examination.getCompletedAt()).append("\n\n");
        description.append("DIAGNOSIS:\n").append(examination.getDiagnosis()).append("\n\n");
        description.append("RECOMMENDATIONS:\n").append(examination.getRecommendations()).append("\n\n");

        if (examination.getFollowUpDate() != null) {
            description.append("FOLLOW-UP:\n").append("Scheduled for: ").append(examination.getFollowUpDate()).append("\n");
        }

        medicalRecord.setDescription(description.toString());

        // Save to patient's medical records
        medicalRecordsRef.child(examination.getPatientId())
                .child(medicalRecord.getRecordId())
                .setValue(medicalRecord)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Medical record created for patient: " + examination.getPatientId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to create medical record: " + e.getMessage());
                });
    }

    private void createPatientNotification(CompletedExamination examination) {
        String notificationId = notificationsRef.push().getKey();
        if (notificationId == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("notificationId", notificationId);
        notification.put("patientId", examination.getPatientId());
        notification.put("patientName", examination.getPatientName());
        notification.put("type", "examination_results");
        notification.put("title", "Medical Examination Results Ready");
        notification.put("message", "Your medical examination results are now available. Please check your medical records in the app.");
        notification.put("timestamp", String.valueOf(System.currentTimeMillis()));
        notification.put("read", false);
        notification.put("examinationId", examination.getId());
        notification.put("doctorName", examination.getDoctor());
        notification.put("healthWorkerName", healthWorkerName);

        notificationsRef.child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Patient notification created: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to create patient notification: " + e.getMessage());
                });
    }

    @Override
    public void onViewDetailsClick(CompletedExamination examination) {
        Intent intent = new Intent(this, ExaminationDetailActivity.class);
        intent.putExtra("examinationId", examination.getId());
        intent.putExtra("patientName", examination.getPatientName());
        intent.putExtra("patientId", examination.getPatientId());
        intent.putExtra("doctorName", examination.getDoctor());
        intent.putExtra("diagnosis", examination.getDiagnosis());
        intent.putExtra("recommendations", examination.getRecommendations());
        intent.putExtra("followUpDate", examination.getFollowUpDate());
        intent.putExtra("completedAt", examination.getCompletedAt());
        startActivity(intent);
    }

    @Override
    public void onContactPatientClick(CompletedExamination examination) {
        Intent intent = new Intent(this, ContactPatientActivity.class);
        intent.putExtra("patientId", examination.getPatientId());
        intent.putExtra("patientName", examination.getPatientName());
        intent.putExtra("healthWorkerName", healthWorkerName);
        intent.putExtra("examinationId", examination.getId());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompletedExaminations();
    }
}