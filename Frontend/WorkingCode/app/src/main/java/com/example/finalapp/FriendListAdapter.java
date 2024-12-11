package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {
    private List<UserModel> friends;
    private OnFriendClickListener clickListener;
    private OnRemoveFriendListener removeListener;

    public interface OnFriendClickListener {
        void onFriendClick(UserModel friend);
    }

    public interface OnRemoveFriendListener {
        void onRemoveFriend(UserModel friend);
    }

    public FriendListAdapter(List<UserModel> friends, OnFriendClickListener clickListener, OnRemoveFriendListener removeListener) {
        this.friends = friends;
        this.clickListener = clickListener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel friend = friends.get(position);
        holder.tvUsername.setText(friend.getUsername());
        holder.itemView.setOnClickListener(v -> clickListener.onFriendClick(friend));
        holder.btnRemove.setOnClickListener(v -> removeListener.onRemoveFriend(friend));

        if (friend.isRequestPending()) {
            holder.tvStatus.setText("Request Sent");
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        TextView tvStatus;
        Button btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
} 