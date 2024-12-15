package com.example.finalapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;

public class AddTravelSpaceActivity extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText, expirationDateEditText;
    private Button createTravelSpaceButton;
    private RequestQueue requestQueue;

    private int year, month, day, hour, minute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_travel_space);

        // Initialize views
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        expirationDateEditText = findViewById(R.id.expirationDateEditText);
        createTravelSpaceButton = findViewById(R.id.createTravelSpaceButton);

        // Initialize RequestQueue for Volley
        requestQueue = Volley.newRequestQueue(this);

        // Get current date and time for default values
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        // Set expiration date field to open date picker
        expirationDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Set button click listener
        createTravelSpaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTravelSpace();
            }
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                AddTravelSpaceActivity.this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Update the year, month, and day when the date is picked
                    AddTravelSpaceActivity.this.year = year;
                    AddTravelSpaceActivity.this.month = monthOfYear;
                    AddTravelSpaceActivity.this.day = dayOfMonth;
                    showTimePickerDialog(); // After picking the date, show the time picker
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                AddTravelSpaceActivity.this,
                (view, hourOfDay, minuteOfHour) -> {
                    // Update the hour and minute when the time is picked
                    hour = hourOfDay;
                    minute = minuteOfHour;
                    updateExpirationDateField(); // Update the expiration date field with date and time
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void updateExpirationDateField() {
        // Format the expiration date and time as "YYYY-MM-DDTHH:MM:SS"
        String expirationDate = String.format("%d-%02d-%02dT%02d:%02d:00", year, month + 1, day, hour, minute);
        expirationDateEditText.setText(expirationDate); // Set the picked date and time into the EditText
    }

    private void createTravelSpace() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String expirationDate = expirationDateEditText.getText().toString().trim();

        if (validateInputs(title, description, expirationDate)) {
            // Construct the JSON object for the request body
            JSONObject travelSpaceData = new JSONObject();
            try {
                travelSpaceData.put("title", title);
                travelSpaceData.put("description", description);
                travelSpaceData.put("expirationDate", expirationDate);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating TravelSpace data.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Define the endpoint URL
            String url = ApiConstants.BASE_URL + "/api/travelspace/create/";

            // Create a new JsonObjectRequest
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, travelSpaceData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Toast.makeText(AddTravelSpaceActivity.this, "New Travel Space created successfully.", Toast.LENGTH_SHORT).show();
                            // Navigate to TravelSpacesActivity
                            Intent intent = new Intent(AddTravelSpaceActivity.this, TravelSpacesActivity.class);
                            startActivity(intent);
                            finish(); // Close current activity
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(AddTravelSpaceActivity.this, "Failed to create Travel Space.", Toast.LENGTH_SHORT).show();
                        }
                    });

            // Add the request to the RequestQueue
            requestQueue.add(jsonObjectRequest);
        }
    }

    private boolean validateInputs(String title, String description, String expirationDate) {
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Title is required");
            return false;
        }
        if (TextUtils.isEmpty(description)) {
            descriptionEditText.setError("Description is required");
            return false;
        }
        if (TextUtils.isEmpty(expirationDate)) {
            expirationDateEditText.setError("Expiration date is required");
            return false;
        }
        return true;
    }
}
