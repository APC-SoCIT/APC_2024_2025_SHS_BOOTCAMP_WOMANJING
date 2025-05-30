package com.example.wasteposal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
//Class fields
public class report extends AppCompatActivity {

    private EditText complaintText;
    private Button submitButton;

    private String userId, city, barangay, address;
    private DatabaseReference complaintsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
//UI elements
        complaintText = findViewById(R.id.complaintText);
        submitButton = findViewById(R.id.loginButton); // Submit button

        // Load user data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "01-0001");
        city = prefs.getString("city", "Makati");
        barangay = prefs.getString("barangay", "Magallanes");
        address = prefs.getString("address", "No address");

        // Firebase reference to complaints under this barangay
        complaintsRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("complaints");

        submitButton.setOnClickListener(v -> submitComplaint());
    }
    //
    private void submitComplaint() {
        String message = complaintText.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter your complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current time as timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        // Generate complaintDisplayID
        complaintsRef.child(userId).get().addOnSuccessListener(snapshot -> {
            long count = snapshot.getChildrenCount() + 1;
            String complaintDisplayId = userId + "-" + count;

            HashMap<String, Object> complaintData = new HashMap<>();
            complaintData.put("address", address);
            complaintData.put("complaint_status", "pending");
            complaintData.put("message", message);
            complaintData.put("timestamp", timestamp);
            complaintData.put("userId", userId); // Optional: add userId to the data

            // Save under /complaints/{userId}/{complaintDisplayId}
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
