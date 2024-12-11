package com.example.finalapp;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String error);
} 