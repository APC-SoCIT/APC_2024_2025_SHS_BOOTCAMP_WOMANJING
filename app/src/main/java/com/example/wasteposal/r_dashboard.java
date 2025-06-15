package com.example.wasteposal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
public class r_dashboard extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.r_dashboard);
        // Edge to edge (disable default fitting of system windows)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // FAQ button ngani
        ImageButton helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> {
            FAQDialogHelper.showFAQ(
                    r_dashboard.this,
                    "<b>• Check your Schedule</b><br>" +
                            "- Tap the calendar icon labeled “Schedule”.<br>" +
                            "- This shows the upcoming garbage collection days in your area.<br><br>" +

                            "<b>• Track Garbage Truck</b><br>" +
                            "- Tap the “Track” icon to see where the garbage truck is now.<br>" +
                            "- It shows real-time movement on the map if tracking is active.<br><br>" +

                            "<b>• Report a Problem</b><br>" +
                            "- Tap “Report” if your garbage wasn’t collected or there’s an issue.<br>" +
                            "- Fill in the details and submit your complaint.<br><br>" +

                            "<b>• Read Messages</b><br>" +
                            "- Tap the “Inbox” icon to see announcements or replies to your reports.<br>" +
                            "- Important messages from the barangay or garbage team will appear here.<br><br>" +

                            "<b>• Logout Safely</b><br>" +
                            "- Tap the “Logout” button at the bottom when you're done.<br>" +
                            "- This helps protect your account if others use the phone.",
                    R.drawable.faq_icon
            );
        });


        View root = findViewById(R.id.rootLayout);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } else {
            System.err.println("ERROR: rootLayout not found in dashboard layout!");
        }

        // Schedule
        LinearLayout scheduleButton = findViewById(R.id.r_sched_button);
        if (scheduleButton != null) {
            scheduleButton.setOnClickListener(v -> {
                Intent intent = new Intent(r_dashboard.this, r_schedule.class);
                startActivity(intent);
            });
        }

        // Report
        LinearLayout reportButton = findViewById(R.id.report_button);
        if (reportButton != null) {
            reportButton.setOnClickListener(v -> {
                Intent intent = new Intent(r_dashboard.this, report.class);
                startActivity(intent);
            });
        }

        // Track
        LinearLayout trackButton = findViewById(R.id.r_track_button);
        if (trackButton != null) {
            trackButton.setOnClickListener(v -> {
                Intent intent = new Intent(r_dashboard.this, r_track.class);
                startActivity(intent);
            });
        }

        // Inbox
        FrameLayout inboxButton = findViewById(R.id.r_inbox_button);
        if (inboxButton != null) {
            inboxButton.setOnClickListener(v -> {
                startActivity(new Intent(r_dashboard.this, r_inbox.class));
            });
        }

        // Logout
        Button logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                Intent intent = new Intent(r_dashboard.this, login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
