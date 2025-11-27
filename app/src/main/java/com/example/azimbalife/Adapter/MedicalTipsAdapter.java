package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.MedicalTip;
import com.example.azimbalife.R;

import java.util.List;

public class MedicalTipsAdapter extends RecyclerView.Adapter<MedicalTipsAdapter.ViewHolder> {

    private List<MedicalTip> medicalTipsList;

    public MedicalTipsAdapter(List<MedicalTip> medicalTipsList) {
        this.medicalTipsList = medicalTipsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medical_tip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalTip medicalTip = medicalTipsList.get(position);

        holder.tvTipTitle.setText(medicalTip.getTitle());
        holder.tvTipDescription.setText(medicalTip.getDescription());
        holder.ivTipImage.setImageResource(medicalTip.getImageRes());
    }

    @Override
    public int getItemCount() {
        return medicalTipsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTipImage;
        TextView tvTipTitle, tvTipDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTipImage = itemView.findViewById(R.id.ivTipImage);
            tvTipTitle = itemView.findViewById(R.id.tvTipTitle);
            tvTipDescription = itemView.findViewById(R.id.tvTipDescription);
        }
    }
}