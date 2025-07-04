package com.example.wasteposal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class gc_dashboard extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge to edge (disable default fitting of system windows)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.gc_dashboard);

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

        // Schedule button
        LinearLayout scheduleButton = findViewById(R.id.gc_sched_button);
        if (scheduleButton != null) {
            scheduleButton.setOnClickListener(v -> {
                Intent intent = new Intent(gc_dashboard.this, gc_schedule.class);
                startActivity(intent);
            });
        }

        // Track button
        LinearLayout trackBtn = findViewById(R.id.gc_track_btn);
        if (trackBtn != null) {
            trackBtn.setOnClickListener(v -> {
                Intent intent = new Intent(gc_dashboard.this, gc_track.class);
                startActivity(intent);
            });
        }

        // Inbox button
        FrameLayout InboxBtn = findViewById(R.id.gc_inbox_button);
        if (InboxBtn != null) {
            InboxBtn.setOnClickListener(v -> {
                Intent intent = new Intent(gc_dashboard.this, gc_inbox.class);
                startActivity(intent);
            });
        }

        // Logout button
        Button logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                Intent intent = new Intent(gc_dashboard.this, login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
