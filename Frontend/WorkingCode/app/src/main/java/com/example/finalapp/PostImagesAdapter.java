package com.example.finalapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

public class PostImagesAdapter extends RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder> {
    private Context context;
    private long postId;
    private static final String BASE_URL = "http://coms-3090-010.class.las.iastate.edu:8080";

    public PostImagesAdapter(Context context, long postId) {
        this.context = context;
        this.postId = postId;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = BASE_URL + "/api/travelimage/images/" + postId;
        Log.d("PostImagesAdapter", "Loading image from URL: " + imageUrl);
        
        holder.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .fitCenter()
            .into(holder.imageView);
            
        holder.imageView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ImageViewActivity.class);
            intent.putExtra("imageUrl", imageUrl);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.post_image_item);
        }
    }
}
