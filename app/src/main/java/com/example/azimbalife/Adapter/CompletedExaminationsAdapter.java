package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.CompletedExamination;
import com.example.azimbalife.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompletedExaminationsAdapter extends RecyclerView.Adapter<CompletedExaminationsAdapter.ExaminationViewHolder> {

    private List<CompletedExamination> examinations;
    private OnExaminationClickListener listener;

    public interface OnExaminationClickListener {
        void onNotifyPatientClick(CompletedExamination examination);
        void onViewDetailsClick(CompletedExamination examination);
        void onContactPatientClick(CompletedExamination examination);
    }

    public CompletedExaminationsAdapter(List<CompletedExamination> examinations, OnExaminationClickListener listener) {
        this.examinations = examinations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExaminationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_completed_examination, parent, false);
        return new ExaminationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExaminationViewHolder holder, int position) {
        CompletedExamination examination = examinations.get(position);
        holder.bind(examination, listener);
    }

    @Override
    public int getItemCount() {
        return examinations.size();
    }

    public void updateExaminations(List<CompletedExamination> newExaminations) {
        this.examinations = newExaminations;
        notifyDataSetChanged();
    }

    static class ExaminationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatientName, tvDoctorName, tvCompletionTime, tvExaminationId;
        private Button btnNotifyPatient, btnViewDetails, btnContactPatient;

        public ExaminationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDoctorName = itemView.findViewById(R.id.tvDoctorName);
            tvCompletionTime = itemView.findViewById(R.id.tvCompletionTime);
            tvExaminationId = itemView.findViewById(R.id.tvExaminationId);
            btnNotifyPatient = itemView.findViewById(R.id.btnNotifyPatient);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnContactPatient = itemView.findViewById(R.id.btnContactPatient);
        }

        public void bind(CompletedExamination examination, OnExaminationClickListener listener) {
            tvPatientName.setText(examination.getPatientName() != null ? examination.getPatientName() : "Unknown Patient");
            tvDoctorName.setText("Doctor: " + (examination.getDoctor() != null ? examination.getDoctor() : "Unknown Doctor"));
            tvExaminationId.setText("Exam ID: " + (examination.getId() != null ? examination.getId().substring(0, 8) + "..." : "N/A"));

            // Format completion time
            if (examination.getCompletedAt() != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                    Date date = inputFormat.parse(examination.getCompletedAt());
                    String formattedDate = outputFormat.format(date);
                    tvCompletionTime.setText("Completed: " + formattedDate);
                } catch (Exception e) {
                    tvCompletionTime.setText("Completed: " + examination.getCompletedAt());
                }
            } else {
                tvCompletionTime.setText("Completed: Unknown time");
            }

            btnNotifyPatient.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotifyPatientClick(examination);
                }
            });

            btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClick(examination);
                }
            });

            btnContactPatient.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContactPatientClick(examination);
                }
            });

            // Make entire item clickable for details
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDetailsClick(examination);
                }
            });
        }
    }
}