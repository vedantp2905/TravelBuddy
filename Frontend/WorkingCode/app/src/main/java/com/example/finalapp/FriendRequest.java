package com.example.finalapp;

public class FriendRequest {
    private int senderId;
    private String senderUsername;
    private boolean accepted;

    public FriendRequest(int senderId, String senderUsername, boolean accepted) {
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.accepted = accepted;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
} 