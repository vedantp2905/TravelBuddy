package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ViewProfileActivity extends AppCompatActivity {
    private TextView aboutMeText, travelBudgetText, travelStyleText, travelExperienceLevelText,
            maxTripDurationText, preferredDestinationsText, interestsText, preferredAirlinesText,
            accommodationTypeText, dietaryRestrictionsText, passportCountryText,
            frequentFlyerProgramsText, preferredLanguageText, currencyPreferenceText;
    private ImageView profilePictureImageView;
    private static final int PICK_IMAGE_FILE = 1;
    private Uri selectedImageUri;
    private RequestQueue requestQueue;
    private String userId;
    private TextView currentStatusText;
    private EditText statusInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        requestQueue = Volley.newRequestQueue(this);
        userId = getUserId();

        initializeViews();
        fetchUserProfile();
        fetchProfilePicture();
        fetchUserStatus();
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




    private void initializeViews() {
        // Initialize text fields
        aboutMeText = findViewById(R.id.about_me_text);
        travelBudgetText = findViewById(R.id.travel_budget_text);
        travelStyleText = findViewById(R.id.travel_style_text);
        travelExperienceLevelText = findViewById(R.id.travel_experience_level_text);
        maxTripDurationText = findViewById(R.id.max_trip_duration_text);
        preferredDestinationsText = findViewById(R.id.preferred_destinations_text);
        interestsText = findViewById(R.id.interests_text);
        preferredAirlinesText = findViewById(R.id.preferred_airlines_text);
        accommodationTypeText = findViewById(R.id.accommodation_type_text);
        dietaryRestrictionsText = findViewById(R.id.dietary_restrictions_text);
        passportCountryText = findViewById(R.id.passport_country_text);
        frequentFlyerProgramsText = findViewById(R.id.frequent_flyer_programs_text);
        preferredLanguageText = findViewById(R.id.preferred_language_text);
        currencyPreferenceText = findViewById(R.id.currency_preference_text);

        // Initialize profile picture view and set onClick listener
        profilePictureImageView = findViewById(R.id.profile_image_view);
        profilePictureImageView.setOnClickListener(v -> openImagePicker());
        currentStatusText = findViewById(R.id.current_status_text);
        statusInput = findViewById(R.id.status_input);

        Button updateStatusButton = findViewById(R.id.update_status_button);
        updateStatusButton.setOnClickListener(v -> updateUserStatus());
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }

    private void fetchUserStatus() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/user-status/get/" + userId;
        Log.d("NetworkRequest", url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    String status = response.optString("status", "No status set");
                    Log.d("NetworkRequest", status);
                    Log.d("NetworkRequest", response.toString());
                    currentStatusText.setText(status);
                    if (status != null && !status.isEmpty()) {
                        // Hide the status input and update button if a status is set
                        statusInput.setVisibility(View.GONE);
                        Button updateStatusButton = findViewById(R.id.update_status_button);
                        updateStatusButton.setVisibility(View.GONE);
                    } else {
                        // Show them if no status is set
                        statusInput.setVisibility(View.VISIBLE);
                        Button updateStatusButton = findViewById(R.id.update_status_button);
                        updateStatusButton.setVisibility(View.VISIBLE);
                    }
                },
                error -> handleError(error, ""));

        requestQueue.add(request);
    }

    private void updateUserStatus() {
        String newStatus = statusInput.getText().toString().trim();
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/user-status/add/" + userId;
        if (newStatus.isEmpty()) {
            showToast("Status cannot be empty");
            return;
        }

        JSONObject statusObject = new JSONObject();
        try {
            statusObject.put("prompt", newStatus);
        } catch (JSONException e) {
            showToast("Error preparing status data");
            Log.e("ViewProfileActivity", "JSON Error: " + e.getMessage());
            return;
        }
        Log.d("ViewProfileActivity", "JSON Payload: " + statusObject.toString());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, statusObject,
                response -> {
                    showToast("Status updated successfully");
                    fetchUserStatus();
                },
                error -> handleError(error, "Failed to update status"));

        requestQueue.add(request);
        fetchUserStatus();
    }

    private void handleError(VolleyError error, String userMessage) {
        Log.e("ViewProfileActivity", "Error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_FILE);
        Log.d("ViewProfileActivity", "Opening image picker");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                uploadProfilePicture();
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

            if (fileName == null) {
                Toast.makeText(this, "Error getting file name", Toast.LENGTH_SHORT).show();
                return;
            }

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
                        Toast.makeText(ViewProfileActivity.this,
                                "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("NetworkRequest", "Error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body().string();
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            try {
                                // Check if activity is still valid
                                if (!isFinishing() && !isDestroyed()) {
                                    // Clear any cached images
                                    Glide.get(ViewProfileActivity.this).clearMemory();
                                    new Thread(() -> {
                                        try {
                                            Glide.get(ViewProfileActivity.this).clearDiskCache();
                                            runOnUiThread(() -> {
                                                try {
                                                    // Set the current selected image immediately
                                                    profilePictureImageView.setImageURI(selectedImageUri);
                                                    // Then fetch from server to ensure sync
                                                    fetchProfilePicture();
                                                    Toast.makeText(ViewProfileActivity.this,
                                                            "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show();
                                                } catch (Exception e) {
                                                    Log.e("ViewProfileActivity", "Error updating UI: " + e.getMessage());
                                                }
                                            });
                                        } catch (Exception e) {
                                            Log.e("ViewProfileActivity", "Error clearing cache: " + e.getMessage());
                                        }
                                    }).start();
                                }
                            } catch (Exception e) {
                                Log.e("ViewProfileActivity", "Error in response handling: " + e.getMessage());
                            }
                        } else {
                            Toast.makeText(ViewProfileActivity.this,
                                    "Upload failed: " + responseBody, Toast.LENGTH_SHORT).show();
                            Log.e("NetworkRequest", "Error: " + responseBody);
                        }
                    });
                }
            });

        } catch (IOException e) {
            Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
            Log.e("ViewProfileActivity", "Error reading file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Unexpected error during upload", Toast.LENGTH_SHORT).show();
            Log.e("ViewProfileActivity", "Unexpected error: " + e.getMessage());
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

    private void fetchUserProfile() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                this::populateFields,
                error -> handleErrorResponse(error, "Failed to fetch profile"));

        requestQueue.add(request);
    }

    // Modify this method to handle the profile picture URL
    private void populateFields(JSONObject response) {
        try {
            // Populate other fields
            aboutMeText.setText(response.optString("aboutMe", ""));
            travelBudgetText.setText(String.valueOf(response.optDouble("travelBudget", 0.0)));
            travelStyleText.setText(response.optString("travelStyle", ""));
            travelExperienceLevelText.setText(response.optString("travelExperienceLevel", ""));
            maxTripDurationText.setText(String.valueOf(response.optDouble("maxTripDuration", 0.0)));
            preferredDestinationsText.setText(getArrayAsString(response, "preferredDestinations"));
            interestsText.setText(getArrayAsString(response, "interests"));
            preferredAirlinesText.setText(getArrayAsString(response, "preferredAirlines"));
            accommodationTypeText.setText(response.optString("preferredAccommodationType", ""));
            dietaryRestrictionsText.setText(getArrayAsString(response, "dietaryRestrictions"));
            passportCountryText.setText(response.optString("passportCountry", ""));
            frequentFlyerProgramsText.setText(getArrayAsString(response, "frequentFlyerPrograms"));
            preferredLanguageText.setText(response.optString("preferredLanguage", ""));
            currencyPreferenceText.setText(response.optString("currencyPreference", ""));

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Error loading profile data");
        }
    }

    private void setProfilePicture(String profilePictureUrl) {
        if (!profilePictureUrl.isEmpty()) {
            Glide.with(this)
                    .load(profilePictureUrl)
                    .placeholder(R.drawable.profilepicture)  // Default image
                    .error(R.drawable.profilepicture)  // Error image
                    .into(profilePictureImageView);  // ImageView to load the image
        } else {
            profilePictureImageView.setImageResource(R.drawable.profilepicture);  // Default image
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
            String value = jsonArray.optString(i, "").trim();
            value = value.replaceAll("[\\[\\]]", "");
            result.append(value);
        }
        return result.toString();
    }

    private void handleErrorResponse(VolleyError error, String message) {
        // Log the detailed error for debugging
        String detailedError = error.getMessage() != null ? error.getMessage() : "Unknown error occurred";
        Log.e("ViewProfileActivity", "Error details: " + detailedError);

        // Show user-friendly message
        showToast("Unable to update profile. Please try again.");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private String getFieldNameFromView(View view) {
        if (view.getId() == R.id.about_me_text) return "About Me";
        if (view.getId() == R.id.travel_budget_text) return "Travel Budget";
        if (view.getId() == R.id.travel_style_text) return "Travel Style";
        if (view.getId() == R.id.travel_experience_level_text) return "Travel Experience Level";
        if (view.getId() == R.id.max_trip_duration_text) return "Max Trip Duration";
        if (view.getId() == R.id.preferred_destinations_text) return "Preferred Destinations";
        if (view.getId() == R.id.interests_text) return "Interests";
        if (view.getId() == R.id.preferred_airlines_text) return "Preferred Airlines";
        if (view.getId() == R.id.accommodation_type_text) return "Accommodation Type";
        if (view.getId() == R.id.dietary_restrictions_text) return "Dietary Restrictions";
        if (view.getId() == R.id.passport_country_text) return "Passport Country";
        if (view.getId() == R.id.frequent_flyer_programs_text) return "Frequent Flyer Programs";
        if (view.getId() == R.id.preferred_language_text) return "Preferred Language";
        if (view.getId() == R.id.currency_preference_text) return "Currency Preference";
        return "Field";
    }
    private String getFieldKeyForBackend(int viewId) {
        if (viewId == R.id.about_me_text) return "aboutMe";
        if (viewId == R.id.travel_budget_text) return "travelBudget";
        if (viewId == R.id.travel_style_text) return "travelStyle";
        if (viewId == R.id.travel_experience_level_text) return "travelExperienceLevel";
        if (viewId == R.id.max_trip_duration_text) return "maxTripDuration";
        if (viewId == R.id.preferred_destinations_text) return "preferredDestinations";
        if (viewId == R.id.interests_text) return "interests";
        if (viewId == R.id.preferred_airlines_text) return "preferredAirlines";
        if (viewId == R.id.accommodation_type_text) return "accommodationType";
        if (viewId == R.id.dietary_restrictions_text) return "dietaryRestrictions";
        if (viewId == R.id.passport_country_text) return "passportCountry";
        if (viewId == R.id.frequent_flyer_programs_text) return "frequentFlyerPrograms";
        if (viewId == R.id.preferred_language_text) return "preferredLanguage";
        if (viewId == R.id.currency_preference_text) return "currencyPreference";
        return "";
    }
    private void sendUpdateToBackend(JSONObject updateData) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + userId;

        JsonObjectRequest updateRequest = new JsonObjectRequest(
                Request.Method.PATCH, url, updateData,
                response -> {
                    String message = response.optString("message", "Profile updated successfully");
                    showToast(message);
                    fetchUserProfile();
                },
                error -> {
                    Log.e("ViewProfileActivity", "Error updating profile", error);
                    showToast("Unable to update profile. Please try again.");
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };

        updateRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(updateRequest);
    }
    public void showEditDialog(View view) {
        String fieldName = getFieldNameFromView(view);
        String currentValue = ((TextView) view).getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.edit_profile_dialog, null);
        EditText input = dialogView.findViewById(R.id.dialog_edit_text);
        input.setText(currentValue);

        AlertDialog dialog = builder.setTitle("Edit " + fieldName)
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String newValue = input.getText().toString();
                    updateField(view.getId(), newValue);
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Set dialog window attributes
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.rounded_dialog_background);
        }

        dialog.show();
    }
    private void updateField(int viewId, String newValue) {
        // Update the UI
        ((TextView) findViewById(viewId)).setText(newValue);

        try {
            JSONObject updateData = new JSONObject();
            String fieldKey = getFieldKeyForBackend(viewId);

            // Handle arrays for fields that expect lists
            if (fieldKey.equals("preferredDestinations") ||
                    fieldKey.equals("interests") ||
                    fieldKey.equals("preferredAirlines") ||
                    fieldKey.equals("dietaryRestrictions") ||
                    fieldKey.equals("frequentFlyerPrograms")) {
                JSONArray array = new JSONArray();
                for (String item : newValue.split(",")) {
                    array.put(item.trim());
                }
                updateData.put(fieldKey, array);
            } else {
                updateData.put(fieldKey, newValue);
            }

            sendUpdateToBackend(updateData);
        } catch (JSONException e) {
            e.printStackTrace();
            showToast("Error updating field: " + e.getMessage());
        }
    }


}
