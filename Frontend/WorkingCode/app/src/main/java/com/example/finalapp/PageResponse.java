package com.example.finalapp;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class PageResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
    private boolean first;
    private int numberOfElements;
    
    public static <T> PageResponse<T> fromJson(JSONObject response, Class<T> clazz) throws JSONException {
        PageResponse<T> pageResponse = new PageResponse<>();
        List<T> content = new ArrayList<>();
        
        JSONArray contentArray = response.getJSONArray("content");
        for (int i = 0; i < contentArray.length(); i++) {
            JSONObject itemJson = contentArray.getJSONObject(i);
            if (clazz == TravelPost.class) {
                TravelPost post = new TravelPost();
                post.setId(itemJson.getLong("id"));
                post.setDescription(itemJson.optString("description"));
                post.setDestination(itemJson.optString("destination"));
                post.setCategory(itemJson.optString("category"));
                post.setRating(itemJson.optInt("rating"));
                post.setLikeCount(itemJson.optInt("likeCount"));
                
                String startDate = itemJson.optString("startDate", null);
                if (startDate != null && !startDate.isEmpty()) {
                    post.setStartDate(LocalDateTime.parse(startDate));
                }
                
                String endDate = itemJson.optString("endDate", null);
                if (endDate != null && !endDate.isEmpty()) {
                    post.setEndDate(LocalDateTime.parse(endDate));
                }
                
                String createdAt = itemJson.optString("createdAt", null);
                if (createdAt != null && !createdAt.isEmpty()) {
                    post.setCreatedAt(LocalDateTime.parse(createdAt));
                }
                
                content.add((T) post);
            }
        }

        pageResponse.setContent(content);
        pageResponse.setNumber(response.optInt("number", 0));
        pageResponse.setSize(response.optInt("size", 10));
        pageResponse.setTotalElements(response.optLong("totalElements", 0L));
        pageResponse.setTotalPages(response.optInt("totalPages", 0));
        pageResponse.setLast(response.optBoolean("last", true));
        pageResponse.setFirst(response.optBoolean("first", true));
        pageResponse.setNumberOfElements(response.optInt("numberOfElements", 0));

        return pageResponse;
    }
    
    public PageResponse(List<T> content, int number, int size, long totalElements, int totalPages, 
                       boolean last, boolean first, int numberOfElements) {
        this.content = content;
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
        this.first = first;
        this.numberOfElements = numberOfElements;
    }
    
    public PageResponse() {
    }
    
    // Getters and setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    public int getNumberOfElements() { return numberOfElements; }
    public void setNumberOfElements(int numberOfElements) { this.numberOfElements = numberOfElements; }
} 