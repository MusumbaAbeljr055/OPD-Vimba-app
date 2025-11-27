package com.example.azimbalife.Domain;

import java.io.Serializable;

public class HealthReport implements Serializable {
    private String reportId;
    private String patientUsername;
    private String reportType; // Summary, Detailed, Custom, Emergency
    private String reportName;
    private String generatedDate;
    private String dateRange;
    private String fileUrl;
    private String fileName;
    private long fileSize;
    private String status; // Generated, Processing, Failed
    private String summary;
    private int healthScore;
    private int totalAppointments;
    private int totalTests;
    private int totalPrescriptions;
    private String recommendations;
    private String createdAt;

    // New fields for vital metrics integration
    private int totalVitalRecords;
    private int averageHeartRate;
    private int averageBloodSugar;
    private double averageTemperature;
    private int averageOxygenSaturation;

    // Default constructor required for Firebase
    public HealthReport() {
    }

    public HealthReport(String reportId, String patientUsername, String reportType,
                        String reportName, String generatedDate) {
        this.reportId = reportId;
        this.patientUsername = patientUsername;
        this.reportType = reportType;
        this.reportName = reportName;
        this.generatedDate = generatedDate;
        this.status = "Generated";
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getPatientUsername() { return patientUsername; }
    public void setPatientUsername(String patientUsername) { this.patientUsername = patientUsername; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(String generatedDate) { this.generatedDate = generatedDate; }

    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }

    public int getTotalAppointments() { return totalAppointments; }
    public void setTotalAppointments(int totalAppointments) { this.totalAppointments = totalAppointments; }

    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

    public int getTotalPrescriptions() { return totalPrescriptions; }
    public void setTotalPrescriptions(int totalPrescriptions) { this.totalPrescriptions = totalPrescriptions; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // New getters and setters for vital metrics
    public int getTotalVitalRecords() { return totalVitalRecords; }
    public void setTotalVitalRecords(int totalVitalRecords) { this.totalVitalRecords = totalVitalRecords; }

    public int getAverageHeartRate() { return averageHeartRate; }
    public void setAverageHeartRate(int averageHeartRate) { this.averageHeartRate = averageHeartRate; }

    public int getAverageBloodSugar() { return averageBloodSugar; }
    public void setAverageBloodSugar(int averageBloodSugar) { this.averageBloodSugar = averageBloodSugar; }

    public double getAverageTemperature() { return averageTemperature; }
    public void setAverageTemperature(double averageTemperature) { this.averageTemperature = averageTemperature; }

    public int getAverageOxygenSaturation() { return averageOxygenSaturation; }
    public void setAverageOxygenSaturation(int averageOxygenSaturation) { this.averageOxygenSaturation = averageOxygenSaturation; }

    // Helper methods
    public boolean hasFile() {
        return fileUrl != null && !fileUrl.isEmpty();
    }

    public String getFileSizeFormatted() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public String getHealthScoreColor() {
        if (healthScore >= 80) return "green";
        else if (healthScore >= 60) return "orange";
        else return "red";
    }

    // Additional helper method for vital metrics
    public boolean hasVitalMetrics() {
        return totalVitalRecords > 0;
    }

    public String getVitalMetricsSummary() {
        if (totalVitalRecords == 0) {
            return "No vital metrics data available";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Based on ").append(totalVitalRecords).append(" vital records:\n");

        if (averageHeartRate > 0) {
            summary.append("• Avg Heart Rate: ").append(averageHeartRate).append(" bpm\n");
        }
        if (averageBloodSugar > 0) {
            summary.append("• Avg Blood Sugar: ").append(averageBloodSugar).append(" mg/dL\n");
        }
        if (averageTemperature > 0) {
            summary.append("• Avg Temperature: ").append(String.format("%.1f", averageTemperature)).append(" °C\n");
        }
        if (averageOxygenSaturation > 0) {
            summary.append("• Avg Oxygen Sat: ").append(averageOxygenSaturation).append(" %");
        }

        return summary.toString();
    }
}