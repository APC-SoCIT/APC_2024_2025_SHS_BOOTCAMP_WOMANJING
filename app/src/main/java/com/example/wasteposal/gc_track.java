package com.example.wasteposal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class gc_track extends AppCompatActivity {

    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 10;
    public static final int PERMISSION_FINE_LOCATION = 99;

    private TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;
    private Button btn_newWaypoint, btn_showWayPointList, btn_showMap;
    private Switch sw_locationupdates, sw_gps;

    private boolean updateOn = false;
    private Location currentLocation;
    private List<Location> savedLocations;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final String city = "Makati";
    private final String barangay = "Magallanes";
    private final String collectorId = "01-0002";

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

        setContentView(R.layout.gc_track);

        initViews();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL * 1000L);
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL * 1000L);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateUIValues(location);
                }
            }
        };

        btn_newWaypoint.setOnClickListener(v -> {
            MyApplication myApplication = (MyApplication) getApplicationContext();
            savedLocations = myApplication.getMyLocations();
            if (savedLocations != null && currentLocation != null) {
                savedLocations.add(currentLocation);
                Toast.makeText(gc_track.this, "Waypoint added", Toast.LENGTH_SHORT).show();
                tv_wayPointCounts.setText(String.valueOf(savedLocations.size()));
            } else {
                Toast.makeText(gc_track.this, "Current location not available", Toast.LENGTH_SHORT).show();
            }
        });

        btn_showMap.setOnClickListener(v -> {
            Intent intent = new Intent(gc_track.this, r_track.class);
            startActivity(intent);
        });

        sw_gps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                tv_sensor.setText("Using GPS sensors");
            } else {
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                tv_sensor.setText("Using Towers + WIFI");
            }
            if (updateOn) {
                stopLocationUpdates();
                startLocationUpdates();
            }
        });

        sw_locationupdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        });

        updateGPS();
    }

    private void initViews() {
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);

        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);

        btn_newWaypoint = findViewById(R.id.btn_newWaypoint);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);
        btn_showMap = findViewById(R.id.btn_showMap);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        tv_updates.setText("Location is being tracked");
        updateOn = true;
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        updateOn = false;

        tv_updates.setText("Location is NOT being tracked");
        tv_lat.setText("-");
        tv_lon.setText("-");
        tv_speed.setText("-");
        tv_address.setText("-");
        tv_accuracy.setText("-");
        tv_altitude.setText("-");
        tv_sensor.setText("-");
    }

    private void updateGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = location;
                    updateUIValues(location);
                } else {
                    Toast.makeText(gc_track.this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {
        currentLocation = location;

        tv_lat.setText(String.format(Locale.getDefault(), "%.6f", location.getLatitude()));
        tv_lon.setText(String.format(Locale.getDefault(), "%.6f", location.getLongitude()));
        tv_accuracy.setText(String.format(Locale.getDefault(), "%.2f meters", location.getAccuracy()));
        tv_altitude.setText(location.hasAltitude() ? String.format(Locale.getDefault(), "%.2f meters", location.getAltitude()) : "Not Available");
        tv_speed.setText(location.hasSpeed() ? String.format(Locale.getDefault(), "%.2f m/s", location.getSpeed()) : "Not Available");

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText((addresses != null && !addresses.isEmpty()) ? addresses.get(0).getAddressLine(0) : "Unable to get street address");
        } catch (Exception e) {
            tv_address.setText("Unable to get street address");
        }

        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();
        tv_wayPointCounts.setText(String.valueOf(savedLocations.size()));

        uploadLocationToFirebase(location);
    }

    private void uploadLocationToFirebase(Location location) {
        String city = "Makati";
        String barangay = "Magallanes";
        String collectorId = "01-0002"; // 🔄 Fixed collector ID

        DatabaseReference locationRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("CollectorLocation")
                .child(collectorId); // 🔄 Overwrites location

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("altitude", location.hasAltitude() ? location.getAltitude() : null);
        locationData.put("speed", location.hasSpeed() ? location.getSpeed() : null);
        locationData.put("timestamp", System.currentTimeMillis());

        locationRef.setValue(locationData)
                .addOnSuccessListener(aVoid -> {
                    // Optional: success feedback
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(gc_track.this, "Failed to upload location", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
