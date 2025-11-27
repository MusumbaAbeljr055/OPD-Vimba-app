package com.example.azimbalife.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.TestResult;
import com.example.azimbalife.R;

import java.util.List;

public class TestResultsAdapter extends RecyclerView.Adapter<TestResultsAdapter.ViewHolder> {

    private List<TestResult> testResultsList;
    private OnTestResultClickListener listener;

    public interface OnTestResultClickListener {
        void onViewReportClick(TestResult testResult);
        void onDownloadClick(TestResult testResult);
        void onShareClick(TestResult testResult);
    }

    public TestResultsAdapter(List<TestResult> testResultsList, OnTestResultClickListener listener) {
        this.testResultsList = testResultsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TestResult testResult = testResultsList.get(position);

        holder.tvTestType.setText(testResult.getTestName());
        holder.tvTestDate.setText(testResult.getTestDate());
        holder.tvLabName.setText(testResult.getLabName());
        holder.tvDoctor.setText(testResult.getDoctorName() != null ? testResult.getDoctorName() : "Not specified");
        holder.tvStatus.setText(testResult.getStatus());

        // Set status color
        switch (testResult.getStatus().toLowerCase()) {
            case "normal":
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                break;
            case "abnormal":
                holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // Orange
                break;
            case "critical":
                holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
                break;
            default:
                holder.tvStatus.setTextColor(Color.parseColor("#9E9E9E")); // Grey
        }

        // Set click listeners
        holder.btnViewReport.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewReportClick(testResult);
            }
        });

        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(testResult);
            }
        });
    }

    @Override
    public int getItemCount() {
        return testResultsList.size();
    }

    public void updateTestResults(List<TestResult> testResults) {
        this.testResultsList = testResults;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTestType, tvTestDate, tvLabName, tvDoctor, tvStatus;
        Button btnViewReport, btnDownload;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTestType = itemView.findViewById(R.id.tvTestType);
            tvTestDate = itemView.findViewById(R.id.tvTestDate);
            tvLabName = itemView.findViewById(R.id.tvLabName);
            tvDoctor = itemView.findViewById(R.id.tvDoctor);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnViewReport = itemView.findViewById(R.id.btnViewReport);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }
}