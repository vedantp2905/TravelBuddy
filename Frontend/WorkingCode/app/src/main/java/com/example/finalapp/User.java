package com.example.finalapp;

public class User {
    private int id;
    private String username;
    private String email;
    private boolean newsletterPreference; // Assuming this holds the subscription status

    // Constructor
    public User(int id, String username, String email, boolean newsletterPreference) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.newsletterPreference = newsletterPreference;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean getNewsletterPreference() {
        return newsletterPreference; // Change this to match your logic
    }

    // If you want to use isNewsletterSubscribed, you can rename or add this
    public boolean isNewsletterSubscribed() {
        return newsletterPreference; // Return the actual status
    }
}
