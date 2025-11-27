package com.example.azimbalife.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.azimbalife.R;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int IMAGE_PICKER_REQUEST = 1002;

    private EditText editName, editEmail, editUsername, editPassword;
    private Button saveButton;
    private ImageView profileImageView, editImageButton;

    private Uri imageUri;
    private String usernameUser, nameUser, emailUser, passwordUser;

    private DatabaseReference reference;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lavender));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        initializeViews();
        showData();
        setupPasswordToggle(editPassword);

        reference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        loadProfileImage();

        editImageButton.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveProfileChanges());
    }

    private void initializeViews() {
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        saveButton = findViewById(R.id.saveButton);
        profileImageView = findViewById(R.id.profileImageView);
        editImageButton = findViewById(R.id.editImageButton);
    }

    private void openImagePicker() {
        if (checkAndRequestPermissions()) {
            ImagePicker.with(this)
                    .cropSquare()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .createIntent(intent -> {
                        startActivityForResult(intent, IMAGE_PICKER_REQUEST);
                        return null;
                    });
        }
    }

    private boolean checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && requestCode == IMAGE_PICKER_REQUEST) {
            imageUri = data.getData();
            Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(profileImageView);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileChanges() {
        boolean changed = isNameChanged() || isEmailChanged() || isPasswordChanged();

        if (imageUri != null) {
            uploadImage();
        } else if (changed) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "No changes found", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage() {
        if (imageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Explicitly use correct bucket
        FirebaseStorage storage = FirebaseStorage.getInstance("gs://azimbalife055.appspot.com");
        StorageReference storageReference = storage.getReference("profile_images/" + usernameUser + ".jpg");

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Uploading...");
        pd.setCancelable(false);
        pd.show();

        storageReference.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    pd.setMessage("Uploaded " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Only get the download URL after upload succeeds
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        reference.child(usernameUser).child("imageUrl").setValue(uri.toString())
                                .addOnCompleteListener(task -> {
                                    pd.dismiss();
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed to update database: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }).addOnFailureListener(e -> {
                        pd.dismiss();
                        Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private File createCacheFile() throws Exception {
        File cacheDir = getCacheDir();
        File cacheFile = new File(cacheDir, "temp_profile_" + System.currentTimeMillis() + ".jpg");
        if (cacheFile.exists()) {
            cacheFile.delete();
        }
        return cacheFile;
    }

    private void copyUriToCacheFile(File cacheFile) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(imageUri);
             OutputStream out = new FileOutputStream(cacheFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private void uploadFileToFirebase(File cacheFile, ProgressDialog pd) {
        StorageReference fileRef = storageReference.child(usernameUser + ".jpg");
        Uri fileUri = Uri.fromFile(cacheFile);

        fileRef.putFile(fileUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    pd.setMessage("Uploaded " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        reference.child(usernameUser).child("imageUrl").setValue(uri.toString());
                        pd.dismiss();
                        showToast("Profile updated successfully");
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    showToast("Upload failed: " + e.getMessage());
                });
    }

    private void loadProfileImage() {
        reference.child(usernameUser).child("imageUrl").get()
                .addOnSuccessListener(snapshot -> {
                    String url = snapshot.getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        Glide.with(this)
                                .load(url)
                                .circleCrop()
                                .into(profileImageView);
                    }
                });
    }

    private boolean isNameChanged() {
        String newName = editName.getText().toString().trim();
        if (!newName.equals(nameUser)) {
            reference.child(usernameUser).child("name").setValue(newName);
            nameUser = newName;
            return true;
        }
        return false;
    }

    private boolean isEmailChanged() {
        String newEmail = editEmail.getText().toString().trim();
        if (!newEmail.equals(emailUser)) {
            reference.child(usernameUser).child("email").setValue(newEmail);
            emailUser = newEmail;
            return true;
        }
        return false;
    }

    private boolean isPasswordChanged() {
        String newPass = editPassword.getText().toString().trim();
        if (!newPass.equals(passwordUser)) {
            reference.child(usernameUser).child("password").setValue(newPass);
            passwordUser = newPass;
            return true;
        }
        return false;
    }

    private void showData() {
        Intent i = getIntent();
        nameUser = i.getStringExtra("name");
        emailUser = i.getStringExtra("email");
        usernameUser = i.getStringExtra("username");
        passwordUser = i.getStringExtra("password");

        editName.setText(nameUser);
        editEmail.setText(emailUser);
        editUsername.setText(usernameUser);
        editPassword.setText(passwordUser);
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
                        togglePasswordVisibility(passwordEditText);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText passwordEditText) {
        int inputType = passwordEditText.getInputType();
        if (inputType == (android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            passwordEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        passwordEditText.setSelection(passwordEditText.length());
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}