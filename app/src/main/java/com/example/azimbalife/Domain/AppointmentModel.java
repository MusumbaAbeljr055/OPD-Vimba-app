package com.example.azimbalife.Domain;

public class AppointmentModel {

    private String id;
    private String doctorEmail;
    private String patientName;
    private String date;
    private String time;
    private String doctorName;
    private String specialization;
    private String status; // pending, approved, rejected
    private String reason;

    public AppointmentModel() {
    }

    public AppointmentModel(String id, String doctorEmail, String patientName, String date, String time,
                            String doctorName, String specialization, String status, String reason) {
        this.id = id;
        this.doctorEmail = doctorEmail;
        this.patientName = patientName;
        this.date = date;
        this.time = time;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.status = status;
        this.reason = reason;
    }

    // Getters and setters for all fields
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }       // <-- Add getter
    public void setReason(String reason) { this.reason = reason; }
}
