package com.example.finalapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder> {

    private List<Itinerary> itineraryList;

    public ItineraryAdapter(List<Itinerary> itineraryList) {
        this.itineraryList = itineraryList;
    }

    @NonNull
    @Override
    public ItineraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_itinerary, parent, false);
        return new ItineraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItineraryViewHolder holder, int position) {
        Itinerary itinerary = itineraryList.get(position);
        holder.bind(itinerary);
    }

    @Override
    public int getItemCount() {
        return itineraryList.size();
    }

    class ItineraryViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private TextView descriptionView;

        public ItineraryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.itineraryTitle);
            descriptionView = itemView.findViewById(R.id.itineraryDescription);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Itinerary itinerary = itineraryList.get(position);
                    Intent intent = new Intent(itemView.getContext(), ItineraryDetailActivity.class);
                    intent.putExtra("country", itinerary.getCountry());
                    intent.putExtra("cities", itinerary.getCities());
                    intent.putExtra("startDate", itinerary.getStartDate());
                    intent.putExtra("endDate", itinerary.getEndDate());
                    intent.putExtra("adults", itinerary.getNumberOfAdults());
                    intent.putExtra("children", itinerary.getNumberOfChildren());
                    intent.putExtra("location", itinerary.getUserLocation());
                    intent.putExtra("generatedItinerary", itinerary.getGeneratedItinerary());
                    intent.putExtra("postID", itinerary.getPostID());
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        public void bind(Itinerary itinerary) {
            titleView.setText(itinerary.getCountry());
            descriptionView.setText(String.format("%s to %s", 
                itinerary.getStartDate(), itinerary.getEndDate()));
        }
    }
}

