package com.example.azimbalife.Domain;

import java.io.Serializable;

public class CompletedExamination implements Serializable {
    private String id;
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String doctor;
    private String doctorId;
    private String diagnosis;
    private String prescription;
    private String recommendations;
    private String labTests;
    private String followUpDate;
    private String completedAt;
    private String notificationStatus;
    private String timestamp;

    // Fields for nested Firebase data
    private Findings findings;
    private FollowUp followUp;

    // Default constructor required for Firebase
    public CompletedExamination() {}

    // Nested class for findings
    public static class Findings implements Serializable {
        private String chiefComplaints;
        private String clinicalObservations;

        public Findings() {}

        public String getChiefComplaints() { return chiefComplaints; }
        public void setChiefComplaints(String chiefComplaints) { this.chiefComplaints = chiefComplaints; }

        public String getClinicalObservations() { return clinicalObservations; }
        public void setClinicalObservations(String clinicalObservations) { this.clinicalObservations = clinicalObservations; }
    }

    // Nested class for follow-up
    public static class FollowUp implements Serializable {
        private String date;
        private String recommendations;

        public FollowUp() {}

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getRecommendations() { return recommendations; }
        public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getLabTests() { return labTests; }
    public void setLabTests(String labTests) { this.labTests = labTests; }

    public String getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(String followUpDate) { this.followUpDate = followUpDate; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public String getNotificationStatus() { return notificationStatus; }
    public void setNotificationStatus(String notificationStatus) { this.notificationStatus = notificationStatus; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Findings getFindings() { return findings; }
    public void setFindings(Findings findings) { this.findings = findings; }

    public FollowUp getFollowUp() { return followUp; }
    public void setFollowUp(FollowUp followUp) { this.followUp = followUp; }

    // Helper methods to get data from nested structures
    public String getChiefComplaints() {
        return findings != null ? findings.getChiefComplaints() : null;
    }

    public String getClinicalObservations() {
        return findings != null ? findings.getClinicalObservations() : null;
    }

    public String getFollowUpRecommendations() {
        return followUp != null ? followUp.getRecommendations() : null;
    }

    public String getNestedFollowUpDate() {
        return followUp != null ? followUp.getDate() : null;
    }
}