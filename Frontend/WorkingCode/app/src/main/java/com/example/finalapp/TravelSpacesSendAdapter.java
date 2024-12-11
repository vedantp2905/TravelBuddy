package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TravelSpacesSendAdapter extends RecyclerView.Adapter<TravelSpacesSendAdapter.ViewHolder> {

    private List<TravelSpace> travelSpaceList;
    private View.OnClickListener clickListener;

    public TravelSpacesSendAdapter(List<TravelSpace> travelSpaceList, View.OnClickListener clickListener) {
        this.travelSpaceList = travelSpaceList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_travel_space, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TravelSpace travelSpace = travelSpaceList.get(position);
        holder.bind(travelSpace);
    }

    @Override
    public int getItemCount() {
        return travelSpaceList != null ? travelSpaceList.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView expiryTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            expiryTextView = itemView.findViewById(R.id.expiryTextView);

            // Set the click listener on the item view to handle item clicks
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(v);
                }
            });
        }

        public void bind(TravelSpace travelSpace) {
            if (titleTextView != null) {
                titleTextView.setText(travelSpace.getTitle());
            }
            if (descriptionTextView != null) {
                descriptionTextView.setText(travelSpace.getDescription());
            }
            if (expiryTextView != null) {
                expiryTextView.setText("Expires on: " + travelSpace.getExpiryDate());
            }
        }
    }
}
