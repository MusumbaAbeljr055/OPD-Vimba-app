package com.example.azimbalife.Domain;

public class Specialist {
    private String name;
    private String specialty;
    private String availability;
    private float rating;
    private int imageRes;

    public Specialist(String name, String specialty, String availability, float rating, int imageRes) {
        this.name = name;
        this.specialty = specialty;
        this.availability = availability;
        this.rating = rating;
        this.imageRes = imageRes;
    }

    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getAvailability() { return availability; }
    public float getRating() { return rating; }
    public int getImageRes() { return imageRes; }
}
