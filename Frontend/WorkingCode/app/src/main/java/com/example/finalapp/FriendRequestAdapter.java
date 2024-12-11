package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    private List<FriendRequest> requests;
    private OnAcceptListener acceptListener;
    private OnRejectListener rejectListener;
    private OnItemClickListener clickListener;

    public interface OnAcceptListener {
        void onAccept(FriendRequest request);
    }

    public interface OnRejectListener {
        void onReject(FriendRequest request);
    }

    public interface OnItemClickListener {
        void onItemClick(FriendRequest request);
    }

    public FriendRequestAdapter(List<FriendRequest> requests, 
                              OnAcceptListener acceptListener,
                              OnRejectListener rejectListener,
                              OnItemClickListener clickListener) {
        this.requests = requests;
        this.acceptListener = acceptListener;
        this.rejectListener = rejectListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_request_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest request = requests.get(position);
        holder.usernameText.setText(request.getSenderUsername());
        holder.acceptButton.setOnClickListener(v -> acceptListener.onAccept(request));
        holder.rejectButton.setOnClickListener(v -> rejectListener.onReject(request));
        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        Button acceptButton;
        Button rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.tvUsername);
            acceptButton = itemView.findViewById(R.id.btnAccept);
            rejectButton = itemView.findViewById(R.id.btnReject);
        }
    }
} 