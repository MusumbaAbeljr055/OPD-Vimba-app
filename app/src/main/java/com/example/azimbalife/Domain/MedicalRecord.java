package com.example.azimbalife.Domain;

import java.io.Serializable;

public class MedicalRecord implements Serializable {
    private String recordId;
    private String patientUsername;
    private String title;
    private String recordType; // prescription, diagnosis, lab_result, vaccination, surgery
    private String date;
    private String timestamp; // Added missing timestamp field
    private String doctorName;
    private String hospitalName;
    private String diagnosisText; // CHANGED: Renamed from diagnosis to avoid conflicts
    private String medications;
    private String dosage;
    private String instructions;
    private String treatment;
    private String followUpDate;
    private String status; // active, completed, ongoing
    private String urgency; // normal, urgent, emergency
    private String description;
    private String fileUrl; // PDF/document URL
    private String fileName;
    private String fileSize;
    private boolean downloaded;
    private boolean shared;
    private String createdAt;
    private String updatedAt;
    private String labResults;
    private String vitalSigns;
    private String allergies;
    private String notes;
    private String bloodType; // Added bloodType field

    // Boolean fields for type checking - ADDED to avoid getter conflicts
    private boolean isPrescription;
    private boolean isDiagnosis;
    private boolean isVaccination;
    private boolean isLabResult;
    private boolean isSurgery;
    private boolean isVitalMetrics;

    // Default constructor
    public MedicalRecord() {
        this.downloaded = false;
        this.shared = false;
        this.createdAt = String.valueOf(System.currentTimeMillis());

        // Initialize boolean fields
        this.isPrescription = false;
        this.isDiagnosis = false;
        this.isVaccination = false;
        this.isLabResult = false;
        this.isSurgery = false;
        this.isVitalMetrics = false;
    }

    // Constructor for diagnosis
    public MedicalRecord(String recordId, String patientUsername, String title,
                         String doctorName, String diagnosisText, String treatment) {
        this();
        this.recordId = recordId;
        this.patientUsername = patientUsername;
        this.title = title;
        this.recordType = "diagnosis";
        this.doctorName = doctorName;
        this.diagnosisText = diagnosisText; // CHANGED: Use diagnosisText
        this.treatment = treatment;
        this.date = new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date());
        this.timestamp = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date());
        this.isDiagnosis = true; // SET boolean field
    }

    // Getters and Setters
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getPatientUsername() { return patientUsername; }
    public void setPatientUsername(String patientUsername) { this.patientUsername = patientUsername; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) {
        this.recordType = recordType;
        // Update boolean fields when recordType is set
        updateBooleanFields();
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    // CHANGED: Use diagnosisText instead of diagnosis to avoid conflicts
    public String getDiagnosisText() { return diagnosisText; }
    public void setDiagnosisText(String diagnosisText) {
        this.diagnosisText = diagnosisText;
        if (diagnosisText != null && !diagnosisText.isEmpty()) {
            this.isDiagnosis = true;
        }
    }

    public String getMedications() { return medications; }
    public void setMedications(String medications) {
        this.medications = medications;
        if (medications != null && !medications.isEmpty()) {
            this.isPrescription = true;
        }
    }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }

    public String getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(String followUpDate) { this.followUpDate = followUpDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileSize() { return fileSize; }
    public void setFileSize(String fileSize) { this.fileSize = fileSize; }

    public boolean isDownloaded() { return downloaded; }
    public void setDownloaded(boolean downloaded) { this.downloaded = downloaded; }

    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getLabResults() { return labResults; }
    public void setLabResults(String labResults) {
        this.labResults = labResults;
        if (labResults != null && !labResults.isEmpty()) {
            this.isLabResult = true;
        }
    }

    public String getVitalSigns() { return vitalSigns; }
    public void setVitalSigns(String vitalSigns) { this.vitalSigns = vitalSigns; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    // NEW: Boolean field getters and setters
    public boolean getIsPrescription() { return isPrescription; }
    public void setIsPrescription(boolean isPrescription) { this.isPrescription = isPrescription; }

    public boolean getIsDiagnosis() { return isDiagnosis; }
    public void setIsDiagnosis(boolean isDiagnosis) { this.isDiagnosis = isDiagnosis; }

    public boolean getIsVaccination() { return isVaccination; }
    public void setIsVaccination(boolean isVaccination) { this.isVaccination = isVaccination; }

    public boolean getIsLabResult() { return isLabResult; }
    public void setIsLabResult(boolean isLabResult) { this.isLabResult = isLabResult; }

    public boolean getIsSurgery() { return isSurgery; }
    public void setIsSurgery(boolean isSurgery) { this.isSurgery = isSurgery; }

    public boolean getIsVitalMetrics() { return isVitalMetrics; }
    public void setIsVitalMetrics(boolean isVitalMetrics) { this.isVitalMetrics = isVitalMetrics; }

    // Helper methods - UPDATED to use boolean fields
    public boolean checkIsPrescription() {
        return isPrescription || "prescription".equalsIgnoreCase(recordType) || (medications != null && !medications.isEmpty());
    }

    public boolean checkIsDiagnosis() {
        return isDiagnosis || "diagnosis".equalsIgnoreCase(recordType) || (diagnosisText != null && !diagnosisText.isEmpty());
    }

    public boolean checkIsVaccination() {
        return isVaccination || "vaccination".equalsIgnoreCase(recordType);
    }

    public boolean checkIsLabResult() {
        return isLabResult || "lab_result".equalsIgnoreCase(recordType) || (labResults != null && !labResults.isEmpty());
    }

    public boolean checkIsSurgery() {
        return isSurgery || "surgery".equalsIgnoreCase(recordType);
    }

    public boolean checkIsVitalMetrics() {
        return isVitalMetrics || "vital_metrics".equalsIgnoreCase(recordType);
    }

    public boolean hasFile() {
        return fileUrl != null && !fileUrl.isEmpty();
    }

    public boolean canBeShared() {
        return !isShared(); // Prevent multiple shares if needed
    }

    public String getFormattedRecordType() {
        if (recordType == null) return "Unknown";
        switch (recordType.toLowerCase()) {
            case "prescription": return "Prescription";
            case "diagnosis": return "Diagnosis Report";
            case "lab_result": return "Lab Results";
            case "vaccination": return "Vaccination Record";
            case "surgery": return "Surgery Report";
            case "vital_metrics": return "Vital Signs";
            default: return recordType;
        }
    }

    // Additional helper methods for vital metrics conversion
    public String getFormattedDateWithTime() {
        if (timestamp != null && !timestamp.isEmpty()) {
            return timestamp;
        } else if (date != null && !date.isEmpty()) {
            return date + " (Date only)";
        }
        return "Date not available";
    }

    // Method to check if record has vital signs data
    public boolean hasVitalSignsData() {
        return vitalSigns != null && !vitalSigns.isEmpty();
    }

    // Method to check if record has lab results
    public boolean hasLabResultsData() {
        return labResults != null && !labResults.isEmpty();
    }

    // Method to check if record has allergies information
    public boolean hasAllergiesData() {
        return allergies != null && !allergies.isEmpty();
    }

    // Method to get urgency color (for UI purposes)
    public String getUrgencyColor() {
        if (urgency == null) return "normal";
        switch (urgency.toLowerCase()) {
            case "emergency": return "red";
            case "urgent": return "orange";
            case "normal": return "green";
            default: return "normal";
        }
    }

    // Method to get status color (for UI purposes)
    public String getStatusColor() {
        if (status == null) return "gray";
        switch (status.toLowerCase()) {
            case "active": return "blue";
            case "completed": return "green";
            case "ongoing": return "orange";
            default: return "gray";
        }
    }

    // NEW: Helper method to update boolean fields based on recordType
    private void updateBooleanFields() {
        // Reset all boolean fields
        this.isPrescription = false;
        this.isDiagnosis = false;
        this.isVaccination = false;
        this.isLabResult = false;
        this.isSurgery = false;
        this.isVitalMetrics = false;

        // Set the appropriate boolean field based on recordType
        if (recordType != null) {
            switch (recordType.toLowerCase()) {
                case "prescription":
                    this.isPrescription = true;
                    break;
                case "diagnosis":
                    this.isDiagnosis = true;
                    break;
                case "vaccination":
                    this.isVaccination = true;
                    break;
                case "lab_result":
                    this.isLabResult = true;
                    break;
                case "surgery":
                    this.isSurgery = true;
                    break;
                case "vital_metrics":
                    this.isVitalMetrics = true;
                    break;
            }
        }
    }

    // NEW: Method to set record type and update boolean fields
    public void setRecordTypeWithUpdate(String recordType) {
        this.recordType = recordType;
        updateBooleanFields();
    }

    // NEW: Method to check if this is a new record (not saved to Firebase yet)
    public boolean isNewRecord() {
        return recordId == null || recordId.isEmpty() || recordId.startsWith("temp_");
    }

    // NEW: Method to generate a temporary ID for new records
    public void generateTempId() {
        if (recordId == null || recordId.isEmpty()) {
            this.recordId = "temp_" + System.currentTimeMillis();
        }
    }

    // NEW: Method to validate required fields
    public boolean isValid() {
        return patientUsername != null && !patientUsername.isEmpty() &&
                title != null && !title.isEmpty() &&
                recordType != null && !recordType.isEmpty() &&
                date != null && !date.isEmpty();
    }

    @Override
    public String toString() {
        return "MedicalRecord{" +
                "recordId='" + recordId + '\'' +
                ", title='" + title + '\'' +
                ", recordType='" + recordType + '\'' +
                ", date='" + date + '\'' +
                ", doctorName='" + doctorName + '\'' +
                ", isDiagnosis=" + isDiagnosis +
                ", isPrescription=" + isPrescription +
                '}';
    }
}