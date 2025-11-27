package com.example.azimbalife.Activity;

public class UserHelper {
    public String name, email, username, password;
    public boolean isDoctor;

    public UserHelper() {}

    public UserHelper(String name, String email, String username, String password, boolean isDoctor) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
        this.isDoctor = isDoctor;
    }
}
