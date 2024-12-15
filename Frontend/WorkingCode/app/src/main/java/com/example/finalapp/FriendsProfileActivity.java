package com.example.finalapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FriendsProfileActivity extends AppCompatActivity {
    private String userId;
    private String viewedUserId;
    private RequestQueue requestQueue;
    private TextView usernameText;
    private TextView statusText;
    private ImageView statusIcon;
    private ImageView profilePictureImageView;

    // New profile field TextViews
    private TextView aboutMeContent;
    private TextView interestsContent;
    private TextView travelExperienceContent;
    private TextView preferredDestinationContent;

    // RecyclerView for posts
    private RecyclerView postsRecyclerView;
    private TravelPostAdapter postAdapter;
    private List<TravelPost> posts;

    private Button unfriendButton;
    private Button messageButton;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_profile); // This should match your XML layout filename

        userId = getIntent().getStringExtra("userId");
        viewedUserId = getIntent().getStringExtra("viewedUserId");
        username = getIntent().getStringExtra("username");
        boolean isFriend = getIntent().getBooleanExtra("isFriend", false);

        requestQueue = Volley.newRequestQueue(this);

        // Find views
        usernameText = findViewById(R.id.tvUsername);
        statusText = findViewById(R.id.tvStatus);
        statusIcon = findViewById(R.id.ivStatus);
        profilePictureImageView = findViewById(R.id.ivProfilePicture);

        // New profile field references
        aboutMeContent = findViewById(R.id.tvAboutMeContent);
        interestsContent = findViewById(R.id.tvInterestsContent);
        travelExperienceContent = findViewById(R.id.tvTravelExperienceContent);
        preferredDestinationContent = findViewById(R.id.tvPreferredDestinationContent);

        // RecyclerView initialization
        postsRecyclerView = findViewById(R.id.travel_feed_recycler_view);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the posts list and adapter
        posts = new ArrayList<>();
        postAdapter = new TravelPostAdapter(this, posts, userId);
        postsRecyclerView.setAdapter(postAdapter);

        // Set username
        usernameText.setText(username);

        // Update UI based on friendship status
        if (isFriend) {
            statusText.setText("Friend");
            statusIcon.setImageResource(R.drawable.ic_check);
            statusIcon.setVisibility(View.VISIBLE);
        } else {
            statusText.setText("Friend request pending");
            statusIcon.setVisibility(View.GONE);
        }

        // Load additional profile data
        loadProfileData();

        // Fetch the status and profile picture
        fetchUserStatus();
        fetchProfilePicture();

        // Load posts data
        loadPostsData();

        unfriendButton = findViewById(R.id.unfriendButton);
        messageButton = findViewById(R.id.messageButton);

        unfriendButton.setOnClickListener(v -> onUnfriend());
        messageButton.setOnClickListener(v -> startDirectMessage());
    }

    private void fetchProfilePicture() {
        String url = ApiConstants.BASE_URL + "/api/profile-picture/get/" + viewedUserId;

        // Create an ImageRequest to fetch the image
        ImageRequest imageRequest = new ImageRequest(url,
                response -> {
                    // Successfully received image response
                    profilePictureImageView.setImageBitmap(response); // Set the profile picture
                },
                0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.RGB_565,
                error -> {
                    // Handle error case
                    Log.e("NetworkRequest", "Error fetching profile picture: " + error.getMessage());
                });

        // Add the request to the request queue
        requestQueue.add(imageRequest);
    }

    private void fetchUserStatus() {
        String url = ApiConstants.BASE_URL + "/api/user-status/get/" + viewedUserId;
        Log.d("NetworkRequest", url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    String status = response.optString("status", "No status set");
                    Log.d("NetworkRequest", status);
                    Log.d("NetworkRequest", response.toString());
                    statusText.setText(status);
                },
                error -> handleError(error, ""));

        requestQueue.add(request);
    }
    private void handleError(VolleyError error, String userMessage) {
        Log.e("ViewProfileActivity", "Error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
    }

    private void loadProfileData() {
        // Endpoint for user profile data
        String url = ApiConstants.BASE_URL + "/api/users/profile/" + viewedUserId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Parse and set profile data
                        if (response.has("aboutMe")) {
                            String aboutMe = response.getString("aboutMe");
                            aboutMeContent.setText(aboutMe);
                        }
                        // Set the "Interests" content
                        if (response.has("interests")) {
                            String interests = response.getString("interests");
                            // Convert the JSON array to a string of comma-separated values
                            if (interests.startsWith("[") && interests.endsWith("]")) {
                                interests = interests.substring(1, interests.length() - 1); // Remove the square brackets
                                interests = interests.replace("\"", ""); // Remove the double quotes
                                interests = interests.replace(",", ", "); // Add space after each comma for better readability
                            }
                            interestsContent.setText(interests);
                        }
                        if (response.has("travelExperienceLevel")) {
                            String travelExperience = response.getString("travelExperienceLevel");
                            travelExperienceContent.setText(travelExperience);
                        }
                        // Set the "Preferred Destination" content
                        if (response.has("preferredDestinations")) {
                            String preferredDestinations = response.getString("preferredDestinations");
                            // Convert the JSON array to a string of comma-separated values
                            if (preferredDestinations.startsWith("[") && preferredDestinations.endsWith("]")) {
                                preferredDestinations = preferredDestinations.substring(1, preferredDestinations.length() - 1); // Remove the square brackets
                                preferredDestinations = preferredDestinations.replace("\"", ""); // Remove the double quotes
                                preferredDestinations = preferredDestinations.replace(",", ", "); // Add space after each comma
                            }
                            preferredDestinationContent.setText(preferredDestinations);
                        }

                        Log.d("FriendsProfileActivity", "Profile data loaded successfully");
                    } catch (Exception e) {
                        Log.e("FriendsProfileActivity", "Error parsing profile data", e);
                        Toast.makeText(this, "Error loading profile details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("FriendsProfileActivity", "Error loading profile: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("FriendsProfileActivity", "Error code: " + error.networkResponse.statusCode);
                    }
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void loadPostsData() {
        // Corrected URL construction
        String url = ApiConstants.BASE_URL + "/api/friend/get-posts/" + viewedUserId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        posts.clear(); // Clear existing posts

                        // Loop through the response array and extract post details
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject postObject = response.getJSONObject(i);

                            // Extract fields for TravelPost
                            long id = postObject.getLong("id");
                            String description = postObject.optString("description", "");
                            String category = postObject.optString("category", "");
                            int rating = postObject.optInt("rating", 0);
                            String destination = postObject.optString("destination", "");
                            int likeCount = postObject.optInt("likeCount", 0);
                            boolean likedByUser = postObject.optBoolean("likedByUser", false);

                            // Parse LocalDateTime fields (startDate, endDate, createdAt)
                            String startDateStr = postObject.optString("startDate", "");
                            String endDateStr = postObject.optString("endDate", "");
                            String createdAtStr = postObject.optString("createdAt", "");

                            // Parse images (handling as a List of Strings)
                            List<String> images = parseImages(postObject.optJSONArray("images"));

                            // Create a TravelPost object and set its values
                            TravelPost post = new TravelPost();
                            post.setId(id);
                            post.setDescription(description);
                            post.setCategory(category);
                            post.setRating(rating);
                            post.setDestination(destination);
                            post.setLikeCount(likeCount);
                            post.setLikedByUser(likedByUser);

                            // Parse dates safely
                            if (!startDateStr.isEmpty()) {
                                try {
                                    post.setStartDate(LocalDateTime.parse(startDateStr));
                                } catch (Exception e) {
                                    Log.e("FriendsProfileActivity", "Error parsing start date", e);
                                }
                            }

                            if (!endDateStr.isEmpty()) {
                                try {
                                    post.setEndDate(LocalDateTime.parse(endDateStr));
                                } catch (Exception e) {
                                    Log.e("FriendsProfileActivity", "Error parsing end date", e);
                                }
                            }

                            if (!createdAtStr.isEmpty()) {
                                try {
                                    post.setCreatedAt(LocalDateTime.parse(createdAtStr));
                                } catch (Exception e) {
                                    Log.e("FriendsProfileActivity", "Error parsing created at", e);
                                }
                            }

                            post.setImages(images);

                            posts.add(post);
                        }

                        // Check if there are no posts and handle accordingly
                        TextView noPostsText = findViewById(R.id.no_posts_text);
                        if (posts.isEmpty()) {
                            noPostsText.setVisibility(View.VISIBLE);
                            postsRecyclerView.setVisibility(View.GONE);
                        } else {
                            noPostsText.setVisibility(View.GONE);
                            postsRecyclerView.setVisibility(View.VISIBLE);
                            postAdapter.notifyDataSetChanged();
                        }

                        Log.d("FriendsProfileActivity", "Posts data loaded successfully");
                    } catch (Exception e) {
                        Log.e("FriendsProfileActivity", "Error parsing posts data", e);
                        Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("FriendsProfileActivity", "Error loading posts: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("FriendsProfileActivity", "Error code: " + error.networkResponse.statusCode);
                    }
                    Toast.makeText(this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return null; // If parsing fails, return null
        }
    }

    private List<String> parseImages(JSONArray imagesArray) {
        List<String> imageList = new ArrayList<>();
        if (imagesArray != null) {
            for (int i = 0; i < imagesArray.length(); i++) {
                imageList.add(imagesArray.optString(i));
            }
        }
        return imageList;
    }

    private void startDirectMessage() {
        Intent intent = new Intent(this, DirectMessageActivity.class);
        intent.putExtra("currentUserId", userId);
        intent.putExtra("otherUserId", viewedUserId);
        intent.putExtra("otherUsername", username);
        startActivity(intent);
    }

    private void onUnfriend() {
        String url = ApiConstants.BASE_URL + "/api/friend/remove/?userId=" + userId + "&friendId=" + viewedUserId;

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
            response -> {
                Toast.makeText(this, "Friend removed successfully", Toast.LENGTH_SHORT).show();
                finish(); // Go back to previous screen
            },
            error -> {
                Toast.makeText(this, "Error removing friend", Toast.LENGTH_SHORT).show();
                Log.e("FriendsProfileActivity", "Remove friend error: " + error.toString());
            }
        );

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }
}
