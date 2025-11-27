package com.example.azimbalife.Domain;

import java.io.Serializable;

public class TimeSlot implements Serializable {
    private String startTime;
    private String endTime;
    private String day;
    private boolean available;
    private String slotType; // normal, emergency, break
    private boolean isBooked;
    private String appointmentId; // If booked

    public TimeSlot() {}

    public TimeSlot(String startTime, String endTime, String day) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.day = day;
        this.available = true;
        this.slotType = "normal";
        this.isBooked = false;
    }

    // Getters and setters
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getSlotType() { return slotType; }
    public void setSlotType(String slotType) { this.slotType = slotType; }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    @Override
    public String toString() {
        return startTime + " - " + endTime + " (" + day + ")" +
                (isBooked ? " [Booked]" : " [Available]");
    }
}