package com.example.wasteposal;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.util.Log;
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

public class r_schedule extends AppCompatActivity {

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
        Log.d("r_schedule", "loadScheduleData called");
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
                    Log.d("FIREBASE", "Area Name: " + areaName);

                    Schedule schedule = areaSnap.getValue(Schedule.class);
                    if (schedule == null) {
                        Log.w("FIREBASE", "Null schedule for area: " + areaName);
                        continue;
                    }

                    if (schedule.days != null) {
                        for (String day : schedule.days) {
                            String normalizedDay = normalizeDay(day);
                            if(normalizedDay.isEmpty()) {
                                Log.w("r_schedule", "Unknown day name '" + day + "' for area: " + areaName);
                                continue;
                            }
                            groupedByDay.putIfAbsent(normalizedDay, new ArrayList<>());
                            groupedByDay.get(normalizedDay).add(new AreaSchedule(areaName, schedule));
                            Log.d("FIREBASE", "Added schedule for: " + areaName + " on " + normalizedDay);
                        }
                    } else {
                        Log.w("FIREBASE", "Days list is null for area: " + areaName);
                    }
                }

                Log.d("r_schedule", "Grouped days from Firebase: " + groupedByDay.keySet());
                displayGroupedSchedule(groupedByDay);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(r_schedule.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                Log.e("FIREBASE", "Database error: " + error.getMessage());
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
            Log.d("r_schedule", "Displaying day: " + day + ", schedules found: " + (schedules == null ? 0 : schedules.size()));

            if (schedules == null || schedules.isEmpty()) continue;

            sortSchedulesByFromTime(schedules);

            TextView dayHeader = new TextView(this);
            dayHeader.setText(day);
            dayHeader.setTextSize(35f);
            dayHeader.setTypeface(null, Typeface.BOLD);
            dayHeader.setGravity(Gravity.CENTER);
            dayHeader.setPadding(0, 20, 0, 20);
            scheduleContainer.addView(dayHeader);

            for (AreaSchedule areaSchedule : schedules) {
                View card = createScheduleCard(areaSchedule);
                scheduleContainer.addView(card);
            }
        }
    }
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
        View cardView = inflater.inflate(R.layout.r_area_schedule_card, scheduleContainer, false);

        TextView timeRange = cardView.findViewById(R.id.tvTime);
        TextView areaName = cardView.findViewById(R.id.tvArea);
        TextView status = cardView.findViewById(R.id.tvStatus);

        String fromTimeFormatted = convertTo12HourFormat(areaSchedule.schedule.from);
        String toTimeFormatted = convertTo12HourFormat(areaSchedule.schedule.to);
        timeRange.setText(fromTimeFormatted + " - " + toTimeFormatted);
        areaName.setText(areaSchedule.areaName);

        String statusValue = areaSchedule.schedule.status;
        if (statusValue == null || statusValue.isEmpty()) {
            statusValue = "Pending";
        }
        status.setText(capitalizeStatus(statusValue));

        return cardView;
    }

    // 🔧 Fix: Add capitalizeStatus() here
    private String capitalizeStatus(String status) {
        if (status == null) return "Pending";
        switch (status.toLowerCase()) {
            case "scheduled":
                return "Scheduled";
            case "ongoing":
                return "Ongoing";
            case "done":
                return "Done";
            case "pending":
                return "Pending";
            default:
                return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }
    }

    // 📦 Model Classes
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
