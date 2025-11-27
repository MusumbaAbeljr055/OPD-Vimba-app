package com.example.azimbalife.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.R;

import java.util.List;

public class HealthWorkerAppointmentsAdapter extends RecyclerView.Adapter<HealthWorkerAppointmentsAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private OnAppointmentActionListener listener;

    public interface OnAppointmentActionListener {
        void onApproveAppointment(Appointment appointment);
        void onRejectAppointment(Appointment appointment);
        void onAssignToken(Appointment appointment, String tokenNumber, String allocatedTime);
    }

    public HealthWorkerAppointmentsAdapter(List<Appointment> appointments, OnAppointmentActionListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    public void updateAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_health_worker_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.bind(appointment);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatientName, tvDepartment, tvUrgency, tvStatus, tvTime, tvToken;
        private Button btnApprove, btnReject, btnAssignToken;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvUrgency = itemView.findViewById(R.id.tvUrgency);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvToken = itemView.findViewById(R.id.tvToken);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnAssignToken = itemView.findViewById(R.id.btnAssignToken);
        }

        public void bind(Appointment appointment) {
            // Set basic appointment info
            tvPatientName.setText(appointment.getPatientName() != null ? appointment.getPatientName() : "Unknown Patient");
            tvDepartment.setText(appointment.getDepartment() != null ? appointment.getDepartment() : "No Department");
            tvUrgency.setText(appointment.getUrgencyLevel() != null ? appointment.getUrgencyLevel() : "Normal");
            tvStatus.setText(appointment.getStatus() != null ? appointment.getStatus() : "Pending");
            tvTime.setText(appointment.getPreferredTime() != null ? appointment.getPreferredTime() : "No Time");

            // Show token if assigned
            if (appointment.getTokenNumber() != null && !appointment.getTokenNumber().isEmpty()) {
                tvToken.setText("Token: " + appointment.getTokenNumber());
                tvToken.setVisibility(View.VISIBLE);
            } else {
                tvToken.setVisibility(View.GONE);
            }

            // Handle emergency styling
            boolean isEmergency = appointment.isEmergency() ||
                    "emergency".equalsIgnoreCase(appointment.getUrgencyLevel()) ||
                    "emergency".equalsIgnoreCase(appointment.getStatus());

            if (isEmergency) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.emergency_light_red));
                tvUrgency.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.emergency_red));
                tvUrgency.setText("🚨 EMERGENCY");
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                tvUrgency.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            }

            // Handle button visibility based on status
            String status = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "pending";

            switch (status) {
                case "pending":
                    btnApprove.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnAssignToken.setVisibility(View.VISIBLE);
                    btnApprove.setText("APPROVE");
                    btnAssignToken.setText("ASSIGN DOCTOR");
                    break;

                case "confirmed":
                    btnApprove.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnAssignToken.setVisibility(View.VISIBLE);
                    btnAssignToken.setText("ASSIGN DOCTOR");
                    break;

                case "scheduled":
                    btnApprove.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnAssignToken.setVisibility(View.VISIBLE);
                    btnAssignToken.setText("VIEW ASSIGNMENT");
                    break;

                case "emergency":
                    btnApprove.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnAssignToken.setVisibility(View.VISIBLE);
                    btnApprove.setText("HANDLE EMERGENCY");
                    btnAssignToken.setText("ASSIGN EMERGENCY");
                    break;

                default:
                    btnApprove.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);
                    btnAssignToken.setVisibility(View.VISIBLE);
            }

            // Set button click listeners
            btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApproveAppointment(appointment);
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectAppointment(appointment);
                }
            });

            btnAssignToken.setOnClickListener(v -> {
                if (listener != null) {
                    // For already scheduled appointments, just show current assignment
                    if ("scheduled".equalsIgnoreCase(appointment.getStatus())) {
                        showAssignmentDetails(appointment);
                    } else {
                        // For other statuses, allow assignment
                        listener.onAssignToken(appointment, "", "");
                    }
                }
            });

            // Whole item click for quick access
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (isEmergency) {
                        listener.onApproveAppointment(appointment);
                    } else {
                        listener.onAssignToken(appointment, "", "");
                    }
                }
            });
        }

        private void showAssignmentDetails(Appointment appointment) {
            String message = "Patient: " + appointment.getPatientName() + "\n" +
                    "Doctor: " + (appointment.getAssignedDoctorName() != null ? appointment.getAssignedDoctorName() : "Not assigned") + "\n" +
                    "Token: " + (appointment.getTokenNumber() != null ? appointment.getTokenNumber() : "Not assigned") + "\n" +
                    "Time: " + (appointment.getAllocatedTime() != null ? appointment.getAllocatedTime() : "Not scheduled");

            Toast.makeText(itemView.getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}