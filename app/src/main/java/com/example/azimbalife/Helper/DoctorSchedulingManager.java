package com.example.azimbalife.Helper;

import com.example.azimbalife.Domain.Doctor;
import com.example.azimbalife.Domain.TimeSlot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DoctorSchedulingManager {

    private static final int MAX_PATIENTS_PER_DOCTOR = 20;
    private static final int EMERGENCY_SLOTS_PER_DOCTOR = 5;
    private static final int CONSULTATION_DURATION = 30; // minutes

    public static List<Doctor> findAvailableDoctors(List<Doctor> allDoctors, String department,
                                                    boolean isEmergency, String preferredDate) {
        List<Doctor> availableDoctors = new ArrayList<>();

        for (Doctor doctor : allDoctors) {
            if (isSuitableDoctor(doctor, department, isEmergency, preferredDate)) {
                availableDoctors.add(doctor);
            }
        }

        // Sort by availability (more available slots first)
        availableDoctors.sort((d1, d2) -> {
            if (isEmergency) {
                return Integer.compare(d2.getAvailableEmergencySlots(), d1.getAvailableEmergencySlots());
            } else {
                return Integer.compare(d2.getAvailableSlots(), d1.getAvailableSlots());
            }
        });

        return availableDoctors;
    }

    private static boolean isSuitableDoctor(Doctor doctor, String department,
                                            boolean isEmergency, String preferredDate) {
        // Check department match
        if (!doctor.getDepartment().equalsIgnoreCase(department)) {
            return false;
        }

        // Check if doctor works on the preferred day
        String dayOfWeek = getDayOfWeekFromDate(preferredDate);
        if (!doctor.isWorkingToday(dayOfWeek)) {
            return false;
        }

        // Check availability based on emergency status
        if (isEmergency) {
            return doctor.canAcceptEmergency();
        } else {
            return doctor.canAcceptMorePatients();
        }
    }

    public static String calculateBestTimeSlot(Doctor doctor, boolean isEmergency) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        if (isEmergency) {
            // For emergencies, recommend immediate or nearest possible time
            if (currentHour < 17) { // Before 5 PM
                return "Within 1 hour - Emergency Priority";
            } else {
                return "First available slot tomorrow - Emergency";
            }
        } else {
            // For normal appointments, recommend based on doctor's availability
            int availableSlots = doctor.getAvailableSlots();

            if (availableSlots > 10) {
                return "Morning (9:00 AM - 12:00 PM)";
            } else if (availableSlots > 5) {
                return "Afternoon (1:00 PM - 4:00 PM)";
            } else if (availableSlots > 2) {
                return "Late Afternoon (4:00 PM - 6:00 PM)";
            } else {
                return "Evening (6:00 PM - 8:00 PM)";
            }
        }
    }

    public static String getDayOfWeekFromDate(String date) {
        try {
            String[] parts = date.split("-");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1; // Calendar months are 0-based
            int year = Integer.parseInt(parts[2]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);

            return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()).toLowerCase();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static List<TimeSlot> generateTimeSlotsForDoctor(Doctor doctor, String date) {
        List<TimeSlot> timeSlots = new ArrayList<>();
        String dayOfWeek = getDayOfWeekFromDate(date);

        // Only generate slots if doctor works on this day
        if (!doctor.isWorkingToday(dayOfWeek)) {
            return timeSlots;
        }

        // Generate time slots from 8 AM to 6 PM
        for (int hour = 8; hour < 18; hour++) {
            for (int minute = 0; minute < 60; minute += CONSULTATION_DURATION) {
                String startTime = String.format("%02d:%02d", hour, minute);
                String endTime = String.format("%02d:%02d", hour, minute + CONSULTATION_DURATION);

                TimeSlot slot = new TimeSlot(startTime, endTime, dayOfWeek);
                timeSlots.add(slot);
            }
        }

        return timeSlots;
    }

    public static boolean canDoctorAcceptAppointment(Doctor doctor, boolean isEmergency) {
        if (isEmergency) {
            return doctor.canAcceptEmergency();
        } else {
            return doctor.canAcceptMorePatients();
        }
    }

    public static void updateDoctorAfterAppointment(Doctor doctor, boolean isEmergency) {
        if (isEmergency) {
            doctor.incrementEmergencyCount();
        } else {
            doctor.incrementPatientCount();
        }
    }
}