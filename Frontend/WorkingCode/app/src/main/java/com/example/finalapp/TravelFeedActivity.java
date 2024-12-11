package com.example.finalapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class TravelFeedActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TravelPostAdapter adapter;
    private TravelApiService apiService;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddPost;
    private String userId;
    private boolean likedByUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_feed);

        // Retrieve userId from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = String.valueOf(sharedPreferences.getInt("userId", -1));

        // Initialize views
        recyclerView = findViewById(R.id.travel_feed_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        fabAddPost = findViewById(R.id.fab_add_post);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TravelPostAdapter(this, new ArrayList<>(), userId);
        recyclerView.setAdapter(adapter);

        // Initialize API service
        apiService = new TravelApiService(this);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadPosts);

        // Setup FAB
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            startActivity(intent);
        });

        // Initial load of posts
        loadPosts();
    }

    public boolean isLikedByUser() {
        return likedByUser;
    }

    public void setLikedByUser(boolean likedByUser) {
        this.likedByUser = likedByUser;
    }

    private void loadPosts() {
        swipeRefreshLayout.setRefreshing(true);

        apiService.getPosts(0, 10, true, new TravelApiService.ApiCallback<PageResponse<TravelPost>>() {
            @Override
            public void onSuccess(PageResponse<TravelPost> response) {
                runOnUiThread(() -> {
                    List<TravelPost> posts = response.getContent();
                    TextView noPostsText = findViewById(R.id.no_posts_text);
                    
                    if (posts == null || posts.isEmpty()) {
                        noPostsText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        noPostsText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.updatePosts(posts);
                    }
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(TravelFeedActivity.this, "Error loading posts: " + error, Toast.LENGTH_LONG).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void handlePostCreationResponse(JSONObject response) {
        try {
            String message = response.getString("message");
            String rewardPoints = response.getString("rewardPoints");
            Toast.makeText(this, 
                message + "\nYou earned " + rewardPoints + " reward points!", 
                Toast.LENGTH_LONG).show();
            
            // Broadcast to refresh rewards balance
            Intent intent = new Intent("REFRESH_REWARDS_BALANCE");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            
            // Refresh the post feed
            loadPosts();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
