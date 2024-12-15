package com.example.finalapp;

public class ApiConstants {
    public static final String BASE_URL = "http://10.0.2.2:8080";

    // User endpoints
    public static final String DELETE_USER = BASE_URL + "/api/users/%d";
    public static final String GET_USER = BASE_URL + "/api/users/%d";
    public static final String UPDATE_USER = BASE_URL + "/api/users/update/%d";
    
    // Post endpoints
    public static final String GET_POSTS = BASE_URL + "/api/post/get-posts/";
    public static final String CREATE_POST = BASE_URL + "/api/post/create";
    public static final String DELETE_POST = BASE_URL + "/api/post/delete/%d";
    
    // Like endpoints
    public static final String CREATE_LIKE = BASE_URL + "/api/like/create";
    public static final String DELETE_LIKE = BASE_URL + "/api/like/delete";
    
    // Comment endpoints
    public static final String CREATE_COMMENT = BASE_URL + "/api/comment/create";
    public static final String GET_COMMENTS = BASE_URL + "/api/comment/get-comments/%d";  
    public static final String DELETE_COMMENT = BASE_URL + "/api/comment/delete/%d"; 
    
    // Image endpoint
    public static final String POST_IMAGE = BASE_URL + "/api/image/%d";  
}
