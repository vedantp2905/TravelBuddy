package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TravelSpacesAdapter extends RecyclerView.Adapter<TravelSpacesAdapter.ViewHolder> {

    private final List<TravelSpace> travelSpaceList;
    private final OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(TravelSpace travelSpace);
    }

    public TravelSpacesAdapter(List<TravelSpace> travelSpaceList, OnItemClickListener onItemClickListener) {
        this.travelSpaceList = travelSpaceList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_travel_space, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TravelSpace travelSpace = travelSpaceList.get(position);
        holder.titleTextView.setText(travelSpace.getTitle());
        holder.descriptionTextView.setText(travelSpace.getDescription());
        holder.expiryTextView.setText(travelSpace.getExpiryDate());

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(travelSpace));
    }

    @Override
    public int getItemCount() {
        return travelSpaceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, descriptionTextView, expiryTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            expiryTextView = itemView.findViewById(R.id.expiryTextView);
        }
    }
}
