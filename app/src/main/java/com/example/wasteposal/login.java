package com.example.wasteposal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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

            // Search for user with mobile number
            findUserByMobile(mobile, password);
        });

        dontHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(login.this, signup.class));
        });
    }

    private void findUserByMobile(String mobile, String inputPassword) {
        // We need to search all cities because user data is nested by City -> Barangay -> User -> uid
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            boolean found = false;

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot citySnapshot : snapshot.getChildren()) {
                    if (found) break;

                    for (DataSnapshot barangaySnapshot : citySnapshot.getChildren()) {
                        if (found) break;

                        DataSnapshot usersSnapshot = barangaySnapshot.child("User");
                        if (usersSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                                String storedMobile = userSnapshot.child("mobile").getValue(String.class);
                                String storedPassword = userSnapshot.child("password").getValue(String.class);
                                String role = userSnapshot.child("role").getValue(String.class);

                                if (mobile.equals(storedMobile)) {
                                    found = true;
                                    if (storedPassword != null && storedPassword.equals(inputPassword)) {
                                        // Login success
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
