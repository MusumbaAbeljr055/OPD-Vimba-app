package com.example.azimbalife.Adapter;



public class AppointmentModel {
    private String id;
    private String patientName;
    private String date;
    private String reason;
    private String status; // Pending / Approved / Rejected
    private String doctorMobile;

    public AppointmentModel() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDoctorMobile() { return doctorMobile; }
    public void setDoctorMobile(String doctorMobile) { this.doctorMobile = doctorMobile; }
}

