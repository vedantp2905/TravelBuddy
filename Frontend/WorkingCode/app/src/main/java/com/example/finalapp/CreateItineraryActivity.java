package com.example.finalapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.TimeoutError;
import com.android.volley.RetryPolicy;
import com.android.volley.DefaultRetryPolicy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class CreateItineraryActivity extends AppCompatActivity {
    private static final String TAG = "CreateItineraryActivity";
    private EditText countryInput, citiesInput, numberOfAdultsInput, numberOfChildrenInput, userLocationInput;
    private Button startDateButton, endDateButton, submitTravelPlan;
    private String startDate, endDate;
    private RequestQueue requestQueue;
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_itinerary);

        initializeViews();
        setupRequestQueue();
        setupClickListeners();
    }

    private void initializeViews() {
        countryInput = findViewById(R.id.countryInput);
        citiesInput = findViewById(R.id.citiesInput);
        numberOfAdultsInput = findViewById(R.id.numberOfAdultsInput);
        numberOfChildrenInput = findViewById(R.id.numberOfChildrenInput);
        userLocationInput = findViewById(R.id.userLocationInput);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        submitTravelPlan = findViewById(R.id.submitTravelPlan);
    }

    private void setupRequestQueue() {
        requestQueue = Volley.newRequestQueue(this);
    }

    private void setupClickListeners() {
        startDateButton.setOnClickListener(view -> showDatePickerDialog(true));
        endDateButton.setOnClickListener(view -> showDatePickerDialog(false));
        submitTravelPlan.setOnClickListener(view -> validateAndSubmitTravelPlan());
    }

    private void showDatePickerDialog(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    String selectedDate = String.format("%d-%02d-%02d", year1, (month1 + 1), dayOfMonth);
                    if (isStartDate) {
                        startDate = selectedDate;
                        startDateButton.setText(getString(R.string.start_date_format, selectedDate));
                    } else {
                        endDate = selectedDate;
                        endDateButton.setText(getString(R.string.end_date_format, selectedDate));
                    }
                }, year, month, day);

        if (isStartDate) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        } else if (startDate != null) {
            try {
                Calendar startCal = Calendar.getInstance();
                String[] dateParts = startDate.split("-");
                startCal.set(Integer.parseInt(dateParts[0]),
                        Integer.parseInt(dateParts[1]) - 1,
                        Integer.parseInt(dateParts[2]));
                datePickerDialog.getDatePicker().setMinDate(startCal.getTimeInMillis());
            } catch (Exception e) {
                Log.e(TAG, "Error setting minimum date", e);
            }
        }

        datePickerDialog.show();
    }

    private void validateAndSubmitTravelPlan() {
        if (!validateInputs()) {
            return;
        }
        submitTravelPlan();
    }

    private boolean validateInputs() {
        if (isEmpty(countryInput) || isEmpty(citiesInput) || isEmpty(numberOfAdultsInput) ||
                isEmpty(numberOfChildrenInput) || isEmpty(userLocationInput)) {
            showError("All fields are required");
            return false;
        }

        if (startDate == null || endDate == null) {
            showError("Please select both start and end dates");
            return false;
        }

        try {
            int adults = Integer.parseInt(numberOfAdultsInput.getText().toString());
            int children = Integer.parseInt(numberOfChildrenInput.getText().toString());

            if (adults < 1) {
                showError("Number of adults must be at least 1");
                return false;
            }
            if (children < 0) {
                showError("Number of children cannot be negative");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for adults and children");
            return false;
        }

        return true;
    }

    private boolean isEmpty(EditText input) {
        return input.getText().toString().trim().isEmpty();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void submitTravelPlan() {
        showLoadingDialog();

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("userId", getUserId());
            requestBody.put("country", countryInput.getText().toString().trim());
            requestBody.put("cities", new JSONArray().put(citiesInput.getText().toString().trim()));
            requestBody.put("start_date", startDate);
            requestBody.put("end_date", endDate);
            requestBody.put("number_of_adults", Integer.parseInt(numberOfAdultsInput.getText().toString()));
            requestBody.put("number_of_children", Integer.parseInt(numberOfChildrenInput.getText().toString()));
            requestBody.put("user_location", userLocationInput.getText().toString().trim());

            String url = ApiConstants.BASE_URL + "/api/itineraries/generate";

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                        Toast.makeText(CreateItineraryActivity.this, "Itinerary created successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    this::handleError
            ) {
                @Override
                public RetryPolicy getRetryPolicy() {
                    return new DefaultRetryPolicy(
                        7000, // 7 second timeout
                        0,    // no retries
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    );
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            Log.e(TAG, "Error creating request", e);
            showError("Error creating itinerary");
        }
    }

    private void handleError(VolleyError error) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        String errorMessage;
        if (error instanceof TimeoutError) {
            // Show a more positive message for timeouts
            Toast.makeText(CreateItineraryActivity.this, 
                "Request received! Your itinerary will be emailed to you shortly and will be available in the app within a minute.", 
                Toast.LENGTH_LONG).show();
            finish(); // Return to previous screen
            return;
        }

        if (error.networkResponse != null) {
            switch (error.networkResponse.statusCode) {
                case 400:
                    errorMessage = "Invalid input. Please check your data.";
                    break;
                case 401:
                    errorMessage = "Unauthorized. Please log in again.";
                    break;
                case 500:
                    errorMessage = "Server error. Please try again later.";
                    break;
                default:
                    errorMessage = "Error: " + error.networkResponse.statusCode;
            }
            Log.e(TAG, "Network Response Status Code: " + error.networkResponse.statusCode);
            Log.e(TAG, "Network Response Data: " + new String(error.networkResponse.data));
        } else {
            errorMessage = "Network error. Please try again.";
        }

        showError(errorMessage);
        Log.e(TAG, "Network Error", error);
    }

    private int getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1);
    }

    private void showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Creating itinerary...");
            loadingDialog.setCancelable(false);
        }
        loadingDialog.show();
    }
} 