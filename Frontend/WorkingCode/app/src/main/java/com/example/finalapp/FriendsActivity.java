package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class FriendsActivity extends AppCompatActivity {
    private static final int CHECK_INTERVAL = 30000; // 30 seconds
    private static final int FRIEND_REQUEST_CODE = 1001;
    private Handler handler = new Handler();
    private Runnable friendStatusChecker;
    private String userId;
    private RequestQueue requestQueue;
    private List<UserModel> friends;
    private List<UserModel> removedFriends;
    private FriendsAdapter friendsAdapter;
    private FriendsAdapter removedFriendsAdapter;
    private LinearLayout friendsDropdown;
    private LinearLayout removedFriendsDropdown;
    private ImageView friendsArrow;
    private ImageView removedFriendsArrow;
    private RecyclerView friendsRecyclerView;
    private RecyclerView removedFriendsRecyclerView;
    private AutoCompleteTextView searchView;
    private List<String> searchSuggestions;
    private ArrayAdapter<String> searchSuggestionsAdapter;
    private List<UserModel> searchResults;
    private RecyclerView searchResultsRecyclerView;
    private SearchResultsAdapter searchResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        userId = getIntent().getStringExtra("userId");
        requestQueue = Volley.newRequestQueue(this);
        friends = new ArrayList<>();
        removedFriends = new ArrayList<>();
        searchResults = new ArrayList<>();
        searchSuggestions = new ArrayList<>();

        // Initialize dropdowns
        friendsDropdown = findViewById(R.id.friendsDropdown);
        removedFriendsDropdown = findViewById(R.id.removedFriendsDropdown);
        friendsArrow = findViewById(R.id.friendsArrow);
        removedFriendsArrow = findViewById(R.id.removedFriendsArrow);
        
        // Initialize RecyclerViews
        friendsRecyclerView = findViewById(R.id.rvFriends);
        removedFriendsRecyclerView = findViewById(R.id.rvRemovedFriends);
        
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        removedFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        friendsAdapter = new FriendsAdapter(friends, this::onFriendClick, this::onRemoveFriend, true);
        removedFriendsAdapter = new FriendsAdapter(removedFriends, this::onRemovedFriendClick, null, false);

        friendsRecyclerView.setAdapter(friendsAdapter);
        removedFriendsRecyclerView.setAdapter(removedFriendsAdapter);

        // Set up dropdown clicks
        friendsDropdown.setOnClickListener(v -> toggleDropdown(friendsRecyclerView, friendsArrow));
        removedFriendsDropdown.setOnClickListener(v -> toggleDropdown(removedFriendsRecyclerView, removedFriendsArrow));

        // Initialize search results RecyclerView
        searchResultsRecyclerView = findViewById(R.id.rvSearchResults);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsAdapter = new SearchResultsAdapter(searchResults, this::sendFriendRequest);
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Initialize search view
        searchView = findViewById(R.id.searchView);
        searchSuggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, searchSuggestions);
        searchView.setAdapter(searchSuggestionsAdapter);

        // Set up search text change listener
        searchView.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler();
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    if (s.length() > 0) {
                        searchUsers(s.toString());
                    } else {
                        searchResults.clear();
                        searchResultsAdapter.notifyDataSetChanged();
                        searchResultsRecyclerView.setVisibility(View.GONE);
                    }
                };
                handler.postDelayed(searchRunnable, 300);
            }
        });

        // Initialize friend request button
        Button friendRequestsButton = findViewById(R.id.btnFriendRequests);
        friendRequestsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendRequestActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // Load initial friends list
        loadFriends();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh lists when activity becomes visible
        loadFriends();
        searchResults.clear();
        searchResultsAdapter.notifyDataSetChanged();
        searchResultsRecyclerView.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(friendStatusChecker);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FRIEND_REQUEST_CODE && resultCode == RESULT_OK) {
            // This will still handle specific refresh cases from friend requests
            loadFriends();
        }
    }

    private void loadFriends() {
        String url = ApiConstants.BASE_URL + "/api/friend/get/" + userId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                friends.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject friendObj = response.getJSONObject(i);
                        int friendId = friendObj.getInt("friendId");
                        String username = friendObj.getString("friendUsername");
                        UserModel user = new UserModel(friendId, username);
                        friends.add(user);
                    }
                    friendsAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> Toast.makeText(this, "Error loading friends", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void searchUsers(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = ApiConstants.BASE_URL + "/api/friend/search/?searcherId=" + userId + "&prompt=" + encodedQuery;
            
            if (query.trim().isEmpty()) {
                searchResults.clear();
                searchResultsAdapter.notifyDataSetChanged();
                searchResultsRecyclerView.setVisibility(View.GONE);
                return;
            }
            
            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        searchResults.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject userJson = response.getJSONObject(i);
                            UserModel user = new UserModel(
                                userJson.getInt("id"),
                                userJson.getString("username")
                            );
                            
                            // Skip if user is current user
                            if (user.getId() == Integer.parseInt(userId)) {
                                continue;
                            }
                            
                            // Skip if user is already a friend
                            boolean isAlreadyFriend = false;
                            for (UserModel friend : friends) {
                                if (friend.getId() == user.getId()) {
                                    isAlreadyFriend = true;
                                    break;
                                }
                            }
                            if (isAlreadyFriend) {
                                continue;
                            }
                            
                            searchResults.add(user);
                        }
                        
                        // Update UI
                        searchResultsAdapter.notifyDataSetChanged();
                        searchResultsRecyclerView.setVisibility(
                            searchResults.isEmpty() ? View.GONE : View.VISIBLE
                        );
                        
                    } catch (JSONException e) {
                        Log.e("FriendsActivity", "Error parsing search results: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                error -> {
                    String errorMessage = "Error searching users";
                    if (error.networkResponse != null) {
                        errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                    }
                    Log.e("FriendsActivity", "Search error: " + error.toString());
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    searchResultsRecyclerView.setVisibility(View.GONE);
                }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);
        } catch (UnsupportedEncodingException e) {
            Log.e("FriendsActivity", "Error encoding search query: " + e.getMessage());
            Toast.makeText(this, "Error processing search query", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkIfFriend(int targetUserId) {
        String url = ApiConstants.BASE_URL + "/api/friend/check-friendship?userId=" + userId + "&friendId=" + targetUserId;
                
        try {
            URL checkUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.readLine();
                in.close();
                return Boolean.parseBoolean(response);
            }
        } catch (Exception e) {
            Log.e("FriendsActivity", "Error checking friendship: " + e.getMessage());
        }
        return false;
    }

    private boolean checkPendingRequest(int targetUserId) {
        String url = ApiConstants.BASE_URL + "/api/friend-request/check-pending?senderId=" + userId + "&receiverId=" + targetUserId;
                
        try {
            URL checkUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) checkUrl.openConnection();
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.readLine();
                in.close();
                return Boolean.parseBoolean(response);
            }
        } catch (Exception e) {
            Log.e("FriendsActivity", "Error checking pending requests: " + e.getMessage());
        }
        return false;
    }

    private void sendFriendRequest(UserModel user) {
        String url = ApiConstants.BASE_URL + "/api/friend-request/send-request/?senderId=" + userId + "&receiverId=" + user.getId();

        StringRequest request = new StringRequest(Request.Method.POST, url,
            response -> {
                Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                searchResults.remove(user);
                searchResultsAdapter.notifyDataSetChanged();
                if (searchResults.isEmpty()) {
                    searchResultsRecyclerView.setVisibility(View.GONE);
                }
            },
            error -> {
                String errorMessage;
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    String errorResponse = new String(error.networkResponse.data);
                    Log.e("FriendsActivity", "Error response: " + errorResponse);
                    
                    if (errorResponse.contains("Request already exists")) {
                        errorMessage = "Friend request already pending";
                    } else {
                        errorMessage = "Cannot send friend request at this time";
                    }
                } else {
                    errorMessage = "Cannot send friend request at this time";
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("FriendsActivity", "Send request error: " + error.toString());
            }
        );

        requestQueue.add(request);
    }
    
    // Add method to handle friend request acceptance
    private void acceptFriendRequest(UserModel sender) {
        String url = ApiConstants.BASE_URL + "/api/friend/add/?userId=" + userId + "&friendId=" + sender.getId();
    
        StringRequest request = new StringRequest(Request.Method.POST, url,
            response -> {
                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show();
                loadFriends(); // Refresh the friends list
            },
            error -> {
                Toast.makeText(this, "Error accepting friend request", Toast.LENGTH_SHORT).show();
                Log.e("FriendsActivity", "Accept request error: " + error.toString());
            }
        );
    
        requestQueue.add(request);
    }

    private void onFriendClick(UserModel friend) {
        Intent intent = new Intent(this, FriendsProfileActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("viewedUserId", String.valueOf(friend.getId()));
        intent.putExtra("username", friend.getUsername());
        intent.putExtra("isFriend", true);
        startActivity(intent);
    }

    private void onRemoveFriend(UserModel friend) {
        String url = ApiConstants.BASE_URL + "/api/friend/remove/?userId=" + userId + "&friendId=" + friend.getId();

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
            response -> {
                // Check if friend is not already in removedFriends list
                boolean isDuplicate = false;
                for (UserModel removedFriend : removedFriends) {
                    if (removedFriend.getId() == friend.getId()) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    friends.remove(friend);
                    friendsAdapter.notifyDataSetChanged();
                    removedFriends.add(friend);
                    removedFriendsAdapter.notifyDataSetChanged();
                    
                    // Make sure removed friends section is visible
                    removedFriendsRecyclerView.setVisibility(View.VISIBLE);
                    removedFriendsArrow.setRotation(180);
                    
                    Toast.makeText(this, "Friend removed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "User already in removed friends list", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                String errorMessage = "Error removing friend";
                if (error.networkResponse != null) {
                    switch (error.networkResponse.statusCode) {
                        case 400:
                            errorMessage = "User or friend not found";
                            break;
                        case 500:
                            errorMessage = "Server error occurred";
                            break;
                        default:
                            errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                    }
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("FriendsActivity", "Remove friend error: " + error.toString());
            }
        );

        requestQueue.add(request);
    }

    private void onRemovedFriendClick(UserModel friend) {
        // Add friend back
        String url = ApiConstants.BASE_URL + "/api/friend/add/?userId=" + userId + "&friendId=" + friend.getId();

        StringRequest request = new StringRequest(Request.Method.POST, url,
            response -> {
                removedFriends.remove(friend);
                removedFriendsAdapter.notifyDataSetChanged();
                
                // Check if friend isn't already in friends list
                boolean isDuplicate = false;
                for (UserModel existingFriend : friends) {
                    if (existingFriend.getId() == friend.getId()) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    friends.add(friend);
                    friendsAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Friend added back successfully", Toast.LENGTH_SHORT).show();
                }

                // Hide removed friends section if empty
                if (removedFriends.isEmpty()) {
                    removedFriendsRecyclerView.setVisibility(View.GONE);
                    removedFriendsArrow.setRotation(0);
                }
            },
            error -> {
                String errorMessage = "Error adding friend back";
                if (error.networkResponse != null) {
                    switch (error.networkResponse.statusCode) {
                        case 400:
                            errorMessage = "User not found or already friends";
                            break;
                        case 500:
                            errorMessage = "Server error occurred";
                            break;
                        default:
                            errorMessage += " (Status: " + error.networkResponse.statusCode + ")";
                    }
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("FriendsActivity", "Add friend error: " + error.toString());
            }
        );

        requestQueue.add(request);
    }

    private void getFriendRequests() {
        String url = ApiConstants.BASE_URL + "/api/friend-request/get/" + userId;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
            response -> {
                // Handle incoming friend requests
                Log.d("FriendsActivity", "Received friend requests: " + response.toString());
            },
            error -> {
                Log.e("FriendsActivity", "Error getting friend requests: " + error.toString());
            }
        );

        requestQueue.add(request);
    }

    private void toggleDropdown(RecyclerView recyclerView, ImageView arrow) {
        if (recyclerView.getVisibility() == View.VISIBLE) {
            recyclerView.setVisibility(View.GONE);
            arrow.setRotation(0);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            arrow.setRotation(180);
        }
    }
} 