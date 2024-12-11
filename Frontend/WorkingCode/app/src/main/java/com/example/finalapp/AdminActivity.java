package com.example.finalapp;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class AdminActivity extends AppCompatActivity {

    private EditText userIdEditText;
    private Button deleteUserButton, updateNewsletterButton;
    private RequestQueue requestQueue;
    private String userId;
    private boolean isSubscribed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin); // Change to your actual layout file

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Initialize UI elements
        userIdEditText = findViewById(R.id.user_id_edittext);
        deleteUserButton = findViewById(R.id.delete_user_button);
        updateNewsletterButton = findViewById(R.id.update_newsletter_button);

        // Set up button listeners
        deleteUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = userIdEditText.getText().toString();
                if (!userId.isEmpty()) {
                    deleteUserAccount(userId);
                } else {
                    Toast.makeText(AdminActivity.this, "Please enter a valid user ID", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateNewsletterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = userIdEditText.getText().toString();
                if (!userId.isEmpty()) {
                    updateNewsletter(userId, isSubscribed);
                } else {
                    Toast.makeText(AdminActivity.this, "Please enter a valid user ID", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteUserAccount(String id) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/delete/" + id;

        JsonObjectRequest deleteRequest = new JsonObjectRequest(
                Request.Method.DELETE, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(AdminActivity.this, "User account deleted successfully!", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        handleErrorResponse(error, "Failed to delete user");
                    }
                }
        );

        requestQueue.add(deleteRequest);
    }

    private void updateNewsletter(String id, boolean isSubscribed) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/newsletter-preference/" + id;

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("subscribed", isSubscribed);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest updateRequest = new JsonObjectRequest(
                Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(AdminActivity.this, "Newsletter preference updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        handleErrorResponse(error, "Failed to update newsletter preference");
                    }
                }
        );

        requestQueue.add(updateRequest);
    }

    private void handleErrorResponse(VolleyError error, String message) {
        String errorMessage = "";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                errorMessage = new String(error.networkResponse.data, "UTF-8");
                Log.e("AdminActivity", "Error response body: " + errorMessage);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            errorMessage = error.getMessage();
        }
        Log.e("AdminActivity", "Error: " + errorMessage);
        Toast.makeText(AdminActivity.this, message + ": " + errorMessage, Toast.LENGTH_SHORT).show();
    }
}
