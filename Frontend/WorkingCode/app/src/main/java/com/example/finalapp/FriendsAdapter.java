package com.example.finalapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
    private List<UserModel> friends;
    private OnFriendClickListener clickListener;
    private OnRemoveFriendListener removeListener;
    private boolean showRemoveButton;

    public interface OnFriendClickListener {
        void onFriendClick(UserModel friend);
    }

    public interface OnRemoveFriendListener {
        void onRemoveFriend(UserModel friend);
    }

    public FriendsAdapter(List<UserModel> friends, OnFriendClickListener clickListener, 
                         OnRemoveFriendListener removeListener, boolean showRemoveButton) {
        this.friends = friends;
        this.clickListener = clickListener;
        this.removeListener = removeListener;
        this.showRemoveButton = showRemoveButton;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UserModel friend = friends.get(position);
        holder.nameText.setText(friend.getUsername());
        
        // Show/hide remove button based on adapter type
        if (showRemoveButton && removeListener != null) {
            holder.removeButton.setVisibility(View.VISIBLE);
        } else {
            holder.removeButton.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onFriendClick(friend));
        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveFriend(friend);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        ImageButton removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tvFriendName);
            removeButton = itemView.findViewById(R.id.btnRemoveFriend);
        }
    }
} 