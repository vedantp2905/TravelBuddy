package com.example.finalapp.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Message {
    private Long id;
    private String content;
    private String sender;
    private String senderUsername;
    private Long conversationId;
    private String timestamp;
    private Long userId;
    private String replyToMessageId;
    private String replyToContent;
    private Long replyToId;
    private Boolean deleted = false;
    private Map<String, List<MessageReaction>> reactions = new HashMap<>();
    private Long senderId;

    public Message() {
        // Default constructor
    }

    public Message(String content, String sender, String conversationId, long timestamp) {
        this.content = content;
        this.sender = sender;
        this.conversationId = Long.parseLong(conversationId);
        this.userId = Long.parseLong(sender);
        this.timestamp = String.valueOf(timestamp);
    }

    // Add getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    
    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplyToContent() {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }

    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Map<String, List<MessageReaction>> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, List<MessageReaction>> reactions) {
        this.reactions = reactions != null ? reactions : new HashMap<>();
    }

    public void addReaction(MessageReaction reaction) {
        String type = reaction.getReactionType().toString();
        if (!reactions.containsKey(type)) {
            reactions.put(type, new ArrayList<>());
        }
        reactions.get(type).add(reaction);
    }

    public void removeReaction(MessageReaction reaction) {
        String reactionType = reaction.getReactionType().toString();
        if (reactions.containsKey(reactionType)) {
            reactions.get(reactionType).removeIf(r -> 
                r.getUserId().equals(reaction.getUserId()) && 
                r.getReactionType().equals(reaction.getReactionType())
            );
        }
    }

    public void toggleReaction(MessageReaction reaction) {
        if (reactions == null) {
            reactions = new HashMap<>();
        }
        
        // Get current reaction type for this user
        MessageReaction.ReactionType currentType = getUserReactionType(String.valueOf(reaction.getUserId()));
        
        // Remove any existing reaction from this user from all reaction types
        for (Iterator<Map.Entry<String, List<MessageReaction>>> it = reactions.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<MessageReaction>> entry = it.next();
            List<MessageReaction> reactionList = entry.getValue();
            reactionList.removeIf(r -> r.getUserId().equals(reaction.getUserId()));
            if (reactionList.isEmpty()) {
                it.remove();
            }
        }
        
        // Only add the new reaction if it's different from the current one
        if (currentType != reaction.getReactionType()) {
            String type = reaction.getReactionType().toString();
            if (!reactions.containsKey(type)) {
                reactions.put(type, new ArrayList<>());
            }
            reactions.get(type).add(reaction);
        }
    }

    public boolean hasUserReacted(String userId) {
        if (reactions == null) return false;
        return reactions.values().stream()
            .anyMatch(list -> list.stream()
                .anyMatch(r -> String.valueOf(r.getUserId()).equals(userId)));
    }

    public MessageReaction.ReactionType getUserReactionType(String userId) {
        if (reactions == null) return null;
        for (List<MessageReaction> reactionList : reactions.values()) {
            for (MessageReaction reaction : reactionList) {
                if (String.valueOf(reaction.getUserId()).equals(userId)) {
                    return reaction.getReactionType();
                }
            }
        }
        return null;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
} 