package com.example.finalapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class TravelApiService {

    private static final String BASE_URL = ApiConstants.BASE_URL;
    private final RequestQueue requestQueue;
    private static final String TAG = "TravelApiService";
    private final OkHttpClient client;

    public TravelApiService(Context context) {
        this.requestQueue = Volley.newRequestQueue(context);
        this.client = new OkHttpClient();
    }


    public void getPosts(int page, int size, boolean newest, final ApiCallback<PageResponse<TravelPost>> callback) {
        String url = BASE_URL + "/api/post/get-posts/?page=" + page + "&size=" + size + "&newest=" + newest;
        Log.d(TAG, "Loading posts from: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    PageResponse<TravelPost> pageResponse = PageResponse.fromJson(response, TravelPost.class);
                    callback.onSuccess(pageResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing posts response", e);
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            },
            error -> {
                Log.e(TAG, "Error fetching posts", error);
                callback.onError(getErrorMessage(error));
            });

        requestQueue.add(request);
    }

    public void deletePost(Long postId, ApiCallback<String> callback) {
        String url = BASE_URL + "/api/post/delete/" + postId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                callback::onSuccess,
                error -> handleError(error, callback)
        );
        request.setRetryPolicy(getRetryPolicy());
        requestQueue.add(request);
    }

    public void createComment(Long userId, Long postId, String description, ApiCallback<String> callback) {
        String url = BASE_URL + "/api/comment/create/";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", String.valueOf(userId));
            jsonBody.put("postId", String.valueOf(postId));
            jsonBody.put("description", description);

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        Log.d(TAG, "Comment created successfully");
                        callback.onSuccess("Success");
                    },
                    error -> {
                        Log.e(TAG, "Error creating comment", error);
                        callback.onError("Failed to create comment");
                    }
            ) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };

            requestQueue.add(request);
        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    public void createLike(Long userId, Long postId, ApiCallback<Integer> callback) {
        String url = BASE_URL + "/api/like/create/";
        Log.d(TAG, "Creating like - URL: " + url + " userId: " + userId + " postId: " + postId);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", String.valueOf(userId));
            jsonBody.put("postId", String.valueOf(postId));
            Log.d(TAG, "Request body: " + jsonBody.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body", e);
            callback.onError("Error creating request: " + e.getMessage());
            return;
        }

        final String requestBody = jsonBody.toString();

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Like creation successful: " + response);
                    callback.onSuccess(1);
                },
                error -> {
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "UTF-8");
                            Log.d(TAG, "Error response body: " + responseBody);
                            
                            if (error.networkResponse.statusCode == 400 && 
                                responseBody.contains("already exists")) {
                                Log.d(TAG, "Like already exists - treating as success");
                                callback.onSuccess(1);
                                return;
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "Error reading error response", e);
                        }
                    }
                    
                    String errorMessage = "Error creating like";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status " + error.networkResponse.statusCode + ")";
                    }
                    Log.e(TAG, errorMessage, error);
                    callback.onError(errorMessage);
                }) {
            @Override
            public byte[] getBody() {
                return requestBody.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    public void deleteLike(Long userId, Long postId, ApiCallback<Integer> callback) {
        String url = BASE_URL + "/api/like/delete/?userId=" + userId + "&travelPostId=" + postId;
        Log.d(TAG, "Deleting like - URL: " + url);

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Log.d(TAG, "Like deletion successful: " + response);
                    callback.onSuccess(-1);
                },
                error -> {
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "UTF-8");
                            Log.d(TAG, "Error response body: " + responseBody);
                            
                            if (error.networkResponse.statusCode == 404 && 
                                responseBody.contains("Like not found")) {
                                Log.d(TAG, "Like not found - treating as already deleted");
                                callback.onSuccess(-1);
                                return;
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "Error reading error response", e);
                        }
                    }
                    
                    String errorMessage = "Error deleting like";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status " + error.networkResponse.statusCode + ")";
                    }
                    Log.e(TAG, errorMessage, error);
                    callback.onError(errorMessage);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    public void getComments(Long postId, ApiCallback<List<Comment>> callback) {
        String url = BASE_URL + "/api/post/get-comments/" + postId;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> callback.onSuccess(parseCommentsResponse(response)),
                error -> handleError(error, callback)
        );
        request.setRetryPolicy(getRetryPolicy());
        requestQueue.add(request);
    }

    private List<Comment> parseCommentsResponse(JSONArray response) {
        List<Comment> comments = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject commentJson = response.getJSONObject(i);
                Comment comment = new Comment();
                comment.setId(commentJson.getLong("id"));
                comment.setCreatedAt(commentJson.getString("createdAt"));
                comment.setDescription(commentJson.getString("description"));
                comments.add(comment);
            }
        } catch (JSONException e) {
            Log.e("TravelApiService", "Error parsing comments", e);
        }
        return comments;
    }

    private PageResponse<TravelPost> parsePageResponse(JSONObject response) throws JSONException {
        List<TravelPost> posts = new ArrayList<>();
        JSONArray content = response.getJSONArray("content");

        for (int i = 0; i < content.length(); i++) {
            JSONObject postJson = content.getJSONObject(i);
            TravelPost post = new TravelPost();
            post.setId(postJson.getLong("id"));
            post.setDescription(postJson.optString("description", ""));
            post.setCategory(postJson.optString("category", ""));
            post.setRating(postJson.optInt("rating", 0));
            post.setDestination(postJson.optString("destination", ""));
            post.setLikeCount(postJson.optInt("likeCount", 0));

            String startDate = postJson.optString("startDate", null);
            if (startDate != null && !startDate.isEmpty()) {
                post.setStartDate(LocalDateTime.parse(startDate));
            }
            String endDate = postJson.optString("endDate", null);
            if (endDate != null && !endDate.isEmpty()) {
                post.setEndDate(LocalDateTime.parse(endDate));
            }
            String createdAt = postJson.optString("createdAt", null);
            if (createdAt != null && !createdAt.isEmpty()) {
                post.setCreatedAt(LocalDateTime.parse(createdAt));
            }

            posts.add(post);
        }

        return new PageResponse<>(
                posts,
                response.optInt("number", 0),
                response.optInt("size", 10),
                response.optLong("totalElements", 0L),
                response.optInt("totalPages", 0),
                response.optBoolean("last", true),
                response.optBoolean("first", true),
                response.optInt("numberOfElements", 0)
        );
    }

    private DefaultRetryPolicy getRetryPolicy() {
        return new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        );
    }

    private Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    private void handleError(com.android.volley.VolleyError error, ApiCallback<?> callback) {
        String errorMessage = "Network error occurred";
        if (error.networkResponse != null) {
            errorMessage = String.format("Error: %d - %s",
                    error.networkResponse.statusCode,
                    new String(error.networkResponse.data));
        }
        callback.onError(errorMessage);
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void getPost(Long postId, ApiCallback<TravelPost> callback) {
        String url = BASE_URL + "/api/post/getAllPosts";

        Log.d(TAG, "Fetching post with URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Post response: " + response.toString());
                    try {
                        TravelPost post = parsePostFromJson(response);
                        callback.onSuccess(post);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing post: " + e.getMessage());
                        callback.onError("Error parsing post: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMessage = "Error getting post";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status " + error.networkResponse.statusCode + ")";
                        try {
                            String responseBody = new String(error.networkResponse.data, "UTF-8");
                            Log.e(TAG, "Error response: " + responseBody);
                        } catch (UnsupportedEncodingException e) {
                            Log.e(TAG, "Error reading error response", e);
                        }
                    }
                    callback.onError(errorMessage);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private TravelPost parsePostFromJson(JSONObject json) throws JSONException {
        TravelPost post = new TravelPost();
        post.setId(json.getLong("id"));
        post.setDescription(json.getString("description"));
        post.setDestination(json.getString("destination"));
        post.setCategory(json.getString("category"));
        post.setLikeCount(json.getInt("likeCount"));
        post.setLikedByUser(json.getBoolean("likedByUser"));
        // Add any other fields you need to parse
        return post;
    }

    public void deleteComment(Long commentId, ApiCallback<String> callback) {
        String url = BASE_URL + "/api/comment/delete/" + commentId;

        StringRequest stringRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> callback.onSuccess("Comment deleted successfully"),
                error -> {
                    String errorMessage = getErrorMessage(error);
                    callback.onError(errorMessage);
                }
        );

        requestQueue.add(stringRequest);
    }

    private String getErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            return "Error: " + error.networkResponse.statusCode;
        } else if (error.getMessage() != null) {
            return "Error: " + error.getMessage();
        } else {
            return "An unknown error occurred";
        }
    }

    public void createPost(JSONObject postData, ApiCallback<JSONObject> callback) {
        String url = BASE_URL + "/api/post/create/";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
            response -> callback.onSuccess(response),
            error -> {
                String errorMessage = "Network error occurred";
                if (error.networkResponse != null) {
                    errorMessage = new String(error.networkResponse.data);
                }
                callback.onError(errorMessage);
            });

        requestQueue.add(request);
    }

    public void uploadImage(long postId, byte[] imageBytes, ApiCallback<String> callback) {
        String url = BASE_URL + "/api/travelimage/" + postId + "/images";
        Log.d(TAG, "Starting image upload to: " + url);
        Log.d(TAG, "Image bytes length: " + imageBytes.length);

        try {
            MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");
            RequestBody imageBody = RequestBody.create(MEDIA_TYPE_JPG, imageBytes);
            
            MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image_" + System.currentTimeMillis() + ".jpg", imageBody);

            RequestBody requestBody = builder.build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

            Log.d(TAG, "Sending request to server...");
            
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    String errorMsg = "Upload failed: " + e.getMessage();
                    Log.e(TAG, errorMsg, e);
                    callback.onError(errorMsg);
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        callback.onSuccess(responseBody);
                    } else {
                        String errorMsg = "Server error: " + response.code() + " - " + responseBody;
                        Log.e(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                }
            });
        } catch (Exception e) {
            String errorMsg = "Error creating request: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            callback.onError(errorMsg);
        }
    }

}
