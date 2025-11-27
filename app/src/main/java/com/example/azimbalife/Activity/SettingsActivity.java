package com.example.azimbalife.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.azimbalife.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial notificationSwitch, darkModeSwitch;
    private TextView languageText, contactSupport, appVersion;
    private TextView feedback, privacyPolicy, termsConditions;
    private ImageView backArrow;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPrefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        // Apply saved language and theme globally
        applySavedLanguage();
        applySavedThemeForApp();

        initializeViews();
        loadSavedPreferences();
        setupClickListeners();
        setupVersionInfo();
        applyAnimations();
    }

    private void initializeViews() {
        backArrow = findViewById(R.id.back_arrow);
        notificationSwitch = findViewById(R.id.switch_notifications);
        darkModeSwitch = findViewById(R.id.switch_darkmode);
        languageText = findViewById(R.id.language_text);
        contactSupport = findViewById(R.id.contact_support);
        feedback = findViewById(R.id.feedback);
        privacyPolicy = findViewById(R.id.privacy_policy);
        termsConditions = findViewById(R.id.terms_conditions);
        appVersion = findViewById(R.id.app_version);
    }

    private void loadSavedPreferences() {
        boolean notificationsEnabled = sharedPrefs.getBoolean("notifications", true);
        boolean darkModeEnabled = sharedPrefs.getBoolean("dark_mode", false);

        notificationSwitch.setChecked(notificationsEnabled);
        darkModeSwitch.setChecked(darkModeEnabled);
    }

    private void setupClickListeners() {
        Animation clickAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

        backArrow.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            finish();
        });

        // Notifications toggle
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor = sharedPrefs.edit();
            editor.putBoolean("notifications", isChecked);
            editor.apply();
            showToast("Notifications " + (isChecked ? "enabled" : "disabled"));
        });

        // Dark/Light mode toggle
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor = sharedPrefs.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            // Apply theme globally
            applySavedThemeForApp();

            // Restart app to apply theme everywhere
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Language selection
        languageText.setOnClickListener(v -> showLanguageDialog());

        // Contact & feedback
        contactSupport.setOnClickListener(v -> launchEmailIntent("Support Request"));
        feedback.setOnClickListener(v -> launchEmailIntent("App Feedback"));

        // Policies
        privacyPolicy.setOnClickListener(v -> launchWebBrowser("https://yourdomain.com/privacy"));
        termsConditions.setOnClickListener(v -> launchWebBrowser("https://yourdomain.com/terms"));
    }

    private void setupVersionInfo() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appVersion.setText(getString(R.string.app_version, versionName));
        } catch (Exception e) {
            e.printStackTrace();
            appVersion.setText(getString(R.string.app_version, "1.0"));
        }
    }

    // Apply theme globally
    private void applySavedThemeForApp() {
        boolean isDark = sharedPrefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // Apply saved language
    private void applySavedLanguage() {
        String langCode = sharedPrefs.getString("lang", "en");
        setLocale(langCode);
    }

    private void showLanguageDialog() {
        String[] languages = getResources().getStringArray(R.array.language_names);
        String[] langCodes = getResources().getStringArray(R.array.language_codes);

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_language)
                .setItems(languages, (dialog, which) -> {
                    editor = sharedPrefs.edit();
                    editor.putString("lang", langCodes[which]);
                    editor.apply();
                    setLocale(langCodes[which]);
                    recreate(); // Refresh only SettingsActivity
                })
                .show();
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void launchEmailIntent(String subject) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:ssenkubgeabbey055@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email)));
    }

    private void launchWebBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void applyAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        backArrow.startAnimation(fadeIn);
        notificationSwitch.startAnimation(fadeIn);
        darkModeSwitch.startAnimation(fadeIn);
        languageText.startAnimation(fadeIn);
        contactSupport.startAnimation(fadeIn);
        feedback.startAnimation(fadeIn);
        privacyPolicy.startAnimation(fadeIn);
        termsConditions.startAnimation(fadeIn);
        appVersion.startAnimation(fadeIn);
    }
}
