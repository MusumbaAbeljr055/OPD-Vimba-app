package com.example.azimbalife.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.azimbalife.Adapter.SpecialistAdapter;
import com.example.azimbalife.Domain.Specialist;
import com.example.azimbalife.R;

import java.util.ArrayList;
import java.util.List;

public class SpecialistActivity extends AppCompatActivity {

    private RecyclerView specialistRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specialist);

        specialistRecyclerView = findViewById(R.id.specialistRecyclerView);
        specialistRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Specialist> specialistList = new ArrayList<>();
        specialistList.add(new Specialist("Dr. John ", "Cardiologist", "Available Today", 4.5f, R.drawable.health));
        specialistList.add(new Specialist("Dr. Kato", "Neurologist", "Available Tomorrow", 4.8f, R.drawable.health));
        specialistList.add(new Specialist("Dr. Alice", "Pediatrician", "Available Today", 4.6f, R.drawable.health));
        specialistList.add(new Specialist("Dr. Mercy", "Dermatologist", "Available Tomorrow", 4.7f, R.drawable.health));

        SpecialistAdapter adapter = new SpecialistAdapter(specialistList, this);
        specialistRecyclerView.setAdapter(adapter);
    }
}
