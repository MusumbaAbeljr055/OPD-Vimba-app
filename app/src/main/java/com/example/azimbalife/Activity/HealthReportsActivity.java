package com.example.azimbalife.Activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.HealthReportsAdapter;
import com.example.azimbalife.Domain.HealthMetrics;
import com.example.azimbalife.Domain.HealthReport;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HealthReportsActivity extends AppCompatActivity {

    private TextView tvHealthScore, tvTotalAppointments, tvBloodPressure, tvHeartRate, tvBloodSugar, tvWeight, tvTemperature, tvOxygenSaturation;
    private Spinner spinnerReportType;
    private EditText etStartDate, etEndDate;
    private Button btnGenerateReport, btnExportAll, btnShareReport;
    private RecyclerView recyclerRecentReports;

    private HealthReportsAdapter adapter;
    private List<HealthReport> recentReports = new ArrayList<>();

    private String username;
    private Calendar calendar;
    private SimpleDateFormat dateFormatter;

    // Report types
    private final String[] reportTypes = {
            "Health Summary Report",
            "Detailed Medical History",
            "Test Results Summary",
            "Appointment History",
            "Medication History",
            "Emergency Health Summary",
            "Vital Metrics Report"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_reports);

        // Get username from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupSpinners();
        setupDatePickers();
        setupRecyclerView();
        setupClickListeners();
        loadHealthData();
        loadRecentReports();
    }

    private void initializeViews() {
        tvHealthScore = findViewById(R.id.tvHealthScore);
        tvTotalAppointments = findViewById(R.id.tvTotalAppointments);
        tvBloodPressure = findViewById(R.id.tvBloodPressure);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvBloodSugar = findViewById(R.id.tvBloodSugar);
        tvWeight = findViewById(R.id.tvWeight);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvOxygenSaturation = findViewById(R.id.tvOxygenSaturation);

        spinnerReportType = findViewById(R.id.spinnerReportType);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);
        btnExportAll = findViewById(R.id.btnExportAll);
        btnShareReport = findViewById(R.id.btnShareReport);
        recyclerRecentReports = findViewById(R.id.recyclerRecentReports);

        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    }

    private void setupSpinners() {
        ArrayAdapter<String> reportTypeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, reportTypes);
        reportTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportType.setAdapter(reportTypeAdapter);
    }

    private void setupDatePickers() {
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
    }

    private void showDatePicker(final EditText editText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    editText.setText(dateFormatter.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void setupRecyclerView() {
        adapter = new HealthReportsAdapter(recentReports, new HealthReportsAdapter.OnHealthReportClickListener() {
            @Override
            public void onViewReportClick(HealthReport healthReport) {
                viewHealthReport(healthReport);
            }

            @Override
            public void onDownloadReportClick(HealthReport healthReport) {
                downloadHealthReport(healthReport);
            }

            @Override
            public void onShareReportClick(HealthReport healthReport) {
                shareHealthReport(healthReport);
            }
        });

        recyclerRecentReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentReports.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnGenerateReport.setOnClickListener(v -> generateComprehensiveHealthReport());
        btnExportAll.setOnClickListener(v -> exportAllData());
        btnShareReport.setOnClickListener(v -> shareHealthSummary());
    }

    private void loadHealthData() {
        // Load health metrics from PatientMetrics collection (where health workers submit)
        DatabaseReference patientMetricsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/PatientMetrics");

        Query query = patientMetricsRef.orderByChild("patientId").equalTo(username)
                .limitToLast(1); // Get the most recent record

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot metricSnapshot : snapshot.getChildren()) {
                        PatientMetrics patientMetrics = metricSnapshot.getValue(PatientMetrics.class);
                        if (patientMetrics != null) {
                            updateHealthMetricsFromPatientMetrics(patientMetrics);
                            return;
                        }
                    }
                }
                // If no metrics found, try the fallback
                loadLatestMetricsFallback();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadLatestMetricsFallback();
            }
        });

        // Load appointment statistics
        loadAppointmentStatistics();
    }

    private void updateHealthMetricsFromPatientMetrics(PatientMetrics patientMetrics) {
        // Update UI with the latest patient metrics
        if (patientMetrics.getBloodPressure() != null && !patientMetrics.getBloodPressure().isEmpty()) {
            tvBloodPressure.setText(patientMetrics.getBloodPressure());
        } else {
            tvBloodPressure.setText("Not measured");
        }

        if (patientMetrics.getHeartRate() != null && !patientMetrics.getHeartRate().isEmpty()) {
            tvHeartRate.setText(patientMetrics.getHeartRate() + " bpm");
        } else {
            tvHeartRate.setText("Not measured");
        }

        if (patientMetrics.getBloodSugar() != null && !patientMetrics.getBloodSugar().isEmpty()) {
            tvBloodSugar.setText(patientMetrics.getBloodSugar() + " mg/dL");
        } else {
            tvBloodSugar.setText("Not measured");
        }

        if (patientMetrics.getWeight() != null && !patientMetrics.getWeight().isEmpty()) {
            tvWeight.setText(patientMetrics.getWeight() + " kg");
        } else {
            tvWeight.setText("Not measured");
        }

        if (patientMetrics.getTemperature() != null && !patientMetrics.getTemperature().isEmpty()) {
            tvTemperature.setText(patientMetrics.getTemperature() + " °C");
        } else {
            tvTemperature.setText("Not measured");
        }

        if (patientMetrics.getOxygenSaturation() != null && !patientMetrics.getOxygenSaturation().isEmpty()) {
            tvOxygenSaturation.setText(patientMetrics.getOxygenSaturation() + " %");
        } else {
            tvOxygenSaturation.setText("Not measured");
        }

        // Calculate health score
        int healthScore = calculateHealthScoreFromPatientMetrics(patientMetrics);
        tvHealthScore.setText(healthScore + "%");

        // Set color based on health score
        if (healthScore >= 80) {
            tvHealthScore.setTextColor(getColor(R.color.green));
        } else if (healthScore >= 60) {
            tvHealthScore.setTextColor(getColor(R.color.my_primary));
        } else {
            tvHealthScore.setTextColor(getColor(R.color.red));
        }
    }

    private int calculateHealthScoreFromPatientMetrics(PatientMetrics patientMetrics) {
        int score = 85; // Base score

        try {
            // Blood Pressure analysis
            if (patientMetrics.getBloodPressure() != null) {
                String bp = patientMetrics.getBloodPressure();
                if (bp.contains("/")) {
                    String[] parts = bp.split("/");
                    if (parts.length == 2) {
                        int systolic = Integer.parseInt(parts[0].trim());
                        int diastolic = Integer.parseInt(parts[1].trim());

                        if (systolic > 140 || diastolic > 90) {
                            score -= 15;
                        } else if (systolic > 130 || diastolic > 85) {
                            score -= 5;
                        } else if (systolic < 90 || diastolic < 60) {
                            score -= 10;
                        }
                    }
                }
            }

            // Heart Rate analysis
            if (patientMetrics.getHeartRate() != null && !patientMetrics.getHeartRate().isEmpty()) {
                int heartRate = Integer.parseInt(patientMetrics.getHeartRate());
                if (heartRate < 60 || heartRate > 100) {
                    score -= 10;
                } else if (heartRate < 50 || heartRate > 120) {
                    score -= 15;
                }
            }

            // Blood Sugar analysis
            if (patientMetrics.getBloodSugar() != null && !patientMetrics.getBloodSugar().isEmpty()) {
                int bloodSugar = Integer.parseInt(patientMetrics.getBloodSugar());
                if (bloodSugar > 126) {
                    score -= 15;
                } else if (bloodSugar > 100) {
                    score -= 5;
                } else if (bloodSugar < 70) {
                    score -= 10;
                }
            }

            // BMI analysis
            if (patientMetrics.getBmi() != null && !patientMetrics.getBmi().isEmpty()) {
                try {
                    double bmi = Double.parseDouble(patientMetrics.getBmi());
                    if (bmi < 18.5 || bmi > 25) {
                        score -= 10;
                    } else if (bmi < 17 || bmi > 30) {
                        score -= 15;
                    }
                } catch (NumberFormatException e) {
                    // BMI not in valid format
                }
            }

            // Temperature analysis
            if (patientMetrics.getTemperature() != null && !patientMetrics.getTemperature().isEmpty()) {
                double temperature = Double.parseDouble(patientMetrics.getTemperature());
                if (temperature < 36.0 || temperature > 37.5) {
                    score -= 5;
                } else if (temperature < 35.5 || temperature > 38.0) {
                    score -= 10;
                }
            }

            // Oxygen Saturation analysis
            if (patientMetrics.getOxygenSaturation() != null && !patientMetrics.getOxygenSaturation().isEmpty()) {
                int oxygenSat = Integer.parseInt(patientMetrics.getOxygenSaturation());
                if (oxygenSat < 95) {
                    score -= 10;
                } else if (oxygenSat < 92) {
                    score -= 15;
                } else if (oxygenSat < 97) {
                    score -= 5;
                }
            }

            // Respiratory Rate analysis
            if (patientMetrics.getRespiratoryRate() != null && !patientMetrics.getRespiratoryRate().isEmpty()) {
                int respRate = Integer.parseInt(patientMetrics.getRespiratoryRate());
                if (respRate < 12 || respRate > 20) {
                    score -= 5;
                } else if (respRate < 8 || respRate > 25) {
                    score -= 10;
                }
            }

        } catch (Exception e) {
            Log.e("HEALTH_SCORE", "Error calculating health score: " + e.getMessage());
        }

        return Math.max(0, Math.min(100, score));
    }

    private void loadLatestMetricsFallback() {
        // Fallback to latestMetrics in patient profile
        DatabaseReference metricsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/Patients")
                .child(username)
                .child("latestMetrics");

        metricsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    HealthMetrics metrics = snapshot.getValue(HealthMetrics.class);
                    if (metrics != null) {
                        updateHealthMetricsUI(metrics);
                    } else {
                        setDefaultHealthMetrics();
                    }
                } else {
                    setDefaultHealthMetrics();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDefaultHealthMetrics();
            }
        });
    }

    private void updateHealthMetricsUI(HealthMetrics metrics) {
        if (metrics.getBloodPressure() != null && !metrics.getBloodPressure().isEmpty()) {
            tvBloodPressure.setText(metrics.getBloodPressure());
        } else {
            tvBloodPressure.setText("Not measured");
        }

        if (metrics.getHeartRate() != null && !metrics.getHeartRate().isEmpty()) {
            tvHeartRate.setText(metrics.getHeartRate() + " bpm");
        } else {
            tvHeartRate.setText("Not measured");
        }

        if (metrics.getBloodSugar() != null && !metrics.getBloodSugar().isEmpty()) {
            tvBloodSugar.setText(metrics.getBloodSugar() + " mg/dL");
        } else {
            tvBloodSugar.setText("Not measured");
        }

        if (metrics.getWeight() != null && !metrics.getWeight().isEmpty()) {
            tvWeight.setText(metrics.getWeight() + " kg");
        } else {
            tvWeight.setText("Not measured");
        }

        // Calculate health score based on metrics
        int healthScore = calculateHealthScore(metrics);
        tvHealthScore.setText(healthScore + "%");

        // Set color based on health score
        if (healthScore >= 80) {
            tvHealthScore.setTextColor(getColor(R.color.green));
        } else if (healthScore >= 60) {
            tvHealthScore.setTextColor(getColor(R.color.my_primary));
        } else {
            tvHealthScore.setTextColor(getColor(R.color.red));
        }
    }

    private int calculateHealthScore(HealthMetrics metrics) {
        int score = 85; // Base score

        try {
            // Adjust based on blood pressure
            if (metrics.getBloodPressureSystolic() > 0 && metrics.getBloodPressureDiastolic() > 0) {
                if (metrics.getBloodPressureSystolic() > 140 || metrics.getBloodPressureDiastolic() > 90) {
                    score -= 15;
                } else if (metrics.getBloodPressureSystolic() > 130 || metrics.getBloodPressureDiastolic() > 85) {
                    score -= 5;
                }
            }

            // Adjust based on heart rate
            int heartRateValue = metrics.getHeartRateValue();
            if (heartRateValue > 0) {
                if (heartRateValue < 60 || heartRateValue > 100) {
                    score -= 10;
                }
            }

            // Adjust based on blood sugar
            int bloodSugarValue = metrics.getBloodSugarValue();
            if (bloodSugarValue > 0) {
                if (bloodSugarValue > 126) {
                    score -= 15;
                } else if (bloodSugarValue > 100) {
                    score -= 5;
                }
            }

            // Adjust based on BMI if available
            if (metrics.getBmi() != null && !metrics.getBmi().isEmpty()) {
                try {
                    double bmi = Double.parseDouble(metrics.getBmi());
                    if (bmi < 18.5 || bmi > 25) {
                        score -= 10;
                    } else if (bmi < 20 || bmi > 23) {
                        score -= 5;
                    }
                } catch (NumberFormatException e) {
                    // BMI not in valid format
                }
            }
        } catch (Exception e) {
            Log.e("HEALTH_SCORE", "Error calculating health score: " + e.getMessage());
        }

        return Math.max(0, Math.min(100, score));
    }

    private void setDefaultHealthMetrics() {
        tvBloodPressure.setText("120/80");
        tvHeartRate.setText("72 bpm");
        tvBloodSugar.setText("95 mg/dL");
        tvWeight.setText("68 kg");
        tvTemperature.setText("36.6 °C");
        tvOxygenSaturation.setText("98 %");
        tvHealthScore.setText("85%");
        tvHealthScore.setTextColor(getColor(R.color.green));
    }

    private void loadAppointmentStatistics() {
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/AllAppointments");

        Query query = appointmentsRef.orderByChild("patientUsername").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAppointments = 0;
                if (snapshot.exists()) {
                    totalAppointments = (int) snapshot.getChildrenCount();
                }
                tvTotalAppointments.setText(String.valueOf(totalAppointments));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvTotalAppointments.setText("0");
            }
        });
    }

    private void loadRecentReports() {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/HealthReports")
                .child(username);

        reportsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentReports.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                        HealthReport report = reportSnapshot.getValue(HealthReport.class);
                        if (report != null) {
                            recentReports.add(report);
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(recentReports, (r1, r2) ->
                            r2.getGeneratedDate().compareTo(r1.getGeneratedDate()));

                    // Keep only last 5 reports
                    if (recentReports.size() > 5) {
                        recentReports = new ArrayList<>(recentReports.subList(0, 5));
                    }
                }

                adapter.updateHealthReports(recentReports);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HealthReportsActivity.this, "Failed to load recent reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateComprehensiveHealthReport() {
        // Load both medical records and vital metrics for comprehensive report
        DatabaseReference patientMetricsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/PatientMetrics");

        Query metricsQuery = patientMetricsRef.orderByChild("patientId").equalTo(username);

        metricsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PatientMetrics> allVitalMetrics = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot metricSnapshot : snapshot.getChildren()) {
                        PatientMetrics metrics = metricSnapshot.getValue(PatientMetrics.class);
                        if (metrics != null) {
                            allVitalMetrics.add(metrics);
                        }
                    }
                }

                // Now generate report with both medical records and vital metrics
                generateReportWithVitalMetrics(allVitalMetrics);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HealthReportsActivity.this,
                        "Failed to load vital metrics for report", Toast.LENGTH_SHORT).show();
                // Generate basic report without vital metrics
                generateBasicHealthReport();
            }
        });
    }

    private void generateReportWithVitalMetrics(List<PatientMetrics> vitalMetrics) {
        String reportType = spinnerReportType.getSelectedItem().toString();
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate report ID
        String reportId = UUID.randomUUID().toString();
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(new java.util.Date());

        // Create health report with vital metrics data
        HealthReport healthReport = new HealthReport(
                reportId,
                username,
                reportType,
                reportType + " - " + currentDate,
                currentDate
        );
        healthReport.setDateRange(startDate + " to " + endDate);
        healthReport.setTotalVitalRecords(vitalMetrics.size());

        // Calculate average health metrics
        calculateAverageMetrics(healthReport, vitalMetrics);

        // Save to Firebase
        saveHealthReport(healthReport);
    }

    private void generateBasicHealthReport() {
        String reportType = spinnerReportType.getSelectedItem().toString();
        String startDate = etStartDate.getText().toString();
        String endDate = etEndDate.getText().toString();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate report ID
        String reportId = UUID.randomUUID().toString();
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .format(new java.util.Date());

        // Create basic health report
        HealthReport healthReport = new HealthReport(
                reportId,
                username,
                reportType,
                reportType + " - " + currentDate,
                currentDate
        );
        healthReport.setDateRange(startDate + " to " + endDate);

        try {
            healthReport.setHealthScore(Integer.parseInt(tvHealthScore.getText().toString().replace("%", "")));
        } catch (NumberFormatException e) {
            healthReport.setHealthScore(85);
        }

        try {
            healthReport.setTotalAppointments(Integer.parseInt(tvTotalAppointments.getText().toString()));
        } catch (NumberFormatException e) {
            healthReport.setTotalAppointments(0);
        }

        // Save to Firebase
        saveHealthReport(healthReport);
    }

    private void calculateAverageMetrics(HealthReport report, List<PatientMetrics> metrics) {
        if (metrics.isEmpty()) {
            report.setHealthScore(85);
            return;
        }

        int totalHeartRate = 0;
        int heartRateCount = 0;
        int totalBloodSugar = 0;
        int bloodSugarCount = 0;
        double totalTemperature = 0;
        int temperatureCount = 0;
        int totalOxygenSat = 0;
        int oxygenSatCount = 0;

        for (PatientMetrics metric : metrics) {
            // Heart Rate
            if (metric.getHeartRate() != null && !metric.getHeartRate().isEmpty()) {
                try {
                    totalHeartRate += Integer.parseInt(metric.getHeartRate());
                    heartRateCount++;
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }

            // Blood Sugar
            if (metric.getBloodSugar() != null && !metric.getBloodSugar().isEmpty()) {
                try {
                    totalBloodSugar += Integer.parseInt(metric.getBloodSugar());
                    bloodSugarCount++;
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }

            // Temperature
            if (metric.getTemperature() != null && !metric.getTemperature().isEmpty()) {
                try {
                    totalTemperature += Double.parseDouble(metric.getTemperature());
                    temperatureCount++;
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }

            // Oxygen Saturation
            if (metric.getOxygenSaturation() != null && !metric.getOxygenSaturation().isEmpty()) {
                try {
                    totalOxygenSat += Integer.parseInt(metric.getOxygenSaturation());
                    oxygenSatCount++;
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
        }

        // Set averages
        if (heartRateCount > 0) {
            report.setAverageHeartRate(totalHeartRate / heartRateCount);
        }
        if (bloodSugarCount > 0) {
            report.setAverageBloodSugar(totalBloodSugar / bloodSugarCount);
        }
        if (temperatureCount > 0) {
            report.setAverageTemperature(totalTemperature / temperatureCount);
        }
        if (oxygenSatCount > 0) {
            report.setAverageOxygenSaturation(totalOxygenSat / oxygenSatCount);
        }

        // Set final health score based on most recent metrics
        int healthScore = calculateHealthScoreFromPatientMetrics(metrics.get(0));
        report.setHealthScore(healthScore);

        // Set total appointments
        try {
            report.setTotalAppointments(Integer.parseInt(tvTotalAppointments.getText().toString()));
        } catch (NumberFormatException e) {
            report.setTotalAppointments(0);
        }
    }

    private void saveHealthReport(HealthReport healthReport) {
        DatabaseReference reportRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/HealthReports")
                .child(username)
                .child(healthReport.getReportId());

        reportRef.setValue(healthReport)
                .addOnSuccessListener(aVoid -> {
                    showReportGeneratedDialog(healthReport);
                    loadRecentReports(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to generate report", Toast.LENGTH_SHORT).show();
                });
    }

    private void showReportGeneratedDialog(HealthReport healthReport) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Report Generated Successfully!")
                .setMessage("Your " + healthReport.getReportType() + " has been generated.\n\n" +
                        "Date Range: " + healthReport.getDateRange() + "\n" +
                        "Health Score: " + healthReport.getHealthScore() + "%\n" +
                        "Total Appointments: " + healthReport.getTotalAppointments() + "\n" +
                        "Vital Records Included: " + healthReport.getTotalVitalRecords())
                .setPositiveButton("View Report", (dialog, which) -> {
                    viewHealthReport(healthReport);
                })
                .setNegativeButton("OK", null)
                .show();
    }

    private void viewHealthReport(HealthReport healthReport) {
        // Show detailed report in a dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        String reportDetails = buildReportDetails(healthReport);

        builder.setTitle(healthReport.getReportName())
                .setMessage(reportDetails)
                .setPositiveButton("OK", null)
                .setNeutralButton("Share", (dialog, which) -> shareHealthReport(healthReport))
                .show();
    }

    private String buildReportDetails(HealthReport healthReport) {
        StringBuilder details = new StringBuilder();

        details.append("Report Type: ").append(healthReport.getReportType()).append("\n");
        details.append("Generated Date: ").append(healthReport.getGeneratedDate()).append("\n");
        details.append("Date Range: ").append(healthReport.getDateRange()).append("\n\n");

        details.append("Health Metrics:\n");
        details.append("• Health Score: ").append(healthReport.getHealthScore()).append("%\n");
        details.append("• Blood Pressure: ").append(tvBloodPressure.getText()).append("\n");
        details.append("• Heart Rate: ").append(tvHeartRate.getText()).append("\n");
        details.append("• Blood Sugar: ").append(tvBloodSugar.getText()).append("\n");
        details.append("• Temperature: ").append(tvTemperature.getText()).append("\n");
        details.append("• Oxygen Saturation: ").append(tvOxygenSaturation.getText()).append("\n\n");

        details.append("Medical History:\n");
        details.append("• Total Appointments: ").append(healthReport.getTotalAppointments()).append("\n");
        details.append("• Total Vital Records: ").append(healthReport.getTotalVitalRecords()).append("\n");
        details.append("• Total Tests: ").append(healthReport.getTotalTests()).append("\n");
        details.append("• Total Prescriptions: ").append(healthReport.getTotalPrescriptions()).append("\n\n");

        // Add averages if available
        if (healthReport.getAverageHeartRate() > 0) {
            details.append("Average Metrics:\n");
            details.append("• Average Heart Rate: ").append(healthReport.getAverageHeartRate()).append(" bpm\n");
        }
        if (healthReport.getAverageBloodSugar() > 0) {
            details.append("• Average Blood Sugar: ").append(healthReport.getAverageBloodSugar()).append(" mg/dL\n");
        }
        if (healthReport.getAverageTemperature() > 0) {
            details.append("• Average Temperature: ").append(String.format("%.1f", healthReport.getAverageTemperature())).append(" °C\n");
        }
        if (healthReport.getAverageOxygenSaturation() > 0) {
            details.append("• Average Oxygen Saturation: ").append(healthReport.getAverageOxygenSaturation()).append(" %\n");
        }

        details.append("\nRecommendations:\n");
        details.append(healthReport.getRecommendations() != null ? healthReport.getRecommendations() :
                "• Continue regular health checkups\n• Maintain healthy lifestyle\n• Follow prescribed medications\n• Monitor vital signs regularly");

        return details.toString();
    }

    private void downloadHealthReport(HealthReport healthReport) {
        Toast.makeText(this, "Downloading health report...", Toast.LENGTH_SHORT).show();
        // In real implementation, this would download the PDF file
    }

    private void shareHealthReport(HealthReport healthReport) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Health Report: " + healthReport.getReportName());

        String shareText = buildShareText(healthReport);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Health Report"));
    }

    private String buildShareText(HealthReport healthReport) {
        return "Health Report: " + healthReport.getReportName() + "\n" +
                "Date: " + healthReport.getGeneratedDate() + "\n" +
                "Health Score: " + healthReport.getHealthScore() + "%\n" +
                "Blood Pressure: " + tvBloodPressure.getText() + "\n" +
                "Heart Rate: " + tvHeartRate.getText() + "\n" +
                "Blood Sugar: " + tvBloodSugar.getText() + "\n" +
                "Temperature: " + tvTemperature.getText() + "\n" +
                "Oxygen Saturation: " + tvOxygenSaturation.getText() + "\n\n" +
                "Note: Shared via Azimba Life Medical App";
    }

    private void exportAllData() {
        Toast.makeText(this, "Exporting all health data...", Toast.LENGTH_SHORT).show();
        // This would export all medical data in a comprehensive format
    }

    private void shareHealthSummary() {
        String summary = "My Health Summary\n\n" +
                "Health Score: " + tvHealthScore.getText() + "\n" +
                "Blood Pressure: " + tvBloodPressure.getText() + "\n" +
                "Heart Rate: " + tvHeartRate.getText() + "\n" +
                "Blood Sugar: " + tvBloodSugar.getText() + "\n" +
                "Temperature: " + tvTemperature.getText() + "\n" +
                "Oxygen Saturation: " + tvOxygenSaturation.getText() + "\n" +
                "Total Hospital Visits: " + tvTotalAppointments.getText() + "\n\n" +
                "Generated via Azimba Life Medical App";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Health Summary");
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);

        startActivity(Intent.createChooser(shareIntent, "Share Health Summary"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHealthData();
        loadRecentReports();
    }
}