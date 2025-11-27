package com.example.azimbalife.Activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.azimbalife.Domain.Appointment;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BookAppointmentActivity extends AppCompatActivity {

    private Spinner spinnerDepartment, spinnerTimeSlot, spinnerUrgency;
    private EditText etPreferredDate, etVisitReason;
    private Button btnBookAppointment, btnEmergencyAppointment;
    private LinearLayout layoutEmergencyDetails;
    private EditText etEmergencyDetails, etPatientCondition;
    private CardView cardEmergencyAlert;

    private String username;
    private String patientName;

    private Calendar calendar;
    private SimpleDateFormat dateFormatter;

    // Hospital departments
    private final String[] departments = {
            "Select Department",
            "General Medicine",
            "Pediatrics",
            "Gynecology",
            "Dentistry",
            "Dermatology",
            "Orthopedics",
            "Cardiology",
            "Neurology",
            "Ophthalmology",
            "ENT (Ear, Nose, Throat)",
            "Emergency Department"
    };

    // Time slots based on doctor availability
    private final String[] timeSlots = {
            "Any Available Time",
            "Morning (8:00 AM - 12:00 PM)",
            "Afternoon (12:00 PM - 4:00 PM)",
            "Evening (4:00 PM - 6:00 PM)"
    };

    // Urgency levels with priority weights
    private final String[] urgencyLevels = {
            "Normal",
            "Urgent",
            "Emergency"
    };

    private boolean isEmergencyMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        // Get user data from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupSpinners();
        setupDatePicker();
        setupClickListeners();

        // Load patient name
        loadPatientName();
    }

    private void initializeViews() {
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot);
        spinnerUrgency = findViewById(R.id.spinnerUrgency);
        etPreferredDate = findViewById(R.id.etPreferredDate);
        etVisitReason = findViewById(R.id.etVisitReason);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        btnEmergencyAppointment = findViewById(R.id.btnEmergencyAppointment);
        layoutEmergencyDetails = findViewById(R.id.layoutEmergencyDetails);
        etEmergencyDetails = findViewById(R.id.etEmergencyDetails);
        etPatientCondition = findViewById(R.id.etPatientCondition);
        cardEmergencyAlert = findViewById(R.id.cardEmergencyAlert);

        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        // Hide emergency details initially
        layoutEmergencyDetails.setVisibility(View.GONE);
        cardEmergencyAlert.setVisibility(View.GONE);
    }

    private void setupSpinners() {
        // Department spinner
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, departments);
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // Time slot spinner
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, timeSlots);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeSlot.setAdapter(timeAdapter);

        // Urgency spinner
        ArrayAdapter<String> urgencyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, urgencyLevels);
        urgencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUrgency.setAdapter(urgencyAdapter);
    }

    private void setupDatePicker() {
        etPreferredDate.setOnClickListener(v -> showDatePicker());
        calendar = Calendar.getInstance();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    etPreferredDate.setText(dateFormatter.format(calendar.getTime()));

                    // Check doctor availability for selected date
                    if (!isEmergencyMode) {
                        checkDoctorAvailability();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void checkDoctorAvailability() {
        String selectedDate = etPreferredDate.getText().toString();
        String selectedDepartment = spinnerDepartment.getSelectedItem().toString();

        if (selectedDepartment.equals("Select Department")) {
            return;
        }

        DatabaseReference doctorsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/Doctors");

        doctorsRef.orderByChild("department").equalTo(selectedDepartment)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int availableDoctors = 0;
                        for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                            Boolean isAvailable = doctorSnapshot.child("available").getValue(Boolean.class);
                            Integer currentPatients = doctorSnapshot.child("currentPatientCount").getValue(Integer.class);
                            Integer maxPatients = doctorSnapshot.child("maxPatientsPerDay").getValue(Integer.class);

                            if (isAvailable != null && isAvailable &&
                                    currentPatients != null && maxPatients != null &&
                                    currentPatients < maxPatients) {
                                availableDoctors++;
                            }
                        }

                        if (availableDoctors == 0) {
                            Toast.makeText(BookAppointmentActivity.this,
                                    "No doctors available in " + selectedDepartment + " on " + selectedDate +
                                            ". Please choose another date.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(BookAppointmentActivity.this,
                                    availableDoctors + " doctors available on " + selectedDate,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("AVAILABILITY_CHECK", "Error checking doctor availability: " + error.getMessage());
                    }
                });
    }

    private void setupClickListeners() {
        btnBookAppointment.setOnClickListener(v -> bookAppointment());

        btnEmergencyAppointment.setOnClickListener(v -> toggleEmergencyMode());

        // Listen for urgency level changes
        spinnerUrgency.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedUrgency = spinnerUrgency.getSelectedItem().toString();
                if ("Emergency".equals(selectedUrgency)) {
                    enableEmergencyMode();
                } else {
                    disableEmergencyMode();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void toggleEmergencyMode() {
        isEmergencyMode = !isEmergencyMode;
        if (isEmergencyMode) {
            enableEmergencyMode();
        } else {
            disableEmergencyMode();
        }
    }

    private void enableEmergencyMode() {
        isEmergencyMode = true;
        layoutEmergencyDetails.setVisibility(View.VISIBLE);
        cardEmergencyAlert.setVisibility(View.VISIBLE);
        btnEmergencyAppointment.setText("Cancel Emergency");
        spinnerUrgency.setSelection(2); // Set to Emergency
        spinnerDepartment.setSelection(11); // Set to Emergency Department

        // Set current date and time for emergency
        etPreferredDate.setText(dateFormatter.format(calendar.getTime()));
        spinnerTimeSlot.setSelection(0); // Any Available Time

        Toast.makeText(this, "Emergency mode activated. Please provide emergency details.",
                Toast.LENGTH_LONG).show();
    }

    private void disableEmergencyMode() {
        isEmergencyMode = false;
        layoutEmergencyDetails.setVisibility(View.GONE);
        cardEmergencyAlert.setVisibility(View.GONE);
        btnEmergencyAppointment.setText("🚨 Emergency Appointment");
        etEmergencyDetails.setText("");
        etPatientCondition.setText("");
        spinnerUrgency.setSelection(0); // Reset to Normal
    }

    private void bookAppointment() {
        // Validate inputs
        if (spinnerDepartment.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
            return;
        }

        if (etPreferredDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select preferred date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (etVisitReason.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please describe reason for visit", Toast.LENGTH_SHORT).show();
            return;
        }

        // For emergency, validate emergency details
        if (isEmergencyMode) {
            if (etEmergencyDetails.getText().toString().isEmpty()) {
                etEmergencyDetails.setError("Emergency details are required");
                return;
            }
            if (etPatientCondition.getText().toString().isEmpty()) {
                etPatientCondition.setError("Patient condition is required");
                return;
            }
        }

        // Get form data
        String department = spinnerDepartment.getSelectedItem().toString();
        String preferredDate = etPreferredDate.getText().toString();
        String preferredTime = spinnerTimeSlot.getSelectedItem().toString();
        String visitReason = etVisitReason.getText().toString();
        String urgencyLevel = spinnerUrgency.getSelectedItem().toString();

        // Add emergency details if in emergency mode
        if (isEmergencyMode) {
            visitReason = "EMERGENCY - " + etEmergencyDetails.getText().toString() +
                    " | Condition: " + etPatientCondition.getText().toString() +
                    " | " + visitReason;
        }

        // Calculate priority weight based on urgency
        int priorityWeight = calculatePriorityWeight(urgencyLevel);

        // Create appointment object
        Appointment appointment = new Appointment();
        appointment.setPatientUsername(username);
        appointment.setPatientName(patientName != null ? patientName : "User");
        appointment.setDepartment(department);
        appointment.setPreferredDate(preferredDate);
        appointment.setPreferredTime(preferredTime);
        appointment.setVisitReason(visitReason);
        appointment.setUrgencyLevel(urgencyLevel);
        appointment.setPriorityWeight(priorityWeight);
        appointment.setStatus(isEmergencyMode ? "emergency" : "pending");
        appointment.setCreatedAt(String.valueOf(System.currentTimeMillis()));

        // Use CORRECT method names from Appointment class
        appointment.setIsEmergency(isEmergencyMode); // Correct method name

        // For emergencies, set immediate scheduling using CORRECT methods
        if (isEmergencyMode) {
            appointment.setRequiresImmediateAttention(true); // This method exists in our updated Appointment class
            appointment.setNotificationTimestamp(System.currentTimeMillis()); // Use existing method
        }

        // Save to Firebase
        saveAppointmentToFirebase(appointment);
    }

    private int calculatePriorityWeight(String urgencyLevel) {
        switch (urgencyLevel.toLowerCase()) {
            case "emergency":
                return 3;
            case "urgent":
                return 2;
            case "normal":
            default:
                return 1;
        }
    }

    private void saveAppointmentToFirebase(Appointment appointment) {
        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/AllAppointments");

        String appointmentId = appointmentsRef.push().getKey();

        if (appointmentId == null) {
            Toast.makeText(this, "Failed to create appointment", Toast.LENGTH_SHORT).show();
            return;
        }

        appointment.setAppointmentId(appointmentId);

        Log.d("APPOINTMENT_CREATION", "=== SAVING APPOINTMENT ===");
        Log.d("APPOINTMENT_CREATION", "Appointment ID: " + appointmentId);
        Log.d("APPOINTMENT_CREATION", "Department: " + appointment.getDepartment());
        Log.d("APPOINTMENT_CREATION", "Urgency: " + appointment.getUrgencyLevel());
        Log.d("APPOINTMENT_CREATION", "Priority: " + appointment.getPriorityWeight());
        Log.d("APPOINTMENT_CREATION", "Emergency: " + appointment.getIsEmergency());

        appointmentsRef.child(appointmentId).setValue(appointment)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String message = isEmergencyMode ?
                                "Emergency appointment registered successfully! You will receive immediate attention." :
                                "Appointment booked successfully!";

                        Toast.makeText(BookAppointmentActivity.this, message, Toast.LENGTH_SHORT).show();
                        Log.d("APPOINTMENT_CREATION", "✅ Appointment saved successfully");

                        // Also save to user's personal appointments node
                        saveToUserAppointments(appointment);

                        // Show success dialog
                        showSuccessDialog(appointment);
                    } else {
                        Toast.makeText(BookAppointmentActivity.this,
                                "Failed to book appointment", Toast.LENGTH_SHORT).show();
                        Log.e("APPOINTMENT_CREATION", "❌ Failed to save appointment: " + task.getException());
                    }
                });
    }

    private void saveToUserAppointments(Appointment appointment) {
        if (appointment.getPatientUsername() != null) {
            DatabaseReference userAppointmentsRef = FirebaseDatabase.getInstance()
                    .getReference("MbararaHospital/Appointments")
                    .child(appointment.getPatientUsername())
                    .child(appointment.getAppointmentId());

            userAppointmentsRef.setValue(appointment)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("APPOINTMENT_CREATION", "✅ Also saved to user's appointments");
                        } else {
                            Log.e("APPOINTMENT_CREATION", "❌ Failed to save to user appointments: " + task.getException());
                        }
                    });
        }
    }

    private void showSuccessDialog(Appointment appointment) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        String message;
        if (isEmergencyMode) {
            message = "🚨 EMERGENCY APPOINTMENT REGISTERED!\n\n" +
                    "Department: " + appointment.getDepartment() + "\n" +
                    "Date: " + appointment.getPreferredDate() + "\n" +
                    "Urgency Level: " + appointment.getUrgencyLevel() + "\n\n" +
                    "You will receive immediate medical attention. " +
                    "Please proceed to the emergency department immediately.";
        } else {
            message = "Appointment requested successfully!\n\n" +
                    "Department: " + appointment.getDepartment() + "\n" +
                    "Preferred Date: " + appointment.getPreferredDate() + "\n" +
                    "Urgency Level: " + appointment.getUrgencyLevel() + "\n\n" +
                    "You will receive a confirmation with your token number and allocated time soon.";
        }

        builder.setTitle(isEmergencyMode ? "Emergency Registered!" : "Appointment Requested!")
                .setMessage(message)
                .setPositiveButton("View My Appointments", (dialog, which) -> {
                    Intent intent = new Intent(this, MyAppointmentsActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Book Another", (dialog, which) -> {
                    // Reset form for new appointment
                    resetForm();
                })
                .setCancelable(false)
                .show();
    }

    private void resetForm() {
        spinnerDepartment.setSelection(0);
        spinnerTimeSlot.setSelection(0);
        spinnerUrgency.setSelection(0);
        etPreferredDate.setText("");
        etVisitReason.setText("");
        etEmergencyDetails.setText("");
        etPatientCondition.setText("");

        // Reset emergency mode
        isEmergencyMode = false;
        layoutEmergencyDetails.setVisibility(View.GONE);
        cardEmergencyAlert.setVisibility(View.GONE);
        btnEmergencyAppointment.setText("🚨 Emergency Appointment");
    }

    private void loadPatientName() {
        if (username != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(username);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            patientName = name;
                        } else {
                            patientName = snapshot.child("username").getValue(String.class);
                        }
                        Log.d("PATIENT_NAME", "Loaded patient name: " + patientName);
                    } else {
                        patientName = "Patient";
                        Log.d("PATIENT_NAME", "Using default patient name");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    patientName = "Patient";
                    Log.e("PATIENT_NAME", "Error loading patient name: " + error.getMessage());
                }
            });
        } else {
            patientName = "Patient";
        }
    }
}