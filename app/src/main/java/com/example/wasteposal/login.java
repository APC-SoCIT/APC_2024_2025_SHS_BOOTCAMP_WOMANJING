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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class login extends AppCompatActivity {

    private EditText mobileNumEditText, passwordEditText;
    private Button loginButton;
    private TextView dontHaveAccount;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

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

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase
                .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        // Views
        mobileNumEditText = findViewById(R.id.mobilenum);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        dontHaveAccount = findViewById(R.id.dontHaveAccount);

        loginButton.setOnClickListener(v -> {
            String mobile = mobileNumEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String email = mobile + "@wasteposal.com";

            if (TextUtils.isEmpty(mobile)) {
                mobileNumEditText.setError("Mobile number is required");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                passwordEditText.setError("Password is required");
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError("Password must be at least 6 characters");
                return;
            }

            // Resident or Collector
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();

                                userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            String role = snapshot.child("role").getValue(String.class);
                                            if ("admin".equalsIgnoreCase(role)) {
                                                Toast.makeText(login.this, "Admin login successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(login.this, gc_dashboard.class));
                                            } else {
                                                Toast.makeText(login.this, "Resident login successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(login.this, r_dashboard.class));
                                            }
                                            finish();
                                        } else {
                                            Toast.makeText(login.this, "User data not found in Realtime Database", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(login.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(login.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dontHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(login.this, signup.class));
        });
    }
}
