package com.example.wasteposal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.*;

public class login extends AppCompatActivity {

    private EditText mobileNumEditText, passwordEditText;
    private Button loginButton;
    private TextView dontHaveAccount;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Auto log-in
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            String userId = prefs.getString("userId", null);
            if (userId != null && userId.startsWith("02")) {
                startActivity(new Intent(this, gc_dashboard.class));
            } else {
                startActivity(new Intent(this, r_dashboard.class));
            }
            finish();
            return;
        }

        rootRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();

// Find the views in the layout
        mobileNumEditText = findViewById(R.id.mobilenum);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        dontHaveAccount = findViewById(R.id.dontHaveAccount);

// FAQ button ngani
        ImageButton helpButton = findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> {
            FAQDialogHelper.showFAQ(
                    login.this,
                    "<b>• Step 1: Enter your mobile number</b><br>" +
                            "- Tap the box that says “+63 *** *** ****”<br>" +
                            "- Type the phone number you used when you first signed up.<br><br>" +

                            "<b>• Step 2: Enter your password</b><br>" +
                            "- Tap the box under “Password”<br>" +
                            "- Type the password you created during sign-up.<br><br>" +

                            "<b>• Step 3: Tap the green “Login” button</b><br>" +
                            "- This checks if your mobile number and password are correct.<br>" +
                            "- If correct, you will be taken to your dashboard (Resident or Garbage Collector).<br><br>" +

                            "<b>• Don’t have an account yet?</b><br>" +
                            "- Tap the sentence that says “Don’t have an account?”<br>" +
                            "- You’ll be taken to the Signup screen to create your account.",
                    R.drawable.faq_icon
            );
        });

// When the login button is clicked
        loginButton.setOnClickListener(v -> {
            String mobile = mobileNumEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Make sure mobile number isn't empty
            if (TextUtils.isEmpty(mobile)) {
                mobileNumEditText.setError("Mobile number is required");
                return;
            }

            // Make sure password isn't empty
            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Password is required");
                return;
            }

            // Try to find the user with the given mobile and password
            findUserByMobile(mobile, password);
        });

// If the user doesn't have an account, send them to the signup page
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

                                    // Verify password
                                    if (storedPassword != null && storedPassword.equals(inputPassword)) {
                                        String userId = userSnapshot.getKey();
                                        String address = userSnapshot.child("address").getValue(String.class);

                                        // Save user info locally for session management
                                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                                                .edit()
                                                .putString("userId", userId)
                                                .putString("city", city)
                                                .putString("barangay", barangay)
                                                .putString("address", address)
                                                .putBoolean("isLoggedIn", true)
                                                .apply();

                                        // Direct user based on their role
                                        if ("collector".equalsIgnoreCase(role)) {
                                            Toast.makeText(login.this, "Collector login successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(login.this, gc_dashboard.class));
                                        } else {
                                            Toast.makeText(login.this, "Resident login successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(login.this, r_dashboard.class));
                                        }

                                        finish(); // Close login activity
                                    } else {
                                        // Wrong password entered
                                        Toast.makeText(login.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                // If after searching all cities and barangays no user was found
                if (!found) {
                    Toast.makeText(login.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle database errors here
                Toast.makeText(login.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}