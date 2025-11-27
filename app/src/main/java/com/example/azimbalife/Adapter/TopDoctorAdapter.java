package com.example.azimbalife.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.example.azimbalife.Activity.DetailActivity;
import com.example.azimbalife.Domain.DoctorsModel;
import com.example.azimbalife.databinding.ViewholderTopDoctorsBinding;

import java.util.ArrayList;
import java.util.List;

public class TopDoctorAdapter extends RecyclerView.Adapter<TopDoctorAdapter.Viewholder> implements Filterable {

    private List<DoctorsModel> items;
    private final List<DoctorsModel> fullItems; // Full list for restoring after search
    private final Context context;
    private final String username;

    public TopDoctorAdapter(List<DoctorsModel> items, Context context, String username) {
        this.items = items;
        this.fullItems = new ArrayList<>(items); // Clone original list
        this.context = context;
        this.username = username;
    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewholderTopDoctorsBinding binding = ViewholderTopDoctorsBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new Viewholder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
        DoctorsModel doctor = items.get(position);
        holder.binding.nameTxt.setText(doctor.getName());

        // FIXED: Use getSpecialty() instead of getSpecial()
        holder.binding.special.setText(doctor.getSpecialty());




        holder.binding.getRoot().setOnClickListener(v -> {
            Intent i = new Intent(context, DetailActivity.class);
            i.putExtra("object", doctor);
            i.putExtra("username", username);
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Viewholder extends RecyclerView.ViewHolder {
        private final ViewholderTopDoctorsBinding binding;

        public Viewholder(ViewholderTopDoctorsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @Override
    public Filter getFilter() {
        return doctorFilter;
    }

    private final Filter doctorFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<DoctorsModel> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(fullItems); // Show all if empty
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (DoctorsModel doctor : fullItems) {
                    String name = doctor.getName() != null ?
                            doctor.getName().toLowerCase().replace("dr. ", "").trim() : "";

                    // FIXED: Use getSpecialty() instead of getSpecial()
                    String specialty = doctor.getSpecialty() != null ?
                            doctor.getSpecialty().toLowerCase() : "";

                    if (name.contains(filterPattern.replace("dr. ", "")) ||
                            ("dr. " + name).contains(filterPattern) ||
                            specialty.contains(filterPattern)) {
                        filteredList.add(doctor);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            items.clear();
            items.addAll((List<DoctorsModel>) results.values);
            notifyDataSetChanged();

            // Show message if no results found
            if (items.isEmpty() && constraint != null && constraint.length() > 0) {
                // You can add a Toast here if needed
            }
        }
    };
}