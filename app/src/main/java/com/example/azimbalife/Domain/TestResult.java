package com.example.azimbalife.Domain;

import java.io.Serializable;

public class TestResult implements Serializable {
    private String resultId;
    private String patientUsername;
    private String testType; // Blood Test, X-Ray, MRI, Ultrasound, etc.
    private String testName;
    private String testDate;
    private String resultDate;
    private String labName;
    private String doctorName;
    private String findings;
    private String recommendations;
    private String fileUrl; // PDF report URL
    private String fileName;
    private String status; // Normal, Abnormal, Critical
    private String createdAt;
    private boolean isDownloaded;

    // Default constructor required for Firebase
    public TestResult() {
    }

    public TestResult(String resultId, String patientUsername, String testType,
                      String testName, String testDate, String labName) {
        this.resultId = resultId;
        this.patientUsername = patientUsername;
        this.testType = testType;
        this.testName = testName;
        this.testDate = testDate;
        this.labName = labName;
        this.status = "Normal";
        this.resultDate = testDate;
        this.createdAt = String.valueOf(System.currentTimeMillis());
        this.isDownloaded = false;
    }

    // Getters and Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }

    public String getPatientUsername() { return patientUsername; }
    public void setPatientUsername(String patientUsername) { this.patientUsername = patientUsername; }

    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getTestDate() { return testDate; }
    public void setTestDate(String testDate) { this.testDate = testDate; }

    public String getResultDate() { return resultDate; }
    public void setResultDate(String resultDate) { this.resultDate = resultDate; }

    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isDownloaded() { return isDownloaded; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }

    // Helper methods
    public boolean hasFile() {
        return fileUrl != null && !fileUrl.isEmpty();
    }

    public boolean isAbnormal() {
        return "Abnormal".equalsIgnoreCase(status) || "Critical".equalsIgnoreCase(status);
    }

    public String getStatusColor() {
        switch (status.toLowerCase()) {
            case "normal": return "green";
            case "abnormal": return "orange";
            case "critical": return "red";
            default: return "grey";
        }
    }
}