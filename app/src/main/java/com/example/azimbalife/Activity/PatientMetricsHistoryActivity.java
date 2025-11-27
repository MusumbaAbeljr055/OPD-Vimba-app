package com.example.azimbalife.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.PatientMetricsHistoryAdapter;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PatientMetricsHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerMetricsHistory;
    private ProgressBar progressBar;
    private TextView tvPatientName, tvNoRecords, tvHistoryTitle;

    private PatientMetricsHistoryAdapter metricsHistoryAdapter;
    private List<PatientMetrics> metricsHistoryList;

    private String patientId;
    private String healthWorkerId;
    private String healthWorkerName;
    private String patientName;

    private DatabaseReference patientsRef;
    private DatabaseReference metricsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_metrics_history);

        // Get data from intent
        patientId = getIntent().getStringExtra("patientId");
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");

        if (patientId == null) {
            Toast.makeText(this, "Patient not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        loadPatientInfo();
        loadMetricsHistory();
    }

    private void initializeViews() {
        recyclerMetricsHistory = findViewById(R.id.recyclerMetricsHistory);
        progressBar = findViewById(R.id.progressBar);
        tvPatientName = findViewById(R.id.tvPatientName);
        tvNoRecords = findViewById(R.id.tvNoRecords);
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);

        metricsHistoryList = new ArrayList<>();
    }

    private void setupFirebase() {
        patientsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Patients");
        metricsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/PatientMetrics");
    }

    private void setupRecyclerView() {
        metricsHistoryAdapter = new PatientMetricsHistoryAdapter(metricsHistoryList);
        recyclerMetricsHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerMetricsHistory.setAdapter(metricsHistoryAdapter);
    }

    private void loadPatientInfo() {
        patientsRef.child(patientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    patientName = snapshot.child("name").getValue(String.class);
                    if (patientName != null) {
                        tvPatientName.setText(patientName);
                        tvHistoryTitle.setText("Vital Signs History - " + patientName);
                    } else {
                        tvPatientName.setText("Patient ID: " + patientId);
                    }

                    // Load patient demographics for context
                    String age = snapshot.child("age").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);

                    if (age != null || gender != null) {
                        String demographics = "";
                        if (age != null) demographics += age + " years";
                        if (gender != null) {
                            if (!demographics.isEmpty()) demographics += " • ";
                            demographics += gender;
                        }
                        tvPatientName.append("\n" + demographics);
                    }
                } else {
                    tvPatientName.setText("Patient ID: " + patientId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvPatientName.setText("Patient ID: " + patientId);
            }
        });
    }

    private void loadMetricsHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoRecords.setVisibility(View.GONE);

        // Query metrics for this specific patient, ordered by timestamp
        Query patientMetricsQuery = metricsRef.orderByChild("patientId").equalTo(patientId);

        patientMetricsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                metricsHistoryList.clear();

                for (DataSnapshot metricsSnapshot : snapshot.getChildren()) {
                    PatientMetrics metrics = metricsSnapshot.getValue(PatientMetrics.class);
                    if (metrics != null) {
                        metricsHistoryList.add(metrics);
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(metricsHistoryList, new Comparator<PatientMetrics>() {
                    @Override
                    public int compare(PatientMetrics m1, PatientMetrics m2) {
                        return m2.getTimestamp().compareTo(m1.getTimestamp());
                    }
                });

                progressBar.setVisibility(View.GONE);

                if (metricsHistoryList.isEmpty()) {
                    tvNoRecords.setVisibility(View.VISIBLE);
                    tvNoRecords.setText("No vital signs records found for this patient");
                } else {
                    tvNoRecords.setVisibility(View.GONE);
                    metricsHistoryAdapter.updateMetricsList(metricsHistoryList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvNoRecords.setVisibility(View.VISIBLE);
                tvNoRecords.setText("Failed to load records. Please try again.");
                Toast.makeText(PatientMetricsHistoryActivity.this,
                        "Error loading patient history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to filter records by date range (can be called from a filter dialog)
    public void filterRecordsByDate(String startDate, String endDate) {
        if (startDate.isEmpty() || endDate.isEmpty()) {
            loadMetricsHistory(); // Reload all if no date range
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Query dateRangeQuery = metricsRef.orderByChild("patientId").equalTo(patientId);
        dateRangeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PatientMetrics> filteredList = new ArrayList<>();

                for (DataSnapshot metricsSnapshot : snapshot.getChildren()) {
                    PatientMetrics metrics = metricsSnapshot.getValue(PatientMetrics.class);
                    if (metrics != null && isDateInRange(metrics.getDate(), startDate, endDate)) {
                        filteredList.add(metrics);
                    }
                }

                // Sort by timestamp (newest first)
                Collections.sort(filteredList, new Comparator<PatientMetrics>() {
                    @Override
                    public int compare(PatientMetrics m1, PatientMetrics m2) {
                        return m2.getTimestamp().compareTo(m1.getTimestamp());
                    }
                });

                progressBar.setVisibility(View.GONE);
                metricsHistoryList.clear();
                metricsHistoryList.addAll(filteredList);
                metricsHistoryAdapter.updateMetricsList(metricsHistoryList);

                if (filteredList.isEmpty()) {
                    tvNoRecords.setVisibility(View.VISIBLE);
                    tvNoRecords.setText("No records found for the selected date range");
                } else {
                    tvNoRecords.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PatientMetricsHistoryActivity.this,
                        "Error filtering records", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isDateInRange(String recordDate, String startDate, String endDate) {
        try {
            // Assuming date format is "dd-MM-yyyy"
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date record = sdf.parse(recordDate);
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            return (record != null && start != null && end != null) &&
                    !record.before(start) && !record.after(end);
        } catch (Exception e) {
            return false;
        }
    }

    public void onBackClicked(View view) {
        onBackPressed();
    }

    public void onRefreshClicked(View view) {
        loadMetricsHistory();
    }

    public void onFilterClicked(View view) {
        showDateFilterDialog();
    }

    private void showDateFilterDialog() {
        // Implement a date range picker dialog
        // This is a simplified version - you might want to use a proper DatePickerDialog
        Toast.makeText(this, "Date filter feature - to be implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (patientId != null) {
            loadMetricsHistory();
        }
    }
}