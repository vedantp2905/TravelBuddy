package com.example.finalapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private List<Message> messages;
    private OnItemClickListener listener;

    public MessagesAdapter(List<Message> messages, OnItemClickListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for message items
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message, listener);

        // Display the parent message if available
        String parentMessage = message.getParentMessage();
        if (parentMessage != null && !parentMessage.equals("null")) {
            holder.repliesTextView.setText("Replying to: " + parentMessage);
            holder.repliesTextView.setVisibility(View.VISIBLE);  // Make it visible if there's a parent message
        } else {
            holder.repliesTextView.setVisibility(View.GONE);  // Hide if no parent message
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView commentTextView;
        TextView senderTextView;
        TextView timestampTextView;
        TextView repliesTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            commentTextView = itemView.findViewById(R.id.commentTextView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            repliesTextView = itemView.findViewById(R.id.repliesTextView);
        }

        public void bind(Message message, OnItemClickListener listener) {
            commentTextView.setText(message.getMessage());
            senderTextView.setText(message.getSender());
            timestampTextView.setText(message.getTimestamp());

            // Set up the item click listener for each message
            itemView.setOnClickListener(v -> {
                if ("HTML".equals(message.getMessageType())) {
                    // If the message type is HTML, navigate to ItineraryDetailActivity
                    Intent intent = new Intent(itemView.getContext(), TravelSpaceReplyActivity.class);
                    intent.putExtra("htmlMessage", message.getMessage().split(" ")[0]);  // Pass HTML message
                    intent.putExtra("messageType", message.getMessageType());  // Pass HTML message
                    itemView.getContext().startActivity(intent);
                } else {
                    // Otherwise, use the normal listener for non-HTML messages
                    listener.onItemClick(message);
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Message message);  // Handle item click
    }
}
