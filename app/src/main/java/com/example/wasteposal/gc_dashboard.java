package com.example.wasteposal;

import android.content.Intent; // <-- Add this
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout; // <-- Add this
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class gc_dashboard extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Schedule
        LinearLayout scheduleButton = findViewById(R.id.gc_sched_button);
        if (scheduleButton != null) {
            scheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(gc_dashboard.this, gc_schedule.class);
                    startActivity(intent);
                }
            });
        }
        // GC Track
        LinearLayout trackBtn = findViewById(R.id.gc_track_btn);
        trackBtn.setOnClickListener(v -> {
            Intent intent = new Intent(gc_dashboard.this, gc_track.class);
            startActivity(intent);
        });
    }
}
