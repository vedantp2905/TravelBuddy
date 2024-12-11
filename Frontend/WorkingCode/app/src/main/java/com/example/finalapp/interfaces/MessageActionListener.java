package com.example.finalapp.interfaces;

import com.example.finalapp.models.Message;
import com.example.finalapp.models.MessageReaction;

public interface MessageActionListener {
    void onReaction(Message message, MessageReaction.ReactionType reactionType);
    void onRemoveReaction(Message message, MessageReaction.ReactionType reactionType);
    void onReply(Message message);
    void onDelete(Message message);
} 