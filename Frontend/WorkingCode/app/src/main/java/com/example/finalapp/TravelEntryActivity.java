package com.example.finalapp;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.cardview.widget.CardView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class TravelEntryActivity extends AppCompatActivity {

    private static final String TAG = "TravelEntryActivity";
    private static final int TIMEOUT_MS = 20000; // 20 seconds timeout

    private EditText countryInput, citiesInput, numberOfAdultsInput, numberOfChildrenInput, userLocationInput;
    private Button startDateButton, endDateButton, submitTravelPlan;
    private TextView startDateText, endDateText;
    private String startDate, endDate;
    private RequestQueue requestQueue;
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.travel_entry_screen);

        Button createItineraryButton = findViewById(R.id.createItineraryButton);
        Button viewAllItinerariesButton = findViewById(R.id.viewAllItinerariesButton);

        createItineraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TravelEntryActivity.this, CreateItineraryActivity.class);
                startActivity(intent);
            }
        });

        viewAllItinerariesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TravelEntryActivity.this, ItinerariesActivity.class);
                startActivity(intent);
            }
        });
    }
}
