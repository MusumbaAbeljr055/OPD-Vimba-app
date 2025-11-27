package com.example.azimbalife.Activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.azimbalife.Domain.CompletedExamination;
import com.example.azimbalife.R;

public class ExaminationDetailActivity extends AppCompatActivity {

    private TextView tvPatientName, tvDoctorName, tvDiagnosis, tvPrescription, tvRecommendations;
    private TextView tvLabTests, tvTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_examination_detail);

        initializeViews();
        loadExaminationData();
    }

    private void initializeViews() {
        tvPatientName = findViewById(R.id.tvPatientName);
        tvDoctorName = findViewById(R.id.tvDoctorName);
        tvDiagnosis = findViewById(R.id.tvDiagnosis);
        tvPrescription = findViewById(R.id.tvPrescription);
        tvRecommendations = findViewById(R.id.tvRecommendations);
        tvLabTests = findViewById(R.id.tvLabTests);
        tvTimestamp = findViewById(R.id.tvTimestamp);

        // Remove tvNotificationStatus reference since it's not in the layout
    }

    private void loadExaminationData() {
        CompletedExamination examination = (CompletedExamination) getIntent().getSerializableExtra("examination");

        if (examination != null) {
            // Set basic information
            tvPatientName.setText("Patient: " + getSafeString(examination.getPatientName()));
            tvDoctorName.setText("Doctor: " + getSafeString(examination.getDoctor()));

            // Set medical information
            tvDiagnosis.setText(getSafeString(examination.getDiagnosis(), "Diagnosis not specified"));
            tvPrescription.setText(getSafeString(examination.getPrescription(), "Prescription not specified"));
            tvRecommendations.setText(getSafeString(examination.getRecommendations(), "No specific recommendations"));
            tvLabTests.setText(getSafeString(examination.getLabTests(), "No lab tests requested"));

            // Set timestamp
            tvTimestamp.setText("Completed: " + formatTimestamp(examination.getTimestamp() != null ? examination.getTimestamp() : examination.getCompletedAt()));
        } else {
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        tvPatientName.setText("Patient: Information not available");
        tvDoctorName.setText("Doctor: Information not available");
        tvDiagnosis.setText("Diagnosis: Information not available");
        tvPrescription.setText("Prescription: Information not available");
        tvRecommendations.setText("Recommendations: Information not available");
        tvLabTests.setText("Lab Tests: Information not available");
        tvTimestamp.setText("Completed: Unknown date");
    }

    private String getSafeString(String value) {
        return value != null && !value.isEmpty() ? value : "Not specified";
    }

    private String getSafeString(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Unknown date";
        }

        try {
            // Simple formatting
            if (timestamp.contains("T")) {
                // ISO format: 2024-01-15T10:30:00
                String datePart = timestamp.split("T")[0];
                return datePart;
            }
            return timestamp;
        } catch (Exception e) {
            return timestamp;
        }
    }
}