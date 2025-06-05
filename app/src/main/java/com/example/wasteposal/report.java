package com.example.wasteposal;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.WindowCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class report extends AppCompatActivity {

    private EditText complaintText;
    private Button submitButton;
    private AppCompatImageButton backButton;

    private String userId, city, barangay, address;
    private DatabaseReference complaintsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.report);

        complaintText = findViewById(R.id.complaintText);
        submitButton = findViewById(R.id.submitButton);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
        });

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "01-0001");
        city = prefs.getString("city", "Makati");
        barangay = prefs.getString("barangay", "Magallanes");
        address = prefs.getString("address", "No address");

        complaintsRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("complaints");

        submitButton.setOnClickListener(v -> submitComplaint());
    }

    private void submitComplaint() {
        String message = complaintText.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter your complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        complaintsRef.child(userId).get().addOnSuccessListener(snapshot -> {
            long count = snapshot.getChildrenCount() + 1;
            String complaintDisplayId = userId + "-" + count;

            HashMap<String, Object> complaintData = new HashMap<>();
            complaintData.put("address", address);
            complaintData.put("complaint_status", "pending");
            complaintData.put("message", message);
            complaintData.put("timestamp", timestamp);
            complaintData.put("userId", userId);

            complaintsRef.child(userId).child(complaintDisplayId)
                    .setValue(complaintData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(report.this, "Complaint submitted successfully.", Toast.LENGTH_SHORT).show();
                        complaintText.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(report.this, "Failed to submit complaint: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(report.this, "Failed to access complaint data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
