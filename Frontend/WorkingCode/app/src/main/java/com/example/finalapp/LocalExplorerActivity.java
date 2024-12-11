package com.example.finalapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.material.tabs.TabLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import android.app.ProgressDialog;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import java.util.Locale;
import android.content.Intent;
import java.util.ArrayList;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.TextView;
import android.content.SharedPreferences;


public class LocalExplorerActivity extends AppCompatActivity {
    private EditText citySearchInput;
    private ViewPager2 categoryPager;
    private TabLayout tabLayout;
    private static final String BASE_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/local-explorer/";
    private static final String TAG = "LocalExplorerActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isSearchInProgress = false;
    private ProgressDialog loadingDialog;
    private boolean locationFetched = false; // Flag to track location retrieval
    private RecyclerView recyclerView;
    private static final String PREFS_NAME = "LocalExplorerPrefs";
    private static final String LAST_SEARCH_KEY = "lastSearch";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_explorer);

        citySearchInput = findViewById(R.id.citySearchInput);
        categoryPager = findViewById(R.id.categoryPager);
        tabLayout = findViewById(R.id.tabLayout);

        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            if (!isSearchInProgress) {
                searchCity();
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupViewPager();


       if (!checkLocationPermission()) {
            requestLocationPermission(); // Helper method to simplify
        } else { // Permission is already granted
            getCurrentLocationAndPopulateSearchBar();
        }

        // Initialize loading dialog
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Finding the best places for you...");
        loadingDialog.setCancelable(false);

        recyclerView = findViewById(R.id.places_recycler_view);
    }

    
    private void getCurrentLocationAndPopulateSearchBar() {
        Log.d(TAG, "Attempting to get current location");
        if (!checkLocationPermission()) {
            Log.e(TAG, "Location permission not granted");
            handleLocationError("Location permission not granted");
            return;
        }
    
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    Log.d(TAG, "Location obtained: " + location.toString());
                    getCityFromLocation(location);
                } else {
                    Log.w(TAG, "Last known location is null. Requesting updated location...");
                    requestCurrentLocation(); // Use LocationRequest if the last known location is null
                }
            })
            .addOnFailureListener(this, e -> {
                Log.e(TAG, "Failed to get location", e);
                handleLocationError("Failed to retrieve location: " + e.getMessage());
            });
    }
    

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCurrentLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000); // Request updates every second (adjust if needed)
    
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    handleLocationError("Unable to get location update");
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    getCityFromLocation(location);
                }
                fusedLocationClient.removeLocationUpdates(this); // Stop updates after getting a location
            }
        };
    
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }
    
    

    private void requestLocationPermission() { // Helper method to request permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void handleLocationError(String message) {  // Helper method to handle errors
        Log.e(TAG, message);
        citySearchInput.setText("Your Default City");  // Set default if location is not found
        Toast.makeText(this, message + " Using default.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with getting the location
                getCurrentLocationAndPopulateSearchBar();  // Call the correct method!
            } else {
                // Permission denied, handle accordingly (e.g., disable location-based features)
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();

                // You could set a default location here since permission is denied
                citySearchInput.setText("Your Default City"); // Or handle as needed
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getCityFromLocation(location);
                    }
                });
    }

    private void getCityFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                if (city != null && !city.isEmpty()) {
                    // Just set the text, don't trigger search
                    citySearchInput.setText(city);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error getting city name: " + e.getMessage());
        }
    }

   private void searchCity() {
    if (citySearchInput.getText().toString().isEmpty()) {
        Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
        return;
    }

    loadingDialog.show();
    isSearchInProgress = true;

    String url = BASE_URL + "search";
    url += "?location=" + citySearchInput.getText().toString();

    JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                loadingDialog.dismiss();
                isSearchInProgress = false;
                Log.d(TAG, "Complete raw response: " + response.toString());
                onSearchSuccess(response);
            },
            error -> {
                loadingDialog.dismiss();
                isSearchInProgress = false;
                Log.e(TAG, "Error fetching data: " + error.getMessage());
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
    ) {
        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Content-Type", "application/json");
            return headers;
        }
    };

    RequestQueue queue = Volley.newRequestQueue(this);
    queue.add(request);
}





private void onSearchSuccess(JSONObject response) {
    Log.d(TAG, "Search success response: " + response.toString());
    
    // Save to SharedPreferences
    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
    editor.putString(LAST_SEARCH_KEY, response.toString());
    editor.apply();
    
    // Clear any existing data in the adapter
    LocalExplorerAdapter pagerAdapter = (LocalExplorerAdapter) categoryPager.getAdapter();
    if (pagerAdapter != null) {
        pagerAdapter.clearData();
        pagerAdapter.updateData(response);
        // Force refresh the current page
        categoryPager.setCurrentItem(categoryPager.getCurrentItem());
    }
    
    if (loadingDialog != null && loadingDialog.isShowing()) {
        loadingDialog.dismiss();
    }
}






    private void setupViewPager() {
        LocalExplorerAdapter adapter = new LocalExplorerAdapter(this);
        categoryPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, categoryPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Attractions");
                            break;
                        case 1:
                            tab.setText("Restaurants");
                            break;
                        case 2:
                            tab.setText("Historical");
                            break;
                    }
                }).attach();
    }

    public String createGoogleMapsUrl(double latitude, double longitude) {
        return "https://www.google.com/maps?q=" + latitude + "," + longitude;
    }

    public void openInMaps(String mapsUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
        intent.setPackage("com.google.android.apps.maps");
        
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    private void displayPlaces(JSONObject response) {
        // Update the adapter with new data
        LocalExplorerAdapter pagerAdapter = (LocalExplorerAdapter) categoryPager.getAdapter();
        if (pagerAdapter != null) {
            pagerAdapter.updateData(response);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLastSearch();
    }

    private void loadLastSearch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastSearch = prefs.getString(LAST_SEARCH_KEY, null);
        if (lastSearch != null) {
            try {
                JSONObject response = new JSONObject(lastSearch);
                displayPlaces(response);
            } catch (JSONException e) {
                Log.e(TAG, "Error loading last search", e);
            }
        }
    }

    private void performSearch(double latitude, double longitude) {
        if (isSearchInProgress) return;
        isSearchInProgress = true;

        Log.d(TAG, "Performing search for coordinates: " + latitude + ", " + longitude);
        
        showLoadingDialog();

        String url = BASE_URL + "places?latitude=" + latitude + "&longitude=" + longitude;
        Log.d(TAG, "Request URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                Log.d(TAG, "Search response received");
                isSearchInProgress = false;
                onSearchSuccess(response);
            },
            error -> {
                Log.e(TAG, "Search error: " + error.toString());
                isSearchInProgress = false;
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                String errorMessage = "Error fetching places";
                if (error instanceof TimeoutError) {
                    errorMessage = "Request timed out";
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Cache-Control", "no-cache");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Finding the best places for you...");
            loadingDialog.setCancelable(false);
        }
        loadingDialog.show();
    }
}

