package com.example.finalapp;
public class ApiConstants {
    private static final String BASE_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/post";
    //private static final String BASE_URL = "http://localhost:8080/api/post";

    public static final String GET_POSTS = BASE_URL + "/get-posts/";
    public static final String CREATE_POST = BASE_URL + "/create";
    public static final String DELETE_POST = BASE_URL + "/delete/%d";
    
    // Like endpoints
    public static final String CREATE_LIKE = BASE_URL + "/like/create";
    public static final String DELETE_LIKE = BASE_URL + "/like/delete";
    
    // Comment endpoints
    public static final String CREATE_COMMENT = BASE_URL + "/comment/create";
    public static final String GET_COMMENTS = BASE_URL + "/get-comments/%d";  
    public static final String DELETE_COMMENT = BASE_URL + "/comment/delete/%d"; 
    
    // Image endpoint
    public static final String POST_IMAGE = BASE_URL + "/image/%d";  
}
