package com.example.wasteposal;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class gc_schedule extends AppCompatActivity {

    private LinearLayout scheduleContainer;
    private DatabaseReference scheduleRef;
    private String city = "Makati";
    private String barangay = "Magallanes";

    private static final String[] DAY_ORDER = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.r_schedule);

        scheduleContainer = findViewById(R.id.scheduleContainer);
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app");
        scheduleRef = db.getReference(city).child(barangay).child("Areas");

        Toast.makeText(this, "Loading data...", Toast.LENGTH_SHORT).show();
        loadScheduleData();
    }

    private void loadScheduleData() {
        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<AreaSchedule>> groupedByDay = new HashMap<>();

                for (DataSnapshot areaSnap : snapshot.getChildren()) {
                    String areaName = areaSnap.getKey();
                    Schedule schedule = areaSnap.getValue(Schedule.class);
                    if (schedule == null) continue;

                    if (schedule.days != null) {
                        for (String day : schedule.days) {
                            String normalizedDay = normalizeDay(day);
                            if (normalizedDay.isEmpty()) continue;
                            groupedByDay.putIfAbsent(normalizedDay, new ArrayList<>());
                            groupedByDay.get(normalizedDay).add(new AreaSchedule(areaName, schedule));
                        }
                    }
                }

                displayGroupedSchedule(groupedByDay);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gc_schedule.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String normalizeDay(String day) {
        if (day == null) return "";
        day = day.trim().toLowerCase();
        switch (day) {
            case "sun":
                return "Sunday";
            case "mon":
                return "Monday";
            case "tue":
                return "Tuesday";
            case "wed":
                return "Wednesday";
            case "thu":
                return "Thursday";
            case "fri":
                return "Friday";
            case "sat":
                return "Saturday";
            default:
                return "";
        }
    }

    private void displayGroupedSchedule(Map<String, List<AreaSchedule>> groupedData) {
        scheduleContainer.removeAllViews();

        for (String day : DAY_ORDER) {
            List<AreaSchedule> schedules = groupedData.get(day);
            if (schedules == null || schedules.isEmpty()) continue;

            sortSchedulesByFromTime(schedules);

            TextView dayHeader = new TextView(this);
            dayHeader.setText(day);
            dayHeader.setTextSize(35f);
            dayHeader.setTypeface(null, Typeface.BOLD);
            dayHeader.setGravity(Gravity.END);
            dayHeader.setPadding(0, 20, 0, 20);
            scheduleContainer.addView(dayHeader);

            for (AreaSchedule areaSchedule : schedules) {
                View card = createScheduleCard(areaSchedule);
                scheduleContainer.addView(card);
            }
        }
    }
    // Convert time to 12-hour format
    private String convertTo12HourFormat(String time24) {
        try {
            java.text.SimpleDateFormat sdf24 = new java.text.SimpleDateFormat("HH:mm");
            java.text.SimpleDateFormat sdf12 = new java.text.SimpleDateFormat("hh:mm a");
            java.util.Date date = sdf24.parse(time24);
            return sdf12.format(date);
        } catch (Exception e) {
            return time24;
        }
    }
    private void sortSchedulesByFromTime(List<AreaSchedule> schedules) {
        java.text.SimpleDateFormat sdf24 = new java.text.SimpleDateFormat("HH:mm");

        schedules.sort((a, b) -> {
            try {
                java.util.Date timeA = sdf24.parse(a.schedule.from);
                java.util.Date timeB = sdf24.parse(b.schedule.from);
                return timeA.compareTo(timeB);
            } catch (Exception e) {
                return 0; // fallback, no change
            }
        });
    }
    private View createScheduleCard(AreaSchedule areaSchedule) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.gc_area_schedule_card, scheduleContainer, false);

        TextView timeRange = cardView.findViewById(R.id.tvTime);
        TextView areaName = cardView.findViewById(R.id.tvArea);
        TextView status = cardView.findViewById(R.id.tvStatus);
        Button btnStatusAction = cardView.findViewById(R.id.btnStatusAction);

        String fromTimeFormatted = convertTo12HourFormat(areaSchedule.schedule.from);
        String toTimeFormatted = convertTo12HourFormat(areaSchedule.schedule.to);
        timeRange.setText(fromTimeFormatted + " - " + toTimeFormatted);
        areaName.setText(areaSchedule.areaName);

        String statusValue = areaSchedule.schedule.status;
        if (statusValue == null) statusValue = "scheduled";

        status.setText(capitalizeStatus(statusValue));
        updateButtonAppearance(btnStatusAction, statusValue);

        btnStatusAction.setOnClickListener(v -> {
            String currentStatus = areaSchedule.schedule.status;
            if (currentStatus == null) currentStatus = "scheduled";

            // Button status
            String newStatus;
            switch (currentStatus) {
                case "scheduled":
                    newStatus = "ongoing";
                    break;
                case "ongoing":
                    newStatus = "done";
                    break;
                case "done":
                    newStatus = "scheduled";
                    break;
                default:
                    newStatus = "scheduled";
            }

            // Update Firebase
            scheduleRef.child(areaSchedule.areaName).child("status").setValue(newStatus);

            // Update model locally
            areaSchedule.schedule.status = newStatus;

            // Update UI
            status.setText(capitalizeStatus(newStatus));
            updateButtonAppearance(btnStatusAction, newStatus);
        });

        return cardView;
    }

    private String capitalizeStatus(String status) {
        switch (status) {
            case "scheduled": return "Scheduled";
            case "ongoing": return "Ongoing";
            case "done": return "Done";
            default: return "Unknown";
        }
    }

    private void updateButtonAppearance(Button button, String status) {
        switch (status) {
            case "scheduled": // Notify state
                button.setText("Notify");
                button.setBackgroundTintList(getResources().getColorStateList(R.color.darkgreen));
                button.setEnabled(true);
                break;
            case "ongoing":   // Complete state
                button.setText("Complete");
                button.setBackgroundTintList(getResources().getColorStateList(R.color.lightgreen));
                button.setEnabled(true);
                break;
            case "done":      // Completed state
                button.setText("Completed");
                button.setBackgroundTintList(getResources().getColorStateList(R.color.lightgrey));
                button.setEnabled(true);
                break;
            default:
                button.setText("Unknown");
                button.setBackgroundTintList(getResources().getColorStateList(R.color.lightgrey));
                button.setEnabled(false);
                break;
        }
    }

    public static class Schedule {
        public List<String> days;
        public String from;
        public String to;
        public String status;

        public Schedule() {}
    }

    public static class AreaSchedule {
        public String areaName;
        public Schedule schedule;

        public AreaSchedule(String areaName, Schedule schedule) {
            this.areaName = areaName;
            this.schedule = schedule;
        }
    }
}
