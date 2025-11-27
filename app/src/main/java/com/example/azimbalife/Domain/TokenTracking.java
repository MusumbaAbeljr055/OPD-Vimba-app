package com.example.azimbalife.Domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TokenTracking implements Serializable {
    private String tokenId;
    private String patient;
    private String doctor;
    private String specialty;
    private String timestamp;
    private Map<String, Stage> stages;
    private String currentStage;
    private String overallStatus;

    // NEW: Queue position fields
    private int queuePosition;
    private int totalInQueue;
    private boolean isNextInQueue;
    private String estimatedWaitTime;
    private String nextCallEstimate;

    public TokenTracking() {
        stages = new HashMap<>();
        stages.put("registration", new Stage("completed", null));
        stages.put("triage", new Stage("waiting", null));
        stages.put("consultation", new Stage("waiting", null));
        stages.put("pharmacy", new Stage("waiting", null));
        currentStage = "registration";
        overallStatus = "in_progress";

        // Initialize queue fields
        queuePosition = 0;
        totalInQueue = 0;
        isNextInQueue = false;
        estimatedWaitTime = "Calculating...";
        nextCallEstimate = "Not available";
    }

    public static class Stage implements Serializable {
        private String status;
        private String time;

        public Stage() {}

        public Stage(String status, String time) {
            this.status = status;
            this.time = time;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }

    // Getters and setters for existing fields
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
    public String getPatient() { return patient; }
    public void setPatient(String patient) { this.patient = patient; }
    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public Map<String, Stage> getStages() { return stages; }
    public void setStages(Map<String, Stage> stages) { this.stages = stages; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    // NEW: Getters and setters for queue fields
    public int getQueuePosition() { return queuePosition; }
    public void setQueuePosition(int queuePosition) { this.queuePosition = queuePosition; }
    public int getTotalInQueue() { return totalInQueue; }
    public void setTotalInQueue(int totalInQueue) { this.totalInQueue = totalInQueue; }
    public boolean isNextInQueue() { return isNextInQueue; }
    public void setNextInQueue(boolean nextInQueue) { isNextInQueue = nextInQueue; }
    public String getEstimatedWaitTime() { return estimatedWaitTime; }
    public void setEstimatedWaitTime(String estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }
    public String getNextCallEstimate() { return nextCallEstimate; }
    public void setNextCallEstimate(String nextCallEstimate) { this.nextCallEstimate = nextCallEstimate; }
}