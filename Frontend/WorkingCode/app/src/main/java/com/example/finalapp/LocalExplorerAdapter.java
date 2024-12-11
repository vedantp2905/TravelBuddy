package com.example.finalapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class LocalExplorerAdapter extends FragmentStateAdapter {
    private static final String TAG = "LocalExplorerAdapter";
    private JSONArray restaurants;
    private JSONArray historical;
    private JSONArray attractions;
    private FragmentActivity activity;

    public LocalExplorerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.activity = fragmentActivity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        PlacesFragment fragment = new PlacesFragment();
        fragment.setCategory(position);
        JSONObject data = new JSONObject();
        try {
            switch (position) {
                case 0:
                    if (attractions != null) data.put("attractions", attractions);
                    break;
                case 1:
                    if (restaurants != null) data.put("restaurants", restaurants);
                    break;
                case 2:
                    if (historical != null) data.put("historical", historical);
                    break;
            }
            fragment.setData(data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating fragment: " + e.getMessage());
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3; // Attractions, Restaurants, Historical
    }

    public void clearData() {
        restaurants = null;
        historical = null;
        attractions = null;
        
        // Clear data in all attached fragments
        List<Fragment> fragments = getAttachedFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof PlacesFragment) {
                ((PlacesFragment) fragment).clearData();
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(JSONObject data) {
        try {
            if (data.has("restaurants")) {
                restaurants = data.getJSONArray("restaurants");
            }
            if (data.has("historical")) {
                historical = data.getJSONArray("historical");
            }
            if (data.has("attractions")) {
                attractions = data.getJSONArray("attractions");
            }

            // Update all attached fragments
            List<Fragment> fragments = getAttachedFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof PlacesFragment) {
                    ((PlacesFragment) fragment).setData(data);
                }
            }
            notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e(TAG, "Error updating data: " + e.getMessage());
        }
    }

    private List<Fragment> getAttachedFragments() {
        List<Fragment> fragments = new ArrayList<>();
        if (activity != null) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof PlacesFragment) {
                    fragments.add(fragment);
                }
            }
        }
        return fragments;
    }
}
