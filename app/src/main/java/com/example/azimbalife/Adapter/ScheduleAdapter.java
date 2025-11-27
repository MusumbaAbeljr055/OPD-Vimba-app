package com.example.azimbalife.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Domain.Schedule;
import com.example.azimbalife.R;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private Context context;
    private List<Schedule> scheduleList;

    public ScheduleAdapter(Context context, List<Schedule> scheduleList) {
        this.context = context;
        this.scheduleList = scheduleList;
    }

    public void updateData(List<Schedule> newScheduleList) {
        this.scheduleList.clear();
        if (newScheduleList != null) {
            this.scheduleList.addAll(newScheduleList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_card, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = scheduleList.get(position);

        // Highlight Closed or Ongoing
        if(schedule.getStartTime().equalsIgnoreCase("Closed")){
            holder.timeText.setText("Closed");
            holder.timeText.setTextColor(Color.RED);
            holder.activitiesText.setText("No services available");
        } else {
            holder.timeText.setText(schedule.getStartTime() + " - " + schedule.getEndTime());
            holder.timeText.setTextColor(Color.parseColor("#1A3C6D"));
            holder.activitiesText.setText("🩺 Services: " + String.join(", ", schedule.getActivities()));
        }

        // Doctors
        holder.doctorsText.setText("👨‍⚕️ Doctors: " + String.join(", ", schedule.getDoctors()));

        // Icon
        holder.iconView.setImageResource(schedule.getIconRes());
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView timeText, doctorsText, activitiesText;
        ImageView iconView;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText);
            doctorsText = itemView.findViewById(R.id.doctorsText);
            activitiesText = itemView.findViewById(R.id.activitiesText);
            iconView = itemView.findViewById(R.id.iconView);
        }
    }
}
