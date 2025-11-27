package com.example.azimbalife.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.azimbalife.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    ImageView profileImg;
    TextView profileName, profileEmail, profileUsername, profilePassword;
    TextView titleName, titleUsername;
    Button editProfile;

    String username; // Logged-in user's username

    private static final int EDIT_PROFILE_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lavender));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        profileImg = findViewById(R.id.profileImg);

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profileUsername = findViewById(R.id.profileUsername);
        profilePassword = findViewById(R.id.profilePassword);
        titleName = findViewById(R.id.titleName);
        titleUsername = findViewById(R.id.titleUsername);
        editProfile = findViewById(R.id.editButton);

        username = getIntent().getStringExtra("username");

        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Error: Username missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUserData(username);

        editProfile.setOnClickListener(view -> passUserData());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            loadUserData(username); // Reload updated profile data immediately after editing
        }
    }

    private void loadUserData(String username) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = reference.orderByChild("username").equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String nameFromDB = userSnapshot.child("name").getValue(String.class);
                        String emailFromDB = userSnapshot.child("email").getValue(String.class);
                        String usernameFromDB = userSnapshot.child("username").getValue(String.class);
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);
                        String imageUrl = userSnapshot.child("imageUrl").getValue(String.class);

                        // Set text views
                        titleName.setText(nameFromDB);
                        titleUsername.setText(usernameFromDB);
                        profileName.setText(nameFromDB);
                        profileEmail.setText(emailFromDB);
                        profileUsername.setText(usernameFromDB);
                        profilePassword.setText(passwordFromDB);

                        // Load profile image with Glide (with placeholder & error fallback)
                        Glide.with(ProfileActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.person_icon)
                                .error(R.drawable.person_icon)
                                .circleCrop()
                                .into(profileImg);

                        break;  // We only need the first matching user
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to load user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void passUserData() {
        String userUsername = profileUsername.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userUsername);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String nameFromDB = userSnapshot.child("name").getValue(String.class);
                        String emailFromDB = userSnapshot.child("email").getValue(String.class);
                        String usernameFromDB = userSnapshot.child("username").getValue(String.class);
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);
                        String imageUrl = userSnapshot.child("imageUrl").getValue(String.class);

                        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                        intent.putExtra("name", nameFromDB);
                        intent.putExtra("email", emailFromDB);
                        intent.putExtra("username", usernameFromDB);
                        intent.putExtra("password", passwordFromDB);
                        intent.putExtra("imageUrl", imageUrl);  // Pass image URL too
                        startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE);  // Use startActivityForResult here
                        break;
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User not found for editing!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Failed to fetch user for edit: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
