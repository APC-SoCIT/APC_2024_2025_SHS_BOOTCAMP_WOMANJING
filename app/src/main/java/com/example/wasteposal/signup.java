package com.example.wasteposal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
// Class Fields
public class signup extends AppCompatActivity {
    private EditText mobileField, passwordField, addressField;
    private Spinner spinnerCity, spinnerBarangay;
    private ArrayList<String> cityList = new ArrayList<>();
    private ArrayList<String> barangayList = new ArrayList<>();
    private ArrayAdapter<String> cityAdapter, barangayAdapter;
//To initialize signup screen
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

        setContentView(R.layout.signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
//Connects Java code to XML
        mobileField = findViewById(R.id.editTextMobile);
        passwordField = findViewById(R.id.editTextPassword);
        addressField = findViewById(R.id.editTextAddress);
        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
//Sets up spinner selection
        loadCitiesFromFirebase();
        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = cityList.get(position);
                loadBarangaysFromFirebase(selectedCity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // From Signup to Login screen
        TextView alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        alreadyHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(signup.this, login.class));
        });

        findViewById(R.id.SignupButton).setOnClickListener(this::signup);
    }
// Fetch City from database
    private void loadCitiesFromFirebase() {
        DatabaseReference rootRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        rootRef.get().addOnSuccessListener(snapshot -> {
            cityList.clear();
            cityList.add("-------- Please Select --------");

            for (DataSnapshot citySnap : snapshot.getChildren()) {
                cityList.add(citySnap.getKey());
            }
            cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cityList);
            cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCity.setAdapter(cityAdapter);
        });
    }


// Fetch Barangay from database
    private void loadBarangaysFromFirebase(String city) {
        DatabaseReference barangayRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city);  // No "Locations"

        barangayRef.get().addOnSuccessListener(snapshot -> {
            barangayList.clear();
            barangayList.add("-------- Please Select --------");
            for (DataSnapshot barangaySnap : snapshot.getChildren()) {
                barangayList.add(barangaySnap.getKey());
            }
            barangayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barangayList);
            barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerBarangay.setAdapter(barangayAdapter);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load barangays: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

// Normal Signup process
    public void signup(View view) {
        String mobile = mobileField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String address = addressField.getText().toString().trim();
        String city = spinnerCity.getSelectedItem() != null ? spinnerCity.getSelectedItem().toString().trim() : "";
        String barangay = spinnerBarangay.getSelectedItem() != null ? spinnerBarangay.getSelectedItem().toString().trim() : "";

        if (mobile.isEmpty() || password.isEmpty() || address.isEmpty() || city.isEmpty() || barangay.isEmpty()) {
            Toast.makeText(this, "Please enter all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        else if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
//Generates a user ID and adds the user information to the database after validating
        DatabaseReference userRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference()
                .child(city)
                .child(barangay)
                .child("User");

        userRef.get().addOnSuccessListener(snapshot -> {
            long count = snapshot.getChildrenCount();
            long nextNumber = count + 1;
            String userId = String.format("01-%04d", nextNumber); // Format: 01-0001

            HashMap<String, Object> userData = new HashMap<>();
            userData.put("address", address);
            userData.put("mobile", mobile);
            userData.put("password", password);
            userData.put("role", "resident");

            userRef.child(userId)
                    .setValue(userData)
                    .addOnSuccessListener(unused -> {
                        // Save to SharedPreferences
                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("city", city)
                                .putString("barangay", barangay)
                                .putString("userId", userId)
                                .apply();
                        Toast.makeText(signup.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(signup.this, login.class));
                        finish();
                    })

                    .addOnFailureListener(e -> {
                        Toast.makeText(signup.this, "Database write failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(signup.this, "Failed to read users: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
