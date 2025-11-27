package com.example.azimbalife.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.azimbalife.Domain.HealthWorkerHelper;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {

    EditText signupName, signupUsername, signupEmail, signupPassword;
    TextView loginRedirectText;
    Button signupButton;
    DatabaseReference usersRef, doctorsRef, healthWorkersRef;
    List<String> doctorNames = new ArrayList<>();
    List<String> healthWorkerNames = new ArrayList<>();

    // Health worker email domains
    private static final String[] HEALTH_WORKER_DOMAINS = {
            "mrrh.ac.ug",
            "hospital.org",
            "health.gov.ug",
            "clinic.ug"
            // Add more health worker domains as needed
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lavender));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupUsername = findViewById(R.id.signup_username);
        signupPassword = findViewById(R.id.signup_password);
        setupPasswordToggle(signupPassword);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        signupButton = findViewById(R.id.signup_button);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        doctorsRef = FirebaseDatabase.getInstance().getReference("Doctors");
        healthWorkersRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/HealthWorkers");

        // Load all doctor names from Doctors node
        doctorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorNames.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String doctorName = data.child("Name").getValue(String.class);
                    if (doctorName != null) {
                        doctorNames.add(doctorName.toLowerCase());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Load all health worker names from HealthWorkers node
        healthWorkersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                healthWorkerNames.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String healthWorkerName = data.child("name").getValue(String.class);
                    if (healthWorkerName != null) {
                        healthWorkerNames.add(healthWorkerName.toLowerCase());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        signupButton.setOnClickListener(view -> {
            String name = signupName.getText().toString().trim();
            String email = signupEmail.getText().toString().trim().toLowerCase();
            String username = signupUsername.getText().toString().trim().toLowerCase();
            String password = signupPassword.getText().toString().trim();

            // Validation
            if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidEmail(email)) {
                Toast.makeText(SignUpActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(password)) {
                Toast.makeText(SignUpActivity.this,
                        "Password must be at least 8 characters and contain:\n• One uppercase letter\n• One digit\n• One special character",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Check if user is a health worker based on email domain
            if (isHealthWorkerEmail(email)) {
                registerHealthWorker(name, email, username, password);
            } else {
                registerRegularUser(name, email, username, password);
            }
        });

        loginRedirectText.setOnClickListener(view -> startActivity(new Intent(SignUpActivity.this, LoginActivity.class)));
    }

    private boolean isHealthWorkerEmail(String email) {
        for (String domain : HEALTH_WORKER_DOMAINS) {
            if (email.endsWith("@" + domain)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void registerHealthWorker(String name, String email, String username, String password) {
        // First check if username already exists
        usersRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(SignUpActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if health worker name exists in pre-registered list
                boolean isPreRegistered = healthWorkerNames.contains(name.toLowerCase());
                boolean isDoctor = doctorNames.contains(name.toLowerCase());

                // Create health worker entry in Users table
                UserHelper user = new UserHelper(name, email, username, password, true);
                usersRef.child(username).setValue(user).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Also create/update entry in HealthWorkers table
                        createHealthWorkerRecord(name, email, username, password, isPreRegistered, isDoctor);

                        String message = isPreRegistered ?
                                "Health worker account activated successfully!" :
                                "New health worker account created successfully!";

                        Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SignUpActivity.this, "Error checking username: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createHealthWorkerRecord(String name, String email, String username, String password,
                                          boolean isPreRegistered, boolean isDoctor) {
        // Determine user type and department based on available information
        String userType = determineUserType(name, email, isDoctor);
        String department = determineDepartment(name, email);
        String specialization = determineSpecialization(name, email);

        // Create health worker data
        HealthWorkerHelper healthWorker = new HealthWorkerHelper();
        healthWorker.setName(name);
        healthWorker.setEmail(email);
        healthWorker.setUsername(username);
        healthWorker.setPassword(password);
        healthWorker.setUserType(userType);
        healthWorker.setDepartment(department);
        healthWorker.setSpecialization(specialization);
        healthWorker.setIsActive(true);
        healthWorker.setIsPreRegistered(isPreRegistered);
        healthWorker.setCreatedAt(String.valueOf(System.currentTimeMillis()));

        // Save to HealthWorkers node
        healthWorkersRef.child(username).setValue(healthWorker);
    }

    private String determineUserType(String name, String email, boolean isDoctor) {
        if (isDoctor) {
            return "doctor";
        } else if (name.toLowerCase().contains("nurse")) {
            return "nurse";
        } else if (name.toLowerCase().contains("lab") || name.toLowerCase().contains("technician")) {
            return "lab_technician";
        } else if (name.toLowerCase().contains("admin") || name.toLowerCase().contains("administrator")) {
            return "administrator";
        } else {
            return "health_worker"; // default
        }
    }

    private String determineDepartment(String name, String email) {
        // Simple department detection based on email or name
        if (email.contains("cardio") || name.toLowerCase().contains("heart") || name.toLowerCase().contains("cardio")) {
            return "Cardiology";
        } else if (email.contains("emergency") || name.toLowerCase().contains("emergency")) {
            return "Emergency";
        } else if (email.contains("pediatric") || name.toLowerCase().contains("child")) {
            return "Pediatrics";
        } else if (email.contains("surgery") || name.toLowerCase().contains("surgical")) {
            return "Surgery";
        } else if (email.contains("maternity") || name.toLowerCase().contains("obgyn")) {
            return "Maternity";
        } else {
            return "General Medicine";
        }
    }

    private String determineSpecialization(String name, String email) {
        // Simple specialization detection
        if (name.toLowerCase().contains("surgeon")) {
            return "Surgeon";
        } else if (name.toLowerCase().contains("physician")) {
            return "Physician";
        } else if (name.toLowerCase().contains("specialist")) {
            return "Specialist";
        } else if (name.toLowerCase().contains("anesthetist")) {
            return "Anesthetist";
        } else {
            return "General Practitioner";
        }
    }

    private void registerRegularUser(String name, String email, String username, String password) {
        // Check if username already exists
        usersRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(SignUpActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Automatically detect if it's a doctor based on name
                boolean isDoctor = doctorNames.contains(name.toLowerCase());

                // Save regular user to Users node
                UserHelper user = new UserHelper(name, email, username, password, isDoctor);
                usersRef.child(username).setValue(user).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SignUpActivity.this, "Error checking username: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordToggle(EditText passwordEditText) {
        passwordEditText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (passwordEditText.getCompoundDrawables()[DRAWABLE_END] != null) {
                    int drawableWidth = passwordEditText.getCompoundDrawables()[DRAWABLE_END].getBounds().width();
                    float touchAreaStart = passwordEditText.getWidth() - passwordEditText.getPaddingEnd() - drawableWidth;
                    if (event.getX() >= touchAreaStart) {
                        if (passwordEditText.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_baseline_lock_24, 0, R.drawable.ic_eye_on, 0);
                        } else {
                            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_baseline_lock_24, 0, R.drawable.ic_eye_off, 0);
                        }
                        passwordEditText.setSelection(passwordEditText.length());
                        return true;
                    }
                }
            }
            return false;
        });
    }
}