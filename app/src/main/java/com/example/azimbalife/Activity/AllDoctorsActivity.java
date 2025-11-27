package com.example.azimbalife.Activity;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.azimbalife.Adapter.TopDoctorAdapter;
import com.example.azimbalife.Domain.DoctorsModel;
import com.example.azimbalife.R;
import com.example.azimbalife.databinding.ActivityAllDoctorsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AllDoctorsActivity extends AppCompatActivity {

    private ActivityAllDoctorsBinding binding;
    private List<DoctorsModel> doctorsList = new ArrayList<>();
    private TopDoctorAdapter adapter;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAllDoctorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get username from intent extras; default fallback
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = "defaultUser";
        }

        // Set status bar color and icon visibility for better contrast
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.purple_700));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        // Setup toolbar title and navigation click
        binding.topAppBar.setTitle("Available Doctors");
        binding.topAppBar.setTitleTextColor(getResources().getColor(android.R.color.white));
        binding.topAppBar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView with vertical LinearLayoutManager
        binding.allDoctorRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter and set it to RecyclerView
        adapter = new TopDoctorAdapter(doctorsList, this, username);
        binding.allDoctorRecyclerView.setAdapter(adapter);

        // Load doctors data from Firebase
        loadAllDoctors();
    }

    private void loadAllDoctors() {
        binding.progressBarAll.setVisibility(View.VISIBLE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Doctors");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorsList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    DoctorsModel doctor = snap.getValue(DoctorsModel.class);
                    if (doctor != null) {
                        doctorsList.add(doctor);
                    }
                }
                adapter.notifyDataSetChanged();
                binding.progressBarAll.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarAll.setVisibility(View.GONE);
                Toast.makeText(AllDoctorsActivity.this, "Error loading doctors", Toast.LENGTH_SHORT).show();
            }
        });
    }
}