package com.example.azimbalife.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.Doctor;
import com.example.azimbalife.R;

import java.util.ArrayList;
import java.util.List;

public class TopDoctorAdapter1 extends RecyclerView.Adapter<TopDoctorAdapter1.ViewHolder> implements Filterable {
    private List<Doctor> doctorList;
    private List<Doctor> doctorListFull; // Full list for filtering
    private Context context;

    public TopDoctorAdapter1(List<Doctor> doctorList, Context context) {
        this.doctorList = doctorList;
        this.doctorListFull = new ArrayList<>(doctorList); // Copy for filtering
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doctor doctor = doctorList.get(position);

        if (doctor != null) {
            holder.doctorName.setText(doctor.getName() != null ? doctor.getName() : "Unknown Doctor");
            holder.doctorSpecialty.setText(doctor.getSpecialty() != null ? doctor.getSpecialty() : "General Medicine");

            // You can add more fields here if needed
        }
    }

    @Override
    public int getItemCount() {
        return doctorList != null ? doctorList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView doctorName;
        TextView doctorSpecialty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            doctorName = itemView.findViewById(R.id.doctorName);
            doctorSpecialty = itemView.findViewById(R.id.doctorSpecialty);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Doctor> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // If no filter, return full list
                    filteredList.addAll(doctorListFull);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (Doctor doctor : doctorListFull) {
                        // Filter by specialty
                        if (doctor.getSpecialty() != null &&
                                doctor.getSpecialty().toLowerCase().contains(filterPattern)) {
                            filteredList.add(doctor);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                doctorList.clear();
                doctorList.addAll((List<Doctor>) results.values);
                notifyDataSetChanged();

                // Show message if no results
                if (doctorList.isEmpty() && constraint != null && constraint.length() > 0) {
                    Toast.makeText(context, "No doctors found for: " + constraint, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    // Method to update the full list when data changes
    public void updateFullList(List<Doctor> newList) {
        doctorListFull.clear();
        doctorListFull.addAll(newList);
        doctorList.clear();
        doctorList.addAll(newList);
        notifyDataSetChanged();
    }
}