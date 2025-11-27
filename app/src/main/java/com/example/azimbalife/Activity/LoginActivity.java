package com.example.azimbalife.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText loginUsername, loginPassword;
    Button loginButton;
    TextView signupRedirectText;
    DatabaseReference usersRef, healthWorkersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences("LoginSession", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String userType = prefs.getString("userType", "patient");
        String savedUsername = prefs.getString("username", null);
        String name = prefs.getString("name", null);

        if (isLoggedIn && savedUsername != null) {
            redirectToDashboard(userType, name, savedUsername);
            return;
        }

        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lavender));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        healthWorkersRef = FirebaseDatabase.getInstance().getReference("MbararaHospital/HealthWorkers");

        loginButton.setOnClickListener(view -> checkUser());
        signupRedirectText.setOnClickListener(view ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
    }

    private void checkUser() {
        String username = loginUsername.getText().toString().trim().toLowerCase();
        String password = loginPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // First check if it's a health worker
        checkHealthWorker(username, password);
    }

    private void checkHealthWorker(String username, String password) {
        healthWorkersRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User is a health worker
                    String passFromDB = snapshot.child("password").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String userType = snapshot.child("userType").getValue(String.class);
                    String department = snapshot.child("department").getValue(String.class);
                    String specialization = snapshot.child("specialization").getValue(String.class);
                    Boolean isActive = snapshot.child("isActive").getValue(Boolean.class);

                    // Check if health worker account is active
                    if (isActive == null || !isActive) {
                        Toast.makeText(LoginActivity.this,
                                "Health worker account is not active. Please contact administration.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (passFromDB != null && passFromDB.equals(password)) {
                        // Health worker login successful
                        saveLoginSession(username, name, userType, true);
                        redirectToHealthWorkerDashboard(name, username, userType, department, specialization);
                    } else {
                        // Wrong password for health worker, check regular users
                        checkRegularUser(username, password);
                    }
                } else {
                    // Not a health worker, check regular users
                    checkRegularUser(username, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Login failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkRegularUser(String username, String password) {
        usersRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String passFromDB = snapshot.child("password").getValue(String.class);
                    Boolean isDoctor = snapshot.child("isDoctor").getValue(Boolean.class);
                    Boolean isHealthWorker = snapshot.child("isHealthWorker").getValue(Boolean.class);
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    if (passFromDB != null && passFromDB.equals(password)) {
                        // Determine user type
                        String userType;
                        if (isHealthWorker != null && isHealthWorker) {
                            userType = "health_worker";
                        } else if (isDoctor != null && isDoctor) {
                            userType = "doctor";
                        } else {
                            userType = "patient";
                        }

                        saveLoginSession(username, name, userType, isHealthWorker != null && isHealthWorker);
                        redirectToAppropriateDashboard(userType, name, username, email);
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Login failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLoginSession(String username, String name, String userType, boolean isHealthWorker) {
        SharedPreferences.Editor editor = getSharedPreferences("LoginSession", MODE_PRIVATE).edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putString("name", name);
        editor.putString("userType", userType);
        editor.putBoolean("isHealthWorker", isHealthWorker);
        editor.apply();
    }

    private void redirectToDashboard(String userType, String name, String username) {
        if (isHealthWorkerUserType(userType)) {
            // For health workers, get their details from HealthWorkers table
            healthWorkersRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userType = snapshot.child("userType").getValue(String.class);
                        String department = snapshot.child("department").getValue(String.class);
                        String specialization = snapshot.child("specialization").getValue(String.class);
                        redirectToHealthWorkerDashboard(name, username, userType, department, specialization);
                    } else {
                        // Fallback - user is in Users table as health worker but not in HealthWorkers
                        redirectToHealthWorkerDashboard(name, username, "health_worker", "General", "General Practitioner");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Fallback to health worker dashboard with default values
                    redirectToHealthWorkerDashboard(name, username, "health_worker", "General", "General Practitioner");
                }
            });
        } else {
            // For regular users, get email from Users table
            usersRef.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String email = snapshot.child("email").getValue(String.class);
                    redirectToAppropriateDashboard(userType, name, username, email);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    redirectToAppropriateDashboard(userType, name, username, null);
                }
            });
        }
    }

    private void redirectToAppropriateDashboard(String userType, String name, String username, String email) {
        Intent intent;

        if (isHealthWorkerUserType(userType)) {
            // Health workers go to Health Worker Dashboard
            intent = new Intent(LoginActivity.this, HealthWorkerDashboardActivity.class);
            intent.putExtra("healthWorkerId", username);
            intent.putExtra("healthWorkerName", name);
            intent.putExtra("userType", userType);
            intent.putExtra("department", getDepartmentFromUserType(userType));
            intent.putExtra("specialization", getSpecializationFromUserType(userType));
        } else if ("doctor".equals(userType)) {
            // Regular doctors (not health workers) go to MainActivity
            intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("mobile", username);
            intent.putExtra("userType", "doctor");
        } else {
            // Regular patients go to MainActivity
            intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("userType", "patient");
        }

        startActivity(intent);
        finish();
    }

    private void redirectToHealthWorkerDashboard(String name, String username, String userType,
                                                 String department, String specialization) {
        Intent intent = new Intent(LoginActivity.this, HealthWorkerDashboardActivity.class);
        intent.putExtra("healthWorkerId", username);
        intent.putExtra("healthWorkerName", name);
        intent.putExtra("userType", userType != null ? userType : "health_worker");
        intent.putExtra("department", department != null ? department : "General Medicine");
        intent.putExtra("specialization", specialization != null ? specialization : "General Practitioner");
        startActivity(intent);
        finish();
    }

    private boolean isHealthWorkerUserType(String userType) {
        return "health_worker".equals(userType) ||
                "doctor".equals(userType) ||
                "nurse".equals(userType) ||
                "lab_technician".equals(userType) ||
                "administrator".equals(userType);
    }

    private String getDepartmentFromUserType(String userType) {
        switch (userType) {
            case "doctor": return "General Medicine";
            case "nurse": return "Nursing";
            case "lab_technician": return "Laboratory";
            case "administrator": return "Administration";
            default: return "General Medicine";
        }
    }

    private String getSpecializationFromUserType(String userType) {
        switch (userType) {
            case "doctor": return "Physician";
            case "nurse": return "Registered Nurse";
            case "lab_technician": return "Lab Technician";
            case "administrator": return "Administrator";
            default: return "General Practitioner";
        }
    }
}