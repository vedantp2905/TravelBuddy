package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

public class SignupActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etEmail, etPassword, etAge, etGender, confirmPasswordEdt;
    private Button btnSignUp;
    private static final String SIGNUP_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/signup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize the fields
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etAge = findViewById(R.id.etAge);
        etGender = findViewById(R.id.etGender);
        btnSignUp = findViewById(R.id.btnSignUp);
        confirmPasswordEdt = findViewById(R.id.confirmPasswordEdt);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String confirmPassword = confirmPasswordEdt.getText().toString().trim();

        // Validate input
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || age.isEmpty() || gender.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password length
        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare JSON object
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("firstName", firstName);
            jsonBody.put("lastName", lastName);
            jsonBody.put("username", username);
            jsonBody.put("age", Integer.parseInt(age));
            jsonBody.put("gender", gender);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            // Handle JSON error (rare case since input is validated)
            Toast.makeText(this, "Error creating request data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create JSON request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SIGNUP_URL, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Check for a successful response
                        if (response.has("message")) {
                            String message = response.optString("message", "Account created successfully.");
                            Toast.makeText(SignupActivity.this, message + ". Please verify your email.", Toast.LENGTH_LONG).show();

                            // Redirect to login after signup
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish(); // Optionally close the SignupActivity
                        } else {
                            // Handle unexpected response gracefully
                            Toast.makeText(SignupActivity.this, "Account creation failed. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Handle error response
                if (error.networkResponse != null) {
                    String errorMessage = new String(error.networkResponse.data);
                    Toast.makeText(SignupActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignupActivity.this, "Account created, please verify your email.", Toast.LENGTH_SHORT).show();

                    // Use Handler to delay the redirect
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        }
                    }, 3000);
                }
            }
        });

        // Add request to the request queue
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    // Method to validate email using a regular expression
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }
}
