package com.example.azimbalife.Domain;

public class Notification {
    private String notificationId;
    private String patientId;
    private String patientName;
    private String appointmentId;
    private String title;
    private String message;
    private boolean isRead;
    private long timestamp;
    private String type;
    private String department;
    private String healthWorkerName;
    private String tokenNumber;
    private String allocatedTime;
    private String appointmentDate;

    public Notification() {
        // Default constructor required for Firebase
    }

    public Notification(String notificationId, String patientId, String patientName,
                        String appointmentId, String title, String message,
                        boolean isRead, long timestamp, String type) {
        this.notificationId = notificationId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.timestamp = timestamp;
        this.type = type;
    }

    // Getters and setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getHealthWorkerName() { return healthWorkerName; }
    public void setHealthWorkerName(String healthWorkerName) { this.healthWorkerName = healthWorkerName; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getAllocatedTime() { return allocatedTime; }
    public void setAllocatedTime(String allocatedTime) { this.allocatedTime = allocatedTime; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }
}