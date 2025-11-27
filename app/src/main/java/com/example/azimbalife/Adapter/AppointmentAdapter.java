package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.R;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onViewDetailsClick(Appointment appointment);
        void onCancelClick(Appointment appointment);
    }

    public AppointmentAdapter(List<Appointment> appointments, OnAppointmentClickListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);

        // Set data to views
        holder.tvDepartment.setText(appointment.getDepartment());
        holder.tvDate.setText(appointment.getPreferredDate());
        holder.tvTime.setText(appointment.getAllocatedTime() != null ?
                appointment.getAllocatedTime() : "Time not allocated");
        holder.tvToken.setText(appointment.getTokenNumber() != null ?
                "Token: " + appointment.getTokenNumber() : "Token: Not assigned");
        holder.tvStatus.setText(appointment.getStatus());
        holder.tvReason.setText(appointment.getVisitReason());

        // Set status color
        setStatusColor(holder.tvStatus, appointment.getStatus());

        // View Details button
        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetailsClick(appointment);
            }
        });

        // Cancel button (only show for pending appointments)
        if ("pending".equalsIgnoreCase(appointment.getStatus())) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelClick(appointment);
                }
            });
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    private void setStatusColor(TextView statusView, String status) {
        switch (status.toLowerCase()) {
            case "confirmed":
                statusView.setTextColor(statusView.getContext().getColor(R.color.green));
                break;
            case "pending":
                statusView.setTextColor(statusView.getContext().getColor(R.color.my_primary));
                break;
            case "completed":
                statusView.setTextColor(statusView.getContext().getColor(R.color.blue));
                break;
            case "cancelled":
                statusView.setTextColor(statusView.getContext().getColor(R.color.red));
                break;
            default:
                statusView.setTextColor(statusView.getContext().getColor(R.color.grey));
        }
    }

    @Override
    public int getItemCount() {
        return appointments != null ? appointments.size() : 0;
    }

    public void updateAppointments(List<Appointment> newAppointments) {
        this.appointments = newAppointments;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDepartment, tvDate, tvTime, tvToken, tvStatus, tvReason;
        Button btnViewDetails, btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views - make sure these IDs match your item_appointment.xml
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvToken = itemView.findViewById(R.id.tvToken);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
}