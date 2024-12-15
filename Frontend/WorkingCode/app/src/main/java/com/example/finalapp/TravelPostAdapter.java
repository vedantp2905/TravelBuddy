package com.example.finalapp;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TravelPostAdapter extends RecyclerView.Adapter<TravelPostAdapter.ViewHolder> {
    private static final String TAG = "TravelPostAdapter";
    private List<TravelPost> posts;
    private Context context;
    private String userId;
    private TravelApiService apiService;
    private Set<Long> likedPosts;
    private static final String BASE_URL = ApiConstants.BASE_URL; // Use your actual backend URL


    public TravelPostAdapter(Context context, List<TravelPost> posts, String userId) {
        this.context = context;
        this.posts = posts;
        this.userId = userId;
        this.apiService = new TravelApiService(context);
        this.likedPosts = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_travel_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TravelPost post = posts.get(position);
        holder.bind(post);

        boolean isLiked = likedPosts.contains(post.getId());
        holder.likeButton.setText(isLiked ? "Unlike" : "Like");
        holder.likeButton.setOnClickListener(v -> {
            boolean currentlyLiked = likedPosts.contains(post.getId());
            if (currentlyLiked) {
                removeLike(post, holder);
            } else {
                addLike(post, holder);
            }
        });

        holder.commentButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", (int) post.getId());
            intent.putExtra("userId", Integer.parseInt(userId));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void addPosts(List<TravelPost> newPosts) {
        int startPosition = posts.size();
        posts.addAll(newPosts);
        notifyItemRangeInserted(startPosition, newPosts.size());
    }

    public void updatePosts(List<TravelPost> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView descriptionText;
        private TextView destinationText;
        private TextView categoryText;
        private TextView ratingText;
        private TextView likesCount;
        private Button likeButton;
        private Button commentButton;
        private RecyclerView imageRecyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            descriptionText = itemView.findViewById(R.id.description_text);
            destinationText = itemView.findViewById(R.id.destination_text);
            categoryText = itemView.findViewById(R.id.category_text);
            ratingText = itemView.findViewById(R.id.rating_text);
            likesCount = itemView.findViewById(R.id.likes_count);
            likeButton = itemView.findViewById(R.id.like_button);
            commentButton = itemView.findViewById(R.id.comment_button);
            imageRecyclerView = itemView.findViewById(R.id.post_images_recycler);
            imageRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        }

        public void bind(TravelPost post) {
            descriptionText.setText(post.getDescription());
            destinationText.setText(post.getDestination());
            categoryText.setText("Category: " + post.getCategory());
            ratingText.setText("Rating: " + post.getRating() + "★");
            likesCount.setText(String.valueOf(post.getLikeCount()));
            updateLikeButtonState(post.isLikedByUser());
            
            likeButton.setOnClickListener(v -> {
                if (post.isLikedByUser()) {
                    handleUnlike(post, this);
                } else {
                    handleLike(post, this);
                }
            });

            // Set up images recycler view first
            PostImagesAdapter imagesAdapter = new PostImagesAdapter(context, post.getId());
            imageRecyclerView.setAdapter(imagesAdapter);
            
            // Center the images in the RecyclerView
            imageRecyclerView.setPadding(0, 0, 0, 0);
            
            imageRecyclerView.setVisibility(View.VISIBLE);
        }

        public void updateLikeButtonState(boolean isLiked) {
            if (isLiked) {
                likeButton.setText("Unlike");
                likeButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light));
            } else {
                likeButton.setText("Like");
                likeButton.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
            }
        }
    }

    private void handleLikeClick(TravelPost post, ViewHolder holder) {
        if (post.isLikedByUser()) {
            handleUnlike(post, holder);
        } else {
            handleLike(post, holder);
        }
    }

    private void handleLike(TravelPost post, ViewHolder holder) {
        Log.d(TAG, "Attempting to like post: " + post.getId() + " by user: " + userId);
        holder.likeButton.setEnabled(false);

        // If already liked, handle unlike
        if (post.isLikedByUser()) {
            handleUnlike(post, holder);
            return;
        }

        try {
            Long userIdLong = Long.parseLong(userId);
            
            // Update UI immediately
            post.setLikedByUser(true);
            post.setLikeCount(post.getLikeCount() + 1);
            holder.updateLikeButtonState(true);
            holder.likesCount.setText(String.valueOf(post.getLikeCount()));

            apiService.createLike(userIdLong, post.getId(), new TravelApiService.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        holder.likeButton.setEnabled(true);
                        Log.d(TAG, "Like successful, count: " + post.getLikeCount());
                    });
                }

                @Override
                public void onError(String error) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!error.contains("already exists")) {
                            // Revert the UI changes if there was an actual error
                            post.setLikedByUser(false);
                            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                            holder.updateLikeButtonState(false);
                            holder.likesCount.setText(String.valueOf(post.getLikeCount()));
                            Toast.makeText(context, "Error liking post: " + error, Toast.LENGTH_SHORT).show();
                        }
                        holder.likeButton.setEnabled(true);
                        Log.e(TAG, "Error liking post: " + error);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing userId: " + userId, e);
            Toast.makeText(context, "Invalid user ID", Toast.LENGTH_SHORT).show();
            holder.likeButton.setEnabled(true);
        }
    }

    private void handleUnlike(TravelPost post, ViewHolder holder) {
        Log.d(TAG, "Attempting to unlike post: " + post.getId() + " by user: " + userId);
        holder.likeButton.setEnabled(false);

        try {
            Long userIdLong = Long.parseLong(userId);
            
            // Only decrement if current count is greater than 0
            int newCount = Math.max(0, post.getLikeCount() - 1);
            
            // Update UI immediately
            post.setLikedByUser(false);
            post.setLikeCount(newCount);
            holder.updateLikeButtonState(false);
            holder.likesCount.setText(String.valueOf(newCount));

            apiService.deleteLike(userIdLong, post.getId(), new TravelApiService.ApiCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        holder.likeButton.setEnabled(true);
                        Log.d(TAG, "Unlike successful, count: " + post.getLikeCount());
                    });
                }

                @Override
                public void onError(String error) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!error.contains("Like not found")) {
                            // Revert the UI changes if there was an actual error
                            post.setLikedByUser(true);
                            post.setLikeCount(post.getLikeCount() + 1);
                            holder.updateLikeButtonState(true);
                            holder.likesCount.setText(String.valueOf(post.getLikeCount()));
                            Toast.makeText(context, "Error unliking post: " + error, Toast.LENGTH_SHORT).show();
                        }
                        holder.likeButton.setEnabled(true);
                        Log.e(TAG, "Error unliking post: " + error);
                    });
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing userId: " + userId, e);
            Toast.makeText(context, "Invalid user ID", Toast.LENGTH_SHORT).show();
            holder.likeButton.setEnabled(true);
        }
    }

    private void addLike(TravelPost post, ViewHolder holder) {
        Long postId = post.getId();
        // Add logging to debug
        Log.d("TravelPostAdapter", "Attempting to like post: " + postId + " by user: " + userId);

        // First check if the post is already liked
        if (likedPosts.contains(postId)) {
            Log.d("TravelPostAdapter", "Post already liked locally");
            Toast.makeText(context, "You've already liked this post", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.createLike(Long.parseLong(userId), postId, new TravelApiService.ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer updatedLikeCount) {
                Log.d("TravelPostAdapter", "Like successful, new count: " + updatedLikeCount);
                likedPosts.add(postId);
                post.setLikeCount(updatedLikeCount);
                notifyItemChanged(holder.getAdapterPosition());
            }

            @Override
            public void onError(String error) {
                Log.e("TravelPostAdapter", "Like error: " + error);
                // If the error is due to already liked post, we should still update the UI
                if (error.contains("already exists")) {
                    likedPosts.add(postId);
                    notifyItemChanged(holder.getAdapterPosition());
                }
                Toast.makeText(context, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeLike(TravelPost post, ViewHolder holder) {
        Long postId = post.getId();
        TravelApiService apiService = new TravelApiService(context);
        apiService.deleteLike(Long.parseLong(userId), postId, new TravelApiService.ApiCallback<Integer>() {
            @Override
            public void onSuccess(Integer updatedLikeCount) {
                likedPosts.remove(postId);
                post.setLikeCount(updatedLikeCount);
                notifyItemChanged(holder.getAdapterPosition());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(context, "Error unliking post: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCommentButton(View convertView, TravelPost post) {
        Button commentButton = convertView.findViewById(R.id.comment_button);
        commentButton.setOnClickListener(v -> {
            Log.d("PostAdapter", "Comment button clicked for post: " + post.getId());
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("postId", (int) post.getId());
            intent.putExtra("userId", Integer.parseInt(userId));
            context.startActivity(intent);
        });
    }

}
