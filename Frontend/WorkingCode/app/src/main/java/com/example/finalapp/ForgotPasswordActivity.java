package com.example.finalapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity to handle the forgot password process by sending a password reset request to the server.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private ProgressDialog loadingDialog;

    // Endpoint URL for password reset
    private static final String RESET_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/reset-password-request";

    /**
     * Initializes the activity and sets up UI components.
     *
     * @param savedInstanceState The saved instance state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Sending reset link...");
        loadingDialog.setCancelable(false);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    /**
     * Validates the email input and sends a password reset request to the server.
     */
    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            return;
        }

        loadingDialog.show();
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (Exception e) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomJsonRequest request = new CustomJsonRequest(
                Request.Method.POST,
                RESET_URL,
                requestBody,
                response -> {
                    loadingDialog.dismiss();
                    String message = "Password reset instructions sent to your email";
                    try {
                        if (response.has("message")) {
                            message = response.getString("message");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ForgotPasswordActivity.this, VerifyCodeActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                },
                error -> {
                    loadingDialog.dismiss();
                    handleError(error);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,  // Timeout duration in milliseconds.
                0,      // Number of retry attempts.
                1.0f    // Backoff multiplier.
        ));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Handles errors returned by the server or network during the password reset request.
     *
     * @param error The error object containing details about the failure.
     */
    private void handleError(VolleyError error) {
        String errorMessage = "Failed to send reset link";
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
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Validates the format of an email address.
     *
     * @param email The email address to validate.
     * @return True if the email is valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Custom implementation of JsonObjectRequest to handle specific server responses.
     */
    private class CustomJsonRequest extends JsonObjectRequest {

        /**
         * Constructs a new CustomJsonRequest.
         *
         * @param method        The HTTP method to use (GET, POST, etc.).
         * @param url           The URL of the server endpoint.
         * @param jsonRequest   The JSON body of the request.
         * @param listener      The listener for successful responses.
         * @param errorListener The listener for errors.
         */
        public CustomJsonRequest(int method, String url, JSONObject jsonRequest,
                                 Response.Listener<JSONObject> listener,
                                 Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
        }

        /**
         * Parses the network response into a JSON object.
         *
         * @param response The raw network response.
         * @return A successful response containing a JSON object or an error.
         */
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
