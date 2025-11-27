package com.example.azimbalife.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.MedicalRecordsAdapter;
import com.example.azimbalife.Domain.MedicalRecord;
import com.example.azimbalife.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

public class ManageMedicalRecordsActivity extends AppCompatActivity implements MedicalRecordsAdapter.OnMedicalRecordClickListener {

    // UI Components
    private TextInputEditText etPatientUsername, etTitle, etDescription, etDiagnosis, etMedications,
            etDosage, etInstructions, etTreatment, etFollowUpDate, etLabResults,
            etVitalSigns, etAllergies, etNotes;
    private TextInputLayout tilPatientUsername, tilTitle;
    private MaterialButton btnSaveRecord, btnUploadFile, btnViewAllRecords, btnSearchPatient;
    private LinearProgressIndicator progressBar;
    private TextView tvFileName, tvRecordsCount;
    private RecyclerView recyclerRecentRecords;
    private LinearLayout emptyState;

    // Add Spinner declarations
    private Spinner spinnerRecordType, spinnerStatus, spinnerUrgency;

    // Adapter and Data
    private MedicalRecordsAdapter medicalRecordsAdapter;
    private List<MedicalRecord> recentMedicalRecords;

    // User Information
    private String healthWorkerId;
    private String healthWorkerName;

    // Firebase References
    private DatabaseReference patientsRef;
    private DatabaseReference medicalRecordsRef;
    private StorageReference storageRef;

    // File Handling
    private Uri fileUri;
    private static final int FILE_PICKER_REQUEST_CODE = 1001;

    private static final String TAG = "ManageMedicalRecords";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove the setTheme line to avoid conflicts
        setContentView(R.layout.activity_manage_medical_records);

        // Debug: Check all intent extras
        Log.d(TAG, "=== INTENT EXTRAS DEBUG ===");
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d(TAG, "Key: " + key + " = " + extras.get(key));
            }
        } else {
            Log.d(TAG, "No extras found in intent");
        }

        // Get health worker data from intent with multiple possible key names
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");

        // Try alternative key names
        if (healthWorkerId == null) {
            healthWorkerId = getIntent().getStringExtra("userId");
            healthWorkerId = getIntent().getStringExtra("id");
            healthWorkerId = getIntent().getStringExtra("workerId");
        }

        if (healthWorkerName == null) {
            healthWorkerName = getIntent().getStringExtra("userName");
            healthWorkerName = getIntent().getStringExtra("name");
            healthWorkerName = getIntent().getStringExtra("workerName");
            healthWorkerName = getIntent().getStringExtra("username");
        }

        // If still null, try fallback authentication
        if (healthWorkerId == null || healthWorkerName == null) {
            Log.d(TAG, "Health worker info not found in intent, trying fallback...");
            tryFallbackAuthentication();
            return;
        }

        Log.d(TAG, "Health Worker: " + healthWorkerName + " ID: " + healthWorkerId);

        try {
            initializeViews();
            setupFirebase();
            setupSpinners();
            setupClickListeners();
            setupRecyclerView();
            loadRecentMedicalRecords();
        } catch (Exception e) {
            Log.e(TAG, "Error during initialization: " + e.getMessage(), e);
            showToast("Error initializing activity");
            finish();
        }
    }

    private void tryFallbackAuthentication() {
        Log.d(TAG, "Attempting fallback authentication...");

        // Try to get health worker info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("HealthWorkerPrefs", MODE_PRIVATE);
        healthWorkerId = prefs.getString("healthWorkerId", null);
        healthWorkerName = prefs.getString("healthWorkerName", null);

        // Try alternative preference keys
        if (healthWorkerId == null) {
            healthWorkerId = prefs.getString("userId", null);
            healthWorkerId = prefs.getString("id", null);
        }

        if (healthWorkerName == null) {
            healthWorkerName = prefs.getString("userName", null);
            healthWorkerName = prefs.getString("name", null);
            healthWorkerName = prefs.getString("username", null);
        }

        if (healthWorkerId != null && healthWorkerName != null) {
            Log.d(TAG, "Recovered from SharedPreferences: " + healthWorkerName);
            initializeViews();
            setupFirebase();
            setupSpinners();
            setupClickListeners();
            setupRecyclerView();
            loadRecentMedicalRecords();
        } else {
            // TEMPORARY FIX: Use test data for development
            Log.d(TAG, "No health worker data found, using test data for development");
            healthWorkerId = "test_health_worker_001";
            healthWorkerName = "Dr. Test User";

            showToast("Using test mode. In production, please login first.");

            // Proceed with test data
            initializeViews();
            setupFirebase();
            setupSpinners();
            setupClickListeners();
            setupRecyclerView();
            loadRecentMedicalRecords();
        }
    }

    private void initializeViews() {
        // Initialize TextInputLayouts
        tilPatientUsername = findViewById(R.id.tilPatientUsername);
        tilTitle = findViewById(R.id.tilTitle);

        // Initialize EditTexts
        etPatientUsername = findViewById(R.id.etPatientUsername);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDiagnosis = findViewById(R.id.etDiagnosis);
        etMedications = findViewById(R.id.etMedications);
        etDosage = findViewById(R.id.etDosage);
        etInstructions = findViewById(R.id.etInstructions);
        etTreatment = findViewById(R.id.etTreatment);
        etFollowUpDate = findViewById(R.id.etFollowUpDate);
        etLabResults = findViewById(R.id.etLabResults);
        etVitalSigns = findViewById(R.id.etVitalSigns);
        etAllergies = findViewById(R.id.etAllergies);
        etNotes = findViewById(R.id.etNotes);

        // Initialize Spinners
        spinnerRecordType = findViewById(R.id.spinnerRecordType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerUrgency = findViewById(R.id.spinnerUrgency);

        // Initialize Buttons
        btnSaveRecord = findViewById(R.id.btnSaveRecord);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnViewAllRecords = findViewById(R.id.btnViewAllRecords);
        btnSearchPatient = findViewById(R.id.btnSearchPatient);

        // Initialize Other Views
        progressBar = findViewById(R.id.progressBar);
        tvFileName = findViewById(R.id.tvFileName);
        tvRecordsCount = findViewById(R.id.tvRecordsCount);
        recyclerRecentRecords = findViewById(R.id.recyclerRecentRecords);
        emptyState = findViewById(R.id.emptyState);

        // Initialize Data Structures
        recentMedicalRecords = new ArrayList<>();

        // Set health worker name in UI if needed
        Log.d(TAG, "Activity initialized for: " + healthWorkerName);
    }

    private void setupFirebase() {
        try {
            patientsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/Patients");
            medicalRecordsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/MedicalRecords");
            storageRef = FirebaseStorage.getInstance().getReference("MbararaHospital/MedicalRecords");
            Log.d(TAG, "Firebase references initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Firebase: " + e.getMessage(), e);
            showToast("Error connecting to database");
        }
    }

    private void setupSpinners() {
        try {
            // Setup Record Type Spinner
            ArrayAdapter<CharSequence> recordTypeAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.record_types_array,
                    android.R.layout.simple_spinner_item
            );
            recordTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRecordType.setAdapter(recordTypeAdapter);

            // Setup Status Spinner
            ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.status_array,
                    android.R.layout.simple_spinner_item
            );
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStatus.setAdapter(statusAdapter);

            // Setup Urgency Spinner
            ArrayAdapter<CharSequence> urgencyAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.urgency_array,
                    android.R.layout.simple_spinner_item
            );
            urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUrgency.setAdapter(urgencyAdapter);

            Log.d(TAG, "Spinners initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners: " + e.getMessage(), e);
            showToast("Error initializing form fields");
        }
    }

    private void setupClickListeners() {
        btnSaveRecord.setOnClickListener(v -> saveMedicalRecord());
        btnUploadFile.setOnClickListener(v -> openFilePicker());
        btnSearchPatient.setOnClickListener(v -> searchPatient());
        btnViewAllRecords.setOnClickListener(v -> viewAllMedicalRecords());

        // Add text change listeners for real-time validation
        etPatientUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !TextUtils.isEmpty(etPatientUsername.getText())) {
                validatePatientUsername();
            }
        });

        etTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateRequiredField(etTitle, tilTitle, "Record title is required");
            }
        });
    }

    private void viewAllMedicalRecords() {
        showToast("View All Records feature coming soon");
    }

    private void setupRecyclerView() {
        medicalRecordsAdapter = new MedicalRecordsAdapter(recentMedicalRecords, this);
        recyclerRecentRecords.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentRecords.setAdapter(medicalRecordsAdapter);
        recyclerRecentRecords.setHasFixedSize(true);
    }

    private void searchPatient() {
        String username = etPatientUsername.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            showError(tilPatientUsername, "Please enter patient username");
            return;
        }

        showLoading("Searching for patient...");

        Query patientQuery = patientsRef.orderByChild("username").equalTo(username);
        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoading();
                if (snapshot.exists()) {
                    for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                        String patientName = patientSnapshot.child("name").getValue(String.class);
                        showSuccess("Patient verified: " + (patientName != null ? patientName : username));
                        clearError(tilPatientUsername);
                        return;
                    }
                } else {
                    showError(tilPatientUsername, "Patient not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showError(tilPatientUsername, "Error searching patient: " + error.getMessage());
                Log.e(TAG, "Patient search error: " + error.getMessage());
            }
        });
    }

    private void validatePatientUsername() {
        String username = etPatientUsername.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            showError(tilPatientUsername, "Patient username is required");
            return;
        }

        if (username.length() < 3) {
            showError(tilPatientUsername, "Username too short");
        } else {
            clearError(tilPatientUsername);
        }
    }

    private void validateRequiredField(TextInputEditText editText, TextInputLayout layout, String errorMessage) {
        if (TextUtils.isEmpty(editText.getText())) {
            showError(layout, errorMessage);
        } else {
            clearError(layout);
        }
    }

    private void showError(TextInputLayout layout, String message) {
        layout.setError(message);
    }

    private void clearError(TextInputLayout layout) {
        layout.setError(null);
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            Intent chooser = Intent.createChooser(intent, "Select Medical Document");
            startActivityForResult(chooser, FILE_PICKER_REQUEST_CODE);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file picker: " + e.getMessage(), e);
            showToast("Error opening file picker");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            fileUri = data.getData();
            if (fileUri != null) {
                String fileName = getFileName(fileUri);
                tvFileName.setText("Selected: " + fileName);
                tvFileName.setVisibility(View.VISIBLE);
                showSuccess("File selected successfully");
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (var cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex("_display_name");
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name: " + e.getMessage(), e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void saveMedicalRecord() {
        if (!validateForm()) {
            return;
        }

        showLoading("Saving medical record...");

        String patientUsername = etPatientUsername.getText().toString().trim();

        Query patientQuery = patientsRef.orderByChild("username").equalTo(patientUsername);
        patientQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    hideLoading();
                    showError(tilPatientUsername, "Patient username not found");
                    return;
                }

                if (fileUri != null) {
                    uploadFileAndSaveRecord();
                } else {
                    saveMedicalRecordToDatabase(null, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showToast("Error verifying patient: " + error.getMessage());
                Log.e(TAG, "Patient verification error: " + error.getMessage());
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(etPatientUsername.getText())) {
            showError(tilPatientUsername, "Patient username is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(etTitle.getText())) {
            showError(tilTitle, "Record title is required");
            isValid = false;
        }

        if (isValid) {
            clearError(tilPatientUsername);
            clearError(tilTitle);
        }

        return isValid;
    }

    private void uploadFileAndSaveRecord() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Document");
        progressDialog.setMessage("Please wait while we upload your file...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        String patientUsername = etPatientUsername.getText().toString().trim();
        String fileName = "medical_record_" + System.currentTimeMillis() + "_" + patientUsername;
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        progressDialog.dismiss();
                        saveMedicalRecordToDatabase(downloadUri.toString(), getFileName(fileUri));
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        hideLoading();
                        showToast("Error getting download URL: " + e.getMessage());
                        Log.e(TAG, "Download URL error: " + e.getMessage(), e);
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    hideLoading();
                    showToast("File upload failed: " + e.getMessage());
                    Log.e(TAG, "File upload error: " + e.getMessage(), e);
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setProgress((int) progress);
                });
    }

    private void saveMedicalRecordToDatabase(String fileUrl, String fileName) {
        try {
            // Gather all form data
            String patientUsername = etPatientUsername.getText().toString().trim();
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String diagnosis = etDiagnosis.getText().toString().trim();
            String medications = etMedications.getText().toString().trim();
            String dosage = etDosage.getText().toString().trim();
            String instructions = etInstructions.getText().toString().trim();
            String treatment = etTreatment.getText().toString().trim();
            String followUpDate = etFollowUpDate.getText().toString().trim();
            String labResults = etLabResults.getText().toString().trim();
            String vitalSigns = etVitalSigns.getText().toString().trim();
            String allergies = etAllergies.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            // Get spinner values
            String recordType = spinnerRecordType.getSelectedItem().toString();
            String status = spinnerStatus.getSelectedItem().toString();
            String urgency = spinnerUrgency.getSelectedItem().toString();

            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            String recordId = medicalRecordsRef.push().getKey();

            if (recordId == null) {
                hideLoading();
                showToast("Error generating record ID");
                return;
            }

            // Create MedicalRecord object
            MedicalRecord medicalRecord = createMedicalRecord(
                    recordId, patientUsername, title, description, diagnosis, medications,
                    dosage, instructions, treatment, followUpDate, labResults, vitalSigns,
                    allergies, notes, fileUrl, fileName, currentDate, recordType, status, urgency
            );

            medicalRecordsRef.child(recordId).setValue(medicalRecord)
                    .addOnCompleteListener(task -> {
                        hideLoading();

                        if (task.isSuccessful()) {
                            showSuccess("Medical record saved successfully!");
                            clearForm();
                            loadRecentMedicalRecords();
                        } else {
                            showToast("Failed to save medical record: " + task.getException().getMessage());
                            Log.e(TAG, "Save record error: " + task.getException().getMessage(), task.getException());
                        }
                    });
        } catch (Exception e) {
            hideLoading();
            showToast("Error saving record: " + e.getMessage());
            Log.e(TAG, "Save record exception: " + e.getMessage(), e);
        }
    }

    private MedicalRecord createMedicalRecord(String recordId, String patientUsername, String title,
                                              String description, String diagnosis, String medications,
                                              String dosage, String instructions, String treatment,
                                              String followUpDate, String labResults, String vitalSigns,
                                              String allergies, String notes, String fileUrl,
                                              String fileName, String currentDate, String recordType,
                                              String status, String urgency) {

        MedicalRecord record = new MedicalRecord();
        record.setRecordId(recordId);
        record.setPatientUsername(patientUsername);
        record.setTitle(title);
        record.setDescription(description);

        // FIXED: Use setDiagnosisText() instead of setDiagnosis()
        record.setDiagnosisText(diagnosis);

        record.setMedications(medications);
        record.setDosage(dosage);
        record.setInstructions(instructions);
        record.setTreatment(treatment);
        record.setFollowUpDate(followUpDate);
        record.setLabResults(labResults);
        record.setVitalSigns(vitalSigns);
        record.setAllergies(allergies);
        record.setNotes(notes);
        record.setFileUrl(fileUrl);
        record.setFileName(fileName);
        record.setDate(currentDate);

        // FIXED: Use setRecordTypeWithUpdate() to properly set boolean fields
        record.setRecordTypeWithUpdate(recordType);

        record.setStatus(status);
        record.setUrgency(urgency);
        record.setDoctorName(healthWorkerName);
        record.setHospitalName("Mbarara Hospital");
        record.setCreatedAt(String.valueOf(System.currentTimeMillis()));
        record.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

        return record;
    }

    private void loadRecentMedicalRecords() {
        progressBar.setVisibility(View.VISIBLE);

        Query recentRecordsQuery = medicalRecordsRef.orderByChild("createdAt").limitToLast(5);

        recentRecordsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentMedicalRecords.clear();

                for (DataSnapshot recordSnapshot : snapshot.getChildren()) {
                    MedicalRecord medicalRecord = recordSnapshot.getValue(MedicalRecord.class);
                    if (medicalRecord != null) {
                        recentMedicalRecords.add(0, medicalRecord);
                    }
                }

                progressBar.setVisibility(View.GONE);
                updateRecordsUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                showToast("Failed to load recent records: " + error.getMessage());
                Log.e(TAG, "Load records error: " + error.getMessage());
                updateRecordsUI();
            }
        });
    }

    private void updateRecordsUI() {
        if (recentMedicalRecords.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerRecentRecords.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerRecentRecords.setVisibility(View.VISIBLE);
            medicalRecordsAdapter.updateMedicalRecords(recentMedicalRecords);
        }

        tvRecordsCount.setText(recentMedicalRecords.size() + " records");
    }

    private void clearForm() {
        // Clear all EditText fields
        etPatientUsername.setText("");
        etTitle.setText("");
        etDescription.setText("");
        etDiagnosis.setText("");
        etMedications.setText("");
        etDosage.setText("");
        etInstructions.setText("");
        etTreatment.setText("");
        etFollowUpDate.setText("");
        etLabResults.setText("");
        etVitalSigns.setText("");
        etAllergies.setText("");
        etNotes.setText("");

        // Reset spinners to first position
        if (spinnerRecordType.getAdapter() != null && spinnerRecordType.getAdapter().getCount() > 0) {
            spinnerRecordType.setSelection(0);
        }
        if (spinnerStatus.getAdapter() != null && spinnerStatus.getAdapter().getCount() > 0) {
            spinnerStatus.setSelection(0);
        }
        if (spinnerUrgency.getAdapter() != null && spinnerUrgency.getAdapter().getCount() > 0) {
            spinnerUrgency.setSelection(0);
        }

        // Clear file selection
        tvFileName.setVisibility(View.GONE);
        fileUri = null;

        // Clear errors
        clearError(tilPatientUsername);
        clearError(tilTitle);

        // Reset focus
        etPatientUsername.requestFocus();
    }

    // Utility Methods
    private void showLoading(String message) {
        progressBar.setVisibility(View.VISIBLE);
        btnSaveRecord.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnSaveRecord.setEnabled(true);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, "✓ " + message, Toast.LENGTH_SHORT).show();
    }

    // Implement MedicalRecordsAdapter click listeners
    @Override
    public void onViewDetailsClick(MedicalRecord medicalRecord) {
        Intent intent = new Intent(this, MedicalRecordDetailActivity.class);
        intent.putExtra("medicalRecord", medicalRecord);
        startActivity(intent);
    }

    @Override
    public void onDownloadClick(MedicalRecord medicalRecord) {
        if (medicalRecord.getFileUrl() != null && !medicalRecord.getFileUrl().isEmpty()) {
            downloadMedicalRecordFile(medicalRecord);
        } else {
            showToast("No file available for download");
        }
    }

    @Override
    public void onShareClick(MedicalRecord medicalRecord) {
        shareMedicalRecord(medicalRecord);
    }

    @Override
    public void onPrintClick(MedicalRecord medicalRecord) {
        showToast("Print feature coming soon for: " + medicalRecord.getTitle());
    }

    @Override
    public void onEmailClick(MedicalRecord medicalRecord) {
        emailMedicalRecord(medicalRecord);
    }

    private void downloadMedicalRecordFile(MedicalRecord medicalRecord) {
        showLoading("Downloading file...");
        // Implement download logic
        hideLoading();
        showSuccess("Download started: " + medicalRecord.getFileName());
    }

    private void shareMedicalRecord(MedicalRecord medicalRecord) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Medical Record: " + medicalRecord.getTitle());

        // FIXED: Use getDiagnosisText() instead of getDiagnosis()
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Medical Record Summary:\n\n" +
                        "Patient: " + medicalRecord.getPatientUsername() + "\n" +
                        "Title: " + medicalRecord.getTitle() + "\n" +
                        "Date: " + medicalRecord.getDate() + "\n" +
                        "Doctor: " + medicalRecord.getDoctorName() + "\n" +
                        "Diagnosis: " + (medicalRecord.getDiagnosisText() != null ? medicalRecord.getDiagnosisText() : "N/A") + "\n" +
                        "Treatment: " + (medicalRecord.getTreatment() != null ? medicalRecord.getTreatment() : "N/A"));

        startActivity(Intent.createChooser(shareIntent, "Share Medical Record"));
    }

    private void emailMedicalRecord(MedicalRecord medicalRecord) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Medical Record: " + medicalRecord.getTitle());

        // FIXED: Use getDiagnosisText() instead of getDiagnosis()
        emailIntent.putExtra(Intent.EXTRA_TEXT,
                "Medical Record Details:\n\n" +
                        "Patient: " + medicalRecord.getPatientUsername() + "\n" +
                        "Record Type: " + medicalRecord.getRecordType() + "\n" +
                        "Date: " + medicalRecord.getDate() + "\n" +
                        "Doctor: " + medicalRecord.getDoctorName() + "\n" +
                        "Hospital: " + medicalRecord.getHospitalName() + "\n\n" +
                        "Description: " + (medicalRecord.getDescription() != null ? medicalRecord.getDescription() : "N/A") + "\n\n" +
                        "Diagnosis: " + (medicalRecord.getDiagnosisText() != null ? medicalRecord.getDiagnosisText() : "N/A") + "\n\n" +
                        "Treatment: " + (medicalRecord.getTreatment() != null ? medicalRecord.getTreatment() : "N/A") + "\n\n" +
                        "Medications: " + (medicalRecord.getMedications() != null ? medicalRecord.getMedications() : "N/A"));

        startActivity(Intent.createChooser(emailIntent, "Send via Email"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentMedicalRecords();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (recentMedicalRecords != null) {
            recentMedicalRecords.clear();
        }
    }
}