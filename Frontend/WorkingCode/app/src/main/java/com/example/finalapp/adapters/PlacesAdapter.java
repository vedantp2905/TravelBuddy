package com.example.finalapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

import com.example.finalapp.R;
import com.example.finalapp.models.Place;
import com.example.finalapp.ImageViewActivity;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {
    private List<Place> places = new ArrayList<>();
    private final Context context;

    public PlacesAdapter(Context context) {
        this.context = context;
    }

    public void clearPlaces() {
        places.clear();
        notifyDataSetChanged();
    }

    public void setPlaces(List<Place> newPlaces) {
        places.clear();
        places.addAll(newPlaces);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView addressView;
        private final TextView ratingView;
        private final TextView descriptionView;
        private final ImageButton mapButton;
        private final View mapContainer;
        private final ImageView placeImage;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.place_name);
            addressView = itemView.findViewById(R.id.place_address);
            ratingView = itemView.findViewById(R.id.place_rating);
            descriptionView = itemView.findViewById(R.id.place_description);
            mapButton = itemView.findViewById(R.id.btnOpenMap);
            mapContainer = itemView.findViewById(R.id.mapContainer);
            placeImage = itemView.findViewById(R.id.placeImage);
            
            View.OnClickListener mapClickListener = v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Place place = places.get(position);
                    openInMaps(place.getLatitude(), place.getLongitude());
                }
            };

            // Set the same click listener for both the container and button
            mapContainer.setOnClickListener(mapClickListener);
            mapButton.setOnClickListener(mapClickListener);
        }

        public void bind(Place place) {
            nameView.setText(place.getName());
            addressView.setText(place.getAddress());
            ratingView.setText("Rating: " + place.getRating());
            descriptionView.setText(place.getDescription());
            
            // Load thumbnail using Glide
            if (place.getThumbnailUrl() != null && !place.getThumbnailUrl().isEmpty()) {
                Glide.with(context)
                    .load(place.getThumbnailUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(placeImage);
                
                // Add click listener to the image
                placeImage.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ImageViewActivity.class);
                    intent.putExtra("imageUrl", place.getThumbnailUrl());
                    context.startActivity(intent);
                });
            } else {
                placeImage.setImageResource(R.drawable.placeholder_image);
                placeImage.setOnClickListener(null); // Remove click listener if no image
            }
            
            // Check if latitude and longitude are non-zero
            if (place.getLatitude() != 0.0 && place.getLongitude() != 0.0) {
                mapContainer.setVisibility(View.VISIBLE);
                mapContainer.setEnabled(true);
            } else {
                mapContainer.setVisibility(View.GONE);
                mapContainer.setEnabled(false);
            }
        }
    }

    private void openInMaps(double latitude, double longitude) {
        String mapsUrl = "https://www.google.com/maps?q=" + latitude + "," + longitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
        intent.setPackage("com.google.android.apps.maps");
        
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            // If Google Maps isn't installed, open in browser
            intent.setPackage(null);
            context.startActivity(intent);
        }
    }
}
