package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private List<UserModel> searchResults;
    private OnAddFriendListener addFriendListener;

    public interface OnAddFriendListener {
        void onAddFriend(UserModel user);
    }

    public SearchResultAdapter(List<UserModel> searchResults, OnAddFriendListener addFriendListener) {
        this.searchResults = searchResults;
        this.addFriendListener = addFriendListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = searchResults.get(position);
        holder.usernameText.setText(user.getUsername());
        holder.addButton.setOnClickListener(v -> addFriendListener.onAddFriend(user));
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        Button addButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.tvUsername);
            addButton = itemView.findViewById(R.id.btnAddFriend);
        }
    }
} 