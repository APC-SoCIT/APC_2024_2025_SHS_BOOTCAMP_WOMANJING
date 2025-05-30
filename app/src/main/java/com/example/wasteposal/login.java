package com.example.wasteposal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.database.*;

public class login extends AppCompatActivity {

    private EditText mobileNumEditText, passwordEditText;
    private Button loginButton;
    private TextView dontHaveAccount;
    private DatabaseReference rootRef;

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
        setContentView(R.layout.login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Auto-login if already logged in
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            String userId = prefs.getString("userId", null);
            if (userId != null && userId.startsWith("GC")) {
                startActivity(new Intent(this, gc_dashboard.class));
            } else {
                startActivity(new Intent(this, r_dashboard.class));
            }
            finish();
            return;
        }

        // ✅ Setup login form
        rootRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

        mobileNumEditText = findViewById(R.id.mobilenum);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        dontHaveAccount = findViewById(R.id.dontHaveAccount);

        loginButton.setOnClickListener(v -> {
            String mobile = mobileNumEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(mobile)) {
                mobileNumEditText.setError("Mobile number is required");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Password is required");
                return;
            }

            findUserByMobile(mobile, password);
        });

        dontHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(login.this, signup.class));
        });
    }

    private void findUserByMobile(String mobile, String inputPassword) {
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            boolean found = false;

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                    if (found) break;

                    String city = citySnapshot.getKey();

                    for (DataSnapshot barangaySnapshot : citySnapshot.getChildren()) {
                        if (found) break;

                        String barangay = barangaySnapshot.getKey();

                        DataSnapshot usersSnapshot = barangaySnapshot.child("User");
                        if (usersSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                                String storedMobile = userSnapshot.child("mobile").getValue(String.class);
                                String storedPassword = userSnapshot.child("password").getValue(String.class);
                                String role = userSnapshot.child("role").getValue(String.class);

                                if (mobile.equals(storedMobile)) {
                                    found = true;

                                    if (storedPassword != null && storedPassword.equals(inputPassword)) {
                                        String userId = userSnapshot.getKey();
                                        String address = userSnapshot.child("address").getValue(String.class);

                                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                .edit()
                                                .putString("userId", userId)
                                                .putString("city", city)
                                                .putString("barangay", barangay)
                                                .putString("address", address)
                                                .putBoolean("isLoggedIn", true)
                                                .apply();

                                        if ("collector".equalsIgnoreCase(role)) {
                                            Toast.makeText(login.this, "Collector login successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(login.this, gc_dashboard.class));
                                        } else {
                                            Toast.makeText(login.this, "Resident login successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(login.this, r_dashboard.class));
                                        }

                                        finish();
                                    } else {
                                        Toast.makeText(login.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                if (!found) {
                    Toast.makeText(login.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(login.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
