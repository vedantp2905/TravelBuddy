package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

public class TravelSpacesActivity extends AppCompatActivity {

    private static final String TAG = "TravelSpacesActivity";
    private static final String BASE_URL = ApiConstants.BASE_URL + "/api/travelspace/get/";
    private static final String USERS_URL = ApiConstants.BASE_URL + "/api/travelspace/get-users/";

    private RecyclerView travelSpacesRecyclerView;
    private TravelSpacesAdapter travelSpacesAdapter;
    private List<TravelSpace> travelSpaceList;
    private Button addTravelSpaceButton;
    private RequestQueue requestQueue;

    private String userId;  // Assume userId is available from shared preferences or session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel_spaces);

        travelSpacesRecyclerView = findViewById(R.id.travelSpacesRecyclerView);
        travelSpacesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        addTravelSpaceButton = findViewById(R.id.addTravelSpaceButton);
        addTravelSpaceButton.setOnClickListener(v -> openAddTravelSpaceActivity());

        travelSpaceList = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);

        travelSpacesAdapter = new TravelSpacesAdapter(travelSpaceList, this::openTravelSpace);
        travelSpacesRecyclerView.setAdapter(travelSpacesAdapter);

        // Assume userId is fetched from shared preferences or passed via intent
        userId = "currentUserId"; // Replace with actual user ID fetching logic

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
                        Toast.makeText(TravelSpacesActivity.this, "Failed to load travel spaces", Toast.LENGTH_SHORT).show();
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

            travelSpacesAdapter.notifyDataSetChanged();

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
        }
    }

    private void openTravelSpace(TravelSpace travelSpace) {
        // Fetch the users of the current travel space
        getUsersInTravelSpace(travelSpace.getId(), travelSpace.getTitle(), travelSpace.getDescription());
    }

    private void getUsersInTravelSpace(String postId, String title, String description) {
        String url = USERS_URL + postId;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        checkIfUserAlreadyJoined(response, postId, title, description);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching users", error);
                        Toast.makeText(TravelSpacesActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    private void checkIfUserAlreadyJoined(JSONArray usersArray, String postId, String title, String description) {
        boolean isUserJoined = false;

        try {
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userJson = usersArray.getJSONObject(i);
                String userIdFromApi = userJson.getString("userId");

                // Check if the current user is in the list
                if (userIdFromApi.equals(userId)) {
                    isUserJoined = true;
                    break;
                }
            }
            isUserJoined=true;
            if (isUserJoined) {
                // User already joined, open the comments activity
                openCommentActivity(postId, title, description);
            } else {
                // User is not in the space, ask to join
                openJoinTravelSpaceActivity(postId, title, description);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error checking user in users array", e);
        }
    }

    private void openCommentActivity(String postId, String title, String description) {
        Intent intent = new Intent(this, TravelSpaceCommentActivity.class);
        intent.putExtra("travelSpaceId", postId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);  // Pass the description
        startActivity(intent);
    }

    private void openJoinTravelSpaceActivity(String postId, String title, String description) {
        Intent intent = new Intent(this, JoinTravelSpaceActivity.class);
        intent.putExtra("travelSpaceId", postId);
        intent.putExtra("title", title);
        intent.putExtra("description", description);  // Pass the description
        startActivity(intent);
    }

    private void openAddTravelSpaceActivity() {
        Intent intent = new Intent(this, AddTravelSpaceActivity.class);
        startActivity(intent);
    }
}
