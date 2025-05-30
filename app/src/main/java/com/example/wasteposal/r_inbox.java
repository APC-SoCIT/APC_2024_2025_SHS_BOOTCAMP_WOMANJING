package com.example.wasteposal;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class r_inbox extends AppCompatActivity {

    private LinearLayout inboxContainer;
    private DatabaseReference complaintsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.r_inbox);

        inboxContainer = findViewById(R.id.inboxContainer);

        // Read user data from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        String city = prefs.getString("city", null);
        String barangay = prefs.getString("barangay", null);

        if (userId == null || city == null || barangay == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app");
        complaintsRef = db.getReference(city).child(barangay).child("complaints").child(userId);

        loadInbox();
    }

    private void loadInbox() {
        complaintsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                inboxContainer.removeAllViews();

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Complaint complaint = snap.getValue(Complaint.class);
                    if (complaint != null) {
                        View card = createInboxCard(complaint);
                        inboxContainer.addView(card);
                    }
                }

                if (!snapshot.hasChildren()) {
                    Toast.makeText(r_inbox.this, "No complaints found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(r_inbox.this, "Failed to load inbox", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View createInboxCard(Complaint complaint) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.r_area_schedule_card, inboxContainer, false);

        TextView tvTime = cardView.findViewById(R.id.tvTime);
        TextView tvArea = cardView.findViewById(R.id.tvArea);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        ImageView icon = cardView.findViewById(R.id.StatusIcon);

        String status = complaint.complaint_status != null ? complaint.complaint_status : "pending";
        String previewText;

        if (complaint.response != null && !complaint.response.isEmpty()) {
            previewText = complaint.response;
        } else {
            previewText = complaint.message != null ? complaint.message : "No message";
        }

        // 🛠 Fix: Remove line breaks so it doesn't force multiple lines
        previewText = previewText.replace("\n", " ").trim();

        String displayTime = complaint.timestamp != null && !complaint.timestamp.isEmpty()
                ? formatTimestamp(complaint.timestamp) : "Pending";

        tvTime.setText(displayTime);
        tvArea.setText(previewText);
        tvStatus.setText(capitalizeStatus(status));

        switch (status.toLowerCase()) {
            case "accepted":
                icon.setImageResource(R.drawable.track_icon); // Use appropriate icon
                break;
            case "rejected":
                icon.setImageResource(R.drawable.track_icon); // Use appropriate icon
                break;
            default:
                icon.setImageResource(R.drawable.track_icon); // Default icon
                break;
        }

        cardView.setOnClickListener(v -> showComplaintDialog(complaint));

        return cardView;
    }

    private void showComplaintDialog(Complaint complaint) {
        String title = "Complaint Details";
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("Message:\n").append(complaint.message != null ? complaint.message : "No message").append("\n\n");
        messageBuilder.append("Status: ").append(capitalizeStatus(complaint.complaint_status)).append("\n\n");

        if (complaint.response != null && !complaint.response.isEmpty()) {
            messageBuilder.append("Response:\n").append(complaint.response);
        } else {
            messageBuilder.append("Response:\n(No response yet)");
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(messageBuilder.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private String formatTimestamp(String isoDate) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoDate);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return "Unknown time";
        }
    }

    private String capitalizeStatus(String status) {
        if (status == null || status.isEmpty()) return "Pending";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }

    // Firebase model class
    public static class Complaint {
        public String address;
        public String complaint_status;
        public String message;
        public String response;
        public String timestamp;

        public Complaint() {
        }
    }
}
