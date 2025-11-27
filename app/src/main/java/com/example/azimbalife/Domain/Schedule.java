package com.example.azimbalife.Domain;

public class Schedule {
    private String day;
    private String startTime;
    private String endTime;
    private String[] doctors;
    private String[] activities;
    private int iconRes;
    private String status; // Added missing field

    public Schedule(String day, String startTime, String endTime, String[] doctors,
                    String[] activities, int iconRes, String status) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.doctors = doctors;
        this.activities = activities;
        this.iconRes = iconRes;
        this.status = status;
    }

    public String getDay() { return day; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String[] getDoctors() { return doctors; }
    public String[] getActivities() { return activities; }
    public int getIconRes() { return iconRes; }
    public String getStatus() { return status; } // Added getter
}