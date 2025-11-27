package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.azimbalife.Domain.DoctorsModel;
import com.example.azimbalife.R;

import java.util.ArrayList;
import java.util.List;

public class DoctorsAdapter extends RecyclerView.Adapter<DoctorsAdapter.DoctorViewHolder> {

    private List<DoctorsModel> doctorsList;
    private List<DoctorsModel> doctorsListFull;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DoctorsModel doctor);
    }

    public DoctorsAdapter(List<DoctorsModel> doctorsList) {
        this.doctorsList = new ArrayList<>(doctorsList);
        this.doctorsListFull = new ArrayList<>(doctorsList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.doctor_item, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        DoctorsModel doctor = doctorsList.get(position);
        holder.tvName.setText(doctor.getName());


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(doctor);
        });
    }

    @Override
    public int getItemCount() {
        return doctorsList.size();
    }

    // Filtering method
    public void filter(String text) {
        text = text.toLowerCase().trim();
        doctorsList.clear();
        if (text.isEmpty()) {
            doctorsList.addAll(doctorsListFull);
        } else {
            for (DoctorsModel doctor : doctorsListFull) {

            }
        }
        notifyDataSetChanged();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        ImageView imageDoctor;
        TextView tvName, tvSpecial;

        public DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            imageDoctor = itemView.findViewById(R.id.imageDoctor);
            tvName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecial = itemView.findViewById(R.id.tvSpecialization);
        }
    }
}
