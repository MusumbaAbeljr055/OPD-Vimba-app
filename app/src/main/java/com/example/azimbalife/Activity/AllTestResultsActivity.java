package com.example.azimbalife.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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

import java.util.ArrayList;
import java.util.List;

public class AllTestResultsActivity extends AppCompatActivity implements TestResultsAdapter.OnTestResultClickListener {

    private RecyclerView recyclerAllTestResults;
    private ProgressBar progressBar;
    private TextView tvNoResults, tvResultsCount;
    private SearchView searchView;

    private TestResultsAdapter testResultsAdapter;
    private List<TestResult> allTestResults;
    private List<TestResult> filteredTestResults;

    private String healthWorkerId;
    private String healthWorkerName;

    private DatabaseReference testResultsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_test_results);

        // Get health worker data from intent
        healthWorkerId = getIntent().getStringExtra("healthWorkerId");
        healthWorkerName = getIntent().getStringExtra("healthWorkerName");

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupSearchView();
        loadAllTestResults();
    }

    private void initializeViews() {
        recyclerAllTestResults = findViewById(R.id.recyclerAllTestResults);
        progressBar = findViewById(R.id.progressBar);
        tvNoResults = findViewById(R.id.tvNoResults);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        searchView = findViewById(R.id.searchView);

        allTestResults = new ArrayList<>();
        filteredTestResults = new ArrayList<>();
    }

    private void setupFirebase() {
        testResultsRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/TestResults");
    }

    private void setupRecyclerView() {
        testResultsAdapter = new TestResultsAdapter(filteredTestResults, this);
        recyclerAllTestResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerAllTestResults.setAdapter(testResultsAdapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTestResults(newText);
                return true;
            }
        });
    }

    private void loadAllTestResults() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        Query allResultsQuery = testResultsRef.orderByChild("createdAt");

        allResultsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTestResults.clear();
                filteredTestResults.clear();

                for (DataSnapshot resultSnapshot : snapshot.getChildren()) {
                    TestResult testResult = resultSnapshot.getValue(TestResult.class);
                    if (testResult != null) {
                        allTestResults.add(0, testResult); // Add to beginning for reverse chronological order
                    }
                }

                progressBar.setVisibility(View.GONE);

                if (allTestResults.isEmpty()) {
                    tvNoResults.setVisibility(View.VISIBLE);
                    tvNoResults.setText("No test results found");
                    tvResultsCount.setText("0 results");
                } else {
                    filteredTestResults.addAll(allTestResults);
                    testResultsAdapter.updateTestResults(filteredTestResults);
                    updateResultsCount();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                tvNoResults.setVisibility(View.VISIBLE);
                tvNoResults.setText("Failed to load test results. Please try again.");
                tvResultsCount.setText("0 results");
                Toast.makeText(AllTestResultsActivity.this,
                        "Error loading test results", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterTestResults(String query) {
        filteredTestResults.clear();

        if (TextUtils.isEmpty(query)) {
            filteredTestResults.addAll(allTestResults);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (TestResult result : allTestResults) {
                if (matchesSearchQuery(result, lowerCaseQuery)) {
                    filteredTestResults.add(result);
                }
            }
        }

        testResultsAdapter.updateTestResults(filteredTestResults);
        updateResultsCount();

        if (filteredTestResults.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            tvNoResults.setText("No results found for \"" + query + "\"");
        } else {
            tvNoResults.setVisibility(View.GONE);
        }
    }

    private boolean matchesSearchQuery(TestResult result, String query) {
        return (result.getPatientUsername() != null && result.getPatientUsername().toLowerCase().contains(query)) ||
                (result.getTestName() != null && result.getTestName().toLowerCase().contains(query)) ||
                (result.getTestType() != null && result.getTestType().toLowerCase().contains(query)) ||
                (result.getLabName() != null && result.getLabName().toLowerCase().contains(query)) ||
                (result.getDoctorName() != null && result.getDoctorName().toLowerCase().contains(query)) ||
                (result.getStatus() != null && result.getStatus().toLowerCase().contains(query)) ||
                (result.getFindings() != null && result.getFindings().toLowerCase().contains(query)) ||
                (result.getTestDate() != null && result.getTestDate().toLowerCase().contains(query));
    }

    private void updateResultsCount() {
        String countText = filteredTestResults.size() + " result" + (filteredTestResults.size() != 1 ? "s" : "");
        tvResultsCount.setText(countText);
    }

    // Implement TestResultsAdapter click listeners
    @Override
    public void onViewReportClick(TestResult testResult) {
        if (testResult.hasFile()) {
            // Open the test result file in a browser or PDF viewer
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(testResult.getFileUrl()));
            startActivity(browserIntent);
        } else {
            Toast.makeText(this, "No file attached to this test result", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDownloadClick(TestResult testResult) {
        if (testResult.hasFile()) {
            // Implement download functionality
            downloadTestResultFile(testResult);
        } else {
            Toast.makeText(this, "No file available for download", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShareClick(TestResult testResult) {
        if (testResult.hasFile()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Test Result: " + testResult.getTestName());
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Test Result: " + testResult.getTestName() + "\n" +
                            "Patient: " + testResult.getPatientUsername() + "\n" +
                            "Date: " + testResult.getTestDate() + "\n" +
                            "Status: " + testResult.getStatus() + "\n" +
                            "File: " + testResult.getFileUrl());
            startActivity(Intent.createChooser(shareIntent, "Share Test Result"));
        } else {
            // Share basic test result information without file
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Test Result: " + testResult.getTestName());
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Test Result: " + testResult.getTestName() + "\n" +
                            "Patient: " + testResult.getPatientUsername() + "\n" +
                            "Date: " + testResult.getTestDate() + "\n" +
                            "Status: " + testResult.getStatus() + "\n" +
                            "Findings: " + (testResult.getFindings() != null ? testResult.getFindings() : "N/A"));
            startActivity(Intent.createChooser(shareIntent, "Share Test Result"));
        }
    }

    private void downloadTestResultFile(TestResult testResult) {
        // Implement file download logic using DownloadManager
        Toast.makeText(this, "Downloading: " + testResult.getFileName(), Toast.LENGTH_SHORT).show();

        // Example download implementation:
        /*
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(testResult.getFileUrl()));
        request.setTitle(testResult.getFileName());
        request.setDescription("Downloading test result file");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, testResult.getFileName());

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
        */
    }

    public void onBackClicked(View view) {
        onBackPressed();
    }

    public void onFilterClicked(View view) {
        showFilterDialog();
    }

    public void onRefreshClicked(View view) {
        loadAllTestResults();
    }

    private void showFilterDialog() {
        // Implement filter dialog for status, test type, date range, etc.
        Toast.makeText(this, "Filter feature - to be implemented", Toast.LENGTH_SHORT).show();

        // You can implement a dialog with options to filter by:
        // - Status (Normal, Abnormal, Critical)
        // - Test Type (Blood Test, X-Ray, etc.)
        // - Date range
        // - Patient username
    }

    public void filterByStatus(String status) {
        filteredTestResults.clear();

        if (status.equals("All")) {
            filteredTestResults.addAll(allTestResults);
        } else {
            for (TestResult result : allTestResults) {
                if (status.equals(result.getStatus())) {
                    filteredTestResults.add(result);
                }
            }
        }

        testResultsAdapter.updateTestResults(filteredTestResults);
        updateResultsCount();

        if (filteredTestResults.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            tvNoResults.setText("No " + status.toLowerCase() + " test results found");
        } else {
            tvNoResults.setVisibility(View.GONE);
        }
    }

    public void filterByTestType(String testType) {
        filteredTestResults.clear();

        if (testType.equals("All")) {
            filteredTestResults.addAll(allTestResults);
        } else {
            for (TestResult result : allTestResults) {
                if (testType.equals(result.getTestType())) {
                    filteredTestResults.add(result);
                }
            }
        }

        testResultsAdapter.updateTestResults(filteredTestResults);
        updateResultsCount();

        if (filteredTestResults.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            tvNoResults.setText("No " + testType.toLowerCase() + " results found");
        } else {
            tvNoResults.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadAllTestResults();
    }
}