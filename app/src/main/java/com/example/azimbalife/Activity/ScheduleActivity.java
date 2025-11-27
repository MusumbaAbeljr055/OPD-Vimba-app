package com.example.azimbalife.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.azimbalife.Adapter.ScheduleAdapter;
import com.example.azimbalife.Domain.Schedule;
import com.example.azimbalife.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {

    private RecyclerView scheduleRecycler;
    private ScheduleAdapter scheduleAdapter;
    private List<Schedule> schedules;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView weekDayText, dayText, monthText, yearText, welcomeText, currentTimeText;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView dailyAnnouncementImage, btnBack;
    private View statusIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Initialize views
        initViews();
        setupRecyclerView();
        initSchedules();
        setupClickListeners();

        // Set today's schedule
        scheduleAdapter = new ScheduleAdapter(this, filterScheduleByDay());
        scheduleRecycler.setAdapter(scheduleAdapter);

        updateDateSection();
        updateCurrentTime();
        updateDailyAnnouncement();
        updateHospitalStatus();

        // Pull to refresh
        swipeRefresh.setOnRefreshListener(() -> {
            refreshSchedule();
            swipeRefresh.setRefreshing(false);
        });

        scheduleMidnightUpdate();
        startTimeUpdater();
    }

    private void initViews() {
        scheduleRecycler = findViewById(R.id.scheduleRecycler);
        weekDayText = findViewById(R.id.weekDayText);
        dayText = findViewById(R.id.dayText);
        monthText = findViewById(R.id.monthText);
        yearText = findViewById(R.id.yearText);
        welcomeText = findViewById(R.id.welcomeText);
        currentTimeText = findViewById(R.id.currentTimeText);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        dailyAnnouncementImage = findViewById(R.id.dailyAnnouncementImage);
        btnBack = findViewById(R.id.btnBack);
        statusIndicator = findViewById(R.id.statusIndicator);
    }

    private void setupRecyclerView() {
        scheduleRecycler.setLayoutManager(new LinearLayoutManager(this));
        scheduleRecycler.setHasFixedSize(true);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void initSchedules() {
        schedules = new ArrayList<>();

        // Monday
        schedules.add(new Schedule("Monday", "08:00 AM", "04:00 PM",
                new String[]{"Dr. Joseph Kateregga", "Dr. Sarah Nabukenya", "Dr. James Mugisha"},
                new String[]{"General Checkup", "Maternal Health", "Laboratory Services", "Pharmacy", "Nutrition Counseling", "Vaccination"},
                R.drawable.mrrh, "Active"));

        // Tuesday
        schedules.add(new Schedule("Tuesday", "09:00 AM", "05:00 PM",
                new String[]{"Dr. Patrick Okello", "Dr. Miriam Kato", "Dr. Grace Nalubega"},
                new String[]{"Pediatrics", "Child Nutrition", "Vaccination", "Physiotherapy", "Minor Surgeries", "Dental Checkup"},
                R.drawable.mrrh, "Active"));

        // Wednesday
        schedules.add(new Schedule("Wednesday", "08:30 AM", "04:30 PM",
                new String[]{"Dr. Miriam Kato", "Dr. Samuel Mugisha", "Dr. Rebecca Namutebi"},
                new String[]{"Dermatology", "Traditional Herbal Consultation", "General Surgery", "Laboratory Testing", "Blood Pressure Monitoring", "Eye Care"},
                R.drawable.mrrh, "Active"));

        // Thursday
        schedules.add(new Schedule("Thursday", "09:00 AM", "05:00 PM",
                new String[]{"Dr. Grace Namusoke", "Dr. Michael Ssemanda", "Dr. David Ochieng"},
                new String[]{"Cardiology", "Orthopedics", "Rehabilitation", "Laboratory Services", "Nutrition Counseling", "Mental Health"},
                R.drawable.mrrh, "Active"));

        // Friday
        schedules.add(new Schedule("Friday", "08:00 AM", "03:00 PM",
                new String[]{"Dr. Michael Ssemanda", "Dr. Beatrice Nankya", "Dr. Robert Kalema"},
                new String[]{"Surgery", "Physiotherapy", "Herbal Remedies", "Vaccination", "Maternal Checkup", "Laboratory Services"},
                R.drawable.mrrh, "Active"));

        // Saturday
        schedules.add(new Schedule("Saturday", "08:00 AM", "12:00 PM",
                new String[]{"Dr. Robert Lule", "Dr. Sarah Nankunda", "Nurse Joyce Mbabazi"},
                new String[]{"Community Health Outreach", "Vaccination", "General Checkup", "Pharmacy Services", "Child Nutrition", "First Aid"},
                R.drawable.mrrh, "Limited Services"));

        // Sunday
        schedules.add(new Schedule("Sunday", "Closed", "Closed",
                new String[]{"Emergency Team Only"},
                new String[]{"Emergency Services Only", "24/7 Emergency Care Available"},
                R.drawable.mrrh, "Closed"));
    }

    private List<Schedule> filterScheduleByDay() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String currentDay = dayFormat.format(new Date());

        for (Schedule schedule : schedules) {
            if (schedule.getDay().equals(currentDay)) {
                List<Schedule> currentDaySchedule = new ArrayList<>();
                currentDaySchedule.add(schedule);
                return currentDaySchedule;
            }
        }
        return new ArrayList<>();
    }

    private void updateDateSection() {
        Date now = new Date();
        SimpleDateFormat weekDay = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat day = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat month = new SimpleDateFormat("MMMM", Locale.getDefault());
        SimpleDateFormat year = new SimpleDateFormat("yyyy", Locale.getDefault());

        weekDayText.setText(weekDay.format(now));
        dayText.setText(day.format(now));
        monthText.setText(month.format(now));
        yearText.setText(year.format(now));
        welcomeText.setText("Welcome to Mbarara Regional Referral Hospital");
    }

    private void updateCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        currentTimeText.setText("Current Time: " + currentTime);
    }

    private void updateHospitalStatus() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String currentDay = dayFormat.format(new Date());

        for (Schedule schedule : schedules) {
            if (schedule.getDay().equals(currentDay)) {
                if (schedule.getStatus().equals("Closed")) {
                    statusIndicator.setBackgroundColor(getColor(R.color.red));
                    Toast.makeText(this, "Hospital is closed today. Emergency services available.", Toast.LENGTH_LONG).show();
                } else if (schedule.getStatus().equals("Limited Services")) {
                    statusIndicator.setBackgroundColor(getColor(R.color.my_primary));
                } else {
                    statusIndicator.setBackgroundColor(getColor(R.color.green));
                }
                break;
            }
        }
    }

    private void updateDailyAnnouncement() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String currentDay = dayFormat.format(new Date());

        int announcementRes = R.drawable.mrrh; // Default announcement image

        // You can add different images for different days here
        switch (currentDay) {
            case "Monday":
                announcementRes = R.drawable.mrrh;
                break;
            case "Tuesday":
                announcementRes = R.drawable.mrrh;
                break;
            case "Wednesday":
                announcementRes = R.drawable.mrrh;
                break;
            case "Thursday":
                announcementRes = R.drawable.mrrh;
                break;
            case "Friday":
                announcementRes = R.drawable.mrrh;
                break;
            case "Saturday":
                announcementRes = R.drawable.mrrh;
                break;
            case "Sunday":
                announcementRes = R.drawable.mrrh;
                break;
        }

        dailyAnnouncementImage.setImageResource(announcementRes);
        dailyAnnouncementImage.setVisibility(View.VISIBLE);
    }

    private void refreshSchedule() {
        scheduleAdapter.updateData(filterScheduleByDay());
        updateDateSection();
        updateCurrentTime();
        updateDailyAnnouncement();
        updateHospitalStatus();
        Toast.makeText(this, "Schedule updated", Toast.LENGTH_SHORT).show();
    }

    private void scheduleMidnightUpdate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        handler.postDelayed(() -> {
            refreshSchedule();
            scheduleMidnightUpdate();
        }, delay);
    }

    private void startTimeUpdater() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateCurrentTime();
                handler.postDelayed(this, 60000); // Update every minute
            }
        }, 60000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}