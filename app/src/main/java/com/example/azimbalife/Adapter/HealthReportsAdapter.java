package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.HealthReport;
import com.example.azimbalife.R;

import java.util.List;

public class HealthReportsAdapter extends RecyclerView.Adapter<HealthReportsAdapter.ViewHolder> {

    private List<HealthReport> healthReportsList;
    private OnHealthReportClickListener listener;

    public interface OnHealthReportClickListener {
        void onViewReportClick(HealthReport healthReport);
        void onDownloadReportClick(HealthReport healthReport);
        void onShareReportClick(HealthReport healthReport);
    }

    public HealthReportsAdapter(List<HealthReport> healthReportsList, OnHealthReportClickListener listener) {
        this.healthReportsList = healthReportsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HealthReport healthReport = healthReportsList.get(position);

        holder.tvReportName.setText(healthReport.getReportName());
        holder.tvReportDate.setText(healthReport.getGeneratedDate());
        holder.tvReportType.setText(healthReport.getReportType());

        // Set click listeners
        holder.btnViewReport.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewReportClick(healthReport);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewReportClick(healthReport);
            }
        });
    }

    @Override
    public int getItemCount() {
        return healthReportsList.size();
    }

    public void updateHealthReports(List<HealthReport> healthReports) {
        this.healthReportsList = healthReports;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportName, tvReportDate, tvReportType;
        Button btnViewReport;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvReportName = itemView.findViewById(R.id.tvReportName);
            tvReportDate = itemView.findViewById(R.id.tvReportDate);
            tvReportType = itemView.findViewById(R.id.tvReportType);
            btnViewReport = itemView.findViewById(R.id.btnViewReport);
        }
    }
}