package com.example.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {
    private final List<JSONObject> places;
    private final Context context;

    public PlaceAdapter(Context context, List<JSONObject> places) {
        this.context = context;
        this.places = places;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.place_item, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        try {
            JSONObject place = places.get(position);
            holder.nameView.setText(place.getString("name"));
            holder.addressView.setText(place.getString("address"));
            holder.descriptionView.setText(place.optString("description", "No description available"));
            
            holder.mapButton.setOnClickListener(v -> {
                try {
                    double latitude = place.getDouble("latitude");
                    double longitude = place.getDouble("longitude");
                    String mapsUrl = ((LocalExplorerActivity) context).createGoogleMapsUrl(latitude, longitude);
                    ((LocalExplorerActivity) context).openInMaps(mapsUrl);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView addressView;
        TextView descriptionView;
        ImageButton mapButton;

        PlaceViewHolder(View view) {
            super(view);
            nameView = view.findViewById(R.id.placeName);
            addressView = view.findViewById(R.id.placeAddress);
            descriptionView = view.findViewById(R.id.placeDescription);
            mapButton = view.findViewById(R.id.btnOpenMap);
        }
    }
} 