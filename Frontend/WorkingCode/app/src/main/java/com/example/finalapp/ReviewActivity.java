package com.example.finalapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ReviewActivity extends AppCompatActivity {

    private EditText reviewInput;
    private Button submitButton;
    private LinearLayout reviewContainer;
    private Button backButton;

    private String username;  // User's name to be pulled from server
    private String location;  // User's travel location

    private String USER_INFO_URL = "https://example.com/api/user/details"; // Update with actual API URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        backButton = findViewById(R.id.review_back_btn);
        reviewInput = findViewById(R.id.review_input);
        submitButton = findViewById(R.id.submit_review_button);
        reviewContainer = findViewById(R.id.review_container);

        // Fetch user details when the activity starts
        fetchUserDetails();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReviewActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    private void fetchUserDetails() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, USER_INFO_URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            username = response.getString("username");
                            location = response.getString("travelLocation");
                            Toast.makeText(ReviewActivity.this, "Welcome " + username + " from " + location, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ReviewActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ReviewActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(jsonObjectRequest);
    }

    private void submitReview() {
        String reviewText = reviewInput.getText().toString().trim();
        if (!reviewText.isEmpty()) {
            String reviewContent = username + " from " + location + " says: " + reviewText;

            // Create a new TextView for the review
            TextView reviewView = new TextView(this);
            reviewView.setText(reviewContent);
            reviewContainer.addView(reviewView, 0);
            reviewInput.setText("");
            Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter a review", Toast.LENGTH_SHORT).show();
        }
    }
}
