package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display a list of itineraries. Users can view existing itineraries
 * and navigate to create a new itinerary.
 */
public class ItinerariesActivity extends AppCompatActivity {

    private static final String TAG = "ItinerariesActivity"; // Tag for logging
    private RecyclerView recyclerViewItineraries; // RecyclerView to display itineraries
    private ItineraryAdapter itineraryAdapter; // Adapter for the RecyclerView
    private List<Itinerary> itineraryList; // List to hold itineraries
    private RequestQueue requestQueue; // Volley request queue for API calls

    /**
     * Called when the activity is created. Initializes the UI components and fetches the list of itineraries.
     *
     * @param savedInstanceState Saved instance state bundle, if any.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itineraries);

        // Initialize RecyclerView and itinerary list
        recyclerViewItineraries = findViewById(R.id.recyclerViewItineraries);
        itineraryList = new ArrayList<>();
        itineraryAdapter = new ItineraryAdapter(itineraryList);

        recyclerViewItineraries.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewItineraries.setAdapter(itineraryAdapter);

        // Initialize the request queue for network calls
        requestQueue = Volley.newRequestQueue(this);

        // Fetch the itineraries from the server
        fetchItineraries();

        // Initialize FloatingActionButton to navigate to the CreateItineraryActivity
        FloatingActionButton addItineraryButton = findViewById(R.id.addItineraryButton);
        addItineraryButton.setOnClickListener(v -> {
            Intent intent = new Intent(ItinerariesActivity.this, CreateItineraryActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Fetches the list of itineraries from the server and updates the RecyclerView.
     * Makes a GET request to the server's itinerary endpoint and parses the JSON response.
     */
    private void fetchItineraries() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/itineraries";

        // Create a JSON Array request to fetch itineraries
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Clear the existing itinerary list
                        itineraryList.clear();

                        // Log the entire JSON response for debugging
                        Log.d(TAG, "Response: " + response.toString());

                        // Parse the JSON array and create Itinerary objects
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject itineraryObject = response.getJSONObject(i);
                            try {
                                // Extract itinerary fields from JSON object
                                String country = itineraryObject.optString("country", "");
                                String cities = itineraryObject.optString("cities", "");
                                String startDate = itineraryObject.optString("startDate", "");
                                String endDate = itineraryObject.optString("endDate", "");
                                int numberOfAdults = itineraryObject.optInt("numberOfAdults", 0);
                                int numberOfChildren = itineraryObject.optInt("numberOfChildren", 0);
                                String userLocation = itineraryObject.optString("userLocation", "");
                                String postID = itineraryObject.optString("id", "");
                                String generatedItinerary = itineraryObject.optString("generatedItinerary",

                                        itineraryObject.optString("generated_itinerary", "No itinerary available"));

                                Log.d(TAG, "Itinerary Object: " + itineraryObject.toString());

                                // Create an Itinerary object and add it to the list
                                Itinerary itinerary = new Itinerary(cities, startDate, endDate,
                                        numberOfAdults, numberOfChildren, userLocation, country, generatedItinerary, postID);
                                itineraryList.add(itinerary);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing itinerary at index " + i + ": " + e.getMessage());
                                // Continue parsing the next itinerary
                                continue;
                            }
                        }

                        // Notify the adapter to update the RecyclerView
                        itineraryAdapter.notifyDataSetChanged();

                        // Show a message if no itineraries were found
                        if (itineraryList.isEmpty()) {
                            Toast.makeText(ItinerariesActivity.this, "No itineraries found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                        Toast.makeText(ItinerariesActivity.this, "Error loading itineraries", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Network error: " + error.toString());
                    Toast.makeText(ItinerariesActivity.this,
                            "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                }
        );

        // Add the request to the request queue
        requestQueue.add(jsonArrayRequest);
    }
}
