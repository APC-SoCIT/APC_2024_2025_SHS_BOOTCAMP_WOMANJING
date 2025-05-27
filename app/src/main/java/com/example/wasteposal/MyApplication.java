package com.example.wasteposal;

import android.app.Application;
import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends Application {
    private static List<Location> myLocations = new ArrayList<>();

    public List<Location> getMyLocations() {
        return myLocations;
    }
}
