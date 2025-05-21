package com.example.wasteposal;

public class user {
    public String name;
    public String address;
    public String mobile;
    public String email;

    // Required empty constructor for Firebase
    public user() {}

    public user(String name, String address, String mobile, String email) {
        this.name = name;
        this.address = address;
        this.mobile = mobile;
        this.email = email;
    }
}
