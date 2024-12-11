package com.example.finalapp.models;

public class Place {
    private String name;
    private String address;
    private String rating;
    private String description;
    private double latitude;
    private double longitude;
    private String thumbnailUrl;

    public Place(String name, String address, String rating, String description, double latitude, double longitude, String thumbnailUrl) {
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getRating() { return rating; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getThumbnailUrl() { return thumbnailUrl; }

    public boolean hasValidCoordinates() {
        return latitude != 0.0 && longitude != 0.0;
    }
} 