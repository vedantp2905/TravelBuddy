package com.example.finalapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalapp.adapters.MessageAdapter;
import com.example.finalapp.adapters.MessageAdapter.OnReactionListener;
import com.example.finalapp.api.ChatApiService;
import com.example.finalapp.models.Message;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import androidx.annotation.NonNull;
import com.example.finalapp.api.RetrofitClient;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.dto.StompHeader;
import com.example.finalapp.utils.VerticalSpaceItemDecoration;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import android.widget.ImageButton;
import android.graphics.Color;
import com.example.finalapp.models.MessageReaction;
import java.util.HashMap;
import com.example.finalapp.models.ReactionRequest;
import com.google.gson.JsonObject;
import java.util.Map;


public class DirectMessageActivity extends AppCompatActivity {
    private StompClient stompClient;
    private CompositeDisposable compositeDisposable;
    private String serverUrl = ApiConstants.BASE_URL + "/ws/websocket";
    private MessageAdapter messageAdapter;
    private RecyclerView messagesRecyclerView;
    private String conversationId;
    private String currentUserId;

    // Create an empty list for messages
    private List<Message> messageList = new ArrayList<>();

    private Message replyingToMessage;
    private View replyPreviewLayout;
    private TextView replyPreviewText;

    private void connectWebSocket() {
        if (stompClient != null) {
            stompClient.disconnect();
        }

        String wsUrl = ApiConstants.BASE_URL.replace("http://", "ws://") + "/ws/websocket";
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        
        // Enable heartbeat only
        stompClient.withClientHeartbeat(5000).withServerHeartbeat(5000);
        
        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("userId", currentUserId));
        
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(lifecycleEvent -> {
                switch (lifecycleEvent.getType()) {
                    case OPENED:
                        Log.d("STOMP", "WebSocket Connected successfully");
                        subscribeToTopics();
                        break;
                    case ERROR:
                        Log.e("STOMP", "WebSocket Connection error", lifecycleEvent.getException());
                        reconnectWebSocket();
                        break;
                    case CLOSED:
                        Log.d("STOMP", "WebSocket Connection closed");
                        reconnectWebSocket();
                        break;
                }
            }));

        Log.d("STOMP", "Attempting to connect to WebSocket...");
        stompClient.connect(headers);
    }

    private void subscribeToTopics() {
        Log.d("STOMP", "Subscribing to topics for conversation: " + conversationId);
        
        // Subscribe to conversation-specific messages
        compositeDisposable.add(stompClient.topic("/topic/conversations/" + conversationId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                Log.d("STOMP", "Received message on conversation topic: " + topicMessage.getPayload());
                JsonObject jsonObject = new Gson().fromJson(topicMessage.getPayload(), JsonObject.class);
                
                // Check if it's a reaction update
                if (jsonObject.has("type")) {
                    String type = jsonObject.get("type").getAsString();
                    switch (type) {
                        case "REACTION":
                            MessageReaction reaction = new Gson().fromJson(jsonObject, MessageReaction.class);
                            handleReceivedReaction(reaction);
                            break;
                        case "REMOVE_REACTION":
                            JsonObject messageJson = jsonObject.getAsJsonObject("message");
                            Message updatedMessage = new Gson().fromJson(messageJson, Message.class);
                            
                            runOnUiThread(() -> {
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (messageList.get(i).getId().equals(updatedMessage.getId())) {
                                        messageList.set(i, updatedMessage);
                                        messageAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            });
                            break;
                    }
                } else {
                    Message receivedMessage = new Gson().fromJson(topicMessage.getPayload(), Message.class);
                    handleReceivedMessage(receivedMessage);
                }
            }, throwable -> {
                Log.e("STOMP", "Conversation subscription error", throwable);
                showToast("Connection error: " + throwable.getMessage());
                reconnectWebSocket();
            }));

        // Subscribe to user-specific messages
        compositeDisposable.add(stompClient.topic("/topic/user/" + currentUserId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                Log.d("STOMP", "Received message on user topic: " + topicMessage.getPayload());
                String payload = topicMessage.getPayload();
                
                JsonObject jsonObject = new Gson().fromJson(payload, JsonObject.class);
                if (jsonObject.has("type")) {
                    String type = jsonObject.get("type").getAsString();
                    switch (type) {
                        case "REACTION":
                            MessageReaction reaction = new Gson().fromJson(jsonObject, MessageReaction.class);
                            handleReceivedReaction(reaction);
                            break;
                        case "REMOVE_REACTION":
                            JsonObject messageJson = jsonObject.getAsJsonObject("message");
                            Message updatedMessage = new Gson().fromJson(messageJson, Message.class);
                            
                            runOnUiThread(() -> {
                                for (int i = 0; i < messageList.size(); i++) {
                                    if (messageList.get(i).getId().equals(updatedMessage.getId())) {
                                        messageList.set(i, updatedMessage);
                                        messageAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            });
                            break;
                    }
                } else {
                    Message receivedMessage = new Gson().fromJson(payload, Message.class);
                    if (receivedMessage.getConversationId() != null && 
                        receivedMessage.getConversationId().toString().equals(conversationId)) {
                        handleReceivedMessage(receivedMessage);
                    }
                }
            }, throwable -> {
                Log.e("STOMP", "User subscription error", throwable);
                showToast("Connection error: " + throwable.getMessage());
            }));
    }

    private void reconnectWebSocket() {
        if (stompClient != null) {
            stompClient.disconnect();
        }
        new Handler().postDelayed(this::connectWebSocket, 5000); // Retry after 5 seconds
    }

    private void sendMessage(String content) {
        if (!hasActiveSubscription()) {
            Log.e("STOMP", "No active subscription, reconnecting...");
            connectWebSocket();
            showToast("Reconnecting to chat...");
            return;
        }

        Message message = new Message();
        message.setContent(content);
        message.setUserId(Long.parseLong(currentUserId));
        message.setConversationId(Long.parseLong(conversationId));
        
        if (replyingToMessage != null) {
            message.setReplyToId(replyingToMessage.getId());
            message.setReplyToContent(replyingToMessage.getContent());
        }
        
        // Add message to UI immediately with temporary state
        messageList.add(message);
        int tempPosition = messageList.size() - 1;
        messageAdapter.notifyItemInserted(tempPosition);
        messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        
        String jsonMessage = new Gson().toJson(message);
        Log.d("STOMP", "Sending message: " + jsonMessage);
        
        if (stompClient != null && stompClient.isConnected()) {
            compositeDisposable.add(
                stompClient.send("/app/chat", jsonMessage)
                    .compose(applyCompletableSchedulers())
                    .subscribe(
                        () -> {
                            Log.d("STOMP", "Message sent successfully");
                        },
                        throwable -> {
                            Log.e("STOMP", "Error sending message", throwable);
                            runOnUiThread(() -> {
                                // Remove the message from UI if send failed
                                messageList.remove(tempPosition);
                                messageAdapter.notifyItemRemoved(tempPosition);
                                showToast("Failed to send message: " + throwable.getMessage());
                            });
                        }
                    )
            );
        } else {
            showToast("Not connected to chat server. Reconnecting...");
            reconnectWebSocket();
        }
    }

    private void initializeViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        
        // Get currentUserId first
        currentUserId = getIntent().getStringExtra("currentUserId");
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(false);
        layoutManager.setReverseLayout(false);
        
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setHasFixedSize(false);
        messagesRecyclerView.setNestedScrollingEnabled(true);
        
        // Create adapter with all required listeners
        messageAdapter = new MessageAdapter(
            messageList, 
            currentUserId,
            // OnMessageClickListener
            message -> {
                // Handle message click if needed
            },
            // OnMessageReplyListener
            message -> {
                replyingToMessage = message;
                replyPreviewLayout.setVisibility(View.VISIBLE);
                replyPreviewText.setText("↩ " + message.getContent());
            },
            // OnReplyClickListener
            message -> {
                int position = messageAdapter.findMessagePosition(message.getId());
                if (position != -1) {
                    messagesRecyclerView.smoothScrollToPosition(position);
                    View messageView = messagesRecyclerView.getLayoutManager()
                        .findViewByPosition(position);
                    if (messageView != null) {
                        TextView messageText = messageView.findViewById(R.id.messageText);
                        if (messageText != null) {
                            boolean isSentMessage = String.valueOf(message.getUserId())
                                .equals(currentUserId);
                            
                            messageText.setTag(R.id.original_background, 
                                isSentMessage ? R.drawable.message_bubble_sent : R.drawable.message_bubble);
                            
                            messageText.setBackgroundResource(R.drawable.message_bubble_highlight);
                            
                            new Handler().postDelayed(() -> {
                                Integer originalBackground = (Integer) messageText.getTag(R.id.original_background);
                                if (originalBackground != null) {
                                    messageText.setBackgroundResource(originalBackground);
                                }
                            }, 1000);
                        }
                    }
                }
            },
            // OnMessageDeleteListener
            message -> {
                deleteMessage(message);
            },
            // Add reaction listener
            new OnReactionListener() {
                @Override
                public void onReaction(Message message, MessageReaction.ReactionType type) {
                    handleReaction(message, type);
                }

                @Override
                public void onRemoveReaction(Message message, MessageReaction.ReactionType type) {
                    handleRemoveReaction(message, type);
                }
            }
        );
        messagesRecyclerView.setAdapter(messageAdapter);
        
        EditText messageInput = findViewById(R.id.messageInput);
        Button sendButton = findViewById(R.id.sendButton);
        
        sendButton.setOnClickListener(v -> {
            String content = messageInput.getText().toString().trim();
            if (!content.isEmpty()) {
                sendMessage(content);
                messageInput.setText("");
            }
        });

        TextView chatHeader = findViewById(R.id.chatHeader);
        String otherUsername = getIntent().getStringExtra("otherUsername");
        if (otherUsername != null) {
            chatHeader.setText("Chat with " + otherUsername);
        }

        ImageButton deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());

        replyPreviewLayout = findViewById(R.id.replyPreviewLayout);
        replyPreviewText = findViewById(R.id.replyPreviewText);
        
        findViewById(R.id.cancelReplyButton).setOnClickListener(v -> {
            replyingToMessage = null;
            replyPreviewLayout.setVisibility(View.GONE);
        });
    }

    private void startConversation() {
        currentUserId = getIntent().getStringExtra("currentUserId");
        String otherUserId = getIntent().getStringExtra("otherUserId");
        
        Log.d("DirectMessageActivity", "currentUserId: " + currentUserId);
        Log.d("DirectMessageActivity", "otherUserId: " + otherUserId);
        
        if (currentUserId == null || otherUserId == null) {
            showToast("User information missing");
            finish();
            return;
        }

        ChatApiService chatApiService = RetrofitClient.getInstance().create(ChatApiService.class);
        ChatApiService.ConversationRequest request = new ChatApiService.ConversationRequest(
            currentUserId, otherUserId);

        chatApiService.startConversation(request).enqueue(new Callback<ChatApiService.Conversation>() {
            @Override
            public void onResponse(Call<ChatApiService.Conversation> call, Response<ChatApiService.Conversation> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversationId = response.body().id;
                    Log.d("DirectMessageActivity", "Conversation started with ID: " + conversationId);
                    
                    // Fetch existing messages
                    loadExistingMessages();
                    
                    // Connect websocket after getting conversation ID
                    connectWebSocket();
                } else {
                    showToast("Failed to start conversation");
                }
            }

            @Override
            public void onFailure(Call<ChatApiService.Conversation> call, Throwable t) {
                Log.e("DirectMessageActivity", "Error starting conversation", t);
                showToast("Error starting conversation: " + t.getMessage());
            }
        });
    }

    private void loadExistingMessages() {
        if (conversationId == null) {
            Log.e("DirectMessageActivity", "Cannot load messages: conversationId is null");
            return;
        }

        ChatApiService chatApiService = RetrofitClient.getInstance().create(ChatApiService.class);
        chatApiService.getMessages(conversationId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Message> messages = response.body();
                    Log.d("DirectMessageActivity", "Loaded " + messages.size() + " messages");
                    messageList.clear();
                    messageList.addAll(messages);
                    messageAdapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                } else {
                    Log.e("DirectMessageActivity", "Failed to load messages: " + response.code());
                    showToast("Failed to load messages");
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("DirectMessageActivity", "Error loading messages", t);
                showToast("Error loading messages: " + t.getMessage());
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Conversation")
               .setMessage("Are you sure you want to delete this conversation?")
               .setPositiveButton("Delete", (dialog, which) -> {
                    deleteConversation();
               })
               .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
               .show();
    }

    private void deleteConversation() {
        ChatApiService chatApiService = RetrofitClient.getInstance().create(ChatApiService.class);
        Long convId = Long.parseLong(conversationId);
        Long userId = Long.parseLong(currentUserId);
        
        // First clean up STOMP subscriptions
        if (compositeDisposable != null) {
            compositeDisposable.clear(); // Clear all subscriptions
        }
        
        chatApiService.deleteConversation(convId, userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        // Disconnect WebSocket after successful deletion
                        if (stompClient != null) {
                            stompClient.disconnect();
                            stompClient = null;
                        }
                        Toast.makeText(DirectMessageActivity.this, 
                            "Conversation deleted", Toast.LENGTH_SHORT).show();
                        // Ensure activity finishes immediately
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(DirectMessageActivity.this, 
                            "Failed to delete conversation", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("DirectMessageActivity", "Delete failed", t);
                runOnUiThread(() -> {
                    Toast.makeText(DirectMessageActivity.this, 
                        "Error deleting conversation", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void cancelReply() {
        replyingToMessage = null;
        replyPreviewLayout.setVisibility(View.GONE);
        replyPreviewText.setText("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_message);
        
        initializeViews();
        startConversation();
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

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void handleReceivedMessage(Message receivedMessage) {
        if (receivedMessage == null || receivedMessage.getContent() == null) {
            Log.e("STOMP", "Received null or invalid message");
            return;
        }

        Log.d("STOMP", "Processing received message: " + new Gson().toJson(receivedMessage));
        
        runOnUiThread(() -> {
            // Check if this is an update to an existing message
            int existingPosition = -1;
            for (int i = 0; i < messageList.size(); i++) {
                // Check both ID and content to handle temporary messages
                if ((messageList.get(i).getId() != null && 
                     messageList.get(i).getId().equals(receivedMessage.getId())) ||
                    (messageList.get(i).getId() == null && 
                     messageList.get(i).getContent().equals(receivedMessage.getContent()) &&
                     String.valueOf(messageList.get(i).getUserId()).equals(String.valueOf(receivedMessage.getUserId())))) {
                    existingPosition = i;
                    break;
                }
            }

            if (existingPosition != -1) {
                // Update existing message with server data
                messageList.set(existingPosition, receivedMessage);
                messageAdapter.notifyItemChanged(existingPosition);
                Log.d("STOMP", "Updated existing message at position: " + existingPosition);
            } else if (!String.valueOf(receivedMessage.getUserId()).equals(currentUserId)) {
                // Only add new messages from other users
                messageList.add(receivedMessage);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                Log.d("STOMP", "Added new message from other user");
            }
        });
    }

    private boolean hasActiveSubscription() {
        return stompClient != null 
            && stompClient.isConnected() 
            && compositeDisposable != null 
            && !compositeDisposable.isDisposed();
    }

    private <T> io.reactivex.FlowableTransformer<T, T> applySchedulers() {
        return flowable -> flowable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private io.reactivex.CompletableTransformer applyCompletableSchedulers() {
        return completable -> completable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private void deleteMessage(Message message) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete", (dialog, which) -> {
                ChatApiService chatApiService = RetrofitClient.getInstance().create(ChatApiService.class);
                chatApiService.deleteMessage(message.getId(), Long.parseLong(currentUserId))
                    .enqueue(new Callback<Message>() {
                        @Override
                        public void onResponse(Call<Message> call, Response<Message> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                // Update will come through WebSocket
                            } else {
                                showToast("Failed to delete message");
                            }
                        }

                        @Override
                        public void onFailure(Call<Message> call, Throwable t) {
                            showToast("Error deleting message: " + t.getMessage());
                        }
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void handleReaction(Message message, MessageReaction.ReactionType newReactionType) {
        // Create a new reaction
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(message.getId());
        reaction.setUserId(Long.parseLong(currentUserId));
        reaction.setReactionType(newReactionType);
        
        // Initialize reactions map if null
        if (message.getReactions() == null) {
            message.setReactions(new HashMap<>());
        }
        
        // Get or create the list for this reaction type
        String reactionKey = newReactionType.toString();
        List<MessageReaction> reactionList = message.getReactions().computeIfAbsent(
            reactionKey, k -> new ArrayList<>());
        
        // Remove existing reaction from this user
        reactionList.removeIf(existingReaction -> 
            existingReaction.getUserId() == Long.parseLong(currentUserId));
        
        // Add new reaction
        reactionList.add(reaction);
        
        // Send to server
        sendReactionToServer(message.getId(), newReactionType);
    }

    private void sendReactionToServer(Long messageId, MessageReaction.ReactionType reactionType) {
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(messageId);
        reaction.setUserId(Long.parseLong(currentUserId));
        reaction.setReactionType(reactionType);
        
        // Update UI immediately
        handleReceivedReaction(reaction);
        
        // Convert reaction to JSON and send through WebSocket
        String jsonReaction = new Gson().toJson(reaction);
        
        if (stompClient != null && stompClient.isConnected()) {
            stompClient.send("/app/chat/react", jsonReaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> Log.d("Reaction", "Reaction sent successfully"),
                    throwable -> {
                        showToast("Failed to send reaction: " + throwable.getMessage());
                        // Revert the UI change on failure
                        handleReceivedReaction(reaction);
                    }
                );
        } else {
            showToast("WebSocket connection not available");
        }
    }

    private void handleReceivedReaction(MessageReaction reaction) {
        runOnUiThread(() -> {
            for (Message message : messageList) {
                if (message.getId() != null && message.getId().equals(reaction.getMessageId())) {
                    // Clear existing reactions from this user
                    if (message.getReactions() != null) {
                        for (List<MessageReaction> reactionList : message.getReactions().values()) {
                            reactionList.removeIf(r -> r.getUserId().equals(reaction.getUserId()));
                        }
                    }
                    // Add new reaction
                    message.toggleReaction(reaction);
                    messageAdapter.notifyDataSetChanged();
                    break;
                }
            }
        });
    }

    private void handleRemoveReaction(Message message, MessageReaction.ReactionType reactionType) {
        if (stompClient != null && stompClient.isConnected()) {
            Map<String, Object> request = new HashMap<>();
            request.put("messageId", message.getId());
            request.put("userId", Long.parseLong(currentUserId));
            request.put("reactionType", reactionType);
            
            String jsonRequest = new Gson().toJson(request);
            stompClient.send("/app/chat/remove-reaction", jsonRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> Log.d("Reaction", "Reaction removal sent successfully"),
                    throwable -> showToast("Failed to remove reaction: " + throwable.getMessage())
                );
        }
    }
}