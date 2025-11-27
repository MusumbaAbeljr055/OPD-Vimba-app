package com.example.azimbalife.Domain;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Appointment implements Serializable {
    private String appointmentId;
    private String patientUsername;
    private String patientName;
    private String department;
    private String preferredDate;
    private String preferredTime;
    private String visitReason;
    private String urgencyLevel;
    private String status;
    private String tokenNumber;
    private String allocatedTime;
    private String confirmedDate;
    private String createdAt;
    private String notes;
    private String doctorName;
    private String doctorId;
    private String appointmentType;
    private String paymentStatus;
    private String appointmentDuration;
    private String location;
    private String contactPhone;
    private String symptoms;

    // New fields for priority system
    private int priorityWeight; // 1=Normal, 2=Urgent, 3=Emergency
    private boolean isEmergency;
    private String priorityColor; // For UI display

    // Notification fields
    private String notificationTitle;
    private String notificationMessage;
    private boolean notificationRead;
    private long notificationTimestamp;
    private String approvedBy;
    private String approvedAt;
    private String scheduledAt;

    // Doctor assignment fields
    private String assignedDoctorId;
    private String assignedDoctorName;
    private String assignedDoctorSpecialization;
    private boolean doctorAssigned;
    private String doctorAssignmentTime;

    // Default constructor required for Firebase
    public Appointment() {
        this.status = "pending";
        this.urgencyLevel = "normal";
        this.paymentStatus = "pending";
        this.createdAt = String.valueOf(System.currentTimeMillis());
        this.notificationRead = false;
        this.doctorAssigned = false;
        this.priorityWeight = 1;
        this.isEmergency = false;
        this.priorityColor = "#4CAF50"; // Default green for normal
    }

    // Constructor with basic required fields
    public Appointment(String appointmentId, String patientUsername, String patientName,
                       String department, String preferredDate, String visitReason) {
        this();
        this.appointmentId = appointmentId;
        this.patientUsername = patientUsername;
        this.patientName = patientName;
        this.department = department;
        this.preferredDate = preferredDate;
        this.visitReason = visitReason;
    }

    // Constructor for quick booking
    public Appointment(String appointmentId, String patientUsername, String patientName,
                       String department, String preferredDate, String preferredTime,
                       String visitReason, String urgencyLevel) {
        this();
        this.appointmentId = appointmentId;
        this.patientUsername = patientUsername;
        this.patientName = patientName;
        this.department = department;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.visitReason = visitReason;
        setUrgencyLevel(urgencyLevel); // Use setter to calculate priority
    }

    // Full constructor
    public Appointment(String appointmentId, String patientUsername, String patientName,
                       String department, String preferredDate, String preferredTime,
                       String visitReason, String urgencyLevel, String status,
                       String tokenNumber, String allocatedTime, String contactPhone) {
        this();
        this.appointmentId = appointmentId;
        this.patientUsername = patientUsername;
        this.patientName = patientName;
        this.department = department;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.visitReason = visitReason;
        setUrgencyLevel(urgencyLevel); // Use setter to calculate priority
        this.status = status != null ? status : "pending";
        this.tokenNumber = tokenNumber;
        this.allocatedTime = allocatedTime;
        this.contactPhone = contactPhone;
    }

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientUsername() { return patientUsername; }
    public void setPatientUsername(String patientUsername) { this.patientUsername = patientUsername; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPreferredDate() { return preferredDate; }
    public void setPreferredDate(String preferredDate) { this.preferredDate = preferredDate; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    public String getVisitReason() { return visitReason; }
    public void setVisitReason(String visitReason) { this.visitReason = visitReason; }

    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
        calculatePriority(); // Recalculate priority when urgency changes
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getAllocatedTime() { return allocatedTime; }
    public void setAllocatedTime(String allocatedTime) { this.allocatedTime = allocatedTime; }

    public String getConfirmedDate() { return confirmedDate; }
    public void setConfirmedDate(String confirmedDate) { this.confirmedDate = confirmedDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getAppointmentDuration() { return appointmentDuration; }
    public void setAppointmentDuration(String appointmentDuration) { this.appointmentDuration = appointmentDuration; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    // New priority fields getters and setters
    public int getPriorityWeight() { return priorityWeight; }
    public void setPriorityWeight(int priorityWeight) { this.priorityWeight = priorityWeight; }

    public boolean getIsEmergency() { return isEmergency; }
    public void setIsEmergency(boolean isEmergency) { this.isEmergency = isEmergency; }

    public String getPriorityColor() { return priorityColor; }
    public void setPriorityColor(String priorityColor) { this.priorityColor = priorityColor; }

    // Notification getters and setters
    public String getNotificationTitle() { return notificationTitle; }
    public void setNotificationTitle(String notificationTitle) { this.notificationTitle = notificationTitle; }

    public String getNotificationMessage() { return notificationMessage; }
    public void setNotificationMessage(String notificationMessage) { this.notificationMessage = notificationMessage; }

    public boolean isNotificationRead() { return notificationRead; }
    public void setNotificationRead(boolean notificationRead) { this.notificationRead = notificationRead; }

    public long getNotificationTimestamp() { return notificationTimestamp; }
    public void setNotificationTimestamp(long notificationTimestamp) { this.notificationTimestamp = notificationTimestamp; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedAt() { return approvedAt; }
    public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    // Doctor assignment getters and setters
    public String getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(String assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public String getAssignedDoctorSpecialization() { return assignedDoctorSpecialization; }
    public void setAssignedDoctorSpecialization(String assignedDoctorSpecialization) { this.assignedDoctorSpecialization = assignedDoctorSpecialization; }

    public boolean isDoctorAssigned() { return doctorAssigned; }
    public void setDoctorAssigned(boolean doctorAssigned) { this.doctorAssigned = doctorAssigned; }

    public String getDoctorAssignmentTime() { return doctorAssignmentTime; }
    public void setDoctorAssignmentTime(String doctorAssignmentTime) { this.doctorAssignmentTime = doctorAssignmentTime; }

    // === MISSING METHODS - ADDED BELOW ===

    // Emergency-related methods
    public void setEmergency(boolean emergency) {
        this.isEmergency = emergency;
        if (emergency) {
            setUrgencyLevel("emergency");
        }
    }

    public void setRequiresImmediateAttention(boolean requiresImmediateAttention) {
        if (requiresImmediateAttention) {
            setUrgencyLevel("emergency");
            setIsEmergency(true);
            setPriorityWeight(3);
            setPriorityColor("#FF4444");
        }
    }

    public void setEmergencyTimestamp(long emergencyTimestamp) {
        this.notificationTimestamp = emergencyTimestamp;
    }

    public long getEmergencyTimestamp() {
        return notificationTimestamp;
    }

    // Additional utility methods
    public void markAsEmergency(String emergencyDetails) {
        setIsEmergency(true);
        setUrgencyLevel("emergency");
        setPriorityWeight(3);
        setPriorityColor("#FF4444");
        setNotificationTimestamp(System.currentTimeMillis());

        if (emergencyDetails != null && !emergencyDetails.trim().isEmpty()) {
            setSymptoms("EMERGENCY: " + emergencyDetails);
            setNotes("Emergency Case: " + emergencyDetails);
        }
    }

    public void setImmediateAttentionRequired(boolean immediateAttention) {
        setRequiresImmediateAttention(immediateAttention);
    }

    public boolean requiresImmediateAttention() {
        return getIsEmergency() || "emergency".equalsIgnoreCase(getUrgencyLevel());
    }

    // Timestamp conversion helpers
    public void setCreatedAtTimestamp(long timestamp) {
        this.createdAt = String.valueOf(timestamp);
    }

    public long getCreatedAtTimestamp() {
        try {
            return Long.parseLong(createdAt);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    // Status update helpers
    public void approveAppointment(String approvedBy, String approvedAt) {
        this.status = "approved";
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    public void confirmAppointment(String allocatedTime, String tokenNumber) {
        this.status = "confirmed";
        this.allocatedTime = allocatedTime;
        this.tokenNumber = tokenNumber;
    }

    public void completeAppointment() {
        this.status = "completed";
    }

    public void cancelAppointment(String reason) {
        this.status = "cancelled";
        if (reason != null) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "Cancellation reason: " + reason;
        }
    }

    // Doctor assignment helper
    public void assignDoctor(String doctorId, String doctorName, String specialization) {
        this.assignedDoctorId = doctorId;
        this.assignedDoctorName = doctorName;
        this.assignedDoctorSpecialization = specialization;
        this.doctorAssigned = true;
        this.doctorAssignmentTime = String.valueOf(System.currentTimeMillis());
    }

    // Notification helpers
    public void sendNotification(String title, String message) {
        this.notificationTitle = title;
        this.notificationMessage = message;
        this.notificationTimestamp = System.currentTimeMillis();
        this.notificationRead = false;
    }

    public void markNotificationAsRead() {
        this.notificationRead = true;
    }

    // Priority management
    public void upgradeToEmergency() {
        setUrgencyLevel("emergency");
        setIsEmergency(true);
        setPriorityWeight(3);
        setPriorityColor("#FF4444");
        setNotificationTimestamp(System.currentTimeMillis());
    }

    public void downgradeToUrgent() {
        setUrgencyLevel("urgent");
        setIsEmergency(false);
        setPriorityWeight(2);
        setPriorityColor("#FF9800");
    }

    public void setToNormalPriority() {
        setUrgencyLevel("normal");
        setIsEmergency(false);
        setPriorityWeight(1);
        setPriorityColor("#4CAF50");
    }

    // Date and time validation
    public boolean isToday() {
        if (preferredDate == null) return false;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return preferredDate.equals(today);
    }

    public boolean isUpcoming() {
        if (preferredDate == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date appointmentDate = sdf.parse(preferredDate);
            Date today = new Date();
            return appointmentDate != null && !appointmentDate.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    // Payment helpers
    public void markAsPaid() {
        this.paymentStatus = "paid";
    }

    public void markPaymentPending() {
        this.paymentStatus = "pending";
    }

    // Emergency-specific methods
    public boolean isCriticalEmergency() {
        return getIsEmergency() && getPriorityWeight() == 3;
    }

    public boolean needsImmediateDoctorAttention() {
        return getIsEmergency() && !isCompleted() && !isCancelled();
    }

    // Copy constructor helper
    public void copyFrom(Appointment other) {
        this.appointmentId = other.appointmentId;
        this.patientUsername = other.patientUsername;
        this.patientName = other.patientName;
        this.department = other.department;
        this.preferredDate = other.preferredDate;
        this.preferredTime = other.preferredTime;
        this.visitReason = other.visitReason;
        this.urgencyLevel = other.urgencyLevel;
        this.status = other.status;
        this.tokenNumber = other.tokenNumber;
        this.allocatedTime = other.allocatedTime;
        this.confirmedDate = other.confirmedDate;
        this.createdAt = other.createdAt;
        this.notes = other.notes;
        this.doctorName = other.doctorName;
        this.doctorId = other.doctorId;
        this.appointmentType = other.appointmentType;
        this.paymentStatus = other.paymentStatus;
        this.appointmentDuration = other.appointmentDuration;
        this.location = other.location;
        this.contactPhone = other.contactPhone;
        this.symptoms = other.symptoms;
        this.priorityWeight = other.priorityWeight;
        this.isEmergency = other.isEmergency;
        this.priorityColor = other.priorityColor;
        this.notificationTitle = other.notificationTitle;
        this.notificationMessage = other.notificationMessage;
        this.notificationRead = other.notificationRead;
        this.notificationTimestamp = other.notificationTimestamp;
        this.approvedBy = other.approvedBy;
        this.approvedAt = other.approvedAt;
        this.scheduledAt = other.scheduledAt;
        this.assignedDoctorId = other.assignedDoctorId;
        this.assignedDoctorName = other.assignedDoctorName;
        this.assignedDoctorSpecialization = other.assignedDoctorSpecialization;
        this.doctorAssigned = other.doctorAssigned;
        this.doctorAssignmentTime = other.doctorAssignmentTime;
    }

    // === EXISTING METHODS ===

    // Priority calculation method
    private void calculatePriority() {
        if (urgencyLevel == null) {
            this.priorityWeight = 1;
            this.isEmergency = false;
            this.priorityColor = "#4CAF50"; // Green for normal
            return;
        }

        switch (urgencyLevel.toLowerCase()) {
            case "emergency":
                this.priorityWeight = 3;
                this.isEmergency = true;
                this.priorityColor = "#FF4444"; // Red for emergency
                break;
            case "urgent":
                this.priorityWeight = 2;
                this.isEmergency = false;
                this.priorityColor = "#FF9800"; // Orange for urgent
                break;
            case "normal":
            default:
                this.priorityWeight = 1;
                this.isEmergency = false;
                this.priorityColor = "#4CAF50"; // Green for normal
                break;
        }
    }

    // Helper methods for status checks
    public boolean isPending() { return "pending".equalsIgnoreCase(status); }
    public boolean isConfirmed() { return "confirmed".equalsIgnoreCase(status); }
    public boolean isCompleted() { return "completed".equalsIgnoreCase(status); }
    public boolean isCancelled() { return "cancelled".equalsIgnoreCase(status); }
    public boolean isApproved() { return "approved".equalsIgnoreCase(status); }
    public boolean isRejected() { return "rejected".equalsIgnoreCase(status); }
    public boolean isScheduled() { return "scheduled".equalsIgnoreCase(status); }

    // Helper methods for urgency checks
    public boolean isEmergency() { return "emergency".equalsIgnoreCase(urgencyLevel); }
    public boolean isUrgent() { return "urgent".equalsIgnoreCase(urgencyLevel); }
    public boolean isNormal() { return "normal".equalsIgnoreCase(urgencyLevel); }

    // Priority comparison methods
    public boolean isHigherPriorityThan(Appointment other) {
        return this.priorityWeight > other.priorityWeight;
    }

    public boolean isSamePriorityAs(Appointment other) {
        return this.priorityWeight == other.priorityWeight;
    }

    // Token and time helpers
    public boolean hasToken() { return tokenNumber != null && !tokenNumber.trim().isEmpty(); }
    public boolean hasAllocatedTime() { return allocatedTime != null && !allocatedTime.trim().isEmpty(); }
    public boolean hasPreferredTime() { return preferredTime != null && !preferredTime.trim().isEmpty(); }
    public boolean hasDoctorAssigned() { return assignedDoctorId != null && !assignedDoctorId.trim().isEmpty(); }
    public boolean isPaymentPending() { return "pending".equalsIgnoreCase(paymentStatus); }
    public boolean isPaymentPaid() { return "paid".equalsIgnoreCase(paymentStatus); }

    // Notification helpers
    public boolean hasNotification() { return notificationTitle != null && !notificationTitle.trim().isEmpty(); }
    public boolean isNotificationUnread() { return hasNotification() && !notificationRead; }
    public boolean hasScheduledAt() { return scheduledAt != null && !scheduledAt.trim().isEmpty(); }

    // Validation methods
    public boolean isValid() {
        return appointmentId != null && !appointmentId.trim().isEmpty() &&
                patientUsername != null && !patientUsername.trim().isEmpty() &&
                department != null && !department.trim().isEmpty() &&
                preferredDate != null && !preferredDate.trim().isEmpty() &&
                visitReason != null && !visitReason.trim().isEmpty();
    }

    public boolean canBeCancelled() { return isPending() || isConfirmed() || isScheduled(); }
    public boolean canBeRescheduled() { return isPending() || isConfirmed() || isScheduled(); }

    // Formatting methods
    public String getFormattedStatus() {
        if (status == null) return "Unknown";
        switch (status.toLowerCase()) {
            case "pending": return "Pending Review";
            case "confirmed": return "Confirmed";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            case "approved": return "Approved";
            case "rejected": return "Rejected";
            case "scheduled": return "Scheduled";
            default: return status;
        }
    }

    public String getFormattedUrgency() {
        if (urgencyLevel == null) return "Normal";
        switch (urgencyLevel.toLowerCase()) {
            case "emergency": return "🚨 EMERGENCY";
            case "urgent": return "⚠️ URGENT";
            case "normal": return "⏱ NORMAL";
            case "routine": return "Routine";
            default: return urgencyLevel;
        }
    }

    public String getUrgencyIcon() {
        if (urgencyLevel == null) return "⏱";
        switch (urgencyLevel.toLowerCase()) {
            case "emergency": return "🚨";
            case "urgent": return "⚠️";
            case "normal": return "⏱";
            default: return "⏱";
        }
    }

    public String getDisplayTime() {
        if (hasAllocatedTime()) {
            return allocatedTime;
        } else if (hasPreferredTime()) {
            return preferredTime + " (Preferred)";
        } else {
            return "Time not allocated";
        }
    }

    public String getDisplayToken() {
        if (hasToken()) {
            return "Token: " + tokenNumber;
        } else {
            return "Token: Not assigned";
        }
    }

    public String getDisplayDoctor() {
        if (hasDoctorAssigned()) {
            return "Dr. " + assignedDoctorName + (assignedDoctorSpecialization != null ? " - " + assignedDoctorSpecialization : "");
        } else {
            return "Doctor: Not assigned";
        }
    }

    // Firebase compatibility method
    public String getUsername() { return patientUsername; }
    public void setUsername(String username) { this.patientUsername = username; }

    @Override
    public String toString() {
        return "Appointment{" +
                "appointmentId='" + appointmentId + '\'' +
                ", patientUsername='" + patientUsername + '\'' +
                ", patientName='" + patientName + '\'' +
                ", department='" + department + '\'' +
                ", preferredDate='" + preferredDate + '\'' +
                ", status='" + status + '\'' +
                ", urgencyLevel='" + urgencyLevel + '\'' +
                ", priorityWeight=" + priorityWeight +
                ", isEmergency=" + isEmergency +
                ", tokenNumber='" + tokenNumber + '\'' +
                ", scheduledAt='" + scheduledAt + '\'' +
                ", assignedDoctorName='" + assignedDoctorName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        return appointmentId != null && appointmentId.equals(that.appointmentId);
    }

    @Override
    public int hashCode() {
        return appointmentId != null ? appointmentId.hashCode() : 0;
    }
}