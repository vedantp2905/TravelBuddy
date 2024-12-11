package com.example.finalapp;

public class Itinerary {
    private String cities;
    private String startDate;
    private String endDate;
    private int numberOfAdults;
    private int numberOfChildren;
    private String userLocation;
    private String country;
    private String generatedItinerary;
    private String postID;

    public Itinerary(String cities, String startDate, String endDate, 
                     int numberOfAdults, int numberOfChildren, 
                     String userLocation, String country,
                     String generatedItinerary, String postID) {
        this.cities = cities;
        this.startDate = startDate;
        this.endDate = endDate;
        this.numberOfAdults = numberOfAdults;
        this.numberOfChildren = numberOfChildren;
        this.userLocation = userLocation;
        this.country = country;
        this.generatedItinerary = generatedItinerary;
        this.postID=postID;
    }

    // Getters
    public String getCities() { return cities; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public int getNumberOfAdults() { return numberOfAdults; }
    public int getNumberOfChildren() { return numberOfChildren; }
    public String getUserLocation() { return userLocation; }
    public String getCountry() { return country; }
    public String getGeneratedItinerary() {
        return generatedItinerary;
    }
    public String getPostID() {
        return postID;
    }
}
