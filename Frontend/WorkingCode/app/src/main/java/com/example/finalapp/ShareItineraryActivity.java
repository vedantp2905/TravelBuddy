package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShareItineraryActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ShareItineraryActivity";
    private static final String BASE_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/travelspace/get/";

    private RecyclerView travelSpacesRecyclerView;
    private TravelSpacesSendAdapter travelSpacesSendAdapter;
    private List<TravelSpace> travelSpaceList;
    private RequestQueue requestQueue;

    private String generatedItinerary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_itinerary);

        // Retrieve the itinerary data passed from ItineraryDetailActivity
        generatedItinerary = getIntent().getStringExtra("postID");
        Toast.makeText(ShareItineraryActivity.this, generatedItinerary, Toast.LENGTH_SHORT).show();

        // Initialize RecyclerView
        travelSpacesRecyclerView = findViewById(R.id.travelSpacesRecyclerView);
        travelSpacesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        travelSpaceList = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);

        // Initialize and set the new adapter
        travelSpacesSendAdapter = new TravelSpacesSendAdapter(travelSpaceList, this);
        travelSpacesRecyclerView.setAdapter(travelSpacesSendAdapter);

        // Load travel spaces
        loadTravelSpaces();
    }

    private void loadTravelSpaces() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, BASE_URL, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        parseTravelSpaces(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching travel spaces", error);
                        Toast.makeText(ShareItineraryActivity.this, "Failed to load travel spaces", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    private void parseTravelSpaces(JSONArray travelSpacesArray) {
        travelSpaceList.clear();

        try {
            for (int i = 0; i < travelSpacesArray.length(); i++) {
                JSONObject travelSpaceJson = travelSpacesArray.getJSONObject(i);

                String id = travelSpaceJson.getString("id");
                String title = travelSpaceJson.getString("title");
                String description = travelSpaceJson.getString("description");
                String expirationDate = travelSpaceJson.getString("expirationDate");

                TravelSpace travelSpace = new TravelSpace(id, title, description, expirationDate);
                travelSpaceList.add(travelSpace);
            }

            travelSpacesSendAdapter.notifyDataSetChanged();

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
        }
    }

    @Override
    public void onClick(View view) {
        // This method will be called when an item is clicked in the RecyclerView
        int position = travelSpacesRecyclerView.getChildAdapterPosition(view);
        if (position != RecyclerView.NO_POSITION) {
            TravelSpace clickedTravelSpace = travelSpaceList.get(position);
            shareItinerary(clickedTravelSpace);
        }
    }

    private void shareItinerary(TravelSpace travelSpace) {

        Intent intent = new Intent(ShareItineraryActivity.this, TravelSpaceCommentActivity.class);
        intent.putExtra("message", generatedItinerary);
        intent.putExtra("userId", getUserId());
        intent.putExtra("title", travelSpace.getTitle());
        intent.putExtra("description", travelSpace.getDescription());
        intent.putExtra("userId", getUserId());
        intent.putExtra("travelSpaceId", travelSpace.getId());
        intent.putExtra("messageType", "HTML");

        startActivity(intent);
        Toast.makeText(this, "Itinerary shared to: " + travelSpace.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }
}
