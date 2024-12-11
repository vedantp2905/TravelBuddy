package com.example.finalapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class JoinTravelSpaceActivity extends AppCompatActivity {

    private String travelSpaceId;
    private String userId;
    private String title;
    private String description;
    private String postId;
    private Button joinButton;
    private Spinner colorSpinner;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_travel_space);

        // Initialize views
        joinButton = findViewById(R.id.joinButton);
        colorSpinner = findViewById(R.id.colorSpinner);
        requestQueue = Volley.newRequestQueue(this);


        // Get TravelSpace details from the intent
        travelSpaceId = getIntent().getStringExtra("travelSpaceId");
        userId = getUserId();
        title = getIntent().getStringExtra("title");
        description = getIntent().getStringExtra("description");
        postId = getIntent().getStringExtra("postId");

        // Define the array of colors
        String[] colorsArray = {"Black", "Red", "Blue", "Green", "Yellow", "Orange", "Purple", "Pink", "Brown", "Gray"};

        // Set up the Spinner with the array of colors
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, colorsArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapter);

        // Button click listener to show the spinner and join
        joinButton.setOnClickListener(v -> {
            String selectedColor = colorSpinner.getSelectedItem().toString();
            joinTravelSpace(selectedColor); // Send the color along with other details
        });
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }
    private void joinTravelSpace(String chosenColor) {
        // Create the request payload
        JSONObject requestPayload = new JSONObject();
        try {
            requestPayload.put("userId", userId);
            requestPayload.put("color", chosenColor);
            requestPayload.put("travelSpaceId", travelSpaceId);
            requestPayload.put("title", title);
            requestPayload.put("description", description);
            requestPayload.put("postId", postId);

            String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/travelspace/" + travelSpaceId + "/join";

            // Send POST request to join the TravelSpace
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestPayload,
                    response -> {
                        // Handle success response
                        Toast.makeText(JoinTravelSpaceActivity.this, "Successfully joined the TravelSpace!", Toast.LENGTH_SHORT).show();
                        finish();  // Close the activity after joining
                    },
                    error -> {
                        // Handle error response
                        Toast.makeText(JoinTravelSpaceActivity.this, "Failed to join the TravelSpace.", Toast.LENGTH_SHORT).show();
                    });

            // Add the request to the queue
            requestQueue.add(jsonObjectRequest);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(JoinTravelSpaceActivity.this, "Error joining TravelSpace", Toast.LENGTH_SHORT).show();
        }
    }
}

