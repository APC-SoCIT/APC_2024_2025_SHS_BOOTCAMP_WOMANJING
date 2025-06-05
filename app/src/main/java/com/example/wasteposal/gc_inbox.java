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

public class gc_inbox extends AppCompatActivity {

    private LinearLayout inboxContainer;
    private DatabaseReference dbRef;
    private String userAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
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
                            userAddress = (String) addressObj;
                        } else {
                            userAddress = "No address found";
                        }

                        loadInbox(userId, city, barangay);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(gc_inbox.this, "Failed to get user address", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadInbox(String userId, String city, String barangay) {
        inboxContainer.removeAllViews();

        dbRef.child(city).child(barangay).child("complaints")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String residentId = userSnap.getKey();
                            if (residentId == null || residentId.equals(userId)) continue;

                            for (DataSnapshot complaintSnap : userSnap.getChildren()) {
                                Complaint complaint = complaintSnap.getValue(Complaint.class);
                                if (complaint != null && "accepted".equalsIgnoreCase(complaint.complaint_status)) {
                                    dbRef.child(city).child(barangay).child("User").child(residentId).child("address")
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    String address = "Unknown address";
                                                    Object addressObj = snapshot.getValue();
                                                    if (addressObj instanceof Map) {
                                                        StringBuilder sb = new StringBuilder();
                                                        for (Object value : ((Map) addressObj).values()) {
                                                            sb.append(value).append(", ");
                                                        }
                                                        address = sb.toString().replaceAll(", $", "");
                                                    } else if (addressObj instanceof String) {
                                                        address = (String) addressObj;
                                                    }
                                                    View card = createInboxCard(complaint, address);
                                                    inboxContainer.addView(card);
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    View card = createInboxCard(complaint, "Unknown address");
                                                    inboxContainer.addView(card);
                                                }
                                            });
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(gc_inbox.this, "Failed to load complaints", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private View createInboxCard(Complaint complaint, String residentAddress) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.r_area_schedule_card, inboxContainer, false);

        TextView tvTime = cardView.findViewById(R.id.tvTime);
        TextView tvArea = cardView.findViewById(R.id.tvArea);
        TextView tvStatus = cardView.findViewById(R.id.tvStatus);
        ImageView icon = cardView.findViewById(R.id.StatusIcon);

        String status = complaint.complaint_status != null ? complaint.complaint_status.toLowerCase() : "pending";
        String previewText = (complaint.message != null) ? complaint.message : "No message";

        previewText = previewText.replace("\n", " ").trim();
        String displayTime = (complaint.timestamp != null && !complaint.timestamp.isEmpty())
                ? formatTimestamp(complaint.timestamp) : "Pending";

        tvTime.setText(displayTime);
        tvArea.setText(previewText + "\n(" + residentAddress + ")");
        tvStatus.setText(capitalizeStatus(status));

        switch (status) {
            case "accepted":
                tvStatus.setTextColor(Color.parseColor("#388E3C"));
                icon.setImageResource(R.drawable.track_icon);
                break;
            case "rejected":
                tvStatus.setTextColor(Color.parseColor("#D32F2F"));
                icon.setImageResource(R.drawable.track_icon);
                break;
            case "pending":
            default:
                tvStatus.setTextColor(Color.parseColor("#F9A825"));
                icon.setImageResource(R.drawable.track_icon);
                break;
        }

        cardView.setOnClickListener(v -> showComplaintDialog(complaint, residentAddress));
        return cardView;
    }

    private void showComplaintDialog(Complaint complaint, String residentAddress) {
        StringBuilder message = new StringBuilder();

        message.append("From: ").append(residentAddress).append("\n\n");

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

        new AlertDialog.Builder(gc_inbox.this)
                .setTitle("Complaint Details")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private String formatTimestamp(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "Unknown time";
        String normalized = isoDate.endsWith("Z") ? isoDate.substring(0, isoDate.length() - 1) : isoDate;

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(normalized);
            if (date != null) {
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                return displayFormat.format(date);
            }
        } catch (ParseException ignored) {
        }
        return "Unknown time";
    }

    private String capitalizeStatus(String status) {
        if (status == null || status.isEmpty()) return "Pending";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }

    public static class Complaint {
        public String address;
        public String complaint_status;
        public String message;
        public Map<String, Object> response;
        public String timestamp;

        public Complaint() {
        }
    }
}
