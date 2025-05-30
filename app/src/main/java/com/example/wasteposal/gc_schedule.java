package com.example.wasteposal;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

//Class
public class gc_schedule extends AppCompatActivity {

    private LinearLayout scheduleContainer;
    private DatabaseReference scheduleRef;
    private String city;
    private String barangay;

    private static final String[] DAY_ORDER = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    // Shows gc_schedule
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.r_schedule);

        // Get saved city and barangay from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        this.city = prefs.getString("city", null);
        this.barangay = prefs.getString("barangay", null);

        if (city == null || barangay == null) {
            Toast.makeText(this, "Missing user location data", Toast.LENGTH_SHORT).show();
            finish(); // Close screen if data is missing
            return;
        }

        scheduleContainer = findViewById(R.id.scheduleContainer);
        //Gets data from database
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app");
        scheduleRef = db.getReference(city).child(barangay).child("Areas");

        Toast.makeText(this, "Loading schedule...", Toast.LENGTH_SHORT).show();

        loadScheduleData();
    }

    //Gets data from db, sets up db structure, then calls another method to display
    private void loadScheduleData() {
        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, List<AreaSchedule>> groupedByDay = new HashMap<>();

                for (DataSnapshot areaSnap : snapshot.getChildren()) {
                    String areaName = areaSnap.getKey();

                    for (DataSnapshot daySnap : areaSnap.getChildren()) {
                        String rawDay = daySnap.getKey();
                        String normalizedDay = normalizeDay(rawDay);
                        if (normalizedDay.isEmpty()) continue;

                        Schedule schedule = daySnap.getValue(Schedule.class);
                        if (schedule == null || schedule.from == null || schedule.to == null || schedule.from.isEmpty() || schedule.to.isEmpty()) {
                            continue;
                        }

                        groupedByDay.putIfAbsent(normalizedDay, new ArrayList<>());
                        groupedByDay.get(normalizedDay).add(new AreaSchedule(areaName, rawDay, schedule));
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

    // Converts short strings from db to complete terms
    private String normalizeDay(String day) {
        if (day == null) return "";
        day = day.trim().toLowerCase();
        switch (day) {
            case "sun":
            case "sunday": return "Sunday";
            case "mon":
            case "monday": return "Monday";
            case "tue":
            case "tuesday": return "Tuesday";
            case "wed":
            case "wednesday": return "Wednesday";
            case "thu":
            case "thursday": return "Thursday";
            case "fri":
            case "friday": return "Friday";
            case "sat":
            case "saturday": return "Saturday";
            default: return "";
        }
    }

    // Organizes schedule by day, time, and area
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

    // Sorts time for each area
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

    // Converts to 12 Hour Format
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

    // Updates status icon based on text
    private void updateStatusIcon(ImageView iconView, String status) {
        switch (status.toLowerCase()) {
            case "scheduled":
                iconView.setImageResource(R.drawable.scheduled_icon);
                break;
            case "in-progress":
                iconView.setImageResource(R.drawable.in_progress_icon);
                break;
            case "done":
                iconView.setImageResource(R.drawable.done_icon);
                break;
            default:
                iconView.setImageResource(R.drawable.track_icon);
                break;
        }
    }

    // Schedule card for collector
    private View createScheduleCard(AreaSchedule areaSchedule) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.gc_area_schedule_card, scheduleContainer, false);

        TextView timeRange = cardView.findViewById(R.id.tvTime);
        TextView areaName = cardView.findViewById(R.id.tvArea);
        TextView status = cardView.findViewById(R.id.tvStatus);
        Button btnStatusAction = cardView.findViewById(R.id.btnStatusAction);
        ImageView statusIcon = cardView.findViewById(R.id.StatusIcon);

        String fromTimeFormatted = convertTo12HourFormat(areaSchedule.schedule.from);
        String toTimeFormatted = convertTo12HourFormat(areaSchedule.schedule.to);
        timeRange.setText(fromTimeFormatted + " - " + toTimeFormatted);
        areaName.setText(areaSchedule.areaName);

        String statusValue = areaSchedule.schedule.status;
        if (statusValue == null) statusValue = "scheduled";

        status.setText(capitalizeStatus(statusValue));
        updateButtonAppearance(btnStatusAction, statusValue);
        updateStatusIcon(statusIcon, statusValue);

        // Button press with confirmation
        btnStatusAction.setOnClickListener(v -> {
            String currentStatus = areaSchedule.schedule.status;
            if (currentStatus == null) currentStatus = "scheduled";

            String nextStatus;
            switch (currentStatus) {
                case "scheduled":
                    nextStatus = "in-progress";
                    break;
                case "in-progress":
                    nextStatus = "done";
                    break;
                case "done":
                default:
                    nextStatus = "scheduled";
                    break;
            }

            // Build confirmation message
            String message;
            switch (nextStatus) {
                case "in-progress":
                    message = "Mark this area as In-Progress?";
                    break;
                case "done":
                    message = "Mark this area as Done?";
                    break;
                case "scheduled":
                    message = "Reset this area to Scheduled?";
                    break;
                default:
                    message = "Are you sure you want to proceed?";
            }

            // Show confirmation dialog
            new AlertDialog.Builder(gc_schedule.this)
                    .setTitle("Confirm Action")
                    .setMessage(message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (nextStatus.equals("in-progress")) {
                            checkIfAnyInProgress(areaSchedule, () -> {
                                updateStatusInDatabase(areaSchedule, nextStatus, status, btnStatusAction, statusIcon);
                            }, () -> {
                                Toast.makeText(gc_schedule.this, "Only one area can be In-Progress at a time.", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            updateStatusInDatabase(areaSchedule, nextStatus, status, btnStatusAction, statusIcon);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return cardView;
    }

    // No multiple In-Progress
    private void checkIfAnyInProgress(AreaSchedule current, Runnable onNoneFound, Runnable onFound) {
        scheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot areaSnap : snapshot.getChildren()) {
                    String areaName = areaSnap.getKey();
                    for (DataSnapshot daySnap : areaSnap.getChildren()) {
                        String status = daySnap.child("status").getValue(String.class);
                        if ("in-progress".equalsIgnoreCase(status)) {
                            if (areaName.equals(current.areaName) && daySnap.getKey().equals(current.day)) {
                                continue;
                            }
                            onFound.run();
                            return;
                        }
                    }
                }
                onNoneFound.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(gc_schedule.this, "Error checking status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Updates Firebase and UI
    private void updateStatusInDatabase(AreaSchedule areaSchedule, String newStatus, TextView statusText, Button button, ImageView icon) {
        scheduleRef.child(areaSchedule.areaName)
                .child(areaSchedule.day)
                .child("status")
                .setValue(newStatus);

        areaSchedule.schedule.status = newStatus;
        statusText.setText(capitalizeStatus(newStatus));
        updateButtonAppearance(button, newStatus);
        updateStatusIcon(icon, newStatus);
    }
    private String capitalizeStatus(String status) {
        switch (status) {
            case "scheduled": return "Scheduled";
            case "in-progress": return "In-Progress";
            case "done": return "Done";
            default: return "Unknown";
        }
    }

    // Progress Button
    private void updateButtonAppearance(Button button, String status) {
        switch (status) {
            case "scheduled":
                button.setText("Notify");
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.darkgreen));
                button.setEnabled(true);
                break;
            case "in-progress":
                button.setText("Done?");
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lightgreen));
                button.setEnabled(true);
                break;
            case "done":
                button.setText("Done");
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lightgrey));
                button.setEnabled(true);
                break;
            default:
                button.setText("Unknown");
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lightgrey));
                button.setEnabled(false);
                break;
        }
    }

    // Data model classes
    public static class Schedule {
        public String from;
        public String to;
        public String status;

        public Schedule() {}
    }

    public static class AreaSchedule {
        public String areaName;
        public String day; // e.g., "Mon", "Tue"
        public Schedule schedule;

        public AreaSchedule(String areaName, String day, Schedule schedule) {
            this.areaName = areaName;
            this.day = day;
            this.schedule = schedule;
        }
    }
}