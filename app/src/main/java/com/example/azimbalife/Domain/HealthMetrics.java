package com.example.azimbalife.Domain;

public class HealthMetrics {
    private String bloodPressure;
    private String heartRate;
    private String bloodSugar;
    private String weight;
    private String temperature;
    private String oxygenSaturation;
    private String bmi;
    private String timestamp;
    private String lastUpdated;

    // For health score calculation
    private int bloodPressureSystolic;
    private int bloodPressureDiastolic;

    public HealthMetrics() {
        // Default constructor
    }

    // Getters and setters
    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
        // Parse systolic and diastolic values
        if (bloodPressure != null && bloodPressure.contains("/")) {
            try {
                String[] parts = bloodPressure.split("/");
                this.bloodPressureSystolic = Integer.parseInt(parts[0].trim());
                this.bloodPressureDiastolic = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                this.bloodPressureSystolic = 0;
                this.bloodPressureDiastolic = 0;
            }
        }
    }

    public String getHeartRate() { return heartRate; }
    public void setHeartRate(String heartRate) { this.heartRate = heartRate; }

    public String getBloodSugar() { return bloodSugar; }
    public void setBloodSugar(String bloodSugar) { this.bloodSugar = bloodSugar; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(String oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }

    public String getBmi() { return bmi; }
    public void setBmi(String bmi) { this.bmi = bmi; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public int getBloodPressureSystolic() { return bloodPressureSystolic; }
    public void setBloodPressureSystolic(int bloodPressureSystolic) { this.bloodPressureSystolic = bloodPressureSystolic; }

    public int getBloodPressureDiastolic() { return bloodPressureDiastolic; }
    public void setBloodPressureDiastolic(int bloodPressureDiastolic) { this.bloodPressureDiastolic = bloodPressureDiastolic; }

    // Helper method to get heart rate as integer
    public int getHeartRateValue() {
        try {
            if (heartRate != null && heartRate.contains("bpm")) {
                return Integer.parseInt(heartRate.replace("bpm", "").trim());
            }
            return Integer.parseInt(heartRate);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Helper method to get blood sugar as integer
    public int getBloodSugarValue() {
        try {
            if (bloodSugar != null && bloodSugar.contains("mg/dL")) {
                return Integer.parseInt(bloodSugar.replace("mg/dL", "").trim());
            }
            return Integer.parseInt(bloodSugar);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}