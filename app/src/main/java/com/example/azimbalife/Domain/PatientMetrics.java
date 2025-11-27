package com.example.azimbalife.Domain;

public class PatientMetrics {
    private String recordId;
    private String patientId;
    private String patientName;
    private String healthWorkerId;
    private String healthWorkerName;
    private String temperature;
    private String bloodPressure;
    private String heartRate;
    private String respiratoryRate;
    private String oxygenSaturation;
    private String height;
    private String weight;
    private String bloodSugar;
    private String bmi;
    private String notes;
    private String timestamp;
    private String date;

    public PatientMetrics() {
        // Default constructor required for Firebase
    }

    // Getters and setters for all fields
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getHealthWorkerId() { return healthWorkerId; }
    public void setHealthWorkerId(String healthWorkerId) { this.healthWorkerId = healthWorkerId; }

    public String getHealthWorkerName() { return healthWorkerName; }
    public void setHealthWorkerName(String healthWorkerName) { this.healthWorkerName = healthWorkerName; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public String getHeartRate() { return heartRate; }
    public void setHeartRate(String heartRate) { this.heartRate = heartRate; }

    public String getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(String respiratoryRate) { this.respiratoryRate = respiratoryRate; }

    public String getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(String oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getBloodSugar() { return bloodSugar; }
    public void setBloodSugar(String bloodSugar) { this.bloodSugar = bloodSugar; }

    public String getBmi() { return bmi; }
    public void setBmi(String bmi) { this.bmi = bmi; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}