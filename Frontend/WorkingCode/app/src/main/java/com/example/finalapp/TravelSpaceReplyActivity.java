package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class TravelSpaceReplyActivity extends AppCompatActivity {

    private EditText replyEditText;
    private Button replyButton;
    private TextView postTitleTextView, postDescriptionTextView, itineraryInfoTextView;
    private WebView postContentWebView; // WebView for HTML content
    private RequestQueue requestQueue;

    private String messageId, spaceId, userId, travelSpaceTitle, travelSpaceDescription, messageType, htmlMessage, generatedItinerary;

    private static final String TAG = "TravelSpaceReplyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_detail);

        // Initialize views
        replyEditText = findViewById(R.id.replyEditText);
        replyButton = findViewById(R.id.replyButton);
        postTitleTextView = findViewById(R.id.postTitleTextView);
        postDescriptionTextView = findViewById(R.id.postDescriptionTextView);
        postContentWebView = findViewById(R.id.postContentWebView); // WebView
        itineraryInfoTextView = findViewById(R.id.itineraryInfoTextView); // TextView to display itinerary info
        requestQueue = Volley.newRequestQueue(this);

        // Get the data passed from the previous activity
        Intent intent = getIntent();
        travelSpaceTitle = intent.getStringExtra("travelSpaceTitle");
        travelSpaceDescription = intent.getStringExtra("travelSpaceDescription");
        spaceId = intent.getStringExtra("spaceId");
        messageId = intent.getStringExtra("messageId");
        userId = intent.getStringExtra("userId");
        messageType = intent.getStringExtra("messageType");
        htmlMessage = intent.getStringExtra("htmlMessage");

        // Log the received values for debugging
        Log.d(TAG, "Received values: spaceId=" + spaceId + ", messageId=" + messageId + ", userId=" + userId);

        // Set the passed data to the TextViews
        postTitleTextView.setText(travelSpaceTitle);

        // Handle reply button click
        replyButton.setOnClickListener(v -> {
            String reply = replyEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(reply)) {
                postReply(reply);
            } else {
                replyEditText.setError("Reply cannot be empty");
            }
        });
        // Handle content based on message type
        if ("HTML".equalsIgnoreCase(messageType)) {
            postContentWebView.setVisibility(WebView.VISIBLE); // Show WebView
            // Fetch and display itinerary info
            fetchItinerary(htmlMessage);
        } else {
            postDescriptionTextView.setVisibility(TextView.VISIBLE);
            postContentWebView.setVisibility(WebView.GONE);
            postDescriptionTextView.setText(travelSpaceDescription); // Display plain text
        }

    }

    // Method to configure WebView
    private void configureWebView(WebView webView, String htmlContent) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // Format HTML with basic styles
        String htmlTemplate = "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>body { font-family: Arial, sans-serif; padding: 16px; line-height: 1.6; }</style>" +
                "</head><body>" +
                htmlContent +
                "</body></html>";

        // Load the formatted HTML content
        webView.loadDataWithBaseURL(null, htmlTemplate, "text/html", "UTF-8", null);
    }

    // Method to handle posting a reply
    private void postReply(String reply) {
        // Define the endpoint URL
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/travelspace/" + spaceId + "/messages/" + messageId + "/reply?userId=" + userId;

        Log.d(TAG, "Posting reply to: " + url);  // Log the URL for debugging

        // Create a new StringRequest (sending plain text)
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(TravelSpaceReplyActivity.this, "Reply posted successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Optionally close the activity
                },
                error -> {
                    Toast.makeText(TravelSpaceReplyActivity.this, "Error posting reply: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error posting reply: " + error.getMessage()); // Log the error for debugging
                }) {
            @Override
            public byte[] getBody() {
                // Send the reply text as the request body
                return reply.getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "text/plain"); // Ensure the correct Content-Type is set
                return headers;
            }
        };

        // Add the request to the RequestQueue
        requestQueue.add(stringRequest);
    }

    // Method to fetch itinerary data
    private void fetchItinerary(String spaceId) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/itineraries/" + spaceId;

        Log.d(TAG, "Fetching itinerary for spaceId: " + spaceId);  // Log the spaceId for debugging
        Toast.makeText(TravelSpaceReplyActivity.this, "Fetching itinerary for: " + spaceId, Toast.LENGTH_LONG).show();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String cities = response.getString("cities");
                        String startDate = response.getString("startDate");
                        String endDate = response.getString("endDate");
                        int numberOfAdults = response.getInt("numberOfAdults");
                        int numberOfChildren = response.getInt("numberOfChildren");
                        String userLocation = response.getString("userLocation");
                        String country = response.getString("country");
                        generatedItinerary = response.getString("generatedItinerary");

                        // Create an itinerary summary
                        String itineraryInfo = "Cities: " + cities + "\n" +
                                "Start Date: " + startDate + "\n" +
                                "End Date: " + endDate + "\n" +
                                "Number of Adults: " + numberOfAdults + "\n" +
                                "Number of Children: " + numberOfChildren + "\n" +
                                "Location: " + userLocation + "\n" +
                                "Country: " + country + "\n";

                        // Display the itinerary info in the TextView
                        itineraryInfoTextView.setText(itineraryInfo);
                        itineraryInfoTextView.setVisibility(TextView.VISIBLE);

                        // Only load the WebView after itinerary data is fetched
                        if (generatedItinerary != null) {
                            Toast.makeText(TravelSpaceReplyActivity.this, "Generated itinerary fetched successfully", Toast.LENGTH_SHORT).show();
                            configureWebView(postContentWebView, generatedItinerary); // Load the HTML content
                        }
                    } catch (JSONException e) {
                        Toast.makeText(TravelSpaceReplyActivity.this, "Error fetching itinerary: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error fetching itinerary: " + e.getMessage());  // Log the error for debugging
                    }
                },
                error -> {
                    Toast.makeText(TravelSpaceReplyActivity.this, "Error fetching itinerary: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error fetching itinerary: " + error.getMessage());  // Log the error for debugging
                });

        // Add the request to the request queue
        requestQueue.add(jsonObjectRequest);
    }
}
