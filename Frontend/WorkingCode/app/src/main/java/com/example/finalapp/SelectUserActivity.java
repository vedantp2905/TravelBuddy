package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.finalapp.api.ChatApiService;
import com.example.finalapp.models.User;
import com.example.finalapp.api.RetrofitClient;
import java.io.IOException;

public class SelectUserActivity extends AppCompatActivity {

    private ListView conversationsListView;
    private ListView searchResultsListView;
    private SearchView searchView;
    private ArrayAdapter<String> conversationsAdapter;
    private ArrayAdapter<String> searchAdapter;
    private ArrayList<String> conversationsList;
    private ArrayList<String> searchResults;
    private Map<String, String> userIdMap;
    private String currentUserId;
    private ChatApiService apiService;
    private StompClient stompClient;
    private CompositeDisposable compositeDisposable;
    private static final String SERVER_URL = ApiConstants.BASE_URL + "/ws/websocket";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectuseractivity);

        currentUserId = getIntent().getStringExtra("currentUserId");
        initializeViews();
        setupAdapters();
        setupSearch();
        setupClickListeners();
        loadActiveConversations();
        initializeWebSocket();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveConversations();  // Reload conversations when returning to this activity
    }

    @Override
    protected void onDestroy() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        if (stompClient != null) {
            stompClient.disconnect();
        }
        super.onDestroy();
    }

    private void initializeViews() {
        conversationsListView = findViewById(R.id.conversationsListView);
        searchResultsListView = findViewById(R.id.searchResultsListView);
        searchView = findViewById(R.id.searchView);
        userIdMap = new HashMap<>();
        
        apiService = RetrofitClient.getInstance().create(ChatApiService.class);
    }

    private void setupAdapters() {
        conversationsList = new ArrayList<>();
        searchResults = new ArrayList<>();
        
        conversationsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, conversationsList);
        searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchResults);
        
        conversationsListView.setAdapter(conversationsAdapter);
        searchResultsListView.setAdapter(searchAdapter);
    }

    private void loadActiveConversations() {
        Log.d("Conversations", "Requesting conversations for user ID: " + currentUserId);
        Call<List<ChatApiService.Conversation>> call = apiService.getConversations(currentUserId);
        call.enqueue(new Callback<List<ChatApiService.Conversation>>() {
            @Override
            public void onResponse(Call<List<ChatApiService.Conversation>> call, Response<List<ChatApiService.Conversation>> response) {
                Log.d("Conversations", "Response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    List<ChatApiService.Conversation> conversations = response.body();
                    Log.d("Conversations", "Number of conversations: " + conversations.size());
                    
                    // Add detailed logging for each conversation
                    for (ChatApiService.Conversation conv : conversations) {
                        Log.d("Conversations", String.format(
                            "Conversation - ID: %s, OtherUser: %s (ID: %s)",
                            conv.id, conv.otherUsername, conv.otherUserId
                        ));
                    }

                    conversationsList.clear();
                    if (conversations.isEmpty()) {
                        TextView noConversationsText = findViewById(R.id.noConversationsText);
                        if (noConversationsText != null) {
                            noConversationsText.setVisibility(View.VISIBLE);
                        }
                        conversationsListView.setVisibility(View.GONE);
                    } else {
                        for (ChatApiService.Conversation conv : conversations) {
                            conversationsList.add(conv.otherUsername);
                            userIdMap.put(conv.otherUsername, conv.otherUserId);
                            userIdMap.put(conv.otherUsername + "_conversationId", conv.id);
                        }
                        conversationsAdapter.notifyDataSetChanged();
                        conversationsListView.setVisibility(View.VISIBLE);
                        TextView noConversationsText = findViewById(R.id.noConversationsText);
                        if (noConversationsText != null) {
                            noConversationsText.setVisibility(View.GONE);
                        }
                    }
                } else {
                    String errorBody = "";
                    try {
                        errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                    } catch (IOException e) {
                        errorBody = "Could not read error body";
                    }
                    Log.e("Conversations", "Error response: " + response.code() + " - " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<List<ChatApiService.Conversation>> call, Throwable t) {
                Log.e("Conversations", "Network error loading conversations", t);
                Toast.makeText(SelectUserActivity.this, 
                    "Error loading conversations: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 1) {
                    searchUsers(newText);
                    searchResultsListView.setVisibility(View.VISIBLE);
                    conversationsListView.setVisibility(View.GONE);
                } else {
                    searchResultsListView.setVisibility(View.GONE);
                    conversationsListView.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
    }

    private void searchUsers(String query) {
        Call<List<User>> call = apiService.getAllUsers();
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    searchResults.clear();
                    String lowerCaseQuery = query.toLowerCase();
                    for (User user : response.body()) {
                        if (!user.getId().toString().equals(currentUserId) && 
                            user.getUsername().toLowerCase().contains(lowerCaseQuery)) {
                            searchResults.add(user.getUsername());
                            userIdMap.put(user.getUsername(), user.getId().toString());
                        }
                    }
                    searchAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(SelectUserActivity.this, "Error searching users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        View.OnClickListener messageClickListener = view -> {
            String username = (String) view.getTag();
            String userId = userIdMap.get(username);
            startChat(userId, username);
        };

        conversationsListView.setOnItemClickListener((parent, view, position, id) -> {
            String username = conversationsList.get(position);
            String userId = userIdMap.get(username);
            startChat(userId, username);
        });

        searchResultsListView.setOnItemClickListener((parent, view, position, id) -> {
            String username = searchResults.get(position);
            String userId = userIdMap.get(username);
            startChat(userId, username);
        });
    }

    private void startChat(String otherUserId, String otherUsername) {
        Intent intent = new Intent(this, DirectMessageActivity.class);
        intent.putExtra("currentUserId", currentUserId);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUsername", otherUsername);
        String conversationId = userIdMap.get(otherUsername + "_conversationId");
        if (conversationId != null) {
            intent.putExtra("conversationId", conversationId);
        }
        startActivity(intent);
        loadActiveConversations();
    }

    private void initializeWebSocket() {
        compositeDisposable = new CompositeDisposable();
        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("userId", currentUserId));

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, SERVER_URL);
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        compositeDisposable.add(stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        Log.d("STOMP", "WebSocket Connection Opened");
                        subscribeToConversationUpdates();
                        break;
                    case CLOSED:
                        Log.d("STOMP", "WebSocket Connection Closed");
                        reconnectWebSocket();
                        break;
                }
            }));

        stompClient.connect(headers);
    }

    private void subscribeToConversationUpdates() {
        try {
            // Subscribe to conversation updates with error handling
            compositeDisposable.add(stompClient.topic("/user/" + currentUserId + "/conversations")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e("STOMP", "Conversation subscription error", throwable);
                    reconnectWebSocket();
                })
                .retry(3) // Retry 3 times before giving up
                .subscribe(topicMessage -> {
                    Log.d("STOMP", "Received conversation update: " + topicMessage.getPayload());
                    loadActiveConversations();
                }, throwable -> {
                    Log.e("STOMP", "Final error on subscribe topic", throwable);
                }));

            // Direct messages subscription with error handling
            compositeDisposable.add(stompClient.topic("/topic/user/" + currentUserId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e("STOMP", "Message subscription error", throwable);
                    reconnectWebSocket();
                })
                .retry(3)
                .subscribe(topicMessage -> {
                    Log.d("STOMP", "Received message: " + topicMessage.getPayload());
                    loadActiveConversations();
                }, throwable -> {
                    Log.e("STOMP", "Final error on message subscription", throwable);
                }));
        } catch (Exception e) {
            Log.e("STOMP", "Error setting up subscriptions", e);
            reconnectWebSocket();
        }
    }

    private void reconnectWebSocket() {
        if (stompClient != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                try {
                    if (!stompClient.isConnected()) {
                        Log.d("STOMP", "Attempting to reconnect WebSocket...");
                        stompClient.disconnect();
                        initializeWebSocket();
                    }
                } catch (Exception e) {
                    Log.e("STOMP", "Error during reconnection", e);
                }
            }, 5000); // Wait 5 seconds before reconnecting
        }
    }
}
