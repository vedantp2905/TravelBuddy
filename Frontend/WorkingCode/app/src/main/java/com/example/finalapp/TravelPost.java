package com.example.finalapp;

import java.time.LocalDateTime;
import java.util.List;

public class TravelPost {
    private long id;
    private String description;
    private String category;
    private int rating;
    private String destination;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private int likeCount;
    private boolean likedByUser;
    private String imageUrl;
    private List<String> images;

    // Default constructor
    public TravelPost() {
    }

    // Constructor with all fields
    public TravelPost(long id, String description, String category, int rating, 
                     String destination, LocalDateTime startDate, LocalDateTime endDate, 
                     LocalDateTime createdAt, int likeCount) {
        this.id = id;
        this.description = description;
        this.category = category;
        this.rating = rating;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
    }

    // Getters
    public long getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getRating() { return rating; }
    public String getDestination() { return destination; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public int getLikeCount() { return likeCount; }
    public boolean isLikedByUser() {return likedByUser;}
    public String getImageUrl() {
        return imageUrl;
    }
    public List<String> getImages() {
        return images;
    }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setRating(int rating) { this.rating = rating; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLikeCount(int likeCount) { this.likeCount = Math.max(0, likeCount); }
    public void setLikedByUser(boolean likedByUser) {this.likedByUser = likedByUser;}
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void setImages(List<String> images) {
        this.images = images;
    }
} 
