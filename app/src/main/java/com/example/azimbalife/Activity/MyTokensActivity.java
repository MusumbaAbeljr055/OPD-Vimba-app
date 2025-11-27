package com.example.azimbalife.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.azimbalife.Adapter.MyTokensAdapter;
import com.example.azimbalife.Domain.TokenTracking;
import com.example.azimbalife.databinding.ActivityMyTokensBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyTokensActivity extends AppCompatActivity {

    private ActivityMyTokensBinding binding;
    private String username;
    private MyTokensAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyTokensBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        username = getIntent().getStringExtra("username");
        loadMyTokens();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefresh.setOnClickListener(v -> loadMyTokens());
    }

    private void loadMyTokens() {
        DatabaseReference trackingRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/TokenTracking");

        trackingRef.orderByChild("patient").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<TokenTracking> tokens = new ArrayList<>();

                        for (DataSnapshot tokenSnap : snapshot.getChildren()) {
                            TokenTracking token = tokenSnap.getValue(TokenTracking.class);
                            if (token != null) {
                                tokens.add(token);
                            }
                        }

                        if (tokens.isEmpty()) {
                            binding.tvNoTokens.setVisibility(android.view.View.VISIBLE);
                            binding.tokensRecycler.setVisibility(android.view.View.GONE);
                        } else {
                            binding.tvNoTokens.setVisibility(android.view.View.GONE);
                            binding.tokensRecycler.setVisibility(android.view.View.VISIBLE);

                            adapter = new MyTokensAdapter(tokens, token -> {
                                // Token click - open tracking
                                Intent intent = new Intent(MyTokensActivity.this, TokenTrackingActivity.class);
                                intent.putExtra("username", username);
                                intent.putExtra("tokenId", token.getTokenId());
                                startActivity(intent);
                            });
                            binding.tokensRecycler.setLayoutManager(new LinearLayoutManager(MyTokensActivity.this));
                            binding.tokensRecycler.setAdapter(adapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyTokensActivity.this, "Error loading tokens", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}