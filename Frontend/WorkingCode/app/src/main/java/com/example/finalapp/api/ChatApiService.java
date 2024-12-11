package com.example.finalapp.api;

import com.example.finalapp.models.Message;
import com.example.finalapp.models.ReactionRequest;
import com.example.finalapp.models.User;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

public interface ChatApiService {
    @GET("/api/conversations/{conversationId}/messages")
    Call<List<Message>> getMessages(@Path("conversationId") String conversationId);

    @GET("/api/messages/single/{messageId}")
    Call<Message> getMessage(@Path("messageId") Long messageId);

    @POST("/api/conversations")
    Call<Conversation> startConversation(@Body ConversationRequest request);

    @GET("/api/admin/users/all")
    Call<List<User>> getAllUsers();

    @GET("/api/conversations/{userId}")
    Call<List<Conversation>> getConversations(@Path("userId") String userId);

    @DELETE("/api/conversations/{conversationId}")
    Call<Void> deleteConversation(@Path("conversationId") Long conversationId, @Query("userId") Long userId);

    @DELETE("/api/messages/{messageId}")
    Call<Message> deleteMessage(@Path("messageId") Long messageId, @Query("userId") Long userId);

    class ConversationRequest {
        Long userId;
        Long otherUserId;

        public ConversationRequest(String userId, String otherUserId) {
            this.userId = Long.parseLong(userId);
            this.otherUserId = Long.parseLong(otherUserId);
        }
    }

    public static class Conversation {
        public String id;
        public String otherUserId;
        public String otherUsername;

        public Conversation(String id, String otherUserId, String otherUsername) {
            this.id = id;
            this.otherUserId = otherUserId;
            this.otherUsername = otherUsername;
        }
    }
} 