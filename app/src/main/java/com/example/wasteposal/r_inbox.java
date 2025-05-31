package com.example.wasteposal;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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
    private DatabaseReference dbRef;
    private String userAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge to edge (disable default fitting of system windows)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Make status and nav bars transparent
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Optional: Set status/navigation bar icon colors (false = light icons)
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, decorView);
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        setContentView(R.layout.r_inbox);

        inboxContainer = findViewById(R.id.inboxContainer);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        String city = prefs.getString("city", null);
        String barangay = prefs.getString("barangay", null);

        if (userId == null || city == null || barangay == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app").getReference();

        // First load the user address
        dbRef.child(city).child(barangay).child("User").child(userId).child("address")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userAddress = snapshot.getValue(String.class);
                        loadInbox(userId, city, barangay);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(r_inbox.this, "Failed to get user address", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadInbox(String userId, String city, String barangay) {
        inboxContainer.removeAllViews();

        // Load complaints
        dbRef.child(city).child(barangay).child("complaints").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Complaint complaint = snap.getValue(Complaint.class);
                            if (complaint != null) {
                                View card = createInboxCard(complaint);
                                inboxContainer.addView(card);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(r_inbox.this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
                    }
                });

        // Load announcements based on address match
        dbRef.child(city).child(barangay).child("announcements")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot areaSnap : snapshot.getChildren()) {
                            String areaKey = areaSnap.getKey().toLowerCase();

                            if (userAddress != null && userAddress.toLowerCase().contains(areaKey)) {
                                for (DataSnapshot annSnap : areaSnap.getChildren()) {
                                    Announcement ann = annSnap.getValue(Announcement.class);
                                    if (ann != null) {
                                        View card = createAnnouncementCard(ann);
                                        inboxContainer.addView(card);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(r_inbox.this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
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
        String previewText = (complaint.response != null && !complaint.response.isEmpty()) ?
                complaint.response : (complaint.message != null ? complaint.message : "No message");

        previewText = previewText.replace("\n", " ").trim();
        String displayTime = complaint.timestamp != null && !complaint.timestamp.isEmpty()
                ? formatTimestamp(complaint.timestamp) : "Pending";

        tvTime.setText(displayTime);
        tvArea.setText(previewText);
        tvStatus.setText(capitalizeStatus(status));
        icon.setImageResource(R.drawable.track_icon);

        cardView.setOnClickListener(v -> showComplaintDialog(complaint));
        return cardView;
    }

    private View createAnnouncementCard(Announcement ann) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.r_area_schedule_card, inboxContainer, false);

        TextView tvTime = cardView.findViewById(R.id.tvTime);
        TextView tvArea = cardView.findViewById(R.id.tvArea);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        ImageView icon = cardView.findViewById(R.id.StatusIcon);

        tvTime.setText(formatTimestamp(ann.timestamp));
        tvArea.setText(ann.message);
        tvStatus.setText("Announcement");
        icon.setImageResource(R.drawable.track_icon); // You can use a megaphone icon instead if desired

        cardView.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Announcement")
                    .setMessage(ann.message)
                    .setPositiveButton("Close", null)
                    .show();
        });

        return cardView;
    }

    private String formatTimestamp(String isoDate) {
        try {
            // Adjusted to support fractional seconds and missing 'Z'
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoDate);

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace(); // for debugging
            return "Unknown time";
        }
    }


    private String capitalizeStatus(String status) {
        if (status == null || status.isEmpty()) return "Pending";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }
    private void showComplaintDialog(Complaint complaint) {
        StringBuilder message = new StringBuilder();

        if (complaint.message != null && !complaint.message.isEmpty()) {
            message.append("Complaint:\n").append(complaint.message).append("\n\n");
        }
        if (complaint.response != null && !complaint.response.isEmpty()) {
            message.append("Response:\n").append(complaint.response);
        }

        if (message.length() == 0) {
            message.append("No details available.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Complaint Details")
                .setMessage(message.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    // Firebase model classes
    public static class Complaint {
        public String address;
        public String complaint_status;
        public String message;
        public String response;
        public String timestamp;

        public Complaint() {
        }
    }

    public static class Announcement {
        public String from;
        public String message;
        public String timestamp;
        public String type;

        public Announcement() {
        }
    }
}
