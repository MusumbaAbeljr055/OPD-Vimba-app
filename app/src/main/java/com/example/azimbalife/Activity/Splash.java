package com.example.azimbalife.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.azimbalife.R;
import com.example.azimbalife.databinding.ActivitySplashBinding;

public class Splash extends AppCompatActivity {

    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Make sure the progress bar is initially hidden
        binding.progressBar.setVisibility(View.GONE);

        // Show progress bar
        binding.progressBar.setVisibility(View.VISIBLE);

        // Delay 1 second before checking login status
        new Handler().postDelayed(() -> {
            checkLoginStatus();
        }, 1000); // 1 second delay
    }

    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("LoginSession", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String username = prefs.getString("username", "");

        setTheme(R.style.Theme_AZimbalife);

        if (isLoggedIn && !username.isEmpty()) {
            // User is logged in, go to MainActivity
            Intent intent = new Intent(Splash.this, MainActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        } else {
            // User is not logged in, go to LoginActivity
            Intent intent = new Intent(Splash.this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }
}