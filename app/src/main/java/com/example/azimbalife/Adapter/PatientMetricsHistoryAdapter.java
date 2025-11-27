package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.PatientMetrics;
import com.example.azimbalife.R;

import java.util.List;

public class PatientMetricsHistoryAdapter extends RecyclerView.Adapter<PatientMetricsHistoryAdapter.MetricsViewHolder> {

    private List<PatientMetrics> metricsList;

    public PatientMetricsHistoryAdapter(List<PatientMetrics> metricsList) {
        this.metricsList = metricsList;
    }

    @NonNull
    @Override
    public MetricsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_metrics_history, parent, false);
        return new MetricsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricsViewHolder holder, int position) {
        PatientMetrics metrics = metricsList.get(position);
        holder.bind(metrics);
    }

    @Override
    public int getItemCount() {
        return metricsList.size();
    }

    public void updateMetricsList(List<PatientMetrics> newMetricsList) {
        this.metricsList = newMetricsList;
        notifyDataSetChanged();
    }

    static class MetricsViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDateTime, tvHealthWorker, tvVitals, tvMeasurements, tvNotes;
        private CardView cardMetric;

        public MetricsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvHealthWorker = itemView.findViewById(R.id.tvHealthWorker);
            tvVitals = itemView.findViewById(R.id.tvVitals);
            tvMeasurements = itemView.findViewById(R.id.tvMeasurements);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            cardMetric = itemView.findViewById(R.id.cardMetric);
        }

        public void bind(PatientMetrics metrics) {
            // Set date and time
            if (metrics.getTimestamp() != null) {
                tvDateTime.setText(metrics.getTimestamp());
            } else if (metrics.getDate() != null) {
                tvDateTime.setText(metrics.getDate());
            }

            // Set health worker info
            if (metrics.getHealthWorkerName() != null) {
                tvHealthWorker.setText("By: " + metrics.getHealthWorkerName());
            } else if (metrics.getHealthWorkerId() != null) {
                tvHealthWorker.setText("Health Worker ID: " + metrics.getHealthWorkerId());
            } else {
                tvHealthWorker.setText("Recorded by staff");
            }

            // Build vital signs string
            StringBuilder vitalsBuilder = new StringBuilder();
            if (metrics.getTemperature() != null && !metrics.getTemperature().isEmpty()) {
                vitalsBuilder.append("🌡 ").append(metrics.getTemperature()).append("°C");
            }
            if (metrics.getBloodPressure() != null && !metrics.getBloodPressure().isEmpty()) {
                if (vitalsBuilder.length() > 0) vitalsBuilder.append(" • ");
                vitalsBuilder.append("🩺 ").append(metrics.getBloodPressure());
            }
            if (metrics.getHeartRate() != null && !metrics.getHeartRate().isEmpty()) {
                if (vitalsBuilder.length() > 0) vitalsBuilder.append(" • ");
                vitalsBuilder.append("💗 ").append(metrics.getHeartRate()).append(" bpm");
            }
            if (metrics.getRespiratoryRate() != null && !metrics.getRespiratoryRate().isEmpty()) {
                if (vitalsBuilder.length() > 0) vitalsBuilder.append(" • ");
                vitalsBuilder.append("😮‍💨 ").append(metrics.getRespiratoryRate());
            }
            if (metrics.getOxygenSaturation() != null && !metrics.getOxygenSaturation().isEmpty()) {
                if (vitalsBuilder.length() > 0) vitalsBuilder.append(" • ");
                vitalsBuilder.append("🫁 ").append(metrics.getOxygenSaturation()).append("%");
            }

            if (vitalsBuilder.length() > 0) {
                tvVitals.setText(vitalsBuilder.toString());
                tvVitals.setVisibility(View.VISIBLE);
            } else {
                tvVitals.setVisibility(View.GONE);
            }

            // Build measurements string
            StringBuilder measurementsBuilder = new StringBuilder();
            if (metrics.getHeight() != null && !metrics.getHeight().isEmpty()) {
                measurementsBuilder.append("📏 H: ").append(metrics.getHeight()).append("cm");
            }
            if (metrics.getWeight() != null && !metrics.getWeight().isEmpty()) {
                if (measurementsBuilder.length() > 0) measurementsBuilder.append(" • ");
                measurementsBuilder.append("⚖ W: ").append(metrics.getWeight()).append("kg");
            }
            if (metrics.getBmi() != null && !metrics.getBmi().isEmpty()) {
                if (measurementsBuilder.length() > 0) measurementsBuilder.append(" • ");
                measurementsBuilder.append("BMI: ").append(metrics.getBmi());
            }
            if (metrics.getBloodSugar() != null && !metrics.getBloodSugar().isEmpty()) {
                if (measurementsBuilder.length() > 0) measurementsBuilder.append(" • ");
                measurementsBuilder.append("🩸 Sugar: ").append(metrics.getBloodSugar()).append("mg/dL");
            }

            if (measurementsBuilder.length() > 0) {
                tvMeasurements.setText(measurementsBuilder.toString());
                tvMeasurements.setVisibility(View.VISIBLE);
            } else {
                tvMeasurements.setVisibility(View.GONE);
            }

            // Set notes
            if (metrics.getNotes() != null && !metrics.getNotes().isEmpty()) {
                tvNotes.setText(metrics.getNotes());
                tvNotes.setVisibility(View.VISIBLE);
            } else {
                tvNotes.setVisibility(View.GONE);
            }
        }
    }
}
