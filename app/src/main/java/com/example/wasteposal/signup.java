package com.example.wasteposal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class signup extends AppCompatActivity {
    private EditText emailField, passwordField;
    private Spinner spinnerCity, spinnerBarangay;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.signup);

        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailField = findViewById(R.id.editTextMobile);
        passwordField = findViewById(R.id.editTextPassword);
        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);

        ArrayAdapter<CharSequence> adapterCity = ArrayAdapter.createFromResource(
                this, R.array.city_array, android.R.layout.simple_spinner_item);
        adapterCity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapterCity);

        ArrayAdapter<CharSequence> adapterBarangay = ArrayAdapter.createFromResource(
                this, R.array.barangay_array, android.R.layout.simple_spinner_item);
        adapterBarangay.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(adapterBarangay);

        TextView alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        alreadyHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(signup.this, login.class));
        });

        findViewById(R.id.SignupButton).setOnClickListener(this::signup);
    }

    public void signup(View view) {
        String mobile = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String city = spinnerCity.getSelectedItem().toString();
        String barangay = spinnerBarangay.getSelectedItem().toString();

        if (mobile.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both mobile number and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String getMobNum = mobile;

        mAuth.createUserWithEmailAndPassword(getMobNum, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            userRef = FirebaseDatabase
                                    .getInstance("https://wasteposal-c1fe3afa-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                    .getReference("Users");

                            HashMap<String, Object> userData = new HashMap<>();
                            userData.put("mobile", mobile);
                            userData.put("city", city);
                            userData.put("barangay", barangay);
                            userData.put("role", "resident");

                            userRef.child(uid).setValue(userData)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(signup.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(signup.this, login.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(signup.this, "Database write failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        Toast.makeText(signup.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
