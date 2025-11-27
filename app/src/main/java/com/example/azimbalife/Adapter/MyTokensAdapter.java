package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.TokenTracking;
import com.example.azimbalife.R;

import java.util.List;

public class MyTokensAdapter extends RecyclerView.Adapter<MyTokensAdapter.ViewHolder> {

    private List<TokenTracking> tokens;
    private OnTokenClickListener onTokenClickListener;

    public interface OnTokenClickListener {
        void onTokenClick(TokenTracking token);
    }

    public MyTokensAdapter(List<TokenTracking> tokens, OnTokenClickListener onTokenClickListener) {
        this.tokens = tokens;
        this.onTokenClickListener = onTokenClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.viewholder_my_token, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TokenTracking token = tokens.get(position);

        if (token != null) {
            holder.tvTokenId.setText(token.getTokenId() != null ? token.getTokenId() : "Unknown");
            holder.tvDoctor.setText(token.getDoctor() != null ? token.getDoctor() : "No Doctor");
            holder.tvSpecialty.setText(token.getSpecialty() != null ? token.getSpecialty() : "General");
            holder.tvTime.setText(formatTime(token.getTimestamp()));
            holder.tvCurrentStage.setText(getStageDisplayName(token.getCurrentStage()));

            // Set status and color
            String status = token.getOverallStatus() != null ? token.getOverallStatus() : "waiting";
            holder.tvStatus.setText(getStatusDisplay(status));
            holder.tvStatus.setTextColor(getStatusColor(holder.itemView, status));

            // Calculate progress
            int progress = calculateProgress(token);
            holder.progressBar.setProgress(progress);
            holder.tvProgress.setText(progress + "/4 stages completed");

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                if (onTokenClickListener != null) {
                    onTokenClickListener.onTokenClick(token);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return tokens != null ? tokens.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTokenId, tvDoctor, tvSpecialty, tvTime, tvCurrentStage, tvStatus, tvProgress;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTokenId = itemView.findViewById(R.id.tvTokenId);
            tvDoctor = itemView.findViewById(R.id.tvDoctor);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCurrentStage = itemView.findViewById(R.id.tvCurrentStage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    private String formatTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "Time not set";
        }
        try {
            // Extract time part from "yyyy-MM-dd HH:mm:ss"
            String[] parts = timestamp.split(" ");
            if (parts.length > 1) {
                return parts[1].substring(0, 5); // Return HH:mm
            }
            return timestamp;
        } catch (Exception e) {
            return timestamp;
        }
    }

    private String getStageDisplayName(String stage) {
        if (stage == null) return "Registration";

        switch (stage) {
            case "registration": return "Registration";
            case "triage": return "Triage";
            case "consultation": return "Consultation";
            case "pharmacy": return "Pharmacy";
            default: return stage;
        }
    }

    private String getStatusDisplay(String status) {
        if (status == null) return "Waiting";

        switch (status) {
            case "completed": return "Completed";
            case "in_progress": return "In Progress";
            case "waiting": return "Waiting";
            default: return status;
        }
    }

    private int getStatusColor(View view, String status) {
        if (status == null) return view.getContext().getColor(R.color.grey);

        switch (status) {
            case "completed":
                return view.getContext().getColor(R.color.green);
            case "in_progress":
                return view.getContext().getColor(R.color.blue);
            case "waiting":
                return view.getContext().getColor(R.color.my_primary);
            default:
                return view.getContext().getColor(R.color.grey);
        }
    }

    private int calculateProgress(TokenTracking token) {
        if (token == null || token.getStages() == null) return 0;

        int completedStages = 0;
        for (TokenTracking.Stage stage : token.getStages().values()) {
            if (stage != null && "completed".equals(stage.getStatus())) {
                completedStages++;
            }
        }
        return completedStages;
    }

    // Method to update data
    public void updateData(List<TokenTracking> newTokens) {
        this.tokens = newTokens;
        notifyDataSetChanged();
    }
}