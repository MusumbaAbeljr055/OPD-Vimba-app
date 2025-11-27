package com.example.azimbalife.Domain;

import java.util.ArrayList;
import java.util.List;

public class Doctor {
    private String doctorId;
    private String name;
    private String specialty;
    private String department;
    private int currentPatientCount;
    private boolean available;
    private int maxPatientsPerDay = 20; // Default maximum patients

    // New fields for enhanced scheduling
    private String email;
    private String phone;
    private String hospital = "Mbarara Hospital";
    private String location = "Mbarara";
    private int consultationDuration = 30; // minutes
    private String status = "available"; // available, busy, offline, fully_booked
    private double rating = 0.0;
    private int yearsOfExperience = 0;
    private List<String> qualifications = new ArrayList<>();
    private String bio;
    private String profileImage;

    // Emergency handling
    private boolean acceptsEmergencyCases = true;
    private int emergencySlotsPerDay = 5;
    private int currentEmergencyCount = 0;

    // Working schedule
    private List<String> workingDays = new ArrayList<>();
    private List<TimeSlot> availableTimeSlots = new ArrayList<>();

    // Default constructor
    public Doctor() {
        // Initialize with common working days
        workingDays.add("monday");
        workingDays.add("tuesday");
        workingDays.add("wednesday");
        workingDays.add("thursday");
        workingDays.add("friday");
    }

    // Constructor for default option
    public Doctor(String doctorId, String name, String specialty, String department) {
        this();
        this.doctorId = doctorId;
        this.name = name;
        this.specialty = specialty;
        this.department = department;
        this.available = true;
    }

    // Enhanced constructor for complete doctor profile
    public Doctor(String doctorId, String name, String specialty, String department,
                  String email, String phone, int maxPatientsPerDay) {
        this(doctorId, name, specialty, department);
        this.email = email;
        this.phone = phone;
        this.maxPatientsPerDay = maxPatientsPerDay;
    }

    // === CORE SCHEDULING METHODS ===

    public boolean canAcceptMorePatients() {
        return available && currentPatientCount < maxPatientsPerDay;
    }

    public boolean canAcceptEmergency() {
        return acceptsEmergencyCases && currentEmergencyCount < emergencySlotsPerDay && available;
    }

    public int getAvailableSlots() {
        return maxPatientsPerDay - currentPatientCount;
    }

    public int getAvailableEmergencySlots() {
        return emergencySlotsPerDay - currentEmergencyCount;
    }

    public void incrementPatientCount() {
        this.currentPatientCount++;
        updateAvailabilityStatus();
    }

    public void incrementEmergencyCount() {
        this.currentEmergencyCount++;
        this.currentPatientCount++; // Emergency also counts as a patient
        updateAvailabilityStatus();
    }

    private void updateAvailabilityStatus() {
        if (this.currentPatientCount >= this.maxPatientsPerDay) {
            this.available = false;
            this.status = "fully_booked";
        }
        if (this.currentEmergencyCount >= this.emergencySlotsPerDay) {
            this.acceptsEmergencyCases = false;
        }
    }

    public void resetDailyCounts() {
        this.currentPatientCount = 0;
        this.currentEmergencyCount = 0;
        this.available = true;
        this.acceptsEmergencyCases = true;
        this.status = "available";
    }

    public boolean isWorkingToday(String day) {
        return workingDays.contains(day.toLowerCase());
    }

    public double getWorkloadPercentage() {
        return (double) currentPatientCount / maxPatientsPerDay * 100;
    }

    public String getWorkloadStatus() {
        double workload = getWorkloadPercentage();
        if (workload >= 90) return "Critical";
        if (workload >= 75) return "High";
        if (workload >= 50) return "Moderate";
        return "Low";
    }

    // === GETTERS AND SETTERS ===

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getCurrentPatientCount() { return currentPatientCount; }
    public void setCurrentPatientCount(int currentPatientCount) {
        this.currentPatientCount = currentPatientCount;
        updateAvailabilityStatus();
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) {
        this.available = available;
        this.status = available ? "available" : "unavailable";
    }

    public int getMaxPatientsPerDay() { return maxPatientsPerDay; }
    public void setMaxPatientsPerDay(int maxPatientsPerDay) {
        this.maxPatientsPerDay = maxPatientsPerDay;
        updateAvailabilityStatus();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getConsultationDuration() { return consultationDuration; }
    public void setConsultationDuration(int consultationDuration) { this.consultationDuration = consultationDuration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public List<String> getQualifications() { return qualifications; }
    public void setQualifications(List<String> qualifications) { this.qualifications = qualifications; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public boolean isAcceptsEmergencyCases() { return acceptsEmergencyCases; }
    public void setAcceptsEmergencyCases(boolean acceptsEmergencyCases) { this.acceptsEmergencyCases = acceptsEmergencyCases; }

    public int getEmergencySlotsPerDay() { return emergencySlotsPerDay; }
    public void setEmergencySlotsPerDay(int emergencySlotsPerDay) { this.emergencySlotsPerDay = emergencySlotsPerDay; }

    public int getCurrentEmergencyCount() { return currentEmergencyCount; }
    public void setCurrentEmergencyCount(int currentEmergencyCount) {
        this.currentEmergencyCount = currentEmergencyCount;
        updateAvailabilityStatus();
    }

    public List<String> getWorkingDays() { return workingDays; }
    public void setWorkingDays(List<String> workingDays) { this.workingDays = workingDays; }

    public List<TimeSlot> getAvailableTimeSlots() { return availableTimeSlots; }
    public void setAvailableTimeSlots(List<TimeSlot> availableTimeSlots) { this.availableTimeSlots = availableTimeSlots; }

    // Compatibility method
    public String getSpecialization() { return specialty; }
    public void setSpecialization(String specialization) { this.specialty = specialization; }

    @Override
    public String toString() {
        return name + " - " + specialty + " (" + department + ") - " +
                (available ? "Available" : "Not Available") +
                " - Patients: " + currentPatientCount + "/" + maxPatientsPerDay +
                " - Emergencies: " + currentEmergencyCount + "/" + emergencySlotsPerDay;
    }

    // Helper method for display in spinners
    public String getDisplayText() {
        return name + " - " + specialty +
                " [" + currentPatientCount + "/" + maxPatientsPerDay + "]";
    }
}