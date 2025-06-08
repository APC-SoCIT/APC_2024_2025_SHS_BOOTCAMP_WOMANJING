package com.example.wasteposal;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

public class r_schedule extends AppCompatActivity {

    private LinearLayout scheduleContainer;
    private DatabaseReference scheduleRef;
    private String city, barangay;
    private AppCompatImageButton backButton;

    private static final String[] DAY_ORDER = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge to edge (disable default fitting of system windows)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.r_schedule);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        city = prefs.getString("city", null);
        barangay = prefs.getString("barangay", null);

        if (city == null || barangay == null) {
            Toast.makeText(this, "Missing user location data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scheduleContainer = findViewById(R.id.scheduleContainer);

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app");
        scheduleRef = db.getReference(city).child(barangay).child("Areas");

        Toast.makeText(this, "Loading schedule...", Toast.LENGTH_SHORT).show();

        loadScheduleData();
    }

    private void loadScheduleData() {
        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<AreaSchedule>> groupedByDay = new HashMap<>();

                for (DataSnapshot areaSnap : snapshot.getChildren()) {
                    String areaName = areaSnap.getKey();

                    for (DataSnapshot daySnap : areaSnap.getChildren()) {
                        String dayKey = normalizeDay(daySnap.getKey());
                        if (dayKey.isEmpty()) continue;

                        Schedule schedule = daySnap.getValue(Schedule.class);
                        if (schedule == null || schedule.from == null || schedule.to == null) continue;

                        groupedByDay.putIfAbsent(dayKey, new ArrayList<>());
                        groupedByDay.get(dayKey).add(new AreaSchedule(areaName, schedule));
                    }
                }

                displayGroupedSchedule(groupedByDay);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(r_schedule.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String normalizeDay(String day) {
        if (day == null) return "";
        switch (day.trim().toLowerCase()) {
            case "sun": return "Sunday";
            case "mon": return "Monday";
            case "tue": return "Tuesday";
            case "wed": return "Wednesday";
            case "thu": return "Thursday";
            case "fri": return "Friday";
            case "sat": return "Saturday";
            default: return "";
        }
    }

    private void displayGroupedSchedule(Map<String, List<AreaSchedule>> groupedData) {
        scheduleContainer.removeAllViews();

        for (String day : DAY_ORDER) {
            List<AreaSchedule> schedules = groupedData.get(day);
            if (schedules == null || schedules.isEmpty()) continue;

            sortSchedulesByFromTime(schedules);

            // Day header
            TextView dayHeader = new TextView(this);
            dayHeader.setText(day);
            dayHeader.setTextSize(35f);
            dayHeader.setTypeface(null, Typeface.BOLD);
            dayHeader.setGravity(Gravity.CENTER);
            dayHeader.setPadding(0, 20, 0, 20);
            scheduleContainer.addView(dayHeader);

            // Cards
            for (AreaSchedule areaSchedule : schedules) {
                View card = createScheduleCard(areaSchedule);
                scheduleContainer.addView(card);
            }
        }
    }

    private void sortSchedulesByFromTime(List<AreaSchedule> schedules) {
        SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());

        schedules.sort((a, b) -> {
            try {
                Date timeA = sdf24.parse(a.schedule.from);
                Date timeB = sdf24.parse(b.schedule.from);
                return timeA.compareTo(timeB);
            } catch (Exception e) {
                return 0;
            }
        });
    }

    private String convertTo12HourFormat(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf24.parse(time24);
            return sdf12.format(date);
        } catch (Exception e) {
            return time24;
        }
    }

    private View createScheduleCard(AreaSchedule areaSchedule) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.r_area_schedule_card, scheduleContainer, false);

        TextView timeRange = cardView.findViewById(R.id.tvTime);
        TextView areaName = cardView.findViewById(R.id.tvArea);
        TextView status = cardView.findViewById(R.id.tvStatus);
        ImageView statusIcon = cardView.findViewById(R.id.StatusIcon);

        String fromFormatted = convertTo12HourFormat(areaSchedule.schedule.from);
        String toFormatted = convertTo12HourFormat(areaSchedule.schedule.to);
        timeRange.setText(fromFormatted + " - " + toFormatted);
        areaName.setText(areaSchedule.areaName);

        String currentStatus = (areaSchedule.schedule.status == null || areaSchedule.schedule.status.isEmpty())
                ? "Pending" : areaSchedule.schedule.status.toLowerCase();

        status.setText(capitalizeStatus(currentStatus));

        switch (currentStatus) {
            case "scheduled":
                statusIcon.setImageResource(R.drawable.scheduled_icon);
                break;
            case "in-progress":
                statusIcon.setImageResource(R.drawable.in_progress_icon);
                break;
            case "done":
                statusIcon.setImageResource(R.drawable.done_icon);
                break;
            default:
                statusIcon.setImageResource(R.drawable.track_icon);
                break;
        }

        return cardView;
    }

    private String capitalizeStatus(String status) {
        switch (status.toLowerCase()) {
            case "scheduled": return "Scheduled";
            case "in-progress": return "In-Progress";
            case "done": return "Done";
            case "pending": return "Pending";
            default:
                return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }
    }

    // Firebase model classes
    public static class Schedule {
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
