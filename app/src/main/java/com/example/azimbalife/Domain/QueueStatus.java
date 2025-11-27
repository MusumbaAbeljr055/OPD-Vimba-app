package com.example.azimbalife.Domain;

import java.io.Serializable;

public class QueueStatus implements Serializable {
    private String currentToken;
    private String status;
    private String lastUpdated;

    public QueueStatus() {}

    public QueueStatus(String currentToken, String status, String lastUpdated) {
        this.currentToken = currentToken;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }

    public String getCurrentToken() { return currentToken; }
    public void setCurrentToken(String currentToken) { this.currentToken = currentToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}