package com.example.finalapp;

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

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ChangePassActivity extends AppCompatActivity {

    private EditText changePasswordEdt, confirmPasswordEdt, changeEmailEdt, confirmEmailEdt;
    private Button confirmPasswordBtn, changeEmailBtn, deleteAccountBtn, unsubBtn;
    private RequestQueue requestQueue;
    private String userId;
    private boolean isSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pass);

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Retrieve userId from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        userId = String.valueOf(sharedPreferences.getInt("userId", -1));

        if (userId.equals("-1")) {
            Toast.makeText(this, "User ID not found, please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize UI Elements
        changePasswordEdt = findViewById(R.id.change_password_edt);
        confirmPasswordEdt = findViewById(R.id.confirm_password_edt);
        changeEmailEdt = findViewById(R.id.change_email_edt);
        confirmEmailEdt = findViewById(R.id.confirm_email_edt);
        unsubBtn = findViewById(R.id.change_pass_unsub_btn);

        if (unsubBtn == null) {
            Log.e("ChangePassActivity", "Newsletter button not found in layout");
            return;
        }

        // Initialize Buttons
        confirmPasswordBtn = findViewById(R.id.confirm_password_btn);
        changeEmailBtn = findViewById(R.id.change_email_btn);
        deleteAccountBtn = findViewById(R.id.delete_account_btn);
        getNewsletter();
        // Set onClick listeners
        setOnClickListeners();
    }

    private void setOnClickListeners() {
        confirmPasswordBtn.setOnClickListener(v -> handleChangePassword());
        changeEmailBtn.setOnClickListener(v -> validateAndChangeEmail());
        deleteAccountBtn.setOnClickListener(v -> deleteAccount(userId));
        unsubBtn.setOnClickListener(v -> updateNewsletter(isSub));
    }

    private void getNewsletter() {
        if (userId == null || userId.equals("-1")) {
            Log.e("ChangePassActivity", "Invalid user ID for newsletter check");
            unsubBtn.setText("Subscribe to Newsletter");
            isSub = false;
            unsubBtn.setVisibility(View.VISIBLE);
            return;
        }

        // Convert userId to proper format
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/newsletter-preference/" + 
                    Long.parseLong(userId.trim());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean isSubscribed = response.optBoolean("subscribed", false);
                        unsubBtn.setText(isSubscribed ? "Unsubscribe from Newsletter" : "Subscribe to Newsletter");
                        isSub = isSubscribed;
                        unsubBtn.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Log.e("ChangePassActivity", "Error parsing response", e);
                        unsubBtn.setText("Subscribe to Newsletter");
                        isSub = false;
                        unsubBtn.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    Log.e("ChangePassActivity", "Network error: " + error.toString());
                    unsubBtn.setText("Subscribe to Newsletter");
                    isSub = false;
                    unsubBtn.setVisibility(View.VISIBLE);
                    if (error.networkResponse != null) {
                        Log.e("ChangePassActivity", "Error code: " + error.networkResponse.statusCode);
                    }
                }
        );

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            10000,
            1,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void updateNewsletter(Boolean isSubscribed) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/newsletter-preference/" + 
                    Long.parseLong(userId.trim());

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("subscribed", !isSubscribed);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        
        CustomJsonRequest request = new CustomJsonRequest(
            Request.Method.POST, 
            url, 
            requestBody,
            response -> {
                try {
                    String message = response.has("message") ? 
                        response.getString("message") : 
                        (isSub ? "Successfully unsubscribed from newsletter!" : "Successfully subscribed to newsletter!");
                    Toast.makeText(ChangePassActivity.this, message, Toast.LENGTH_LONG).show();
                    isSub = !isSubscribed;
                    unsubBtn.setText(isSub ? "Unsubscribe from Newsletter" : "Subscribe to Newsletter");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> handleError(error, "Failed to update newsletter preference")
        );
        
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000,
            0,
            1.0f
        ));
        
        requestQueue.add(request);
    }


    private void handleChangePassword() {
        String newPassword = changePasswordEdt.getText().toString();
        String confirmPassword = confirmPasswordEdt.getText().toString();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        // First, check if the new password is same as current
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/check-password/" + userId;
        
        JSONObject checkBody = new JSONObject();
        try {
            checkBody.put("password", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        CustomJsonRequest checkRequest = new CustomJsonRequest(
            Request.Method.POST,
            url,
            checkBody,
            response -> {
                try {
                    boolean isSamePassword = response.getBoolean("isSame");
                    if (isSamePassword) {
                        Toast.makeText(ChangePassActivity.this, "New password must be different from current password", Toast.LENGTH_LONG).show();
                    } else {
                        // Proceed with password change
                        proceedWithPasswordChange(newPassword);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> handleError(error, "Failed to verify password")
        );
        
        requestQueue.add(checkRequest);
    }

    private void proceedWithPasswordChange(String newPassword) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/update-password/" + userId;

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("password", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        CustomJsonRequest request = new CustomJsonRequest(
            Request.Method.PATCH,
            url,
            requestBody,
            response -> {
                try {
                    String message = response.has("message") ? 
                        response.getString("message") : 
                        "Password updated successfully!";
                    Toast.makeText(ChangePassActivity.this, message, Toast.LENGTH_LONG).show();
                    changePasswordEdt.setText("");
                    confirmPasswordEdt.setText("");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> handleError(error, "Failed to update password")
        );
        
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000,
            0,
            1.0f
        ));
        
        requestQueue.add(request);
    }

    private void validateAndChangeEmail() {
        String newEmail = changeEmailEdt.getText().toString().trim();
        String confirmEmail = confirmEmailEdt.getText().toString().trim();

        if (newEmail.isEmpty() || confirmEmail.isEmpty()) {
            Toast.makeText(this, "Please fill in all email fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newEmail.equals(confirmEmail)) {
            Toast.makeText(this, "Emails do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(newEmail)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        changeEmail(userId, newEmail);
    }

    private void changeEmail(String userId, String newEmail) {
        // First, check if the email already exists
        String checkUrl = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/" + userId;

        JsonObjectRequest checkRequest = new JsonObjectRequest(
            Request.Method.GET,
            checkUrl,
            null,
            response -> {
                try {
                    // Get the current user's email to avoid self-comparison
                    String currentEmail = response.getString("email");
                    
                    // If the new email is the same as current email
                    if (currentEmail.equals(newEmail)) {
                        Toast.makeText(ChangePassActivity.this, 
                            "This is already your current email address", 
                            Toast.LENGTH_LONG).show();
                        changeEmailEdt.setText("");
                        confirmEmailEdt.setText("");
                        return;
                    }

                    // Check if the email exists for any other user
                    checkEmailExists(userId, newEmail);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ChangePassActivity.this, 
                        "Email has already been used", 
                        Toast.LENGTH_LONG).show();
                }
            },
            error -> handleError(error, "Email has already been used")
        );

        requestQueue.add(checkRequest);
    }

    private void checkEmailExists(String userId, String newEmail) {
        // Get current user's profile first to compare
        String profileUrl = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/" + userId;

        JsonObjectRequest profileRequest = new JsonObjectRequest(
            Request.Method.GET,
            profileUrl,
            null,
            response -> {
                try {
                    // Get the current user's email to avoid self-comparison
                    String currentEmail = response.getString("email");
                    
                    // If the new email is the same as current email
                    if (currentEmail.equals(newEmail)) {
                        Toast.makeText(ChangePassActivity.this, 
                            "This is already your current email address", 
                            Toast.LENGTH_LONG).show();
                        changeEmailEdt.setText("");
                        confirmEmailEdt.setText("");
                        return;
                    }

                    // If it's a different email, proceed with the change
                    proceedWithEmailChange(userId, newEmail);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ChangePassActivity.this, 
                        "Error checking email availability", 
                        Toast.LENGTH_LONG).show();
                }
            },
            error -> {
                Log.e("ChangePassActivity", "Error getting user profile: " + error.toString());
                handleError(error, "Error checking email availability");
            }
        );

        requestQueue.add(profileRequest);
    }

    private void proceedWithEmailChange(String userId, String newEmail) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/change-email/" + userId;

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", newEmail);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        CustomJsonRequest request = new CustomJsonRequest(
            Request.Method.POST, 
            url, 
            requestBody,
            response -> {
                try {
                    String message = response.has("message") ? 
                        response.getString("message") : 
                        "Email change initiated! Please check your new email for verification.";
                    Toast.makeText(ChangePassActivity.this, message, Toast.LENGTH_LONG).show();
                    changeEmailEdt.setText("");
                    confirmEmailEdt.setText("");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ChangePassActivity.this, "Error processing response", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                if (error.networkResponse != null) {
                    try {
                        String errorData = new String(error.networkResponse.data, "UTF-8");
                        JSONObject errorJson = new JSONObject(errorData);
                        String errorMessage = errorJson.optString("message", "Failed to change email");
                        Toast.makeText(ChangePassActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ChangePassActivity.this, "This email address is already in use", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ChangePassActivity.this, "Failed to change email", Toast.LENGTH_LONG).show();
                }
                changeEmailEdt.setText("");
                confirmEmailEdt.setText("");
            }
        );

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000,
            0,
            1.0f
        ));

        requestQueue.add(request);
    }

    private void deleteAccount(String userId) {
        // First check if user is admin
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/" + userId;
        JsonObjectRequest roleCheckRequest = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    int userRole = response.getInt("role");
                    if (userRole == 1) {  // Admin role
                        Toast.makeText(ChangePassActivity.this, 
                            "Admin accounts cannot be deleted", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        // Show delete confirmation dialog for non-admin users
                        showDeleteConfirmationDialog(userId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(ChangePassActivity.this, 
                        "Error checking user role", 
                        Toast.LENGTH_SHORT).show();
                }
            },
            error -> handleError(error, "Error checking user role")
        );
        requestQueue.add(roleCheckRequest);
    }

    private void showDeleteConfirmationDialog(String userId) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Account")
               .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
               .setPositiveButton("Delete", (dialog, which) -> {
                    performDeleteAccount(userId);
               })
               .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
               })
               .show();
    }

    private void performDeleteAccount(String userId) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/delete/" + userId;

        CustomJsonRequest request = new CustomJsonRequest(
            Request.Method.DELETE, 
            url, 
            null,
            response -> {
                Toast.makeText(ChangePassActivity.this, "Account deleted successfully!", Toast.LENGTH_LONG).show();
                // Clear shared preferences
                SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
                
                // Navigate to login screen
                Intent intent = new Intent(ChangePassActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            },
            error -> handleError(error, "Failed to delete account")
        );
        
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000,  // 30 seconds timeout
            0,      // no retries
            1.0f    // no backoff multiplier
        ));
        
        requestQueue.add(request);
    }

    private void handleError(VolleyError error, String defaultMessage) {
        String errorMessage = defaultMessage;
        if (error.networkResponse != null) {
            try {
                String errorData = new String(error.networkResponse.data, "UTF-8");
                if (errorData.trim().startsWith("{")) {
                    JSONObject errorJson = new JSONObject(errorData);
                    if (errorJson.has("message")) {
                        errorMessage = errorJson.getString("message");
                    }
                } else {
                    errorMessage = errorData.trim();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(ChangePassActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    private class CustomJsonRequest extends JsonObjectRequest {
        public CustomJsonRequest(int method, String url, JSONObject jsonRequest,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                
                JSONObject result;
                if (jsonString.trim().startsWith("{")) {
                    result = new JSONObject(jsonString);
                } else {
                    result = new JSONObject();
                    result.put("message", jsonString.trim());
                }
                
                return Response.success(result, HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new ParseError(e));
            }
        }
    }
}
