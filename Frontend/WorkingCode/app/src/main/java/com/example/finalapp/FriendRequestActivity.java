package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestActivity extends AppCompatActivity {
    private List<FriendRequest> requests;
    private FriendRequestAdapter adapter;
    private RequestQueue requestQueue;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        userId = getIntent().getStringExtra("userId");
        requests = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);

        RecyclerView recyclerView = findViewById(R.id.rvRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new FriendRequestAdapter(
            requests,
            this::onAcceptRequest,    // Accept listener
            this::onRejectRequest,    // Reject listener
            this::onRequestClick      // Click listener
        );
        recyclerView.setAdapter(adapter);

        loadFriendRequests();
    }

    private void loadFriendRequests() {
        String url = ApiConstants.BASE_URL + "/api/friend-request/get/" + userId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                requests.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.getJSONObject(i);
                        int senderId = obj.getInt("senderId");
                        
                        // Load sender username in a separate request
                        loadUserDetails(senderId, obj.getString("sentAt"));
                    }
                    
                    if (response.length() == 0) {
                        TextView emptyView = findViewById(R.id.tvEmptyRequests);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    
                    Log.d("FriendRequestActivity", "Found " + response.length() + " friend requests");
                } catch (JSONException e) {
                    Log.e("FriendRequestActivity", "Error parsing requests: " + e.getMessage());
                    Log.d("FriendRequestActivity", "Response: " + response.toString());
                    Toast.makeText(this, "Error loading friend requests", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                Log.e("FriendRequestActivity", "Error loading requests: " + error.toString());
                Toast.makeText(this, "Error loading friend requests", Toast.LENGTH_SHORT).show();
                TextView emptyView = findViewById(R.id.tvEmptyRequests);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Error loading friend requests");
            }
        );

        requestQueue.add(request);
    }

    private void loadUserDetails(int userId, String sentAt) {
        String url = ApiConstants.BASE_URL + "/api/users/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    String username = response.getString("username");
                    requests.add(new FriendRequest(userId, username, false));
                    adapter.notifyDataSetChanged();
                    
                    TextView emptyView = findViewById(R.id.tvEmptyRequests);
                    emptyView.setVisibility(View.GONE);
                } catch (JSONException e) {
                    Log.e("FriendRequestActivity", "Error parsing user details: " + e.getMessage());
                }
            },
            error -> {
                Log.e("FriendRequestActivity", "Error loading user details: " + error.toString());
            }
        );

        requestQueue.add(request);
    }

    private void onRequestClick(FriendRequest request) {
        Intent intent = new Intent(this, FriendsProfileActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("viewedUserId", String.valueOf(request.getSenderId()));
        intent.putExtra("username", request.getSenderUsername());
        intent.putExtra("isFriend", false); // Since this is from requests
        startActivity(intent);
    }

    private void onAcceptRequest(FriendRequest request) {
        String url = ApiConstants.BASE_URL + "/api/friend-request/accept-request/?senderId=" + request.getSenderId() + "&receiverId=" + userId;

        StringRequest acceptRequest = new StringRequest(Request.Method.DELETE, url,
            response -> {
                // Remove from local list
                int position = requests.indexOf(request);
                if (position != -1) {
                    requests.remove(position);
                    adapter.notifyItemRemoved(position);
                }
                
                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show();
                
                // Update empty state
                if (requests.isEmpty()) {
                    TextView emptyView = findViewById(R.id.tvEmptyRequests);
                    emptyView.setVisibility(View.VISIBLE);
                }
                
                // Set result to refresh friends list when returning
                setResult(RESULT_OK);
                if (requests.isEmpty()) {
                    finish();
                }
            },
            error -> {
                String errorMessage = "Error accepting friend request";
                if (error.networkResponse != null) {
                    switch (error.networkResponse.statusCode) {
                        case 400:
                            errorMessage = "User not found or request doesn't exist";
                            break;
                        case 500:
                            errorMessage = "Server error occurred";
                            break;
                        default:
                            errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                    }
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("FriendRequestActivity", "Accept request error: " + error.toString());
            }
        );

        acceptRequest.setRetryPolicy(new DefaultRetryPolicy(
            10000,
            1,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(acceptRequest);
    }

    private void onRejectRequest(FriendRequest request) {
        String url = ApiConstants.BASE_URL + "/api/friend-request/reject-request/?senderId=" + request.getSenderId() + "&receiverId=" + userId;

        StringRequest rejectRequest = new StringRequest(Request.Method.DELETE, url,
            response -> {
                // Remove from local list
                int position = requests.indexOf(request);
                if (position != -1) {
                    requests.remove(position);
                    adapter.notifyItemRemoved(position);
                }
                
                Toast.makeText(FriendRequestActivity.this, "Friend request rejected", Toast.LENGTH_SHORT).show();
                
                // Update empty state
                if (requests.isEmpty()) {
                    TextView emptyView = findViewById(R.id.tvEmptyRequests);
                    emptyView.setVisibility(View.VISIBLE);
                }
            },
            error -> {
                Toast.makeText(FriendRequestActivity.this, "Error rejecting request", Toast.LENGTH_SHORT).show();
                Log.e("FriendRequestActivity", "Reject request error: " + error.toString());
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    try {
                        String responseBody = new String(error.networkResponse.data, "UTF-8");
                        Log.e("FriendRequestActivity", "Error response: " + responseBody);
                    } catch (Exception e) {
                        Log.e("FriendRequestActivity", "Error parsing error response", e);
                    }
                }
            }
        );

        requestQueue.add(rejectRequest);
    }
} 