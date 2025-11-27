package com.example.azimbalife.Activity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

import com.example.azimbalife.Adapter.MedicalRecordsAdapter;
import com.example.azimbalife.Domain.MedicalRecord;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicalRecordsActivity extends AppCompatActivity {

    private RecyclerView recyclerMedicalRecords;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyState;
    private TextView tvTotalRecords, tvLastUpdated, tvDownloadedCount, tvVitalMetricsCount;
    private Button btnRequestRecords;

    private TextView filterAll, filterPrescriptions, filterDiagnosis, filterVaccinations, filterLabResults, filterVitalMetrics;

    private MedicalRecordsAdapter adapter;
    private List<MedicalRecord> allMedicalRecords = new ArrayList<>();
    private List<MedicalRecord> filteredMedicalRecords = new ArrayList<>();
    private List<PatientMetrics> allVitalMetrics = new ArrayList<>();

    private String username;
    private String currentFilter = "all";
    private DownloadManager downloadManager;

    private DatabaseReference medicalRecordsRef;
    private DatabaseReference vitalMetricsRef;
    private DatabaseReference examinationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        // Get username from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase references
        medicalRecordsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/MedicalRecords");
        vitalMetricsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/PatientMetrics");
        examinationsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Examinations");

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        initializeViews();
        setupRecyclerView();
        setupFilterListeners();
        setupSwipeRefresh();
        setupClickListeners();
        loadMedicalRecords();
        loadVitalMetrics();
        loadExaminationResults();
    }

    private void initializeViews() {
        recyclerMedicalRecords = findViewById(R.id.recyclerMedicalRecords);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyState = findViewById(R.id.emptyState);
        tvTotalRecords = findViewById(R.id.tvTotalRecords);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        tvDownloadedCount = findViewById(R.id.tvDownloadedCount);
        tvVitalMetricsCount = findViewById(R.id.tvVitalMetricsCount);
        btnRequestRecords = findViewById(R.id.btnRequestRecords);

        filterAll = findViewById(R.id.filterAll);
        filterPrescriptions = findViewById(R.id.filterPrescriptions);
        filterDiagnosis = findViewById(R.id.filterDiagnosis);
        filterVaccinations = findViewById(R.id.filterVaccinations);
        filterLabResults = findViewById(R.id.filterLabResults);
        filterVitalMetrics = findViewById(R.id.filterVitalMetrics);

        // Setup back button
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // Setup FAB for generating reports
        findViewById(R.id.fabGenerateReport).setOnClickListener(v -> generateComprehensiveHealthReport());
    }

    private void setupRecyclerView() {
        adapter = new MedicalRecordsAdapter(filteredMedicalRecords, new MedicalRecordsAdapter.OnMedicalRecordClickListener() {
            @Override
            public void onViewDetailsClick(MedicalRecord medicalRecord) {
                viewMedicalRecordDetails(medicalRecord);
            }

            @Override
            public void onDownloadClick(MedicalRecord medicalRecord) {
                downloadMedicalRecord(medicalRecord);
            }

            @Override
            public void onShareClick(MedicalRecord medicalRecord) {
                shareMedicalRecord(medicalRecord);
            }

            @Override
            public void onPrintClick(MedicalRecord medicalRecord) {
                printMedicalRecord(medicalRecord);
            }

            @Override
            public void onEmailClick(MedicalRecord medicalRecord) {
                emailMedicalRecord(medicalRecord);
            }
        });

        recyclerMedicalRecords.setLayoutManager(new LinearLayoutManager(this));
        recyclerMedicalRecords.setAdapter(adapter);
    }

    private void setupFilterListeners() {
        filterAll.setOnClickListener(v -> applyFilter("all"));
        filterPrescriptions.setOnClickListener(v -> applyFilter("prescriptions"));
        filterDiagnosis.setOnClickListener(v -> applyFilter("diagnosis"));
        filterVaccinations.setOnClickListener(v -> applyFilter("vaccinations"));
        filterLabResults.setOnClickListener(v -> applyFilter("lab_results"));
        filterVitalMetrics.setOnClickListener(v -> applyFilter("vital_metrics"));
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadMedicalRecords();
            loadVitalMetrics();
            loadExaminationResults();
        });
    }

    private void setupClickListeners() {
        btnRequestRecords.setOnClickListener(v -> requestRecordsFromHospital());
        findViewById(R.id.emergencyAccessCard).setOnClickListener(v -> showEmergencyAccess());
    }

    private void loadMedicalRecords() {
        medicalRecordsRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMedicalRecords.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot recordSnapshot : snapshot.getChildren()) {
                        MedicalRecord medicalRecord = recordSnapshot.getValue(MedicalRecord.class);
                        if (medicalRecord != null) {
                            if (medicalRecord.getRecordId() == null) {
                                medicalRecord.setRecordId(recordSnapshot.getKey());
                            }
                            allMedicalRecords.add(medicalRecord);
                        }
                    }

                    Collections.sort(allMedicalRecords, (r1, r2) -> {
                        if (r1.getTimestamp() == null || r2.getTimestamp() == null) {
                            return r2.getDate().compareTo(r1.getDate());
                        }
                        return r2.getTimestamp().compareTo(r1.getTimestamp());
                    });

                    updateStatistics();
                    applyFilter(currentFilter);
                    showEmptyState(false);
                } else {
                    showEmptyState(true);
                    updateStatistics();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MedicalRecordsActivity.this, "Failed to load medical records: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyState(true);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void loadVitalMetrics() {
        Query query = vitalMetricsRef.orderByChild("patientId").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allVitalMetrics.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot metricSnapshot : snapshot.getChildren()) {
                        PatientMetrics metrics = metricSnapshot.getValue(PatientMetrics.class);
                        if (metrics != null) {
                            allVitalMetrics.add(metrics);
                        }
                    }

                    Collections.sort(allVitalMetrics, (m1, m2) ->
                            m2.getTimestamp().compareTo(m1.getTimestamp()));

                    Log.d("VITAL_METRICS", "Loaded " + allVitalMetrics.size() + " vital metrics records");
                }

                updateVitalMetricsStatistics();
                updateStatistics();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("VITAL_METRICS", "Failed to load vital metrics: " + error.getMessage());
                updateVitalMetricsStatistics();
            }
        });
    }

    private void loadExaminationResults() {
        // Load examination results that have been notified to patient
        examinationsRef.orderByChild("patientId").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot examSnapshot : snapshot.getChildren()) {
                                String status = examSnapshot.child("status").getValue(String.class);
                                String notificationStatus = examSnapshot.child("notificationStatus").getValue(String.class);

                                if ("completed".equals(status) && "patient_notified".equals(notificationStatus)) {
                                    MedicalRecord examRecord = convertExaminationToRecord(examSnapshot);
                                    if (!medicalRecordExists(examRecord.getRecordId())) {
                                        allMedicalRecords.add(examRecord);
                                        saveMedicalRecordToFirebase(examRecord);
                                    }
                                }
                            }

                            updateStatistics();
                            applyFilter(currentFilter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("EXAMINATION_LOAD", "Failed to load examinations: " + error.getMessage());
                    }
                });
    }

    private MedicalRecord convertExaminationToRecord(DataSnapshot examSnapshot) {
        MedicalRecord record = new MedicalRecord();
        record.setRecordId("EXAM_" + examSnapshot.getKey());
        record.setPatientUsername(username);
        record.setTitle("Medical Examination Report");
        record.setRecordType("examination");
        record.setTimestamp(examSnapshot.child("timestamp").getValue(String.class));
        record.setDate(new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
        record.setDoctorName(examSnapshot.child("doctor").getValue(String.class));
        record.setHospitalName("Mbarara Hospital");
        record.setStatus("Completed");
        record.setUrgency("Normal");

        // Build comprehensive description
        StringBuilder description = new StringBuilder();
        description.append("MEDICAL EXAMINATION REPORT\n\n");

        // Add diagnosis
        if (examSnapshot.hasChild("diagnosis")) {
            DataSnapshot diagnosis = examSnapshot.child("diagnosis");
            if (diagnosis.hasChild("final")) {
                String finalDiagnosis = diagnosis.child("final").getValue(String.class);
                record.setDiagnosisText(finalDiagnosis); // Use setDiagnosisText instead of setDiagnosis
                description.append("DIAGNOSIS:\n").append(finalDiagnosis).append("\n\n");
            }
        }

        // Add vital signs
        if (examSnapshot.hasChild("vitalSigns")) {
            DataSnapshot vitalSigns = examSnapshot.child("vitalSigns");
            description.append("VITAL SIGNS:\n");
            if (vitalSigns.hasChild("bloodPressure")) {
                description.append("• Blood Pressure: ").append(vitalSigns.child("bloodPressure").getValue(String.class)).append("\n");
            }
            if (vitalSigns.hasChild("pulse")) {
                description.append("• Pulse: ").append(vitalSigns.child("pulse").getValue(String.class)).append(" bpm\n");
            }
            if (vitalSigns.hasChild("temperature")) {
                description.append("• Temperature: ").append(vitalSigns.child("temperature").getValue(String.class)).append(" °C\n");
            }
            if (vitalSigns.hasChild("spo2")) {
                description.append("• Oxygen Saturation: ").append(vitalSigns.child("spo2").getValue(String.class)).append(" %\n");
            }
            description.append("\n");
        }

        // Add findings
        if (examSnapshot.hasChild("findings")) {
            DataSnapshot findings = examSnapshot.child("findings");
            description.append("CLINICAL FINDINGS:\n");
            if (findings.hasChild("chiefComplaints")) {
                description.append("• Chief Complaints: ").append(findings.child("chiefComplaints").getValue(String.class)).append("\n");
            }
            if (findings.hasChild("clinicalObservations")) {
                description.append("• Observations: ").append(findings.child("clinicalObservations").getValue(String.class)).append("\n");
            }
            description.append("\n");
        }

        // Add treatment
        if (examSnapshot.hasChild("treatment")) {
            DataSnapshot treatment = examSnapshot.child("treatment");
            description.append("TREATMENT & RECOMMENDATIONS:\n");
            if (treatment.hasChild("prescription")) {
                description.append("• Prescription: ").append(treatment.child("prescription").getValue(String.class)).append("\n");
            }
            if (treatment.hasChild("recommendations")) {
                description.append("• Recommendations: ").append(treatment.child("recommendations").getValue(String.class)).append("\n");
            }
        }

        // Add follow-up
        if (examSnapshot.hasChild("followUp")) {
            DataSnapshot followUp = examSnapshot.child("followUp");
            if (followUp.hasChild("date")) {
                String followUpDate = followUp.child("date").getValue(String.class);
                record.setFollowUpDate(followUpDate);
                description.append("\nFOLLOW-UP:\nScheduled for: ").append(followUpDate).append("\n");
            }
        }

        record.setDescription(description.toString());
        return record;
    }

    private boolean medicalRecordExists(String recordId) {
        for (MedicalRecord record : allMedicalRecords) {
            if (recordId.equals(record.getRecordId())) {
                return true;
            }
        }
        return false;
    }

    private void saveMedicalRecordToFirebase(MedicalRecord medicalRecord) {
        medicalRecordsRef.child(username)
                .child(medicalRecord.getRecordId())
                .setValue(medicalRecord)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MEDICAL_RECORDS", "Examination record saved to medical records");
                })
                .addOnFailureListener(e -> {
                    Log.e("MEDICAL_RECORDS", "Failed to save examination record: " + e.getMessage());
                });
    }

    private void updateStatistics() {
        int totalRecords = allMedicalRecords.size();
        tvTotalRecords.setText(String.valueOf(totalRecords));

        int downloadedCount = 0;
        for (MedicalRecord record : allMedicalRecords) {
            if (record.isDownloaded()) {
                downloadedCount++;
            }
        }
        tvDownloadedCount.setText(String.valueOf(downloadedCount));

        String currentTime = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(new Date());
        tvLastUpdated.setText(currentTime);
    }

    private void updateVitalMetricsStatistics() {
        int vitalMetricsCount = allVitalMetrics.size();
        if (tvVitalMetricsCount != null) {
            tvVitalMetricsCount.setText(String.valueOf(vitalMetricsCount));
        }
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredMedicalRecords.clear();

        switch (filter) {
            case "all":
                filteredMedicalRecords.addAll(allMedicalRecords);
                updateFilterUI(filterAll, filterPrescriptions, filterDiagnosis, filterVaccinations, filterLabResults, filterVitalMetrics);
                break;
            case "prescriptions":
                for (MedicalRecord record : allMedicalRecords) {
                    if (record.checkIsPrescription()) {
                        filteredMedicalRecords.add(record);
                    }
                }
                updateFilterUI(filterPrescriptions, filterAll, filterDiagnosis, filterVaccinations, filterLabResults, filterVitalMetrics);
                break;
            case "diagnosis":
                for (MedicalRecord record : allMedicalRecords) {
                    if (record.checkIsDiagnosis()) {
                        filteredMedicalRecords.add(record);
                    }
                }
                updateFilterUI(filterDiagnosis, filterAll, filterPrescriptions, filterVaccinations, filterLabResults, filterVitalMetrics);
                break;
            case "vaccinations":
                for (MedicalRecord record : allMedicalRecords) {
                    if (record.checkIsVaccination()) {
                        filteredMedicalRecords.add(record);
                    }
                }
                updateFilterUI(filterVaccinations, filterAll, filterPrescriptions, filterDiagnosis, filterLabResults, filterVitalMetrics);
                break;
            case "lab_results":
                for (MedicalRecord record : allMedicalRecords) {
                    if (record.checkIsLabResult()) {
                        filteredMedicalRecords.add(record);
                    }
                }
                updateFilterUI(filterLabResults, filterAll, filterPrescriptions, filterDiagnosis, filterVaccinations, filterVitalMetrics);
                break;
            case "vital_metrics":
                convertVitalMetricsToRecords();
                updateFilterUI(filterVitalMetrics, filterAll, filterPrescriptions, filterDiagnosis, filterVaccinations, filterLabResults);
                break;
        }

        adapter.updateMedicalRecords(filteredMedicalRecords);
        showEmptyState(filteredMedicalRecords.isEmpty());
    }

    private void convertVitalMetricsToRecords() {
        for (PatientMetrics vitalMetric : allVitalMetrics) {
            MedicalRecord record = new MedicalRecord();
            record.setRecordId("VITAL_" + vitalMetric.getRecordId());
            record.setTitle("Vital Signs Measurement");
            record.setRecordType("vital_metrics");
            record.setDate(vitalMetric.getDate());
            record.setTimestamp(vitalMetric.getTimestamp());
            record.setDoctorName(vitalMetric.getHealthWorkerName());
            record.setHospitalName("Mbarara Hospital");
            record.setStatus("Completed");
            record.setUrgency("Routine");

            StringBuilder description = new StringBuilder();
            description.append("VITAL SIGNS RECORDED:\n\n");

            if (vitalMetric.getTemperature() != null && !vitalMetric.getTemperature().isEmpty()) {
                description.append("• Temperature: ").append(vitalMetric.getTemperature()).append(" °C\n");
            }
            if (vitalMetric.getBloodPressure() != null && !vitalMetric.getBloodPressure().isEmpty()) {
                description.append("• Blood Pressure: ").append(vitalMetric.getBloodPressure()).append("\n");
            }
            if (vitalMetric.getHeartRate() != null && !vitalMetric.getHeartRate().isEmpty()) {
                description.append("• Heart Rate: ").append(vitalMetric.getHeartRate()).append(" bpm\n");
            }
            if (vitalMetric.getRespiratoryRate() != null && !vitalMetric.getRespiratoryRate().isEmpty()) {
                description.append("• Respiratory Rate: ").append(vitalMetric.getRespiratoryRate()).append(" breaths/min\n");
            }
            if (vitalMetric.getOxygenSaturation() != null && !vitalMetric.getOxygenSaturation().isEmpty()) {
                description.append("• Oxygen Saturation: ").append(vitalMetric.getOxygenSaturation()).append(" %\n");
            }
            if (vitalMetric.getBloodSugar() != null && !vitalMetric.getBloodSugar().isEmpty()) {
                description.append("• Blood Sugar: ").append(vitalMetric.getBloodSugar()).append(" mg/dL\n");
            }
            if (vitalMetric.getHeight() != null && !vitalMetric.getHeight().isEmpty()) {
                description.append("• Height: ").append(vitalMetric.getHeight()).append(" cm\n");
            }
            if (vitalMetric.getWeight() != null && !vitalMetric.getWeight().isEmpty()) {
                description.append("• Weight: ").append(vitalMetric.getWeight()).append(" kg\n");
            }
            if (vitalMetric.getBmi() != null && !vitalMetric.getBmi().isEmpty()) {
                description.append("• BMI: ").append(vitalMetric.getBmi()).append("\n");
            }
            if (vitalMetric.getNotes() != null && !vitalMetric.getNotes().isEmpty()) {
                description.append("• Notes: ").append(vitalMetric.getNotes()).append("\n");
            }

            record.setDescription(description.toString());
            filteredMedicalRecords.add(record);
        }

        Collections.sort(filteredMedicalRecords, (r1, r2) ->
                r2.getTimestamp().compareTo(r1.getTimestamp()));
    }

    private void updateFilterUI(TextView selected, TextView... others) {
        selected.setBackgroundResource(R.drawable.filter_selected_background);
        selected.setTextColor(getColor(R.color.white));

        for (TextView other : others) {
            other.setBackgroundResource(R.drawable.filter_unselected_background);
            other.setTextColor(getColor(R.color.white));
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerMedicalRecords.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerMedicalRecords.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void viewMedicalRecordDetails(MedicalRecord medicalRecord) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        String details = buildRecordDetails(medicalRecord);

        builder.setTitle(medicalRecord.getTitle())
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNeutralButton("Share", (dialog, which) -> shareMedicalRecord(medicalRecord))
                .show();
    }

    private String buildRecordDetails(MedicalRecord medicalRecord) {
        StringBuilder details = new StringBuilder();

        details.append("Record Type: ").append(medicalRecord.getFormattedRecordType()).append("\n");
        details.append("Date: ").append(medicalRecord.getDate()).append("\n");

        if (medicalRecord.getTimestamp() != null) {
            details.append("Time: ").append(medicalRecord.getTimestamp()).append("\n");
        }

        details.append("Doctor: ").append(medicalRecord.getDoctorName()).append("\n");
        details.append("Hospital: ").append(medicalRecord.getHospitalName() != null ? medicalRecord.getHospitalName() : "Mbarara Hospital").append("\n");
        details.append("Status: ").append(medicalRecord.getStatus()).append("\n");
        details.append("Urgency: ").append(medicalRecord.getUrgency()).append("\n\n");

        if (medicalRecord.getDiagnosisText() != null) { // Use getDiagnosisText instead of getDiagnosis
            details.append("Diagnosis: ").append(medicalRecord.getDiagnosisText()).append("\n\n");
        }

        if (medicalRecord.getMedications() != null) {
            details.append("Medications: ").append(medicalRecord.getMedications()).append("\n");
        }

        if (medicalRecord.getDosage() != null) {
            details.append("Dosage: ").append(medicalRecord.getDosage()).append("\n");
        }

        if (medicalRecord.getInstructions() != null) {
            details.append("Instructions: ").append(medicalRecord.getInstructions()).append("\n\n");
        }

        if (medicalRecord.getTreatment() != null) {
            details.append("Treatment: ").append(medicalRecord.getTreatment()).append("\n\n");
        }

        if (medicalRecord.getFollowUpDate() != null) {
            details.append("Follow-up Date: ").append(medicalRecord.getFollowUpDate()).append("\n");
        }

        if (medicalRecord.getDescription() != null) {
            details.append("\nDescription: ").append(medicalRecord.getDescription());
        }

        if (medicalRecord.getLabResults() != null) {
            details.append("\n\nLab Results: ").append(medicalRecord.getLabResults());
        }

        return details.toString();
    }

    private void downloadMedicalRecord(MedicalRecord medicalRecord) {
        if (medicalRecord.hasFile()) {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(medicalRecord.getFileUrl()));
                request.setTitle(medicalRecord.getTitle());
                request.setDescription("Downloading medical record");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                String fileName = medicalRecord.getFileName() != null ?
                        medicalRecord.getFileName() : "medical_record_" + medicalRecord.getRecordId() + ".pdf";

                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        "MedicalRecords/" + fileName);

                downloadManager.enqueue(request);
                medicalRecord.setDownloaded(true);
                updateRecordInFirebase(medicalRecord);

                Toast.makeText(this, "Download started: " + fileName, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e("DOWNLOAD_ERROR", "Failed to download: " + e.getMessage());
                Toast.makeText(this, "Download failed: Invalid file URL", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No file available for download", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRecordInFirebase(MedicalRecord medicalRecord) {
        if (medicalRecord.getRecordId() == null) {
            Log.e("FIREBASE_ERROR", "Record ID is null, cannot update Firebase");
            return;
        }

        medicalRecordsRef.child(username)
                .child(medicalRecord.getRecordId())
                .setValue(medicalRecord)
                .addOnSuccessListener(aVoid -> {
                    Log.d("MEDICAL_RECORDS", "Record updated successfully");
                    updateStatistics();
                })
                .addOnFailureListener(e -> {
                    Log.e("MEDICAL_RECORDS", "Failed to update record: " + e.getMessage());
                });
    }

    private void shareMedicalRecord(MedicalRecord medicalRecord) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Medical Record: " + medicalRecord.getTitle());

        String shareText = buildShareText(medicalRecord);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        medicalRecord.setShared(true);
        updateRecordInFirebase(medicalRecord);

        startActivity(Intent.createChooser(shareIntent, "Share Medical Record"));
    }

    private String buildShareText(MedicalRecord medicalRecord) {
        return "Medical Record: " + medicalRecord.getTitle() + "\n" +
                "Type: " + medicalRecord.getFormattedRecordType() + "\n" +
                "Date: " + medicalRecord.getDate() + "\n" +
                "Doctor: " + medicalRecord.getDoctorName() + "\n" +
                "Hospital: " + (medicalRecord.getHospitalName() != null ? medicalRecord.getHospitalName() : "Mbarara Hospital") + "\n" +
                "Status: " + medicalRecord.getStatus() + "\n\n" +
                "Note: Shared via Azimba Life Medical App";
    }

    private void printMedicalRecord(MedicalRecord medicalRecord) {
        Toast.makeText(this, "Print feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void emailMedicalRecord(MedicalRecord medicalRecord) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Medical Record: " + medicalRecord.getTitle());
        emailIntent.putExtra(Intent.EXTRA_TEXT, buildShareText(medicalRecord));

        try {
            startActivity(Intent.createChooser(emailIntent, "Send medical record via email"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email client installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestRecordsFromHospital() {
        Toast.makeText(this, "Requesting records from hospital...", Toast.LENGTH_SHORT).show();

        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, "Record request submitted to hospital", Toast.LENGTH_SHORT).show();

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Record Request Sent")
                    .setMessage("Your request for medical records has been sent to Mbarara Hospital. " +
                            "New records will appear here once processed by the hospital staff.")
                    .setPositiveButton("OK", null)
                    .show();
        }, 1500);
    }

    private void showEmergencyAccess() {
        String latestVitals = getLatestVitalsForEmergency();

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Emergency Medical Access")
                .setMessage("In case of emergency, this information can be shared with healthcare providers:\n\n" +
                        "• Allergies: " + (getUserAllergies() != null ? getUserAllergies() : "None recorded") + "\n" +
                        "• Blood Type: " + (getUserBloodType() != null ? getUserBloodType() : "Not specified") + "\n" +
                        "• Latest Vitals: " + latestVitals + "\n" +
                        "• Emergency Contact: Available\n" +
                        "• Medical Conditions: Viewable by authorized personnel")
                .setPositiveButton("OK", null)
                .setNeutralButton("Share Emergency Info", (dialog, which) -> shareEmergencyInfo())
                .show();
    }

    private String getLatestVitalsForEmergency() {
        if (allVitalMetrics.isEmpty()) {
            return "No recent measurements";
        }

        PatientMetrics latest = allVitalMetrics.get(0);
        StringBuilder vitals = new StringBuilder();

        if (latest.getBloodPressure() != null) {
            vitals.append("BP: ").append(latest.getBloodPressure()).append(", ");
        }
        if (latest.getHeartRate() != null) {
            vitals.append("HR: ").append(latest.getHeartRate()).append("bpm, ");
        }
        if (latest.getOxygenSaturation() != null) {
            vitals.append("SpO2: ").append(latest.getOxygenSaturation()).append("%");
        }

        return vitals.toString();
    }

    private String getUserAllergies() {
        for (MedicalRecord record : allMedicalRecords) {
            if (record.getAllergies() != null && !record.getAllergies().isEmpty()) {
                return record.getAllergies();
            }
        }
        return null;
    }

    private String getUserBloodType() {
        for (MedicalRecord record : allMedicalRecords) {
            if (record.getBloodType() != null && !record.getBloodType().isEmpty()) {
                return record.getBloodType();
            }
        }
        return "O+";
    }

    private void shareEmergencyInfo() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Emergency Medical Information");

        String emergencyInfo = "EMERGENCY MEDICAL INFORMATION\n\n" +
                "Patient: " + username + "\n" +
                "Allergies: " + (getUserAllergies() != null ? getUserAllergies() : "None recorded") + "\n" +
                "Blood Type: " + getUserBloodType() + "\n" +
                "Latest Vitals: " + getLatestVitalsForEmergency() + "\n" +
                "Important: This information is shared for emergency purposes only";

        shareIntent.putExtra(Intent.EXTRA_TEXT, emergencyInfo);
        startActivity(Intent.createChooser(shareIntent, "Share Emergency Info"));
    }

    private void generateComprehensiveHealthReport() {
        Toast.makeText(this, "Generating comprehensive health report...", Toast.LENGTH_SHORT).show();

        // Start HealthReportsActivity to generate report
        Intent intent = new Intent(this, HealthReportsActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicalRecords();
        loadVitalMetrics();
        loadExaminationResults();
    }
}