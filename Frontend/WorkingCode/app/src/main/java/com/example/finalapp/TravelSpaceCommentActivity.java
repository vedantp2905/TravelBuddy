package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import android.text.Html;


/**
 * Activity for viewing and posting comments within a specific travel space.
 * It uses WebSocket communication for real-time updates and REST API for polling comments.
 */
public class TravelSpaceCommentActivity extends AppCompatActivity {

    private TextView travelSpaceTitle, travelSpaceDescription;
    private EditText commentEditText;
    private Button postCommentButton;
    private RecyclerView commentsRecyclerView;
    private MessagesAdapter MessagesAdapter;
    private List<Message> commentsList;
    private RequestQueue requestQueue;
    private String travelSpaceId;
    private WebSocketClient webSocketClient;
    private String userId;
    private boolean isWebSocketConnected = false;
    private Handler pollingHandler;
    private static final int POLLING_INTERVAL = 3000; // Poll every 3 seconds
    private String lastMessageId = "0"; // Keeps track of the last fetched message ID
    private String messageType;
    /**
     * Runnable task for polling new comments from the server.
     */
    private final Runnable pollMessages = new Runnable() {
        @Override
        public void run() {
            loadComments();
            pollingHandler.postDelayed(this, POLLING_INTERVAL);
        }
    };

    /**
     * Called when the activity is first created.
     * Sets up the UI, initializes WebSocket, and starts polling for comments.
     *
     * @param savedInstanceState The saved instance state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        // Initialize handler for polling
        pollingHandler = new Handler(Looper.getMainLooper());

        // Initialize views and basic setup
        initializeViews();
        setupRecyclerView();

        // Get intent extras and initialize travel space data
        handleIntentExtras();

        // Setup WebSocket for sending messages
        setupWebSocket(userId);

        // Start polling for messages
        startPolling();
    }

    /**
     * Initializes the views and sets up listeners.
     */
    private void initializeViews() {
        travelSpaceTitle = findViewById(R.id.travelSpaceTitle);
        travelSpaceDescription = findViewById(R.id.travelSpaceDescription);
        commentEditText = findViewById(R.id.commentEditText);
        postCommentButton = findViewById(R.id.postCommentButton);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        requestQueue = Volley.newRequestQueue(this);
        userId = getUserId();

        // Handle comment posting
        postCommentButton.setOnClickListener(v -> {
            String comment = commentEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(comment)) {
                if (isWebSocketConnected) {
                    sendMessage(comment);
                } else {
                    Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show();
                    setupWebSocket(userId);
                }
            } else {
                commentEditText.setError("Comment cannot be empty");
            }
        });
    }

    /**
     * Sets up the RecyclerView with an adapter and layout manager.
     */
    private void setupRecyclerView() {
        commentsList = new ArrayList<>();
        MessagesAdapter = new MessagesAdapter(commentsList, message -> {
            Intent intent = new Intent(TravelSpaceCommentActivity.this, TravelSpaceReplyActivity.class);
            intent.putExtra("travelSpaceTitle", travelSpaceTitle.getText().toString());
            intent.putExtra("travelSpaceDescription", travelSpaceDescription.getText().toString());
            intent.putExtra("spaceId", travelSpaceId);
            intent.putExtra("message", message.getMessage());
            intent.putExtra("sender", message.getSender());
            intent.putExtra("messageId", message.getId());
            intent.putExtra("userId", getUserId());
            startActivity(intent);
        });
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(MessagesAdapter);
    }

    /**
     * Retrieves extras from the intent and sets up travel space data.
     * If no travel space ID is provided, the activity closes with an error.
     */
    private void handleIntentExtras() {
        travelSpaceId = getIntent().getStringExtra("travelSpaceId");
        if (travelSpaceId != null) {
            String title = getIntent().getStringExtra("title");
            String description = getIntent().getStringExtra("description");
            travelSpaceTitle.setText(title);
            travelSpaceDescription.setText(description);

            // Check for the messageType extra and send the HTML message if needed
            messageType = getIntent().getStringExtra("messageType");

            if ("HTML".equals(messageType)) {
                // Handle the HTML message
                String htmlContent = getIntent().getStringExtra("message");
                if (htmlContent != null) {
                    // Send the HTML content through WebSocket, ensuring connection is established
                    if (isWebSocketConnected) {
                        htmlMessage(htmlContent);
                    } else {
                        // Wait for WebSocket to connect and then send the message
                        setupWebSocket(userId);
                        // Delay sending the message until connection is established
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (isWebSocketConnected) {
                                htmlMessage(htmlContent);
                            } else {
                                Toast.makeText(TravelSpaceCommentActivity.this, "WebSocket connection failed.", Toast.LENGTH_SHORT).show();
                            }
                        }, 3000);  // Adjust delay if necessary
                    }
                }
            }

            loadComments(); // Initial load of comments
        } else {
            Toast.makeText(this, "TravelSpace ID is missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    /**
     * Starts periodic polling for new comments.
     */
    private void startPolling() {
        pollingHandler.postDelayed(pollMessages, POLLING_INTERVAL);
    }

    /**
     * Stops the periodic polling for comments.
     */
    private void stopPolling() {
        pollingHandler.removeCallbacks(pollMessages);
    }

    /**
     * Loads comments from the server and updates the RecyclerView.
     */
    private void loadComments() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/travelspace/get-messages/" + travelSpaceId;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<Message> newMessages = new ArrayList<>();
                    String maxId = lastMessageId;

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject messageObj = response.getJSONObject(i);
                            String id = messageObj.getString("id");
                            String message = messageObj.getString("message");
                            String messageType = messageObj.getString("messageType");

                            // If it's HTML, handle it
                            if ("HTML".equals(messageType)) {
                                // Optionally, you can parse or modify the HTML here if needed
                                message = message+" Shared Itinerary";  // Replace HTML message with a default string or process it
                            }

                            String sender = messageObj.getJSONObject("sender").getString("username");
                            String timestamp = messageObj.getString("timestamp");

                            // Update the maxId if the current message's ID is higher
                            if (Integer.parseInt(id) > Integer.parseInt(maxId)) {
                                maxId = id;
                            }

                            String parentMessage = null;
                            if (messageObj.has("parentMessage") && !messageObj.isNull("parentMessage")) {
                                JSONObject parentObj = messageObj.getJSONObject("parentMessage");
                                parentMessage = parentObj.getString("message");
                            }

                            // Create and add the new message to the list
                            newMessages.add(new Message(id, message, sender, timestamp, parentMessage, messageType));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    // If new comments are fetched, update the message list
                    if (!maxId.equals(lastMessageId)) {
                        lastMessageId = maxId;
                        commentsList.clear();
                        commentsList.addAll(newMessages);
                        MessagesAdapter.notifyDataSetChanged();
                        commentsRecyclerView.scrollToPosition(commentsList.size() - 1);
                    }

                },
                error -> {
                    Toast.makeText(TravelSpaceCommentActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                }
        );

// Add the request to the queue
        requestQueue.add(jsonArrayRequest);
    }

    /**
     * Sends a comment message via WebSocket.
     *
     * @param comment The comment text to send.
     */
    private void sendMessage(String comment) {
        try {
            JSONObject commentMessage = new JSONObject();
            commentMessage.put("message", comment);
            commentMessage.put("userId", userId);
            commentMessage.put("travelSpaceId", travelSpaceId);
            commentMessage.put("messageType", "TEXT");
            webSocketClient.send(commentMessage.toString());
            commentEditText.setText("");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void htmlMessage(String comment) {
        try {
            // Log the message content to verify it
            Log.d("WebSocket", "Sending HTML Message: " + comment);

            JSONObject commentMessage = new JSONObject();
            commentMessage.put("message", comment);  // Send the HTML content directly
            commentMessage.put("userId", userId);
            commentMessage.put("travelSpaceId", travelSpaceId);
            commentMessage.put("messageType", "HTML");  // Mark as HTML

            // Check if WebSocket is connected before sending
            if (isWebSocketConnected) {
                webSocketClient.send(commentMessage.toString());
                commentEditText.setText("");
            } else {
                Log.d("WebSocket", "Hate websockets");

                Toast.makeText(TravelSpaceCommentActivity.this, "WebSocket not connected", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    /**
     * Sets up a WebSocket connection for real-time communication.
     *
     * @param userId The user ID for authentication.
     */
    private void setupWebSocket(String userId) {
        URI uri;
        try {
            uri = new URI("ws://coms-3090-010.class.las.iastate.edu:8080/travelspace/" + userId + "/" + travelSpaceId);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                runOnUiThread(() -> {
                    isWebSocketConnected = true;
                });
            }

            @Override
            public void onMessage(String message) {
                runOnUiThread(() -> loadComments());
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                runOnUiThread(() -> {
                    isWebSocketConnected = false;
                    Toast.makeText(TravelSpaceCommentActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception ex) {
                runOnUiThread(() -> {
                    isWebSocketConnected = false;
                    Toast.makeText(TravelSpaceCommentActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                });
            }
        };

        webSocketClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isWebSocketConnected) {
            setupWebSocket(userId);
        }
        startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    /**
     * Retrieves the user ID from shared preferences.
     *
     * @return The user ID as a string.
     */
    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }
}
