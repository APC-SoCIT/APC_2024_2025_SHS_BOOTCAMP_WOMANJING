package com.example.wasteposal;

import android.content.Intent; // <-- Add this
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout; // <-- Add this
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class r_dashboard extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.r_dashboard);

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
            scheduleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(r_dashboard.this, r_schedule.class);
                    startActivity(intent);
                }
            });
        }
        LinearLayout reportButton = findViewById(R.id.report_button);
        if (reportButton != null) {
            reportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(r_dashboard.this, report.class);
                    startActivity(intent);
                }
            });
        }
    }
}
