package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.DefaultRetryPolicy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class AboutYouProfile extends AppCompatActivity {

    // Static fields
    private EditText dietaryRestrictionsEditText, passportCountryEditText, frequentFlyerProgramsEditText,
            aboutMeEditText, preferredLanguageEditText, currencyPreferenceEditText;

    private Button continueButton;
    private RequestQueue requestQueue;
    private String userId;
    private boolean isFirstVisit = true;
    private Bundle previousProfileData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_you);

        requestQueue = Volley.newRequestQueue(this);
        userId = getUserId();
        boolean isFirstLogin = getIntent().getBooleanExtra("isFirstLogin", false);
        previousProfileData = getIntent().getBundleExtra("profileData");
        
        // Initialize views and setup
        initializeUIElements();
        
        // Modify continue button behavior
        continueButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (isFirstLogin && previousProfileData != null) {
                    // Combine both profile data and send to backend
                    sendCompleteProfile();
                } else {
                    // Normal flow for returning users
                    saveProfileAndContinue();
                }
            }
        });
    }

    private boolean validateInputs() {
        // Add validation for required fields
        if (aboutMeEditText.getText().toString().trim().isEmpty()) {
            aboutMeEditText.setError("This field is required");
            return false;
        }
        // Add other validations as needed
        return true;
    }

    private void saveProfileAndContinue() {
        if (validateInputs()) {
            // Navigate to ProfileActivity with the data
            Intent intent = new Intent(AboutYouProfile.this, ProfileActivity.class);
            intent.putExtra("isFirstLogin", true);
            intent.putExtra("dietaryRestrictions", dietaryRestrictionsEditText.getText().toString());
            intent.putExtra("passportCountry", passportCountryEditText.getText().toString());
            intent.putExtra("frequentFlyerPrograms", frequentFlyerProgramsEditText.getText().toString());
            intent.putExtra("aboutMe", aboutMeEditText.getText().toString());
            intent.putExtra("preferredLanguage", preferredLanguageEditText.getText().toString());
            intent.putExtra("currencyPreference", currencyPreferenceEditText.getText().toString());
            startActivity(intent);
            finish();
        }
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }

    private void initializeUIElements() {
        // Static fields initialization
        dietaryRestrictionsEditText = findViewById(R.id.dietary_restrictions_edittext);
        passportCountryEditText = findViewById(R.id.passport_country_edittext);
        frequentFlyerProgramsEditText = findViewById(R.id.frequent_flyer_programs_edittext);
        aboutMeEditText = findViewById(R.id.about_me_edittext);
        preferredLanguageEditText = findViewById(R.id.preferred_language_edittext);
        currencyPreferenceEditText = findViewById(R.id.currency_preference_edittext);

        continueButton = findViewById(R.id.continue_button);
    }

    private void fetchUserProfile(String id) {
        String url = ApiConstants.BASE_URL + "/api/users/profile/" + id;

        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        populateFields(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        handleErrorResponse(error, "Failed to load profile");
                    }
                }
        );

        requestQueue.add(getRequest);
    }

    private void populateFields(JSONObject response) {
        try {
            dietaryRestrictionsEditText.setText(String.join(",", getArrayFromJson(response, "dietaryRestrictions")));
            passportCountryEditText.setText(response.optString("passportCountry", ""));
            frequentFlyerProgramsEditText.setText(String.join(",", getArrayFromJson(response, "frequentFlyerPrograms")));
            aboutMeEditText.setText(response.optString("aboutMe", ""));
            preferredLanguageEditText.setText(response.optString("preferredLanguage", ""));
            currencyPreferenceEditText.setText(response.optString("currencyPreference", ""));

            isFirstVisit = response.optString("dietaryRestrictions", "").isEmpty();
        } catch (JSONException e) {
            showToast("New Account");
        }
    }

    private String[] getArrayFromJson(JSONObject response, String key) throws JSONException {
        JSONArray jsonArray = response.optJSONArray(key);
        if (jsonArray == null) {
            return new String[0];
        }
        String[] array = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            array[i] = jsonArray.optString(i, "");
        }
        return array;
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
        Toast.makeText(AboutYouProfile.this, message, Toast.LENGTH_SHORT).show();
    }

    private String[] splitStringToArray(String str) {
        return str.split(",\\s*");
    }

    private void sendCompleteProfile() {
        try {
            JSONObject completeProfile = new JSONObject();
            
            // Add data from previous profile
            completeProfile.put("travelBudget", Double.parseDouble(previousProfileData.getString("travelBudget")));
            completeProfile.put("travelStyle", previousProfileData.getString("travelStyle"));
            completeProfile.put("travelExperienceLevel", previousProfileData.getString("travelExperienceLevel"));
            completeProfile.put("maxTripDuration", Integer.parseInt(previousProfileData.getString("maxTripDuration")));
            completeProfile.put("preferredDestinations", new JSONArray(splitStringToArray(previousProfileData.getString("preferredDestinations"))));
            completeProfile.put("interests", new JSONArray(splitStringToArray(previousProfileData.getString("interests"))));
            completeProfile.put("preferredAirlines", new JSONArray(splitStringToArray(previousProfileData.getString("preferredAirlines"))));
            completeProfile.put("preferredAccommodationType", previousProfileData.getString("accommodationType"));
            
            // Add current profile data
            completeProfile.put("dietaryRestrictions", new JSONArray(splitStringToArray(dietaryRestrictionsEditText.getText().toString())));
            completeProfile.put("passportCountry", passportCountryEditText.getText().toString());
            completeProfile.put("frequentFlyerPrograms", new JSONArray(splitStringToArray(frequentFlyerProgramsEditText.getText().toString())));
            completeProfile.put("aboutMe", aboutMeEditText.getText().toString());
            completeProfile.put("preferredLanguage", preferredLanguageEditText.getText().toString());
            completeProfile.put("currencyPreference", currencyPreferenceEditText.getText().toString());
            completeProfile.put("profileCompleted", true);

            String url = ApiConstants.BASE_URL + "/api/users/profile/" + getUserId();
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, completeProfile,
                response -> {
                    // Successfully created profile, navigate to HomeActivity
                    showToast("Profile created successfully!");
                    Intent intent = new Intent(AboutYouProfile.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    if (error.networkResponse != null) {
                        // Check if it's actually a success response
                        if (error.networkResponse.statusCode == 200 || 
                            error.networkResponse.statusCode == 201) {
                            showToast("Profile created successfully!");
                            Intent intent = new Intent(AboutYouProfile.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            try {
                                String errorData = new String(error.networkResponse.data, "UTF-8");
                                showToast("Failed to create profile: " + errorData);
                            } catch (Exception e) {
                                showToast("Failed to create profile");
                            }
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
            
            request.setRetryPolicy(new DefaultRetryPolicy(
                30000,  // 30 seconds timeout
                0,      // no retries
                1.0f    // no backoff multiplier
            ));
            
            Volley.newRequestQueue(this).add(request);
        } catch (JSONException e) {
            e.printStackTrace();
            showToast("Error creating profile");
        }
    }
}
