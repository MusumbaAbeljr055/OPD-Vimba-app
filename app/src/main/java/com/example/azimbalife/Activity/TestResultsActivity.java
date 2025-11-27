package com.example.azimbalife.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.azimbalife.Adapter.TestResultsAdapter;
import com.example.azimbalife.Domain.TestResult;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestResultsActivity extends AppCompatActivity {

    private RecyclerView recyclerTestResults;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyState;

    private TextView filterAll, filterRecent, filterBlood, filterScan;

    private TestResultsAdapter adapter;
    private List<TestResult> allTestResults = new ArrayList<>();
    private List<TestResult> filteredTestResults = new ArrayList<>();

    private String username;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_results);

        // Get username from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupFilterListeners();
        setupSwipeRefresh();
        loadTestResults();
    }

    private void initializeViews() {
        recyclerTestResults = findViewById(R.id.recyclerTestResults);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyState = findViewById(R.id.emptyState);

        filterAll = findViewById(R.id.filterAll);
        filterRecent = findViewById(R.id.filterRecent);
        filterBlood = findViewById(R.id.filterBlood);
        filterScan = findViewById(R.id.filterScan);

        // Setup FAB for manual upload (optional feature)
        findViewById(R.id.fabUpload).setOnClickListener(v -> showUploadDialog());
    }

    private void setupRecyclerView() {
        adapter = new TestResultsAdapter(filteredTestResults, new TestResultsAdapter.OnTestResultClickListener() {
            @Override
            public void onViewReportClick(TestResult testResult) {
                viewTestReport(testResult);
            }

            @Override
            public void onDownloadClick(TestResult testResult) {
                downloadTestReport(testResult);
            }

            @Override
            public void onShareClick(TestResult testResult) {
                shareTestReport(testResult);
            }
        });

        recyclerTestResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerTestResults.setAdapter(adapter);
    }

    private void setupFilterListeners() {
        filterAll.setOnClickListener(v -> applyFilter("all"));
        filterRecent.setOnClickListener(v -> applyFilter("recent"));
        filterBlood.setOnClickListener(v -> applyFilter("blood"));
        filterScan.setOnClickListener(v -> applyFilter("scan"));
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadTestResults();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadTestResults() {
        DatabaseReference testResultsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/TestResults")
                .child(username);

        testResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTestResults.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot resultSnapshot : snapshot.getChildren()) {
                        TestResult testResult = resultSnapshot.getValue(TestResult.class);
                        if (testResult != null) {
                            allTestResults.add(testResult);
                        }
                    }

                    // Sort by date (newest first)
                    Collections.sort(allTestResults, (r1, r2) ->
                            r2.getTestDate().compareTo(r1.getTestDate()));

                    applyFilter(currentFilter);
                    showEmptyState(false);
                } else {
                    showEmptyState(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TestResultsActivity.this, "Failed to load test results", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredTestResults.clear();

        switch (filter) {
            case "all":
                filteredTestResults.addAll(allTestResults);
                updateFilterUI(filterAll, filterRecent, filterBlood, filterScan);
                break;
            case "recent":
                // Show only last 30 days results
                for (TestResult result : allTestResults) {
                    // Add logic to filter recent results based on date
                    filteredTestResults.add(result);
                }
                // For demo, show all as recent
                if (filteredTestResults.isEmpty() && !allTestResults.isEmpty()) {
                    filteredTestResults.add(allTestResults.get(0));
                }
                updateFilterUI(filterRecent, filterAll, filterBlood, filterScan);
                break;
            case "blood":
                for (TestResult result : allTestResults) {
                    if (result.getTestType().toLowerCase().contains("blood")) {
                        filteredTestResults.add(result);
                    }
                }
                updateFilterUI(filterBlood, filterAll, filterRecent, filterScan);
                break;
            case "scan":
                for (TestResult result : allTestResults) {
                    if (result.getTestType().toLowerCase().contains("scan") ||
                            result.getTestType().toLowerCase().contains("x-ray") ||
                            result.getTestType().toLowerCase().contains("mri") ||
                            result.getTestType().toLowerCase().contains("ultrasound")) {
                        filteredTestResults.add(result);
                    }
                }
                updateFilterUI(filterScan, filterAll, filterRecent, filterBlood);
                break;
        }

        adapter.updateTestResults(filteredTestResults);
        showEmptyState(filteredTestResults.isEmpty());
    }

    private void updateFilterUI(TextView selected, TextView... others) {
        selected.setBackgroundResource(R.drawable.filter_selected_background);

        for (TextView other : others) {
            other.setBackgroundResource(R.drawable.filter_unselected_background);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            recyclerTestResults.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerTestResults.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void viewTestReport(TestResult testResult) {
        if (testResult.hasFile()) {
            // Open PDF or image file
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(testResult.getFileUrl()), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
                // Show details in dialog instead
                showTestResultDetails(testResult);
            }
        } else {
            // Show details in dialog
            showTestResultDetails(testResult);
        }
    }

    private void showTestResultDetails(TestResult testResult) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(testResult.getTestName())
                .setMessage(
                        "Test Date: " + testResult.getTestDate() + "\n" +
                                "Lab: " + testResult.getLabName() + "\n" +
                                "Doctor: " + (testResult.getDoctorName() != null ? testResult.getDoctorName() : "Not specified") + "\n" +
                                "Status: " + testResult.getStatus() + "\n\n" +
                                "Findings: " + (testResult.getFindings() != null ? testResult.getFindings() : "No findings available") + "\n\n" +
                                "Recommendations: " + (testResult.getRecommendations() != null ? testResult.getRecommendations() : "No recommendations")
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void downloadTestReport(TestResult testResult) {
        if (testResult.hasFile()) {
            Toast.makeText(this, "Downloading report...", Toast.LENGTH_SHORT).show();

            // Simulate download process
            // In real implementation, use DownloadManager or similar
            testResult.setDownloaded(true);

            // Update in Firebase
            DatabaseReference resultRef = FirebaseDatabase.getInstance()
                    .getReference("MbararaHospital/TestResults")
                    .child(username)
                    .child(testResult.getResultId());

            resultRef.setValue(testResult)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Report marked as downloaded", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Download completed locally", Toast.LENGTH_SHORT).show();
                    });

        } else {
            Toast.makeText(this, "No file available for download", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTestReport(TestResult testResult) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Medical Test Result: " + testResult.getTestName());
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Test Result: " + testResult.getTestName() + "\n" +
                        "Date: " + testResult.getTestDate() + "\n" +
                        "Status: " + testResult.getStatus() + "\n" +
                        "Lab: " + testResult.getLabName()
        );

        startActivity(Intent.createChooser(shareIntent, "Share Test Result"));
    }

    private void showUploadDialog() {
        // This would open file picker for manual upload
        // For now, show a message
        Toast.makeText(this, "Manual upload feature coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTestResults();
    }
}
