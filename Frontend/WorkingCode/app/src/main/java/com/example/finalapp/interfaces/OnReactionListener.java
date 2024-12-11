package com.example.finalapp.interfaces;

import com.example.finalapp.models.Message;
import com.example.finalapp.models.MessageReaction;

public interface OnReactionListener {
    void onReaction(Message message, MessageReaction.ReactionType reactionType);
    void onRemoveReaction(Message message, MessageReaction.ReactionType reactionType);
} 