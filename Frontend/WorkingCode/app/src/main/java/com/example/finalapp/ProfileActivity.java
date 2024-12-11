package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.Listener;
import com.android.volley.Response.ErrorListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;


public class ProfileActivity extends AppCompatActivity {

    private EditText travelBudgetEditText, travelStyleEditText, travelExperienceLevelEditText,
            maxTripDurationEditText, preferredDestinationsEditText, interestsEditText,
            preferredAirlinesEditText, accommodationTypeEditText;
    String dietaryRestrictions;
    String passportCountry;
    String frequentFlyerPrograms;
    String aboutMeEditText;
    String preferredLanguageEditText;
    String currencyPreference;

    private Button submitButton;
    private RequestQueue requestQueue;
    private String userId;
    private boolean isFirstVisit = true;
    private ImageView profilePictureImageView;
    private Button selectPictureButton;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        requestQueue = Volley.newRequestQueue(this);
        userId = getUserId();
        boolean isFirstLogin = getIntent().getBooleanExtra("isFirstLogin", false);

        // Get data from AboutYouProfile
        dietaryRestrictions = getIntent().getStringExtra("dietaryRestrictions");
        passportCountry = getIntent().getStringExtra("passportCountry");
        frequentFlyerPrograms = getIntent().getStringExtra("frequentFlyerPrograms");
        aboutMeEditText = getIntent().getStringExtra("aboutMe");
        preferredLanguageEditText = getIntent().getStringExtra("preferredLanguage");
        currencyPreference = getIntent().getStringExtra("currencyPreference");

        if (userId.equals("-1")) {
            showToast("User ID not found, please log in again.");
            finish();
            return;
        }

        initializeUIElements();
        
        if (!isFirstLogin) {
            fetchUserProfile(userId);
        }

        submitButton.setOnClickListener(v -> {
            try {
                sendUserProfile();
            } catch (JSONException e) {
                e.printStackTrace();
                showToast("Error creating profile");
            }
        });

        profilePictureImageView = findViewById(R.id.profile_picture_imageview);
        selectPictureButton = findViewById(R.id.select_picture_button);
        
        selectPictureButton.setOnClickListener(v -> openImagePicker());
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }

    private void initializeUIElements() {

        travelBudgetEditText = findViewById(R.id.travel_budget_edittext);
        travelStyleEditText = findViewById(R.id.travel_style_edittext);
        travelExperienceLevelEditText = findViewById(R.id.travel_experience_level_edittext);
        maxTripDurationEditText = findViewById(R.id.max_trip_duration_edittext);
        preferredDestinationsEditText = findViewById(R.id.preferred_destinations_edittext);
        interestsEditText = findViewById(R.id.interests_edittext);
        preferredAirlinesEditText = findViewById(R.id.preferred_airlines_edittext);
        accommodationTypeEditText = findViewById(R.id.accommodation_type_edittext);
        submitButton = findViewById(R.id.submit_button);
    }

    private void fetchUserProfile(String id) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + id;

        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    populateFields(response);
                },
                error -> {
                    handleErrorResponse(error, "Empty profile");
                }
        );

        requestQueue.add(getRequest);
    }

    private void populateFields(JSONObject response) {
        try {
            aboutMeEditText = response.optString("aboutMe", "");
            travelBudgetEditText.setText(String.valueOf(response.optDouble("travelBudget", 0.0)));
            travelStyleEditText.setText(response.optString("travelStyle", ""));
            travelExperienceLevelEditText.setText(response.optString("travelExperienceLevel", ""));
            maxTripDurationEditText.setText(String.valueOf(response.optInt("maxTripDuration", 0)));

            preferredDestinationsEditText.setText(getArrayAsString(response, "preferredDestinations"));
            interestsEditText.setText(getArrayAsString(response, "interests"));
            preferredAirlinesEditText.setText(getArrayAsString(response, "preferredAirlines"));
            accommodationTypeEditText.setText(response.optString("preferredAccommodationType", ""));

            isFirstVisit = response.optString("aboutMe", "").isEmpty();
        } catch (JSONException e) {
            showToast("Parsed profile data");
        }
    }

    private String getArrayAsString(JSONObject response, String key) throws JSONException {
        JSONArray jsonArray = response.optJSONArray(key);
        if (jsonArray == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(jsonArray.optString(i, ""));
        }
        return result.toString();
    }

    private void sendUserProfile() throws JSONException {
        if (selectedImageUri == null) {
            showToast("Please select a profile picture");
            return;
        }

        // Validate required fields
        if (!validateInputs()) {
            return;
        }
        
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + userId;
        JSONObject profileData = constructProfileData();
        profileData.put("profileCompleted", true);

        JsonObjectRequest postRequest = new JsonObjectRequest(
                Request.Method.POST, url, profileData,
                response -> {
                    try {
                        String message = response.optString("message", "Profile created successfully!");
                        showToast(message);
                        Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        uploadProfilePicture();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("Error processing response");
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        try {
                            String errorData = new String(error.networkResponse.data, "UTF-8");
                            JSONObject errorJson = new JSONObject(errorData);
                            String errorMessage = errorJson.optString("message", "Failed to create profile");
                            showToast(errorMessage);
                        } catch (Exception e) {
                            showToast("Failed to create profile");
                        }
                    } else {
                        showToast("Network error occurred");
                    }
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        postRequest.setRetryPolicy(new DefaultRetryPolicy(
            30000,  // 30 seconds timeout
            0,      // no retries
            1.0f    // no backoff multiplier
        ));

        requestQueue.add(postRequest);
    }

    private void updateUserProfile() throws JSONException {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + userId;
        JSONObject profileData = constructProfileData();

        JsonObjectRequest updateRequest = new JsonObjectRequest(
            Request.Method.PUT, url, profileData,
            response -> {
                showToast("Profile updated successfully");
                finish();
            },
            error -> {
                Log.e("ProfileActivity", "Error updating profile", error);
                showToast("Unable to update profile. Please try again.");
            }
        );

        requestQueue.add(updateRequest);
    }

    private void deleteUserAccount(String id) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + id;

        JsonObjectRequest deleteRequest = new JsonObjectRequest(
                Request.Method.DELETE, url, null,
                response -> {
                    showToast("Account deleted successfully!");
                    finish();
                },
                error -> {
                    handleErrorResponse(error, "Deleted account");
                }
        );

        requestQueue.add(deleteRequest);
    }

    private void handleErrorResponse(VolleyError error, String message) {
        String errorMessage = "";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                errorMessage = new String(error.networkResponse.data, "UTF-8");
                Log.e("ProfileActivity", "Error response body: " + errorMessage);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            errorMessage = error.getMessage();
        }
        Log.e("ProfileActivity", "Error: " + errorMessage);
        showToast(message + ": " + errorMessage);
    }

    private void showToast(String message) {
        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private String[] splitStringToArray(String str) {
        return str.split(",\\s*");
    }

    private JSONObject constructProfileData() throws JSONException {
        JSONObject profileData = new JSONObject();

        // Handle text fields that can be empty
        profileData.put("aboutMe", aboutMeEditText != null ? aboutMeEditText : "");
        profileData.put("preferredLanguage", preferredLanguageEditText != null ? preferredLanguageEditText : "");
        profileData.put("currencyPreference", currencyPreference != null ? currencyPreference : "");

        // Handle numeric fields with validation
        String travelBudgetStr = travelBudgetEditText.getText().toString().trim();
        double travelBudget = travelBudgetStr.isEmpty() ? 0.0 : Double.parseDouble(travelBudgetStr);
        profileData.put("travelBudget", travelBudget);

        profileData.put("travelStyle", travelStyleEditText.getText().toString());
        profileData.put("travelExperienceLevel", travelExperienceLevelEditText.getText().toString());

        String maxTripDurationStr = maxTripDurationEditText.getText().toString().trim();
        int maxTripDuration = maxTripDurationStr.isEmpty() ? 0 : Integer.parseInt(maxTripDurationStr);
        profileData.put("maxTripDuration", maxTripDuration);

        // Handle array fields
        profileData.put("preferredDestinations", new JSONArray(splitStringToArray(preferredDestinationsEditText.getText().toString())));
        profileData.put("interests", new JSONArray(splitStringToArray(interestsEditText.getText().toString())));
        profileData.put("preferredAirlines", new JSONArray(splitStringToArray(preferredAirlinesEditText.getText().toString())));
        profileData.put("preferredAccommodationType", accommodationTypeEditText.getText().toString());
        profileData.put("dietaryRestrictions", new JSONArray(splitStringToArray(dietaryRestrictions != null ? dietaryRestrictions : "")));
        profileData.put("passportCountry", passportCountry != null ? passportCountry : "");
        profileData.put("frequentFlyerPrograms", new JSONArray(splitStringToArray(frequentFlyerPrograms != null ? frequentFlyerPrograms : "")));

        return profileData;
    }

    private Bundle constructProfileBundle() {
        Bundle profileData = new Bundle();
        try {
            profileData.putString("travelBudget", travelBudgetEditText.getText().toString());
            profileData.putString("travelStyle", travelStyleEditText.getText().toString());
            profileData.putString("travelExperienceLevel", travelExperienceLevelEditText.getText().toString());
            profileData.putString("maxTripDuration", maxTripDurationEditText.getText().toString());
            profileData.putString("preferredDestinations", preferredDestinationsEditText.getText().toString());
            profileData.putString("interests", interestsEditText.getText().toString());
            profileData.putString("preferredAirlines", preferredAirlinesEditText.getText().toString());
            profileData.putString("accommodationType", accommodationTypeEditText.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error creating profile bundle");
        }
        return profileData;
    }

    private boolean validateInputs() {
        if (travelBudgetEditText.getText().toString().trim().isEmpty()) {
            travelBudgetEditText.setError("Travel budget is required");
            return false;
        }
        if (travelStyleEditText.getText().toString().trim().isEmpty()) {
            travelStyleEditText.setError("Travel style is required");
            return false;
        }
        if (travelExperienceLevelEditText.getText().toString().trim().isEmpty()) {
            travelExperienceLevelEditText.setError("Experience level is required");
            return false;
        }
        if (maxTripDurationEditText.getText().toString().trim().isEmpty()) {
            maxTripDurationEditText.setError("Maximum trip duration is required");
            return false;
        }
        return true;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                profilePictureImageView.setImageURI(selectedImageUri);
            }
        }
    }

    private void uploadProfilePicture() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/profile-picture/upload/" + userId;

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            byte[] imageBytes = getBytes(inputStream);
            String fileName = getFileNameFromUri(selectedImageUri);

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", fileName,
                            RequestBody.create(MediaType.parse("image/*"), imageBytes));

            RequestBody requestBody = builder.build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this,
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("NetworkRequest", "Error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    final String responseBody = response.body().string();
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Glide.get(ProfileActivity.this).clearMemory();
                            new Thread(() -> {
                                Glide.get(ProfileActivity.this).clearDiskCache();
                                runOnUiThread(() -> {
                                    profilePictureImageView.setImageURI(selectedImageUri);
                                    fetchProfilePicture();
                                    Toast.makeText(ProfileActivity.this,
                                            "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show();
                                });
                            }).start();
                        } else {
                            Toast.makeText(ProfileActivity.this,
                                    "Upload failed: " + responseBody, Toast.LENGTH_SHORT).show();
                            Log.e("NetworkRequest", "Error: " + responseBody);
                        }
                    });
                }
            });

        } catch (IOException e) {
            Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return null;

        String result = null;
        try {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index != -1) {
                            result = cursor.getString(index);
                        }
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        } catch (Exception e) {
            Log.e("FILE_NAME", "Error getting file name: " + e.getMessage());
            return null;
        }
        return result;
    }

    private void fetchProfilePicture() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/profile-picture/get/" + userId;

        if (!url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.profilepicture)
                    .error(R.drawable.profilepicture)
                    .into(profilePictureImageView);
        } else {
            profilePictureImageView.setImageResource(R.drawable.profilepicture);
        }
    }
}

