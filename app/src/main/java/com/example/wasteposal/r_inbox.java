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
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class r_inbox extends AppCompatActivity {

    private LinearLayout inboxContainer;
    private DatabaseReference dbRef;
    private String userAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Make status and nav bars transparent
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.r_inbox);

        AppCompatImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

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

        // Load the user address first
        dbRef.child(city).child(barangay).child("User").child(userId).child("address")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Object addressObj = snapshot.getValue();
                        if (addressObj instanceof Map) {
                            Map<String, Object> addressMap = (Map<String, Object>) addressObj;
                            StringBuilder addressBuilder = new StringBuilder();
                            for (Object value : addressMap.values()) {
                                addressBuilder.append(value).append(", ");
                            }
                            userAddress = addressBuilder.toString().replaceAll(", $", "");
                        } else if (addressObj instanceof String) {
                            // It's a simple string address
                            userAddress = (String) addressObj;
                        } else {
                            userAddress = "No address found";
                        }

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
                        if (userAddress == null) return; // safety null check
                        String lowerUserAddress = userAddress.toLowerCase();

                        for (DataSnapshot areaSnap : snapshot.getChildren()) {
                            String areaKey = areaSnap.getKey();
                            if (areaKey == null) continue;
                            areaKey = areaKey.toLowerCase();

                            if (lowerUserAddress.contains(areaKey)) {
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

        String status = complaint.complaint_status != null ? complaint.complaint_status.toLowerCase() : "pending";
        String previewText;
        if (complaint.response != null && !complaint.response.isEmpty()) {
            Map<String, Object> res = complaint.response;
            if (res.containsKey("acceptedmessage_response")) {
                Map<String, Object> accepted = (Map<String, Object>) res.get("acceptedmessage_response");
                previewText = accepted.get("message").toString();
            } else if (res.containsKey("rejectedmessage_response")) {
                Map<String, Object> rejected = (Map<String, Object>) res.get("rejectedmessage_response");
                previewText = rejected.get("message").toString();
            } else {
                previewText = "No response message";
            }
        } else {
            previewText = (complaint.message != null) ? complaint.message : "No message";
        }

        previewText = previewText.replace("\n", " ").trim();
        String displayTime = (complaint.timestamp != null && !complaint.timestamp.isEmpty())
                ? formatTimestamp(complaint.timestamp) : "Pending";

        tvTime.setText(displayTime);
        tvArea.setText(previewText);
        tvStatus.setText(capitalizeStatus(status));

        // Color code based on status
        switch (status) {
            case "accepted":
                tvStatus.setTextColor(Color.parseColor("#388E3C"));
                icon.setImageResource(R.drawable.accepted_icon);
                break;
            case "rejected":
                tvStatus.setTextColor(Color.parseColor("#D32F2F"));
                icon.setImageResource(R.drawable.rejected_icon);
                break;
            case "pending":
            default:
                tvStatus.setTextColor(Color.parseColor("#F9A825"));
                icon.setImageResource(R.drawable.pending_icon);
                break;
        }

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
        icon.setImageResource(R.drawable.announcement);

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
        if (isoDate == null || isoDate.isEmpty()) return "Unknown time";
        String normalized = isoDate;
        if (isoDate.endsWith("Z")) {
            normalized = isoDate.substring(0, isoDate.length() - 1);
        }

        // Try multiple date formats for robustness
        String[] possibleFormats = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
        };

        Date date = null;
        for (String format : possibleFormats) {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat(format, Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                date = isoFormat.parse(normalized);
                if (date != null) break;
            } catch (ParseException ignored) {
            }
        }

        if (date == null) return "Unknown time";

        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        return displayFormat.format(date);
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
            message.append("Response:\n");
            Map<String, Object> res = complaint.response;

            if (res.containsKey("acceptedmessage_response")) {
                Map<String, Object> accepted = (Map<String, Object>) res.get("acceptedmessage_response");
                String adminMessage = (String) accepted.get("message");
                message.append(adminMessage);
            } else if (res.containsKey("rejectedmessage_response")) {
                Map<String, Object> rejected = (Map<String, Object>) res.get("rejectedmessage_response");
                String adminMessage = (String) rejected.get("message");
                message.append(adminMessage);
            } else {
                message.append("No specific response found.");
            }
        }

        new AlertDialog.Builder(r_inbox.this)
                .setTitle("Complaint Details")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // Firebase model classes
    public static class Complaint {
        public String address;
        public String complaint_status;
        public String message;
        public Map<String, Object> response;
        public String timestamp;

        public Complaint() {
        }
    }


    public static class Announcement {
        public String from;
        public String message;
        public String timestamp;
        public Announcement() {
        }
    }
}
