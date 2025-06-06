package com.example.wasteposal;

import android.app.Application;
import android.location.Location;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private static List<Location> myLocations = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    public List<Location> getMyLocations() {
        return myLocations;
    }
}