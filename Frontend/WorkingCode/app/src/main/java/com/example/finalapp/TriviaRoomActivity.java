package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.ImageButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.example.finalapp.adapters.PlayerAdapter;
import ua.naiksoftware.stomp.dto.StompHeader;

public class TriviaRoomActivity extends AppCompatActivity {
    private TextView tvRoomCode;
    private TextView tvPlayerCount;
    private Button btnStartGame;
    private RecyclerView rvPlayers;
    private StompClient stompClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private boolean isHost;
    private String roomCode;
    private String userId;
    private boolean isWebSocketConnected = false;
    public static StompClient activeStompClient;
    private boolean isLeaving = false;
    private TextView tvHostName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trivia_room);

        // Initialize views
        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvPlayerCount = findViewById(R.id.tvPlayerCount);
        btnStartGame = findViewById(R.id.btnStartGame);
        rvPlayers = findViewById(R.id.rvPlayers);
        tvHostName = findViewById(R.id.tvHostName);

        // Get intent extras
        isHost = getIntent().getBooleanExtra("isHost", false);
        userId = getIntent().getStringExtra("userId");
        roomCode = getIntent().getStringExtra("roomCode");

        if (!isHost && roomCode != null) {
            tvRoomCode.setText("Room Code: " + roomCode);
            tvPlayerCount.setText("Players: 1"); // Will be updated when player list updates
        }

        // Setup RecyclerView
        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        rvPlayers.setAdapter(new PlayerAdapter(new ArrayList<>()));
        
        // Setup WebSocket connection
        setupWebSocket();

        if (isHost) {
            btnStartGame.setOnClickListener(v -> startGame());
        } else {
            btnStartGame.setVisibility(View.GONE);
        }

        setupShareButton();
    }

    private void setupWebSocket() {
        String wsUrl = "ws://coms-3090-010.class.las.iastate.edu:8080/ws/websocket";
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        stompClient.withClientHeartbeat(5000).withServerHeartbeat(5000);
        
        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("userId", userId));
        
        compositeDisposable.add(stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        isWebSocketConnected = true;
                        if (isHost) {
                            createRoom();
                        } else {
                            joinRoom();
                        }
                        break;
                    case ERROR:
                    case CLOSED:
                        isWebSocketConnected = false;
                        if (!isLeaving) {
                            reconnectWebSocket();
                        }
                        break;
                }
            }));

        // Only subscribe to create topic if host
        if (isHost) {
            compositeDisposable.add(stompClient.topic("/topic/trivia/room/create")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    JSONObject message = new JSONObject(topicMessage.getPayload());
                    handleRoomMessage(message);
                }, throwable -> {
                    Log.e("STOMP", "Error on create subscription", throwable);
                }));
        }

        // Add subscription for user-specific error messages
        compositeDisposable.add(stompClient.topic("/user/" + userId + "/queue/errors")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                try {
                    JSONObject message = new JSONObject(topicMessage.getPayload());
                    if (message.getString("type").equals("ROOM_NOT_FOUND")) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Room does not exist", Toast.LENGTH_SHORT).show();
                            cleanupAndExit();
                        });
                    }
                } catch (JSONException e) {
                    Log.e("STOMP", "Error parsing error message", e);
                }
            }, throwable -> {
                Log.e("STOMP", "Error on error subscription", throwable);
            }));

        stompClient.connect(headers);
    }

    private void handleRoomMessage(JSONObject message) {
        try {
            String type;
            JSONObject messageBody;
            
            // Check if this is a direct message or wrapped in body
            if (message.has("body")) {
                messageBody = new JSONObject(message.getString("body"));
                type = messageBody.getString("type");
            } else {
                messageBody = message;
                type = message.getString("type");
            }
            
            switch (type) {
                case "JOIN":
                    JSONArray players = messageBody.getJSONArray("players");
                    runOnUiThread(() -> {
                        updatePlayersList(players);
                        tvPlayerCount.setText("Players: " + players.length());
                    });
                    break;
                    
                case "ROOM_CREATED":
                    roomCode = messageBody.getString("roomCode");
                    String hostName = messageBody.getString("hostName");
                    runOnUiThread(() -> {
                        tvRoomCode.setText("Room Code: " + roomCode);
                        tvHostName.setText("Host: " + hostName);
                    });
                    subscribeToRoom(roomCode);
                    break;
                case "ROOM_NOT_FOUND":
                    runOnUiThread(() -> {
                        handleRoomNotFound();
                    });
                    break;
                case "GAME_STARTED":
                    Log.d("STOMP", "Game started event received");
                    startTriviaGame();
                    break;
                case "HOST_LEFT":
                    if (!isLeaving) {
                        isLeaving = true;
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Host has left, room closed", Toast.LENGTH_SHORT).show();
                            cleanupAndExit();
                        });
                    }
                    break;
                case "ROOM_DELETED":
                    runOnUiThread(() -> {
                        Toast.makeText(this, "This room has been deleted", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, TriviaActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                    break;
                case "ROOM_ACTIVE":
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Game already in progress", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    break;
                case "PLAYER_LEFT":
                    Log.d("STOMP", "Player left event received");
                    JSONArray remainingPlayers = messageBody.getJSONArray("players");
                    runOnUiThread(() -> {
                        try {
                            updatePlayersList(remainingPlayers);
                            tvPlayerCount.setText("Players: " + remainingPlayers.length());
                            if (remainingPlayers.length() > 0) {
                                tvHostName.setText("Host: " + remainingPlayers.getString(0));
                            }
                        } catch (JSONException e) {
                            Log.e("TriviaRoom", "Error updating players after leave", e);
                        }
                    });
                    break;
                case "ROOM_FULL":
                    runOnUiThread(() -> {
                        try {
                            Toast.makeText(this, messageBody.getString("message"), Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(this, "Room is full", Toast.LENGTH_SHORT).show();
                        }
                        cleanupAndExit();
                    });
                    break;
            }
        } catch (JSONException e) {
            Log.e("STOMP", "Error processing room message", e);
        }
    }

    private void createRoom() {
        if (!isWebSocketConnected) {
            Toast.makeText(this, "Waiting for connection...", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject request = new JSONObject();
            request.put("userId", userId);
            stompClient.send("/app/trivia/create", request.toString())
                .subscribe(() -> {
                    // Initialize the player list with the host
                    runOnUiThread(() -> {
                        List<String> initialPlayers = new ArrayList<>();
                        initialPlayers.add("You (Host)");  // Add host as first player
                        PlayerAdapter adapter = (PlayerAdapter) rvPlayers.getAdapter();
                        if (adapter != null) {
                            adapter.updatePlayers(initialPlayers);
                            adapter.notifyDataSetChanged();
                            tvPlayerCount.setText("Players: 1");
                        }
                    });
                }, throwable -> {
                    Log.e("STOMP", "Error sending create room message", throwable);
                    Toast.makeText(this, "Error creating room", Toast.LENGTH_SHORT).show();
                });
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating room", Toast.LENGTH_SHORT).show();
        }
    }

    private void joinRoom() {
        if (!isWebSocketConnected) {
            handleRoomNotFound();
            return;
        }
        
        try {
            JSONObject request = new JSONObject();
            request.put("userId", userId);
            
            // Subscribe to room messages before sending join request
            if (roomCode != null && !roomCode.isEmpty()) {
                subscribeToRoom(roomCode);
                // Set initial room code display for non-host players
                runOnUiThread(() -> {
                    tvRoomCode.setText("Room Code: " + roomCode);
                });
            }
            
            stompClient.send("/app/trivia/join/" + roomCode, request.toString())
                .subscribe(() -> {}, throwable -> {
                    Log.e("STOMP", "Error joining room", throwable);
                    runOnUiThread(this::handleRoomNotFound);
                });
        } catch (JSONException e) {
            e.printStackTrace();
            handleRoomNotFound();
        }
    }

    private void startGame() {
        if (userId == null || userId.isEmpty()) {
            Log.e("TriviaRoom", "Attempting to start game with null/empty userId");
            Toast.makeText(this, "Error: Invalid user session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            JSONObject request = new JSONObject();
            request.put("userId", userId);
            request.put("roomCode", roomCode);
            
            // Log current players for debugging
            PlayerAdapter adapter = (PlayerAdapter) rvPlayers.getAdapter();
            Log.d("TriviaRoom", "Current player count: " + adapter.getItemCount());
            Log.d("TriviaRoom", "Players in room: " + adapter.getPlayerNames().toString());
            
            if (adapter.getItemCount() < 2) {
                Toast.makeText(this, "At least 2 players needed to start", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (adapter.getItemCount() > 6) {
                Toast.makeText(this, "Maximum 6 players allowed per game", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d("TriviaRoom", "Starting game for room: " + roomCode + " with userId: " + userId);
            stompClient.send("/app/trivia/start/" + roomCode, request.toString()).subscribe();
        } catch (JSONException e) {
            Log.e("TriviaRoom", "Error creating start game request", e);
            Toast.makeText(this, "Error starting game", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTriviaGame() {
        activeStompClient = stompClient;  // Set the static reference
        Intent intent = new Intent(this, TriviaGameActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void updatePlayersList(JSONArray players) {
        List<String> playerNames = new ArrayList<>();
        for (int i = 0; i < players.length(); i++) {
            try {
                playerNames.add(players.getString(i));
            } catch (JSONException e) {
                Log.e("TriviaRoom", "Error parsing player name", e);
            }
        }
        
        runOnUiThread(() -> {
            PlayerAdapter adapter = (PlayerAdapter) rvPlayers.getAdapter();
            if (adapter != null) {
                adapter.updatePlayers(playerNames);
                tvPlayerCount.setText("Players: " + playerNames.size());
            }
        });
    }

    private void reconnectWebSocket() {
        if (stompClient != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (!isWebSocketConnected) {
                        stompClient.connect();
                    }
                } catch (Exception e) {
                    Log.d("STOMP", "Reconnect attempt on disconnected client");
                }
            }, 5000); // 5 second delay before reconnecting
        }
    }

    private void subscribeToRoom(String newRoomCode) {
        Log.d("STOMP", "Subscribing to room: " + newRoomCode);
        compositeDisposable.add(stompClient.topic("/topic/trivia/room/" + newRoomCode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                Log.d("STOMP", "Received room message: " + topicMessage.getPayload());
                JSONObject roomMessage = new JSONObject(topicMessage.getPayload());
                handleRoomMessage(roomMessage);
            }, throwable -> {
                Log.e("STOMP", "Error on room subscription: " + throwable.getMessage());
            }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isLeaving && stompClient != null && stompClient.isConnected()) {
            try {
                isLeaving = true;
                JSONObject leaveMessage = new JSONObject();
                leaveMessage.put("userId", userId);
                
                // Send leave message and wait for confirmation before disconnecting
                stompClient.send("/app/trivia/leave/" + roomCode, leaveMessage.toString())
                    .subscribe(
                        () -> {
                            Log.d("TriviaRoom", "Leave message sent successfully");
                            disconnectStomp();
                        },
                        error -> {
                            Log.e("TriviaRoom", "Error sending leave message", error);
                            disconnectStomp();
                        }
                    );
            } catch (JSONException e) {
                Log.e("TriviaRoom", "Error creating leave message", e);
                disconnectStomp();
            }
        } else {
            disconnectStomp();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!isLeaving && stompClient != null && stompClient.isConnected()) {
            try {
                isLeaving = true;
                JSONObject leaveMessage = new JSONObject();
                leaveMessage.put("userId", userId);

                stompClient.send("/app/trivia/leave/" + roomCode, leaveMessage.toString())
                        .subscribe(
                                () -> {
                                    Log.d("TriviaRoom", "Leave message sent successfully");
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Left room", Toast.LENGTH_SHORT).show();
                                        cleanupAndExit();
                                    });
                                },
                                error -> {
                                    Log.e("TriviaRoom", "Error sending leave message", error);
                                    runOnUiThread(() -> {
                                        Toast.makeText(this, "Error leaving room", Toast.LENGTH_SHORT).show();
                                        cleanupAndExit();
                                    });
                                }
                        );
            } catch (JSONException e) {
                Log.e("TriviaRoom", "Error creating leave message", e);
                Toast.makeText(this, "Error leaving room", Toast.LENGTH_SHORT).show();
                cleanupAndExit();
            }
        } else {
            cleanupAndExit();
        }
    }

    private void cleanupAndExit() {
        // Dispose of all subscriptions
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        
        // Disconnect WebSocket
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        
        // Navigate to trivia activity
        Intent intent = new Intent(this, TriviaActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void handleRoomNotFound() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Room does not exist or has been deleted", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("error", "Room does not exist or has been deleted");
            setResult(RESULT_CANCELED, resultIntent);
            cleanupAndExit();
        });
    }

    private void disconnectStomp() {
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
        
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        
        // Clear the static reference
        TriviaRoomActivity.activeStompClient = null;
    }

    private void setupShareButton() {
        ImageButton btnShareRoom = findViewById(R.id.btnShareRoom);
        btnShareRoom.setOnClickListener(v -> {
            if (roomCode != null && !roomCode.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareMessage = "Join my TravelBuddy Trivia game! Room code: " + roomCode;
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "Share Room Code"));
            }
        });
    }
} 