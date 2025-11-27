package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.MedicalRecord;
import com.example.azimbalife.R;

import java.util.List;

public class MedicalRecordsAdapter extends RecyclerView.Adapter<MedicalRecordsAdapter.ViewHolder> {

    private List<MedicalRecord> medicalRecords;
    private OnMedicalRecordClickListener listener;

    public interface OnMedicalRecordClickListener {
        void onViewDetailsClick(MedicalRecord medicalRecord);
        void onDownloadClick(MedicalRecord medicalRecord);
        void onShareClick(MedicalRecord medicalRecord);
        void onPrintClick(MedicalRecord medicalRecord);
        void onEmailClick(MedicalRecord medicalRecord);
    }

    public MedicalRecordsAdapter(List<MedicalRecord> medicalRecords, OnMedicalRecordClickListener listener) {
        this.medicalRecords = medicalRecords;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medical_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalRecord record = medicalRecords.get(position);

        // Set record data with null checks
        if (holder.tvRecordTitle != null) {
            holder.tvRecordTitle.setText(record.getTitle() != null ? record.getTitle() : "No Title");
        }

        if (holder.tvRecordType != null) {
            holder.tvRecordType.setText(record.getFormattedRecordType() != null ?
                    record.getFormattedRecordType() : "Unknown Type");
        }

        if (holder.tvDate != null) {
            holder.tvDate.setText(record.getDate() != null ? record.getDate() : "No Date");
        }

        if (holder.tvDoctor != null) {
            String doctorName = record.getDoctorName();
            if (doctorName != null && !doctorName.startsWith("Dr. ")) {
                holder.tvDoctor.setText("Dr. " + doctorName);
            } else {
                holder.tvDoctor.setText(doctorName != null ? doctorName : "Unknown Doctor");
            }
        }

        // Set record type icon
        if (holder.ivRecordIcon != null) {
            setRecordTypeIcon(holder.ivRecordIcon, record.getRecordType());
        }

        // Set download status
        if (holder.ivDownloadStatus != null && holder.btnDownload != null) {
            if (record.isDownloaded()) {
                holder.ivDownloadStatus.setImageResource(R.drawable.ic_baseline_check_circle_24);
                holder.ivDownloadStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
                holder.btnDownload.setText("Downloaded");
                holder.btnDownload.setEnabled(false);
                holder.btnDownload.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_gray));
            } else {
                holder.ivDownloadStatus.setImageResource(R.drawable.ic_baseline_file_download_24);
                holder.ivDownloadStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
                holder.btnDownload.setText("Download");
                holder.btnDownload.setEnabled(true);
                holder.btnDownload.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
            }
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetailsClick(record);
            }
        });

        if (holder.btnDownload != null) {
            holder.btnDownload.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadClick(record);
                }
            });
        }

        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(v -> {
                if (listener != null && record.canBeShared()) {
                    listener.onShareClick(record);
                } else {
                    Toast.makeText(holder.itemView.getContext(),
                            "Record already shared or cannot be shared", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (holder.btnPrint != null) {
            holder.btnPrint.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPrintClick(record);
                }
            });
        }

        if (holder.btnEmail != null) {
            holder.btnEmail.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmailClick(record);
                }
            });
        }
    }

    private void setRecordTypeIcon(ImageView imageView, String recordType) {
        if (imageView == null) return;

        if (recordType == null) {
            imageView.setImageResource(R.drawable.ic_baseline_description_24);
            imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.purple_700));
            return;
        }

        switch (recordType.toLowerCase()) {
            case "prescription":
                imageView.setImageResource(R.drawable.ic_baseline_medication_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.green));
                break;
            case "diagnosis":
                imageView.setImageResource(R.drawable.ic_baseline_healing_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.blue));
                break;
            case "lab_result":
                imageView.setImageResource(R.drawable.ic_baseline_science_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.orange));
                break;
            case "vaccination":
                imageView.setImageResource(R.drawable.ic_baseline_vaccines_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.purple_700));
                break;
            case "examination":
                imageView.setImageResource(R.drawable.ic_baseline_medical_services_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.teal_700));
                break;
            case "vital_metrics":
                imageView.setImageResource(R.drawable.ic_baseline_monitor_heart_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.red));
                break;
            default:
                imageView.setImageResource(R.drawable.ic_baseline_description_24);
                imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.purple_700));
        }
    }

    @Override
    public int getItemCount() {
        return medicalRecords != null ? medicalRecords.size() : 0;
    }

    public void updateMedicalRecords(List<MedicalRecord> newMedicalRecords) {
        this.medicalRecords = newMedicalRecords;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecordIcon, ivDownloadStatus;
        TextView tvRecordTitle, tvRecordType, tvDate, tvDoctor;
        Button btnDownload, btnShare, btnPrint, btnEmail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize ALL views with proper null checks
            ivRecordIcon = itemView.findViewById(R.id.ivRecordIcon);
            ivDownloadStatus = itemView.findViewById(R.id.ivDownloadStatus);
            tvRecordTitle = itemView.findViewById(R.id.tvRecordTitle);
            tvRecordType = itemView.findViewById(R.id.tvRecordType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDoctor = itemView.findViewById(R.id.tvDoctor);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnPrint = itemView.findViewById(R.id.btnPrint);
            btnEmail = itemView.findViewById(R.id.btnEmail);

            // Debug logging to check which views are found
            if (ivRecordIcon == null) {
                android.util.Log.d("MedicalRecordsAdapter", "ivRecordIcon not found in layout");
            }
            if (btnShare == null) {
                android.util.Log.d("MedicalRecordsAdapter", "btnShare not found in layout");
            }
            if (btnPrint == null) {
                android.util.Log.d("MedicalRecordsAdapter", "btnPrint not found in layout");
            }
            if (btnEmail == null) {
                android.util.Log.d("MedicalRecordsAdapter", "btnEmail not found in layout");
            }
        }
    }
}