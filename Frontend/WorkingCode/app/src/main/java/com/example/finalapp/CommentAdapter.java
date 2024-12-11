package com.example.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class CommentAdapter extends ArrayAdapter<Comment> {
    private static final String TAG = "CommentAdapter";
    private final List<Comment> comments;
    private final TravelApiService apiService;
    private final Long currentUserId;

    public CommentAdapter(Context context, List<Comment> comments) {
        super(context, 0, comments);
        this.comments = comments;
        this.apiService = new TravelApiService(context);
        this.currentUserId = getCurrentUserId();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_comment_feed, parent, false);
            holder = new ViewHolder();
            holder.descriptionTextView = convertView.findViewById(R.id.comment_description);
            holder.createdAtTextView = convertView.findViewById(R.id.comment_created_at);
            holder.deleteButton = convertView.findViewById(R.id.delete_comment_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Comment comment = getItem(position);
        if (comment != null) {
            holder.descriptionTextView.setText(comment.getDescription());
            holder.createdAtTextView.setText(comment.getCreatedAt());

            if (currentUserId != null && comment.getUserId() != null 
                && currentUserId.equals(comment.getUserId())) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                setupDeleteButton(holder.deleteButton, comment);
            } else {
                holder.deleteButton.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private void setupDeleteButton(ImageButton deleteButton, Comment comment) {
        deleteButton.setOnClickListener(v -> {
            if (comment.getId() != null) {
                apiService.deleteComment(comment.getId(), new TravelApiService.ApiCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        comments.remove(comment);
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), 
                            "Error deleting comment: " + error, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private Long getCurrentUserId() {
        long userId = getContext()
            .getSharedPreferences("MyApp", Context.MODE_PRIVATE)
            .getLong("userId", -1);
        return userId != -1 ? userId : null;
    }

    private static class ViewHolder {
        TextView descriptionTextView;
        TextView createdAtTextView;
        ImageButton deleteButton;
    }
}
