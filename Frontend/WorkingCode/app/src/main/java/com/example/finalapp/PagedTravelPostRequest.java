package com.example.finalapp;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PagedTravelPostRequest extends Request<List<TravelPost>> {

    private final Response.Listener<List<TravelPost>> listener;
    private static final String PROTOCOL_CHARSET = "utf-8";

    public PagedTravelPostRequest(String url, 
                                Response.Listener<List<TravelPost>> listener, 
                                Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        this.listener = listener;
    }

    @Override
    protected Response<List<TravelPost>> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            
            Log.d("PagedTravelPostRequest", "Raw response: " + jsonString);
            
            JSONObject jsonResponse = new JSONObject(jsonString);
            JSONArray contentArray = jsonResponse.getJSONArray("content");
            
            List<TravelPost> posts = new ArrayList<>();
            for (int i = 0; i < contentArray.length(); i++) {
                JSONObject postJson = contentArray.getJSONObject(i);
                TravelPost post = new TravelPost();
                
                post.setId((int) postJson.getLong("id"));
                if (!postJson.isNull("createdAt")) {
                    post.setCreatedAt(LocalDateTime.parse(postJson.getString("createdAt")));
                }
                if (!postJson.isNull("description")) {
                    post.setDescription(postJson.getString("description"));
                }
                if (!postJson.isNull("category")) {
                    post.setCategory(postJson.getString("category"));
                }
                post.setRating(postJson.getInt("rating"));
                if (!postJson.isNull("startDate")) {
                    post.setStartDate(LocalDateTime.parse(postJson.getString("startDate")));
                }
                if (!postJson.isNull("endDate")) {
                    post.setEndDate(LocalDateTime.parse(postJson.getString("endDate")));
                }
                if (!postJson.isNull("destination")) {
                    post.setDestination(postJson.getString("destination"));
                }
                post.setLikeCount(postJson.getInt("likeCount"));
                
                posts.add(post);
            }
            
            return Response.success(posts, HttpHeaderParser.parseCacheHeaders(response));
            
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(List<TravelPost> response) {
        listener.onResponse(response);
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        return headers;
    }
}