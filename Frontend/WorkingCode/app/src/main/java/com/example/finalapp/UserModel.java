package com.example.finalapp;

public class UserModel {
    private int id;
    private String username;
    private boolean requestPending;

    public UserModel(int id, String username) {
        this.id = id;
        this.username = username;
        this.requestPending = false;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean isRequestPending() {
        return requestPending;
    }

    public void setRequestPending(boolean requestPending) {
        this.requestPending = requestPending;
    }
} 