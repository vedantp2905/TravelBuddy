package com.example.finalapp;

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
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnForgotPassword;
    private ProgressDialog loadingDialog;
    private static final String LOGIN_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Logging in...");
        loadingDialog.setCancelable(false);

        btnLogin.setOnClickListener(v -> loginUser());
        btnForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        TextView tvSignUp = findViewById(R.id.tvSignUp);



        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request data", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(Request.Method.POST, LOGIN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        loadingDialog.dismiss();
                        Log.d("Login Response", response);

                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String message = jsonResponse.getString("message");

                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                            if (message.equalsIgnoreCase("Login successful")) {
                                // Get user details from response
                                int userId = jsonResponse.getInt("userId");
                                int role = jsonResponse.getInt("role");  // Retrieve the role

                                // Store userId and role for global use using SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("userId", userId);  // Save userId
                                editor.putInt("role", role);      // Save role
                                editor.apply();

                                // Check if user has a profile
                                checkUserProfile(userId);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loadingDialog.dismiss();
                String errorMessage = "Unknown error";

                if (error.networkResponse != null) {
                    int statusCode = error.networkResponse.statusCode;
                    Log.e("Login Error", "Status code: " + statusCode);

                    try {
                        errorMessage = new String(error.networkResponse.data, "UTF-8");
                        Log.e("Login Error", "Response body: " + errorMessage);
                    } catch (UnsupportedEncodingException e) {
                        Log.e("Login Error", "Encoding error: " + e.getMessage());
                    }
                } else {
                    Log.e("Login Error", "Error: " + error.toString());
                }

                Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return jsonBody.toString().getBytes();
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                20000,  // 20 seconds timeout
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void checkUserProfile(int userId) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/profile/" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    // If profile is empty or doesn't exist, start with AboutYouProfile
                    if (response.length() == 0 || !response.has("aboutMe")) {
                        Intent intent = new Intent(LoginActivity.this, AboutYouProfile.class);
                        intent.putExtra("isFirstLogin", true);
                        startActivity(intent);
                        finish();
                    } else {
                        // Profile exists, go to home
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    }
                },
                error -> {
                    // If error (profile doesn't exist), start with AboutYouProfile
                    Intent intent = new Intent(LoginActivity.this, AboutYouProfile.class);
                    intent.putExtra("isFirstLogin", true);
                    startActivity(intent);
                    finish();
                });
        
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}
