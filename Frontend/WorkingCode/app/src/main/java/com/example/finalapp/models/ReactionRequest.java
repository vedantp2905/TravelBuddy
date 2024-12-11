package com.example.finalapp.models;

public class ReactionRequest {
    private Long messageId;
    private Long userId;
    private MessageReaction.ReactionType reactionType;

    public ReactionRequest(Long messageId, Long userId, MessageReaction.ReactionType reactionType) {
        this.messageId = messageId;
        this.userId = userId;
        this.reactionType = reactionType;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getUserId() {
        return userId;
    }

    public MessageReaction.ReactionType getReactionType() {
        return reactionType;
    }
} 