package com.example.finalapp.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalapp.R;
import com.example.finalapp.models.Message;
import java.util.List;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.content.Context;
import com.example.finalapp.dialogs.MessageActionDialog;
import com.example.finalapp.interfaces.MessageActionListener;
import com.example.finalapp.models.MessageReaction;
import java.util.Map;
import java.util.List;
import android.util.Log;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }
    
    private OnMessageClickListener clickListener;
    private List<Message> messages;
    private String currentUserId;
    private Message replyingTo;
    private final OnMessageReplyListener replyListener;
    private final OnReplyClickListener replyClickListener;
    private OnMessageDeleteListener deleteListener;
    private OnReactionListener reactionListener;

    public interface OnMessageReplyListener {
        void onReply(Message message);
    }

    public interface OnReplyClickListener {
        void onReplyClick(Message message);
    }

    public interface OnMessageDeleteListener {
        void onDelete(Message message);
    }

    public interface OnReactionListener {
        void onReaction(Message message, MessageReaction.ReactionType reactionType);
        void onRemoveReaction(Message message, MessageReaction.ReactionType reactionType);
    }

    public void setReplyingTo(Message message) {
        this.replyingTo = message;
    }

    public MessageAdapter(List<Message> messages, String currentUserId, 
                         OnMessageClickListener clickListener, 
                         OnMessageReplyListener replyListener,
                         OnReplyClickListener replyClickListener,
                         OnMessageDeleteListener deleteListener,
                         OnReactionListener reactionListener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.clickListener = clickListener;
        this.replyListener = replyListener;
        this.replyClickListener = replyClickListener;
        this.deleteListener = deleteListener;
        this.reactionListener = reactionListener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        
        // Show deleted message differently
        if (Boolean.TRUE.equals(message.getDeleted())) {
            holder.messageText.setText("This message was deleted");
            holder.messageText.setTextColor(Color.GRAY);
            holder.messageText.setTypeface(null, Typeface.ITALIC);
        } else {
            holder.messageText.setText(message.getContent());
            holder.messageText.setTextColor(Color.BLACK);
            holder.messageText.setTypeface(null, Typeface.NORMAL);
        }
        
        // Handle reply preview
        if (message.getReplyToContent() != null) {
            holder.replyContainer.setVisibility(View.VISIBLE);
            holder.replyPreview.setText("↩ " + message.getReplyToContent());
            
            // Set reply container gravity based on sender
            LinearLayout.LayoutParams replyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            replyParams.gravity = String.valueOf(message.getUserId()).equals(currentUserId) 
                ? Gravity.END 
                : Gravity.START;
            holder.replyContainer.setLayoutParams(replyParams);
            
            holder.replyContainer.setOnClickListener(v -> {
                int originalPosition = findMessagePosition(message.getReplyToId());
                if (originalPosition != -1) {
                    Message originalMessage = messages.get(originalPosition);
                    replyClickListener.onReplyClick(originalMessage);
                }
            });
        } else {
            holder.replyContainer.setVisibility(View.GONE);
        }
        
        // Set message alignment based on sender
        LinearLayout.LayoutParams messageContainerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        LinearLayout.LayoutParams contentLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        if (String.valueOf(message.getUserId()).equals(currentUserId)) {
            messageContainerParams.gravity = Gravity.END;
            contentLayoutParams.gravity = Gravity.END;
            holder.messageContainer.setLayoutParams(messageContainerParams);
            holder.messageText.setBackgroundResource(R.drawable.message_bubble_sent);
            
            // Find and set layout for messageContentLayout
            LinearLayout messageContentLayout = holder.itemView.findViewById(R.id.messageContentLayout);
            messageContentLayout.setLayoutParams(contentLayoutParams);
            messageContentLayout.setGravity(Gravity.END);
        } else {
            messageContainerParams.gravity = Gravity.START;
            contentLayoutParams.gravity = Gravity.START;
            holder.messageContainer.setLayoutParams(messageContainerParams);
            holder.messageText.setBackgroundResource(R.drawable.message_bubble);
            
            // Find and set layout for messageContentLayout
            LinearLayout messageContentLayout = holder.itemView.findViewById(R.id.messageContentLayout);
            messageContentLayout.setLayoutParams(contentLayoutParams);
            messageContentLayout.setGravity(Gravity.START);
        }

        // Disable reactions for own messages
        boolean isOwnMessage = String.valueOf(message.getUserId()).equals(currentUserId);
        holder.messageContainer.setOnLongClickListener(v -> {
            if (message != null) {
                showMessageActionDialog(v.getContext(), message);
                return true;
            }
            return false;
        });

        // Update reactions
        updateReactions(holder, message);

        // Format and set timestamp
        try {
            LocalDateTime timestamp = LocalDateTime.parse(message.getTimestamp());
            String formattedTime = timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
            holder.timestampText.setText(formattedTime);
        } catch (Exception e) {
            Log.e("MessageAdapter", "Error formatting timestamp", e);
            holder.timestampText.setText("");
        }
    }

    private void showMessageActionDialog(Context context, Message message) {
        if (message == null || message.getId() == null) {
            Log.d("MessageAdapter", "Message or ID is null");
            return;
        }

        boolean isOwnMessage = String.valueOf(message.getUserId()).equals(currentUserId);
        Log.d("MessageAdapter", "Message Details:");
        Log.d("MessageAdapter", "User ID: " + message.getUserId());
        Log.d("MessageAdapter", "Current User ID: " + currentUserId);
        Log.d("MessageAdapter", "Is Own Message: " + isOwnMessage);
        
        MessageActionDialog dialog = new MessageActionDialog(
            context,
            message,
            isOwnMessage,
            new MessageActionListener() {
                @Override
                public void onReply(Message message) {
                    if (replyListener != null) {
                        replyListener.onReply(message);
                    }
                }

                @Override
                public void onDelete(Message message) {
                    if (deleteListener != null) {
                        deleteListener.onDelete(message);
                    }
                }

                @Override
                public void onReaction(Message message, MessageReaction.ReactionType type) {
                    if (reactionListener != null) {
                        reactionListener.onReaction(message, type);
                    }
                }

                @Override
                public void onRemoveReaction(Message message, MessageReaction.ReactionType type) {
                    if (reactionListener != null) {
                        reactionListener.onRemoveReaction(message, type);
                    }
                }
            }
        );
        dialog.show();
    }

    private void updateReactions(MessageViewHolder holder, Message message) {
        LinearLayout reactionsContainer = holder.itemView.findViewById(R.id.reactionsContainer);
        reactionsContainer.removeAllViews();

        // Don't show reactions for deleted messages
        if (Boolean.TRUE.equals(message.getDeleted())) {
            reactionsContainer.setVisibility(View.GONE);
            return;
        }
        
        reactionsContainer.setVisibility(View.VISIBLE);
        boolean isOwnMessage = String.valueOf(message.getUserId()).equals(currentUserId);

        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            for (Map.Entry<String, List<MessageReaction>> entry : message.getReactions().entrySet()) {
                TextView reactionView = new TextView(holder.itemView.getContext());
                reactionView.setText(getReactionEmoji(entry.getKey()) + " " + entry.getValue().size());
                reactionView.setTextSize(16);
                reactionView.setPadding(8, 4, 8, 4);
                
                boolean userHasReacted = entry.getValue().stream()
                    .anyMatch(r -> String.valueOf(r.getUserId()).equals(currentUserId));
                
                if (userHasReacted) {
                    reactionView.setBackgroundResource(R.drawable.reaction_selected_background);
                }
                
                // Only allow reaction clicks for other users' messages
                if (!isOwnMessage) {
                    reactionView.setOnClickListener(v -> {
                        if (reactionListener != null && userHasReacted) {
                            MessageReaction.ReactionType currentType = MessageReaction.ReactionType.valueOf(entry.getKey());
                            reactionListener.onRemoveReaction(message, currentType);
                        }
                    });
                }
                
                reactionsContainer.addView(reactionView);
            }
        }
    }

    private String getReactionEmoji(String reactionType) {
        switch (reactionType) {
            case "THUMBS_UP": return "👍";
            case "THUMBS_DOWN": return "👎";
            case "HEART": return "❤️";
            case "EXCLAMATION": return "❗";
            case "LAUGH": return "😄";
            default: return "";
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        LinearLayout messageContainer;
        TextView replyPreview;
        View replyContainer;
        ImageButton replyButton;
        ImageButton deleteButton;
        LinearLayout reactionsContainer;
        TextView timestampText;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            replyPreview = itemView.findViewById(R.id.replyPreview);
            replyContainer = itemView.findViewById(R.id.replyContainer);
            replyButton = itemView.findViewById(R.id.replyButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            reactionsContainer = itemView.findViewById(R.id.reactionsContainer);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
    }

    public int findMessagePosition(Long messageId) {
        if (messageId == null) return -1;
        
        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
} 