package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class PollActivity extends AppCompatActivity {

    private EditText pollTitleEditText, pollOptionsEditText;
    private Button createPollButton;
    private Button deletePollButton;
    private ListView activePollsListView;
    private ArrayAdapter<String> pollsAdapter;
    private List<Poll> activePollsList = new ArrayList<>();
    private StompClient stompClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poll_screen);

        // Initialize views
        pollTitleEditText = findViewById(R.id.pollTitle);
        pollOptionsEditText = findViewById(R.id.pollOptions);
        createPollButton = findViewById(R.id.createPollButton);
        activePollsListView = findViewById(R.id.activePollsListView);

        // Setup ListView adapter
        pollsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        activePollsListView.setAdapter(pollsAdapter);

        // Load active polls
        getActivePolls();

        // Create poll button click listener
        createPollButton.setOnClickListener(v -> createPoll());

        // Setup WebSocket
        setupWebSocket();

        // Handle poll title clicks
        activePollsListView.setOnItemClickListener((parent, view, position, id) -> {
            Poll selectedPoll = activePollsList.get(position);
            Intent intent = new Intent(PollActivity.this, VoteActivity.class);
            intent.putExtra("poll", (Parcelable) selectedPoll); // Cast to Parcelable
            startActivity(intent);
        });
    }

    private void setupWebSocket() {
        String wsUrl = "ws://coms-3090-010.class.las.iastate.edu:8080/ws";
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        compositeDisposable.add(stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        Log.d("PollActivity", "STOMP connection opened");
                        // Only subscribe after connection is established
                        subscribeToTopics();
                        break;
                    case CLOSED:
                        Log.d("PollActivity", "STOMP connection closed");
                        break;
                    case ERROR:
                        Log.e("PollActivity", "STOMP connection error", lifecycleEvent.getException());
                        break;
                }
            }));

        stompClient.connect();
    }

    private void subscribeToTopics() {
        // Subscribe to poll updates
        compositeDisposable.add(stompClient.topic("/topic/polls")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                String jsonData = topicMessage.getPayload();
                Log.d("PollActivity", "Received message: " + jsonData);
                try {
                    JSONObject pollUpdate = new JSONObject(jsonData);
                    Poll poll = Poll.fromJson(pollUpdate);
                    if (poll != null) {
                        activePollsList.add(poll);
                        pollsAdapter.add(poll.getTitle());
                        pollsAdapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    Log.e("PollActivity", "Failed to parse JSON: " + e.getMessage());
                }
            }, throwable -> {
                Log.e("PollActivity", "Error on subscribe topic", throwable);
            }));
    }

    private void createPoll() {
        String title = pollTitleEditText.getText().toString();
        String optionsString = pollOptionsEditText.getText().toString();

        if (title.isEmpty() || optionsString.isEmpty()) {
            Toast.makeText(this, "Please enter a title and options", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = Arrays.asList(optionsString.split(","));
        JSONObject pollData = new JSONObject();
        try {
            pollData.put("title", title);
            pollData.put("options", new JSONArray(options));
            pollData.put("creatorId", getUserId()); // Use dynamic user ID

            JsonObjectRequest createPollRequest = new JsonObjectRequest(Request.Method.POST,
                    "http://coms-3090-010.class.las.iastate.edu:8080/api/polls",
                    pollData,
                    response -> {
                        Toast.makeText(this, "Poll created!", Toast.LENGTH_SHORT).show();
                        getActivePolls(); // Refresh the poll list
                    },
                    error -> {
                        Toast.makeText(this, "Error creating poll", Toast.LENGTH_SHORT).show();
                        Log.e("PollActivity", "Error creating poll: " + error.toString());
                    });
            Volley.newRequestQueue(this).add(createPollRequest);
        } catch (JSONException e) {
            Log.e("PollActivity", "JSON Exception: " + e.getMessage());
        }
    }

    private void getActivePolls() {
        JsonArrayRequest getActivePollsRequest = new JsonArrayRequest(Request.Method.GET,
                "http://coms-3090-010.class.las.iastate.edu:8080/api/polls",
                null,
                response -> {
                    Log.d("PollActivity", "Active Polls Response: " + response.toString());
                    activePollsList.clear();
                    pollsAdapter.clear(); // Clear existing adapter data
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            Poll poll = Poll.fromJson(response.getJSONObject(i));
                            if (poll != null) {
                                activePollsList.add(poll);
                                pollsAdapter.add(poll.getTitle());
                            } else {
                                Log.e("PollActivity", "Failed to create poll from JSON object");
                            }
                        } catch (JSONException e) {
                            Log.e("PollActivity", "JSON Exception while fetching polls: " + e.getMessage());
                        }
                    }
                    pollsAdapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(this, "Error fetching active polls", Toast.LENGTH_SHORT).show();
                    Log.e("PollActivity", "Error fetching active polls: " + error.toString());
                });
        Volley.newRequestQueue(this).add(getActivePollsRequest);
    }

    private int getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1); // Default to -1 if not found
    }

    @Override
    protected void onDestroy() {
        if (stompClient != null) {
            stompClient.disconnect();
        }
        compositeDisposable.dispose();
        super.onDestroy();
    }
}
