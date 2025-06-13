package com.example.wasteposal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;

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

    // How often location updates happen
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 10;
    public static final int PERMISSION_FINE_LOCATION = 99;

    // UI elements
    private TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;

    // Switches for turning location updates and GPS on/off
    private Switch sw_locationupdates, sw_gps;

    private boolean updateOn = false;
    private Location currentLocation;
    private List<Location> savedLocations;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private final String city = "Makati";
    private final String barangay = "Magallanes";
    private final String collectorId = "02-0001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);


        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.gc_track);

        initViews();

        AppCompatImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Set how often we want to receive location updates
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(DEFAULT_UPDATE_INTERVAL * 1000L); // Normal update
        locationRequest.setFastestInterval(FAST_UPDATE_INTERVAL * 1000L); // Fastest update
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Get the most recent location from the update
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Update the UI
                    updateUIValues(location);
                }
            }
        };


        sw_gps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // User wants GPS for better accuracy
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                tv_sensor.setText("Using GPS sensors");
            } else {
                // Use sources like WiFi and cell towers
                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                tv_sensor.setText("Using Towers + WIFI");
            }
            // If location updates is on, restart
            if (updateOn) {
                stopLocationUpdates();
                startLocationUpdates();
            }
        });

        sw_locationupdates.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startLocationUpdates();
                updateTrackingStatusInFirebase(true); // Set tracking to true
            } else {
                stopLocationUpdates();
                updateTrackingStatusInFirebase(false); // Set tracking to false
            }
        });

        updateGPS();
    }

    private void initViews() {
        // Connect TextViews in the layout
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);

        // Connect Switches for toggling GPS and location updates
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
    }

    private void startLocationUpdates() {
        // Check if location permission is granted before starting updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            return; // Don't start updates if no permission
        }

        // Request location updates
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

        // Update UI to let user know location tracking is active
        tv_updates.setText("Location is being tracked");
        updateOn = true;
    }

    private void stopLocationUpdates() {
        // Stop receiving location updates from the fused location provider
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        updateOn = false; // Mark that updates are no longer running

        // Reset the UI fields to show tracking is off and clear location info
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
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Try to get the last known location
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    // Save location and update UI
                    currentLocation = location;
                    updateUIValues(location);
                } else {
                    Toast.makeText(gc_track.this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Request location permission if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }
    }

    private void updateUIValues(Location location) {
        // Save current location object for reference
        currentLocation = location;

        // Update UI with location details
        tv_lat.setText(String.format(Locale.getDefault(), "%.6f", location.getLatitude()));
        tv_lon.setText(String.format(Locale.getDefault(), "%.6f", location.getLongitude()));
        tv_accuracy.setText(String.format(Locale.getDefault(), "%.2f meters", location.getAccuracy()));
        tv_altitude.setText(location.hasAltitude() ? String.format(Locale.getDefault(), "%.2f meters", location.getAltitude()) : "Not Available");
        tv_speed.setText(location.hasSpeed() ? String.format(Locale.getDefault(), "%.2f m/s", location.getSpeed()) : "Not Available");

        // Try to get the address from the latitude and longitude
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                tv_address.setText(addresses.get(0).getAddressLine(0));
            } else {
                tv_address.setText("Unable to get street address");
            }
        } catch (Exception e) {
            tv_address.setText("Unable to get street address");
        }

        // Get saved locations
        MyApplication myApplication = (MyApplication) getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        // Upload the current location data to Firebase database
        uploadLocationToFirebase(location);
    }

    private void uploadLocationToFirebase(Location location) {
        DatabaseReference locationRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("CollectorLocation")
                .child(collectorId);

        // Prepare the location data to be saved
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("altitude", location.hasAltitude() ? location.getAltitude() : null);
        locationData.put("speed", location.hasSpeed() ? location.getSpeed() : null);
        locationData.put("timestamp", System.currentTimeMillis());

        // Upload the location data to Firebase
        locationRef.setValue(locationData)
                .addOnSuccessListener(aVoid -> {
                    // Location uploaded successfully
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(gc_track.this, "Failed to upload location", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTrackingStatusInFirebase(boolean tracking) {
        DatabaseReference trackingRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("CollectorLocation")
                .child(collectorId)
                .child("tracking");

        // Update the tracking status (true or false) in Firebase
        trackingRef.setValue(tracking)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated tracking status
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(gc_track.this, "Failed to update tracking state", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start updating GPS location
                updateGPS();
            } else {
                // Permission denied, inform user and close activity since location is essential
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}