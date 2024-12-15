package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Interceptor;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;
import ua.naiksoftware.stomp.provider.WebSocketsConnectionProvider;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;

public class VoteActivity extends AppCompatActivity {

    private TextView pollTitleTextView, option1Counter, option2Counter;
    private Button option1Button, option2Button, voteButton;
    private ProgressBar option1ProgressBar, option2ProgressBar;
    private Poll poll; 
    private Map<String, Integer> voteCounts; 
    private StompClient stompClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Poll currentPoll;
    private boolean isWebSocketConnected = false;
    


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        // Retrieve Poll data from Intent
        poll = (Poll) getIntent().getSerializableExtra("poll");
        currentPoll = getPollFromIntent();

        // Initialize vote counts
        voteCounts = new HashMap<>();
        for (String option : poll.getOptions()) {
            voteCounts.put(option, 0); // Initialize each option with 0 votes
        }

        // Initialize UI elements
        pollTitleTextView = findViewById(R.id.pollTitleTextView);
        option1Counter = findViewById(R.id.option1Counter);
        option2Counter = findViewById(R.id.option2Counter);
        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option1ProgressBar = findViewById(R.id.option1ProgressBar);
        option2ProgressBar = findViewById(R.id.option2ProgressBar);


        // Set Poll Title
        pollTitleTextView.setText(poll.getTitle());

        // Set button text for options based on poll data
        List<String> options = poll.getOptions();
        if (options.size() > 0) {
            option1Button.setText(options.get(0));
            option1Counter.setText(voteCounts.get(options.get(0)) + " votes");
        }
        if (options.size() > 1) {
            option2Button.setText(options.get(1));
            option2Counter.setText(voteCounts.get(options.get(1)) + " votes");
        }

        // Set up button click listeners to cast votes
        option1Button.setOnClickListener(v -> castVote(0));
        option2Button.setOnClickListener(v -> castVote(1));

        // Update UI initially to set progress bars
        updateUI();

        setupWebSocket();
    }

    private void castVote(int optionIndex) {
        if (currentPoll != null) {
            String option = currentPoll.getOptions().get(optionIndex);
            sendVote(currentPoll.getId(), option);
        }
    }

    private void updateUI() {
        if (currentPoll == null || voteCounts == null) return;

        List<String> options = currentPoll.getOptions();
        int totalVotes = voteCounts.values().stream().mapToInt(Integer::intValue).sum();

        // Update vote counts
        if (options.size() > 0) {
            String option1 = options.get(0);
            int option1Votes = voteCounts.getOrDefault(option1, 0);
            option1Counter.setText(option1Votes + " votes");
            
            // Calculate and update progress bar
            int option1Percentage = totalVotes > 0 ? 
                (int) ((option1Votes / (float) totalVotes) * 100) : 0;
            option1ProgressBar.setProgress(option1Percentage);
        }

        if (options.size() > 1) {
            String option2 = options.get(1);
            int option2Votes = voteCounts.getOrDefault(option2, 0);
            option2Counter.setText(option2Votes + " votes");
            
            // Calculate and update progress bar
            int option2Percentage = totalVotes > 0 ? 
                (int) ((option2Votes / (float) totalVotes) * 100) : 0;
            option2ProgressBar.setProgress(option2Percentage);
        }
    }

    private void setupWebSocket() {
        // Use the correct endpoint from your Spring config
        String wsUrl = "ws://" + ApiConstants.BASE_URL + "/ws/websocket";
        Log.d("VoteActivity", "Setting up WebSocket connection to: " + wsUrl);
        
        // Create connection headers
        Map<String, String> connectHttpHeaders = new HashMap<>();
        connectHttpHeaders.put("Sec-WebSocket-Protocol", "v10.stomp, v11.stomp, v12.stomp");
        connectHttpHeaders.put("Origin", ApiConstants.BASE_URL);

        // Create StompClient
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl, connectHttpHeaders);

        // Configure STOMP client with prefixes matching your Spring config
        stompClient.withClientHeartbeat(10000)
                  .withServerHeartbeat(10000);

        // Set up lifecycle listener
        compositeDisposable.add(stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        Log.d("VoteActivity", "WebSocket Connected!");
                        isWebSocketConnected = true;
                        subscribeToTopics();
                        break;
                    case ERROR:
                        Log.e("VoteActivity", "WebSocket Error", lifecycleEvent.getException());
                        isWebSocketConnected = false;
                        break;
                    case CLOSED:
                        Log.d("VoteActivity", "WebSocket Closed");
                        isWebSocketConnected = false;
                        break;
                }
            }));

        // Connect
        stompClient.connect();
    }

    private void subscribeToTopics() {
        // Subscribe to the poll updates topic
        compositeDisposable.add(stompClient.topic("/topic/polls")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                Log.d("VoteActivity", "Received poll update: " + topicMessage.getPayload());
                try {
                    JSONObject pollUpdate = new JSONObject(topicMessage.getPayload());
                    updatePollData(pollUpdate);
                } catch (JSONException e) {
                    Log.e("VoteActivity", "Error parsing poll update", e);
                }
            }, throwable -> {
                Log.e("VoteActivity", "Error on subscription", throwable);
            }));

        // Subscribe to votes topic
        compositeDisposable.add(stompClient.topic("/topic/votes")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                Log.d("VoteActivity", "Received vote update: " + topicMessage.getPayload());
                try {
                    JSONObject voteUpdate = new JSONObject(topicMessage.getPayload());
                    updatePollData(voteUpdate);
                } catch (JSONException e) {
                    Log.e("VoteActivity", "Error parsing vote update", e);
                }
            }));
    }

    private void sendVote(int pollId, String option) {
        if (!isWebSocketConnected) {
            Toast.makeText(this, "Connecting to server... Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasUserVoted(pollId)) {
            Toast.makeText(this, "You have already voted!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject voteData = new JSONObject();
            voteData.put("pollId", pollId);
            voteData.put("option", option);
            voteData.put("userId", getUserId());

            stompClient.send("/app/poll/vote", voteData.toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Log.d("VoteActivity", "Vote sent successfully!");
                    saveUserVote(pollId); // Save the vote locally
                    Toast.makeText(this, "Vote submitted!", Toast.LENGTH_SHORT).show();
                    
                    // Disable voting buttons
                    option1Button.setEnabled(false);
                    option2Button.setEnabled(false);
                }, throwable -> {
                    Log.e("VoteActivity", "Error sending vote", throwable);
                    Toast.makeText(this, "Failed to submit vote", Toast.LENGTH_SHORT).show();
                });

        } catch (JSONException e) {
            Log.e("VoteActivity", "Error creating vote JSON", e);
            Toast.makeText(this, "Error creating vote", Toast.LENGTH_SHORT).show();
        }
    }

    // Add this method to handle vote button clicks
    private void onVoteButtonClick(String option) {
        if (currentPoll != null) {
            sendVote(currentPoll.getId(), option);
        } else {
            Toast.makeText(this, "Error: Poll data not available", Toast.LENGTH_SHORT).show();
        }
    }

    private int getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1);
    }

    @Override
    protected void onDestroy() {
        disconnectWebSocket();
        super.onDestroy();
    }

    private Poll getPollFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("poll")) {
            return intent.getParcelableExtra("poll");
        }
        return null;
    }

    public void onVoteButtonClick(View view) {
        String option = "";
        if (view.getId() == R.id.option1Button) {
            option = currentPoll.getOptions().get(0);
        } else if (view.getId() == R.id.option2Button) {
            option = currentPoll.getOptions().get(1);
        }
        
        if (!option.isEmpty()) {
            onVoteButtonClick(option);
        }
    }

    private void updatePollData(JSONObject pollUpdate) {
        try {
            // Check if we have a poll object in the update
            if (pollUpdate.has("poll")) {
                JSONObject pollData = pollUpdate.getJSONObject("poll");
                
                // Update vote counts from server data
                if (pollData.has("votes")) {
                    JSONObject votes = pollData.getJSONObject("votes");
                    voteCounts.clear(); // Clear existing counts
                    
                    // Update counts for each option
                    for (String option : currentPoll.getOptions()) {
                        if (votes.has(option)) {
                            voteCounts.put(option, votes.getInt(option));
                        } else {
                            voteCounts.put(option, 0);
                        }
                    }
                }
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    updateUI();
                    
                    // Disable voting buttons if user has already voted
                    if (hasUserVoted(currentPoll.getId())) {
                        option1Button.setEnabled(false);
                        option2Button.setEnabled(false);
                        Toast.makeText(this, "You have already voted!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (JSONException e) {
            Log.e("VoteActivity", "Error parsing poll update", e);
        }
    }

    private void connectWebSocket() {
        if (stompClient != null && !isWebSocketConnected) {
            Log.d("VoteActivity", "Attempting to connect WebSocket...");
            stompClient.connect();
        }
    }

    private void disconnectWebSocket() {
        if (stompClient != null) {
            stompClient.disconnect();
        }
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    private String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Track user votes in SharedPreferences
    private void saveUserVote(int pollId) {
        SharedPreferences prefs = getSharedPreferences("VotePrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("voted_" + pollId, true).apply();
    }

    private boolean hasUserVoted(int pollId) {
        SharedPreferences prefs = getSharedPreferences("VotePrefs", MODE_PRIVATE);
        return prefs.getBoolean("voted_" + pollId, false);
    }
}

