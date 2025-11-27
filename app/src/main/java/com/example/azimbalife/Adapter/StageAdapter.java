package com.example.azimbalife.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.TokenTracking;
import com.example.azimbalife.R;

import java.util.Map;

public class StageAdapter extends RecyclerView.Adapter<StageAdapter.ViewHolder> {

    private Map<String, TokenTracking.Stage> stages;
    private Context context;

    public StageAdapter(Map<String, TokenTracking.Stage> stages) {
        this.stages = stages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.viewholder_stage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] stageKeys = {"registration", "triage", "consultation", "pharmacy"};
        String stageKey = stageKeys[position];
        TokenTracking.Stage stage = stages.get(stageKey);

        if (stage != null) {
            holder.stageName.setText(getStageDisplayName(stageKey));
            holder.stageStatus.setText(getStatusDisplay(stage.getStatus()));
            holder.stageTime.setText(stage.getTime() != null ? stage.getTime() : "Pending");

            // Set color based on status
            int color = getStatusColor(stage.getStatus());
            holder.stageStatus.setTextColor(color);
        }
    }

    @Override
    public int getItemCount() {
        return stages != null ? stages.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stageName, stageStatus, stageTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stageName = itemView.findViewById(R.id.stageName);
            stageStatus = itemView.findViewById(R.id.stageStatus);
            stageTime = itemView.findViewById(R.id.stageTime);
        }
    }

    private String getStageDisplayName(String stage) {
        if (stage == null) return "Unknown Stage";

        switch (stage) {
            case "registration": return "1. Registration";
            case "triage": return "2. Triage";
            case "consultation": return "3. Doctor Consultation";
            case "pharmacy": return "4. Pharmacy";
            default: return stage;
        }
    }

    private String getStatusDisplay(String status) {
        if (status == null) return "⏳ Waiting";

        switch (status) {
            case "completed": return "✅ Completed";
            case "in_progress": return "🔄 In Progress";
            case "waiting": return "⏳ Waiting";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        if (context == null) return android.graphics.Color.GRAY;
        if (status == null) return context.getColor(R.color.grey);

        switch (status) {
            case "completed":
                return context.getColor(R.color.green);
            case "in_progress":
                return context.getColor(R.color.blue);
            case "waiting":
                return context.getColor(R.color.my_primary);
            default:
                return context.getColor(R.color.grey);
        }
    }
}