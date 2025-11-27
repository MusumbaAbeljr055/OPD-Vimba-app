package com.example.azimbalife.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.azimbalife.Activity.AllDoctorsActivity;
import com.example.azimbalife.Activity.AllDoctorsActivity;
import com.example.azimbalife.Domain.Specialist;
import com.example.azimbalife.R;

import java.util.List;

public class SpecialistAdapter extends RecyclerView.Adapter<SpecialistAdapter.SpecialistViewHolder> {

    private final List<Specialist> specialists;
    private final Context context;

    public SpecialistAdapter(List<Specialist> specialists, Context context) {
        this.specialists = specialists;
        this.context = context;
    }

    @NonNull
    @Override
    public SpecialistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_specialist, parent, false);
        return new SpecialistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpecialistViewHolder holder, int position) {
        Specialist s = specialists.get(position);

        holder.name.setText(s.getName());
        holder.specialty.setText(s.getSpecialty());
        holder.availability.setText(s.getAvailability());
        holder.rating.setText(String.valueOf(s.getRating()) + " ★");
        Glide.with(context).load(s.getImageRes()).circleCrop().into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            // Open Booking Activity
            Intent intent = new Intent(context, AllDoctorsActivity.class);
            intent.putExtra("doctorName", s.getName());
            intent.putExtra("doctorSpecialty", s.getSpecialty());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return specialists.size(); }

    static class SpecialistViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, specialty, availability, rating;

        public SpecialistViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.specialistImage);
            name = itemView.findViewById(R.id.specialistName);
            specialty = itemView.findViewById(R.id.specialistSpecialty);
            availability = itemView.findViewById(R.id.specialistAvailability);
            rating = itemView.findViewById(R.id.specialistRating);
        }
    }
}
