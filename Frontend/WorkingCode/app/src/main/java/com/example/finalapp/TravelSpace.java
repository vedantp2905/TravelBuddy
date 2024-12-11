package com.example.finalapp;

public class TravelSpace {
    private String id;
    private String title;
    private String description;
    private String expiryDate;

    public TravelSpace(String id, String title, String description, String expiryDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.expiryDate = expiryDate;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getExpiryDate() { return expiryDate; }
}
