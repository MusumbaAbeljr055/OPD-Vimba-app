package com.example.azimbalife.Activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.TestResultsAdapter;
import com.example.azimbalife.Domain.TestResult;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageTestResultsActivity extends AppCompatActivity implements TestResultsAdapter.OnTestResultClickListener {

    private EditText etPatientUsername, etTestName, etFindings, etRecommendations;
    private Spinner spinnerTestType, spinnerStatus;
    private Button btnUploadResult, btnUploadFile, btnViewAllResults;
    private ProgressBar progressBar;
    private TextView tvFileName, tvNoResults, tvResultsCount, tvNormalCount, tvAbnormalCount;
    private RecyclerView recyclerRecentResults;

    private TestResultsAdapter testResultsAdapter;
    private List<TestResult> recentTestResults;

    private String healthWorkerId;
    private String healthWorkerName;

    private DatabaseReference patientsRef;
    private DatabaseReference testResultsRef;
    private StorageReference storageRef;

    private Uri fileUri;
    private static final int FILE_PICKER_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_test_results);

        // Get health worker data from intent
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");

        if (healthWorkerId == null) {
            Toast.makeText(this, "Health worker not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Null safety for healthWorkerName
        if (healthWorkerName == null) {
            healthWorkerName = "Health Worker";
            Log.w("HEALTH_WORKER", "healthWorkerName was null, using default");
        }

        initializeViews();
        setupFirebase();
        setupSpinners();
        setupClickListeners();
        setupRecyclerView();
        loadRecentTestResults();
        loadStatistics();
    }

    private void initializeViews() {
        // Form fields
        etPatientUsername = findViewById(R.id.etPatientUsername);
        etTestName = findViewById(R.id.etTestName);
        etFindings = findViewById(R.id.etFindings);
        etRecommendations = findViewById(R.id.etRecommendations);

        // Spinners
        spinnerTestType = findViewById(R.id.spinnerTestType);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        // Buttons
        btnUploadResult = findViewById(R.id.btnUploadResult);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnViewAllResults = findViewById(R.id.btnViewAllResults);

        // Progress and status
        progressBar = findViewById(R.id.progressBar);
        tvFileName = findViewById(R.id.tvFileName);
        tvNoResults = findViewById(R.id.tvNoResults);

        // Statistics
        tvResultsCount = findViewById(R.id.tvResultsCount);
        tvNormalCount = findViewById(R.id.tvNormalCount);
        tvAbnormalCount = findViewById(R.id.tvAbnormalCount);

        // RecyclerView
        recyclerRecentResults = findViewById(R.id.recyclerRecentResults);

        recentTestResults = new ArrayList<>();

        // Log for debugging
        Log.d("VIEW_INIT", "tvNoResults initialized: " + (tvNoResults != null));
        Log.d("VIEW_INIT", "tvResultsCount initialized: " + (tvResultsCount != null));
    }

    private void setupFirebase() {
        patientsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Patients");
        testResultsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/TestResults");
        storageRef = FirebaseStorage.getInstance().getReference("MbararaHospital/TestResults");
    }

    private void setupSpinners() {
        // Test type spinner
        String[] testTypes = {"Select Test Type", "Blood Test", "Urine Test", "X-Ray", "CT Scan",
                "MRI", "Ultrasound", "ECG", "EEG", "Biopsy", "Culture", "Other"};
        ArrayAdapter<String> testTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, testTypes);
        testTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTestType.setAdapter(testTypeAdapter);

        // Status spinner
        String[] statuses = {"Select Status", "Normal", "Abnormal", "Critical"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
    }

    private void setupClickListeners() {
        btnUploadResult.setOnClickListener(v -> uploadTestResult());
        btnUploadFile.setOnClickListener(v -> openFilePicker());
        btnViewAllResults.setOnClickListener(v -> viewAllTestResults());
    }

    private void setupRecyclerView() {
        testResultsAdapter = new TestResultsAdapter(recentTestResults, this);
        recyclerRecentResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentResults.setAdapter(testResultsAdapter);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // All file types
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Test Result File"), FILE_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            if (fileUri != null) {
                String fileName = getFileName(fileUri);
                tvFileName.setText("File: " + fileName);
                tvFileName.setVisibility(View.VISIBLE);
                Log.d("FILE_PICKER", "File selected: " + fileName);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("FILE_NAME", "Error getting file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "Unknown File";
    }

    private void uploadTestResult() {
        String patientUsername = etPatientUsername.getText().toString().trim();
        String testName = etTestName.getText().toString().trim();
        String findings = etFindings.getText().toString().trim();
        String recommendations = etRecommendations.getText().toString().trim();

        String testType = spinnerTestType.getSelectedItem().toString();
        String status = spinnerStatus.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(patientUsername)) {
            etPatientUsername.setError("Patient Username is required");
            etPatientUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(testName)) {
            etTestName.setError("Test name is required");
            etTestName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(findings)) {
            etFindings.setError("Findings are required");
            etFindings.requestFocus();
            return;
        }

        if ("Select Test Type".equals(testType)) {
            Toast.makeText(this, "Please select test type", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Select Status".equals(status)) {
            Toast.makeText(this, "Please select status", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // First verify patient exists
        Query patientQuery = patientsRef.orderByChild("username").equalTo(patientUsername);
        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ManageTestResultsActivity.this,
                            "Patient username not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get patient ID from the snapshot
                String patientId = null;
                for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                    patientId = patientSnapshot.getKey();
                    break;
                }

                if (patientId == null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ManageTestResultsActivity.this,
                            "Patient not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Proceed to upload file if selected, then save test result
                if (fileUri != null) {
                    uploadFileAndSaveResult(patientUsername, patientId, testName, testType,
                            findings, recommendations, status);
                } else {
                    saveTestResult(patientUsername, patientId, testName, testType,
                            findings, recommendations, status, null, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageTestResultsActivity.this,
                        "Error verifying patient: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("PATIENT_VERIFY", "Error: " + error.getMessage());
            }
        });
    }

    private void uploadFileAndSaveResult(String patientUsername, String patientId, String testName,
                                         String testType, String findings, String recommendations,
                                         String status) {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading File");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileName = "test_result_" + System.currentTimeMillis() + "_" + patientId;
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        progressDialog.dismiss();
                        saveTestResult(patientUsername, patientId, testName, testType,
                                findings, recommendations, status, downloadUri.toString(), getFileName(fileUri));
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ManageTestResultsActivity.this,
                            "File upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FILE_UPLOAD", "Upload failed", e);
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading: " + (int) progress + "%");
                });
    }

    private void saveTestResult(String patientUsername, String patientId, String testName,
                                String testType, String findings, String recommendations,
                                String status, String fileUrl, String fileName) {

        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String resultId = testResultsRef.push().getKey();

        if (resultId == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error generating test result ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create TestResult object
        TestResult testResult = new TestResult();
        testResult.setResultId(resultId);
        testResult.setPatientUsername(patientUsername);
        testResult.setTestType(testType);
        testResult.setTestName(testName);
        testResult.setTestDate(currentDate);
        testResult.setResultDate(currentDate);
        testResult.setLabName("Mbarara Hospital Laboratory");
        testResult.setDoctorName(healthWorkerName);
        testResult.setFindings(findings);
        testResult.setRecommendations(recommendations);
        testResult.setFileUrl(fileUrl);
        testResult.setFileName(fileName);
        testResult.setStatus(status);
        testResult.setCreatedAt(String.valueOf(System.currentTimeMillis()));
        testResult.setDownloaded(false);

        testResultsRef.child(resultId).setValue(testResult)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(ManageTestResultsActivity.this,
                                "Test result saved successfully", Toast.LENGTH_SHORT).show();

                        // Update patient's latest test results
                        updatePatientLatestTestResult(patientId, testResult);

                        clearForm();
                        loadRecentTestResults(); // Refresh the recent results list
                        loadStatistics(); // Refresh statistics
                    } else {
                        Toast.makeText(ManageTestResultsActivity.this,
                                "Failed to save test result: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("SAVE_RESULT", "Failed to save", task.getException());
                    }
                });
    }

    private void updatePatientLatestTestResult(String patientId, TestResult testResult) {
        DatabaseReference patientLatestRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/Patients/" + patientId + "/latestTestResult");

        patientLatestRef.setValue(testResult)
                .addOnSuccessListener(aVoid -> Log.d("PATIENT_UPDATE", "Patient test result updated"))
                .addOnFailureListener(e -> Log.e("PATIENT_UPDATE", "Failed to update patient", e));
    }

    private void loadRecentTestResults() {
        progressBar.setVisibility(View.VISIBLE);

        // Safe null check for tvNoResults
        if (tvNoResults != null) {
            tvNoResults.setVisibility(View.GONE);
        }

        // Query recent test results, ordered by creation date
        Query recentResultsQuery = testResultsRef.orderByChild("createdAt").limitToLast(10);

        recentResultsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentTestResults.clear();

                for (DataSnapshot resultSnapshot : snapshot.getChildren()) {
                    TestResult testResult = resultSnapshot.getValue(TestResult.class);
                    if (testResult != null) {
                        recentTestResults.add(0, testResult); // Add to beginning for reverse chronological order
                    }
                }

                progressBar.setVisibility(View.GONE);

                if (recentTestResults.isEmpty()) {
                    if (tvNoResults != null) {
                        tvNoResults.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvNoResults != null) {
                        tvNoResults.setVisibility(View.GONE);
                    }
                    testResultsAdapter.updateTestResults(recentTestResults);
                }

                Log.d("LOAD_RESULTS", "Loaded " + recentTestResults.size() + " recent test results");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                if (tvNoResults != null) {
                    tvNoResults.setVisibility(View.GONE);
                }
                Toast.makeText(ManageTestResultsActivity.this,
                        "Failed to load recent test results: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("LOAD_RESULTS", "Error: " + error.getMessage());
            }
        });
    }

    private void loadStatistics() {
        testResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalCount = (int) snapshot.getChildrenCount();
                int normalCount = 0;
                int abnormalCount = 0;

                for (DataSnapshot resultSnapshot : snapshot.getChildren()) {
                    TestResult testResult = resultSnapshot.getValue(TestResult.class);
                    if (testResult != null) {
                        if ("Normal".equalsIgnoreCase(testResult.getStatus())) {
                            normalCount++;
                        } else if ("Abnormal".equalsIgnoreCase(testResult.getStatus()) ||
                                "Critical".equalsIgnoreCase(testResult.getStatus())) {
                            abnormalCount++;
                        }
                    }
                }

                // Update UI safely
                if (tvResultsCount != null) {
                    tvResultsCount.setText(String.valueOf(totalCount));
                }
                if (tvNormalCount != null) {
                    tvNormalCount.setText(String.valueOf(normalCount));
                }
                if (tvAbnormalCount != null) {
                    tvAbnormalCount.setText(String.valueOf(abnormalCount));
                }

                Log.d("STATISTICS", "Total: " + totalCount + ", Normal: " + normalCount + ", Abnormal: " + abnormalCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("STATISTICS", "Error loading statistics: " + error.getMessage());
            }
        });
    }

    private void clearForm() {
        etPatientUsername.setText("");
        etTestName.setText("");
        etFindings.setText("");
        etRecommendations.setText("");

        if (tvFileName != null) {
            tvFileName.setVisibility(View.GONE);
        }
        fileUri = null;

        spinnerTestType.setSelection(0);
        spinnerStatus.setSelection(0);

        etPatientUsername.requestFocus();
    }

    private void viewAllTestResults() {
        Intent intent = new Intent(this, AllTestResultsActivity.class);
        intent.putExtra("healthWorkerId", healthWorkerId);
        intent.putExtra("healthWorkerName", healthWorkerName);
        startActivity(intent);
    }

    // Implement TestResultsAdapter click listeners
    @Override
    public void onViewReportClick(TestResult testResult) {
        if (testResult != null && testResult.hasFile()) {
            // Open the test result file
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(testResult.getFileUrl()));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "No file attached to this test result", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadClick(TestResult testResult) {
        if (testResult != null && testResult.hasFile()) {
            // Implement download functionality
            downloadTestResultFile(testResult);
        } else {
            Toast.makeText(this, "No file available for download", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShareClick(TestResult testResult) {
        if (testResult != null && testResult.hasFile()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Test Result: " + testResult.getTestName());
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Test Result: " + testResult.getTestName() + "\n" +
                            "Date: " + testResult.getTestDate() + "\n" +
                            "Status: " + testResult.getStatus() + "\n" +
                            "File: " + testResult.getFileUrl());
            startActivity(Intent.createChooser(shareIntent, "Share Test Result"));
        } else {
            Toast.makeText(this, "No file attached to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadTestResultFile(TestResult testResult) {
        // Implement file download logic
        Toast.makeText(this, "Downloading: " + testResult.getFileName(), Toast.LENGTH_SHORT).show();
        // You can use DownloadManager or other download methods here
    }

    public void onSearchPatientClicked(View view) {
        String username = etPatientUsername.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please enter patient username", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search for patient by username
        Query patientQuery = patientsRef.orderByChild("username").equalTo(username);
        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                        String patientName = patientSnapshot.child("name").getValue(String.class);
                        Toast.makeText(ManageTestResultsActivity.this,
                                "Patient found: " + patientName, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(ManageTestResultsActivity.this,
                            "Patient not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageTestResultsActivity.this,
                        "Error searching patient: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh recent results when returning to activity
        loadRecentTestResults();
        loadStatistics();
    }
}