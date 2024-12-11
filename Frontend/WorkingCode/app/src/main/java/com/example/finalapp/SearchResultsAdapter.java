package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private List<UserModel> searchResults;
    private OnAddFriendListener addFriendListener;

    public interface OnAddFriendListener {
        void onAddFriend(UserModel user);
    }

    public SearchResultsAdapter(List<UserModel> searchResults, OnAddFriendListener addFriendListener) {
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
        holder.username.setText(user.getUsername());
        holder.btnAdd.setOnClickListener(v -> addFriendListener.onAddFriend(user));
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        Button btnAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.tvUsername);
            btnAdd = itemView.findViewById(R.id.btnAddFriend);
        }
    }
} 