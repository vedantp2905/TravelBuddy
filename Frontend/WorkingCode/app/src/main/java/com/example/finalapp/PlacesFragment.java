package com.example.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import android.util.Log;
import java.util.Collections;
import org.json.JSONException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.adapters.PlacesAdapter;
import com.example.finalapp.models.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PlacesFragment extends Fragment {
    private static final String TAG = "PlacesFragment";
    private RecyclerView recyclerView;
    private PlacesAdapter adapter;
    private int category; // 0: attractions, 1: restaurants, 2: historical
    private JSONObject data;

    public void setCategory(int category) {
        this.category = category;
    }

    public void setData(JSONObject data) {
        this.data = data;
        updatePlacesList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_places, container, false);
        recyclerView = view.findViewById(R.id.places_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlacesAdapter(getContext());
        recyclerView.setAdapter(adapter);
        if (data != null) {
            updatePlacesList();
        }
        return view;
    }

    public void clearData() {
        if (adapter != null) {
            adapter.clearPlaces();
        }
    }

    private void updatePlacesList() {
        if (data == null || !isAdded()) return;

        try {
            String categoryKey;
            switch (category) {
                case 0:
                    categoryKey = "attractions";
                    break;
                case 1:
                    categoryKey = "restaurants";
                    break;
                case 2:
                    categoryKey = "historical";
                    break;
                default:
                    return;
            }

            Log.d(TAG, "Updating places list for category: " + categoryKey);
            Log.d(TAG, "Data: " + data.toString());

            JSONArray places = data.optJSONArray(categoryKey);
            if (places == null) {
                Log.e(TAG, "No data found for category: " + categoryKey);
                return;
            }

            List<Place> placesList = new ArrayList<>();
            
            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                Place newPlace = new Place(
                    place.optString("name", "Unknown"),
                    place.optString("address", "No address"),
                    place.optString("rating", "N/A"),
                    place.optString("description", "No description available"),
                    place.optDouble("latitude", 0.0),
                    place.optDouble("longitude", 0.0),
                    place.optString("thumbnail", null)
                );
                placesList.add(newPlace);
                Log.d(TAG, "Added place: " + newPlace.getName());
            }
            
            if (adapter != null) {
                adapter.setPlaces(placesList);
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Updated adapter with " + placesList.size() + " places");
            } else {
                Log.e(TAG, "Adapter is null");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing places data: " + e.getMessage());
        }
    }
}
