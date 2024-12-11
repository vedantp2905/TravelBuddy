package com.example.finalapp.models;

public class MessageReaction {
    private Long id;
    private Long messageId;
    private Long userId;
    private ReactionType reactionType;

    public enum ReactionType {
        THUMBS_UP,
        THUMBS_DOWN,
        HEART,
        EXCLAMATION,
        LAUGH
    }

    // Default constructor
    public MessageReaction() {}

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ReactionType getReactionType() {
        return reactionType;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }
} 