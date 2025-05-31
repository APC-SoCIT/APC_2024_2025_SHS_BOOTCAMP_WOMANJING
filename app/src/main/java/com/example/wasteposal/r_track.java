package com.example.wasteposal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class r_track extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Marker collectorMarker;

    // Firebase reference
    private final String city = "Makati";
    private final String barangay = "Magallanes";
    private final String collectorId = "01-0002";

    // Views for bottom panel
    private TextView statusMessage, currentTime, timeRange, locationLabel, currentArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge to edge (disable default fitting of system windows)
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Make status and nav bars transparent
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        // Optional: Set status/navigation bar icon colors (false = light icons)
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat insetsController = new WindowInsetsControllerCompat(window, decorView);
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);

        setContentView(R.layout.r_track);

        // Get references to bottom layout elements
        statusMessage = findViewById(R.id.status_message);
        currentTime = findViewById(R.id.current_time);
        timeRange = findViewById(R.id.time_range);
        locationLabel = findViewById(R.id.location_label);
        currentArea = findViewById(R.id.current_area);

        // Load schedule status from Firebase
        fetchInProgressSchedule();

        // Set up map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(r_track.this, r_dashboard.class);
            startActivity(intent);
        });
    }

    private void fetchInProgressSchedule() {
        DatabaseReference scheduleRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference(city)
                .child(barangay)
                .child("Areas");

        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String inProgressArea = null;
                String fromTime = null;
                String toTime = null;

                for (DataSnapshot areaSnap : snapshot.getChildren()) {
                    String areaName = areaSnap.getKey();

                    for (DataSnapshot daySnap : areaSnap.getChildren()) {
                        String status = String.valueOf(daySnap.child("status").getValue());
                        if ("in-progress".equalsIgnoreCase(status)) {
                            fromTime = String.valueOf(daySnap.child("from").getValue());
                            toTime = String.valueOf(daySnap.child("to").getValue());
                            inProgressArea = areaName;
                            break;
                        }
                    }

                    if (inProgressArea != null) break;
                }

                if (inProgressArea != null) {
                    statusMessage.setText("Garbage Collection In Progress");
                    currentTime.setText("Estimated Pickup Time:");
                    timeRange.setText(formatTimeRange(fromTime, toTime));
                    locationLabel.setText("The Garbage Truck is At:");
                    currentArea.setText(inProgressArea);
                } else {
                    statusMessage.setText("No Ongoing Collection");
                    currentTime.setText("");
                    timeRange.setText("");
                    locationLabel.setText("");
                    currentArea.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(r_track.this, "Failed to load schedule", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatTimeRange(String from, String to) {
        return convertTo12HourFormat(from) + " - " + convertTo12HourFormat(to);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
        listenToCollectorLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void listenToCollectorLocation() {
        DatabaseReference locationRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("CollectorLocation")
                .child(collectorId);

        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double latitude = snapshot.child("latitude").getValue(Double.class);
                Double longitude = snapshot.child("longitude").getValue(Double.class);

                if (latitude != null && longitude != null) {
                    LatLng collectorLocation = new LatLng(latitude, longitude);
                    updateCollectorMarker(collectorLocation);
                } else {
                    Toast.makeText(r_track.this, "Collector location not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(r_track.this, "Failed to load location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCollectorMarker(LatLng location) {
        if (collectorMarker == null) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.truck_icon);
            Bitmap smallIcon = Bitmap.createScaledBitmap(icon, 100, 100, false);

            collectorMarker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Garbage Collector")
                    .icon(BitmapDescriptorFactory.fromBitmap(smallIcon)));
        } else {
            collectorMarker.setPosition(location);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
