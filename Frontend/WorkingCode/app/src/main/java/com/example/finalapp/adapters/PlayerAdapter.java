package com.example.finalapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalapp.R;
import java.util.ArrayList;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.ViewHolder> {
    private List<String> playerNames;
    private final int[] playerColors = new int[]{
        Color.parseColor("#FFB6C1"), // Light Pink
        Color.parseColor("#FFF5EE"),   // Sea Shell
        Color.parseColor("#CCFFCC"), // Light Green
        Color.parseColor("#FFFACD"),     // Light Gold
        Color.parseColor("#F5F5DC"),  // Light Khaki
        Color.parseColor("#FFE4B5")   // Moccasin
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvPlayerName);
        }
    }

    public PlayerAdapter(List<String> playerNames) {
        this.playerNames = playerNames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(String.format("Player %d: %s", position + 1, playerNames.get(position)));
        holder.textView.setBackgroundColor(playerColors[position % playerColors.length]);
        holder.textView.setTextColor(Color.BLACK);
        holder.textView.setTextSize(24);
    }

    @Override
    public int getItemCount() {
        return playerNames.size();
    }

    public void updatePlayers(List<String> newPlayers) {
        this.playerNames = newPlayers;
        notifyDataSetChanged();
    }

    public List<String> getPlayerNames() {
        return new ArrayList<>(playerNames);
    }
} 