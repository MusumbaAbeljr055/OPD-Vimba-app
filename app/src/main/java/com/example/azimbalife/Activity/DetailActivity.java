package com.example.azimbalife.Activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.azimbalife.Adapter.DateAdapter;
import com.example.azimbalife.Adapter.TimeAdapter;
import com.example.azimbalife.Domain.DoctorsModel;
import com.example.azimbalife.R;
import com.example.azimbalife.databinding.ActivityDetailBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private DoctorsModel item;
    private String selectedDate = null;
    private String selectedTime = null;

    private String userName = "Patient"; // Default patient name if fetch fails
    private String userEmail = null;     // Added to store patient email
    private boolean isFavorite = false;
    private static final String PREFS_NAME = "favorites_prefs";
    private static final int REQUEST_CALL_PERMISSION = 101;

    private boolean isFullNameFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        item = (DoctorsModel) getIntent().getSerializableExtra("object");
        if (item == null) {
            Toast.makeText(this, "Doctor data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get username key (e.g. "Abbey") from intent extras - MUST be passed by previous activity!
        String usernameKey = getIntent().getStringExtra("username");
        if (usernameKey == null || usernameKey.isEmpty()) {
            Toast.makeText(this, "Username not provided, defaulting to 'Patient'", Toast.LENGTH_SHORT).show();
            usernameKey = "Patient";
        }

        // Disable appointment button until full name & email are fetched
        binding.button.setEnabled(false);

        // Fetch patient's full name and email asynchronously
        fetchFullNameAndEmailFromFirebase(usernameKey);


        dateInit();
        timeInit();

        isFavorite = loadFavoriteState();
        updateFavoriteIcon();

        binding.backbtn.setOnClickListener(v -> finish());

        binding.imageView9.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            updateFavoriteIcon();
            saveFavoriteState(isFavorite);
            Toast.makeText(this,
                    isFavorite ? "Added to Favorites" : "Removed from Favorites",
                    Toast.LENGTH_SHORT).show();
        });

        binding.button.setOnClickListener(v -> {
            if (!isFullNameFetched) {
                Toast.makeText(this, "Please wait, loading your profile...", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDate == null || selectedTime == null) {
                Toast.makeText(this, "Please select date and time for the appointment.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userEmail == null || userEmail.isEmpty()) {
                Toast.makeText(this, "Your email is missing. Cannot proceed.", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("DetailActivity", "Sending appointment request with userName: " + userName + " and userEmail: " + userEmail);
            sendAppointmentRequest();
        });



        binding.videobtn.setOnClickListener(v -> Toast.makeText(this, "Video call feature coming soon!", Toast.LENGTH_SHORT).show());

        binding.chatbtn.setOnClickListener(v -> {
            Intent intent = new Intent(DetailActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        binding.addressTxt.setOnClickListener(v -> {
            String location = item.getLocation();
            if (location != null && !location.isEmpty()) {
                String geoUri = "geo:0,0?q=" + location.replace(" ", "+");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(DetailActivity.this, "No maps application found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(DetailActivity.this, "Doctor location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fetch patient's full name and email by querying Users node where "username" equals the given username.
     * Enable appointment button only after fetch is complete.
     */
    private void fetchFullNameAndEmailFromFirebase(String usernameKey) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = reference.orderByChild("username").equalTo(usernameKey);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String fullName = userSnapshot.child("name").getValue(String.class);
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (fullName != null && !fullName.isEmpty()) {
                            userName = fullName; // update full name
                        }
                        if (email != null && !email.isEmpty()) {
                            userEmail = email; // update email
                        }
                        break;
                    }
                } else {
                    Log.w("Firebase", "No user found with username: " + usernameKey);
                }
                isFullNameFetched = true;
                Log.d("DetailActivity", "Fetched full name: " + userName + ", email: " + userEmail);

                runOnUiThread(() -> binding.button.setEnabled(true));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to fetch user info: " + error.getMessage());
                isFullNameFetched = true;
                runOnUiThread(() -> binding.button.setEnabled(true));
            }
        });
    }

    private void showCallConfirmationDialog(String doctorName, String doctorNumber) {
        new AlertDialog.Builder(this)
                .setTitle("Call Doctor")
                .setMessage("Do you want to call Dr. " + doctorName + " at " + doctorNumber + "?")
                .setPositiveButton("Call", (dialog, which) -> makePhoneCall(doctorNumber))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void makePhoneCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    REQUEST_CALL_PERMISSION);
        } else {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            binding.imageView9.setImageResource(R.drawable.favorite_red);
        } else {
            binding.imageView9.setImageResource(R.drawable.favorite_white);
        }
    }

    private void saveFavoriteState(boolean state) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(item.getName(), state);
        editor.apply();
    }

    private boolean loadFavoriteState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(item.getName(), false);
    }



    private void dateInit() {
        binding.dateView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        DateAdapter dateAdapter = new DateAdapter(generateDate());
        binding.dateView.setAdapter(dateAdapter);
        dateAdapter.setOnItemClickListener(date -> selectedDate = date);
    }

    private void timeInit() {
        binding.timeView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        TimeAdapter timeAdapter = new TimeAdapter(generateTimeSlots());
        binding.timeView.setAdapter(timeAdapter);
        timeAdapter.setOnItemClickListener(time -> selectedTime = time);
    }

    private void showAppointmentConfirmedNotification(String doctorName, String date, String time) {
        String channelId = "appointment_confirmed_channel";
        String channelName = "Appointment Confirmations";

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String message = "Your appointment with " + doctorName + " on " + date + " at " + time + " has been requested. Please wait for confirmation.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notifications_24dp_999999_fill0_wght400_grad0_opsz24)
                .setContentTitle("Appointment Request Sent")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Makes it expandable
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    // Send appointment POST request to Firebase Cloud Function endpoint
    private void sendAppointmentRequest() {

        String functionUrl = "https://confirmappointment-vy6tk2hf5q-uc.a.run.app/";

        JSONObject postData = new JSONObject();
        try {
            postData.put("userName", userName);      // Patient's full name
            postData.put("userEmail", userEmail);    // Patient's email added here
            postData.put("doctorName", item.getName());

            postData.put("date", selectedDate);
            postData.put("time", selectedTime);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to prepare appointment data", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DetailActivity", "POST data userName: " + userName);
        Log.d("DetailActivity", "POST data userEmail: " + userEmail);
        Log.d("DetailActivity", "POST URL: " + functionUrl);

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, functionUrl, postData,
                response -> {
                    Toast.makeText(this, "Appointment request sent!", Toast.LENGTH_LONG).show();
                    showAppointmentConfirmedNotification(item.getName(), selectedDate, selectedTime);
                },
                error -> {
                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.data != null) {
                        String data = new String(networkResponse.data);
                        Log.e("VolleyError", "Status code: " + networkResponse.statusCode + ", Data: " + data);
                    } else {
                        Log.e("VolleyError", "No network response");
                    }
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to send appointment request", Toast.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        queue.add(jsonObjectRequest);
    }

    public static List<String> generateDate() {
        List<String> date = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE/dd/MMM");
        for (int i = 0; i < 7; i++) {
            date.add(today.plusDays(i).format(formatter));
        }
        return date;
    }

    public static List<String> generateTimeSlots() {
        List<String> timeslots = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        for (int i = 0; i < 24; i += 2) {
            LocalTime time = LocalTime.of(i, 0);
            timeslots.add(time.format(formatter));
        }
        return timeslots;
    }
}
