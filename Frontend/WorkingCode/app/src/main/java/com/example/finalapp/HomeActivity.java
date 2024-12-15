package com.example.finalapp;

import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import com.example.finalapp.LoginActivity;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.DefaultRetryPolicy;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.cardview.widget.CardView;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import androidx.appcompat.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

public class HomeActivity extends AppCompatActivity {

    private CardView btnTravelFeed, btnManageDocuments, btnItineraries, btnTravelSpaces, btnPolls, btnTrivia, btnTaskList;
    private Button btnChangeAccount, btnLogout;
    private ImageButton btnOpenMessages, btnAccountSettings, btnProfile;
    private TextView tvUsername, rewardsBalanceText;
    private RequestQueue requestQueue;
    private String userId, username;
    private int userRole;

    private BroadcastReceiver rewardsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("REFRESH_REWARDS_BALANCE".equals(intent.getAction())) {
                fetchRewardsBalance();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize requestQueue if not already done
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(this);
        }

        // Retrieve userId and role from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = String.valueOf(sharedPreferences.getInt("userId", -1));
        userRole = sharedPreferences.getInt("role", -1);

        // Initialize UI elements
        btnTravelFeed = findViewById(R.id.btnTravelFeed);
        btnManageDocuments = findViewById(R.id.btnManageDocuments);
        btnChangeAccount = findViewById(R.id.btnChangeAccount);
        tvUsername = findViewById(R.id.tvUsername);
        btnLogout = findViewById(R.id.btnLogout);
        btnTravelSpaces = findViewById(R.id.btnForum);
        btnItineraries = findViewById(R.id.btnItineraries);
        rewardsBalanceText = findViewById(R.id.rewardsBalanceText);
        btnOpenMessages = findViewById(R.id.btnOpenMessages);
        btnAccountSettings = findViewById(R.id.btnAccountSettings);
        btnPolls = findViewById(R.id.btnPolls);
        btnProfile = findViewById(R.id.btnProfile);
        btnTrivia = findViewById(R.id.btnTrivia);
        btnTaskList = findViewById(R.id.btnTaskList);

        // Check if userId is valid; if not, prompt user to log in again
        if (userId.equals("-1")) {
            Toast.makeText(this, "User ID not found, please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Fetch user profile to get the username
        fetchUserProfile(userId);

        // Set click listener for Travel Feed button
        btnTravelFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, TravelFeedActivity.class);
                startActivity(intent);
            }
        });

        // Find the premium banner layout and upgrade button
        LinearLayout premiumBanner = findViewById(R.id.premiumBanner);
        btnChangeAccount = findViewById(R.id.btnChangeAccount);

        // Hide premium banner and upgrade button for premium users (role == 3)
        if (userRole == 3) {  // Premium users
            premiumBanner.setVisibility(View.GONE);
            btnChangeAccount.setVisibility(View.GONE);
        } else {  // Normal users and admins can see upgrade option
            premiumBanner.setVisibility(View.VISIBLE);
            btnChangeAccount.setVisibility(View.VISIBLE);
        }

        // Set visibility for Itineraries card (only for admin and premium users)
        if (userRole == 1 || userRole == 3) {  // Admin or Premium users
            btnItineraries.setVisibility(View.VISIBLE);
        } else {  // Normal users
            btnItineraries.setVisibility(View.GONE);
            GridLayout gridLayout = findViewById(R.id.mainFeaturesGrid);
            // Force layout pass
            gridLayout.post(new Runnable() {
                @Override
                public void run() {
                    gridLayout.removeView(btnItineraries);
                    gridLayout.requestLayout();
                }
            });
        }

        // Set button listeners for other functionalities
        btnManageDocuments.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ManageDocumentsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
        btnChangeAccount.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, UpgradeActivity.class)));
        btnTravelSpaces.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, TravelSpacesActivity.class)));


        // Set Poll button listener to navigate to PollActivity
        btnItineraries.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ItinerariesActivity.class)));
        btnOpenMessages.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SelectUserActivity.class);
            intent.putExtra("currentUserId", userId);
            startActivity(intent);
        });

        // Fetch newsletter preference
        fetchNewsletterPreference(userId);

        btnLogout.setOnClickListener(v -> {
            // Use the existing sharedPreferences from line 41
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Navigate to login screen
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnAccountSettings.setOnClickListener(v -> 
            startActivity(new Intent(HomeActivity.this, ChangePassActivity.class)));

        fetchRewardsBalance();

        View walletSection = findViewById(R.id.walletSection);
        walletSection.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, RewardsActivity.class);
            startActivity(intent);
        });

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(rewardsReceiver, new IntentFilter("REFRESH_REWARDS_BALANCE"));

        setAdminPanelVisibility();

        btnPolls.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, PollActivity.class)));
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ViewProfileActivity.class);
            startActivity(intent);
        });
        btnTrivia.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TriviaActivity.class);
            startActivity(intent);
        });

        btnTaskList.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TaskListActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnFriends).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FriendsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        findViewById(R.id.btnLocalExplorer).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LocalExplorerActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Optionally handle back navigation
        moveTaskToBack(true);
    }

    private void fetchUserProfile(String userId) {
        String url = ApiConstants.BASE_URL + "/api/users/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        username = response.getString("username");
                        Log.d("HomeActivity", "Retrieved username: " + username);
                        String welcomeMessage = getString(R.string.welcome_message, username);
                        tvUsername.setText(welcomeMessage);
                        userRole = response.getInt("role");
                        setAdminPanelVisibility();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(HomeActivity.this, "Error retrieving user profile.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> handleError(error, "Fetching user profile")
        );
        requestQueue.add(request);
    }

    private void fetchNewsletterPreference(String userId) {
        String url = ApiConstants.BASE_URL + "/api/users/newsletter-preference/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response != null && response.has("subscribed")) {
                            boolean isSubscribed = response.getBoolean("subscribed");
                            if (!isSubscribed) {
                                showNewsletterPreferencePopup(true);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("HomeActivity", "Error parsing newsletter preference", e);
                        // Don't show error to user, just log it
                    }
                },
                error -> {
                    Log.e("HomeActivity", "Error fetching newsletter preference", error);
                    // Don't show error to user, just log it
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                1, // no retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void showNewsletterPreferencePopup(boolean showImmediately) {
        View dialogView = getLayoutInflater().inflate(R.layout.newsletter_popup_layout, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialog);
        builder.setView(dialogView);

        TextView titleText = dialogView.findViewById(R.id.popup_title);
        TextView messageText = dialogView.findViewById(R.id.popup_message);
        Button subscribeButton = dialogView.findViewById(R.id.subscribe_button);
        Button notNowButton = dialogView.findViewById(R.id.not_now_button);
        ImageView newsletterIcon = dialogView.findViewById(R.id.newsletter_icon);

        titleText.setText("Stay Connected! ✨");
        messageText.setText("Join our TravelBuddy newsletter to receive exciting travel tips, exclusive deals, and amazing destination recommendations!");
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_background);

        subscribeButton.setOnClickListener(v -> {
            updateNewsletterPreference(userId, true);
            dialog.dismiss();
        });

        notNowButton.setOnClickListener(v -> dialog.dismiss());

        if (showImmediately) {
            dialog.show();
        }
    }

    private void updateNewsletterPreference(String userId, boolean subscribe) {
        if (userId == null || userId.equals("-1")) {
            Log.e("HomeActivity", "Invalid user ID for newsletter update");
            return;
        }

        String url = ApiConstants.BASE_URL + "/api/users/newsletter-preference/" + userId;

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("subscribed", subscribe);
        } catch (JSONException e) {
            Log.e("HomeActivity", "Error creating request body", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            response -> {
                try {
                    // If we get here, the update was successful regardless of response content
                    Toast.makeText(HomeActivity.this, 
                        "Newsletter preference updated successfully!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Log the success for debugging
                    Log.d("HomeActivity", "Newsletter preference updated successfully");
                } catch (Exception e) {
                    Log.e("HomeActivity", "Error parsing success response", e);
                }
            },
            error -> {
                // Check if there's actually an error response
                if (error.networkResponse != null && error.networkResponse.data != null) {
                    try {
                        String errorData = new String(error.networkResponse.data, "UTF-8");
                        JSONObject errorJson = new JSONObject(errorData);
                        
                        // Check if this is actually a success response
                        if (error.networkResponse.statusCode == 200 || 
                            error.networkResponse.statusCode == 201) {
                            Toast.makeText(HomeActivity.this, 
                                "Newsletter preference updated successfully!", 
                                Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        String errorMessage = errorJson.optString("message", 
                            "Unable to update newsletter preference");
                        Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        // If we can't parse the error, it might be a success
                        if (error.networkResponse.statusCode == 200 || 
                            error.networkResponse.statusCode == 201) {
                            Toast.makeText(HomeActivity.this, 
                                "Newsletter preference updated successfully!", 
                                Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HomeActivity.this, 
                                "Unable to update newsletter preference", 
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // Check if it's actually a success case
                    if (error instanceof com.android.volley.NoConnectionError) {
                        Toast.makeText(HomeActivity.this, 
                            "Network error. Please check your connection.", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        // Assume success if no clear error
                        Toast.makeText(HomeActivity.this, 
                            "Newsletter preference updated successfully!", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
                Log.d("HomeActivity", "Newsletter update response: " + error.toString());
            }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
            10000, // 10 seconds timeout
            1,     // 1 retry
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void handleError(VolleyError error, String defaultMessage) {
        String errorMessage = defaultMessage;
        if (error.networkResponse != null) {
            try {
                String errorData = new String(error.networkResponse.data, "UTF-8");
                if (errorData.trim().startsWith("{")) {
                    JSONObject errorJson = new JSONObject(errorData);
                    if (errorJson.has("message")) {
                        errorMessage = errorJson.getString("message");
                    }
                } else {
                    errorMessage = errorData.trim();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRewardsBalance();
    }

    private class CustomJsonRequest extends JsonObjectRequest {
        public CustomJsonRequest(int method, String url, JSONObject jsonRequest,
                               Response.Listener<JSONObject> listener,
                               Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                
                JSONObject result;
                if (jsonString.trim().startsWith("{")) {
                    result = new JSONObject(jsonString);
                } else {
                    result = new JSONObject();
                    result.put("message", jsonString.trim());
                }
                
                return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    private void fetchRewardsBalance() {
        String userId = String.valueOf(getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getInt("userId", -1));
        String url = ApiConstants.BASE_URL + "/api/reward/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double rewardsBalance = response.getDouble("balance");
                        rewardsBalanceText.setText(String.format("%.0f pts", rewardsBalance));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("HomeActivity", "Error fetching rewards balance", error)
        );

        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(rewardsReceiver);
    }

    private void setAdminPanelVisibility() {
        Button btnAdminPanel = findViewById(R.id.btnAdminPanel);
        if (userRole == 1) { // Assuming 1 is admin role
            btnAdminPanel.setVisibility(View.VISIBLE);
            btnAdminPanel.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AdminActivity.class);
                startActivity(intent);
            });
        }
    }
}
