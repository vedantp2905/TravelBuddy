package com.example.finalapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class VerifyCodeActivity extends AppCompatActivity {
    private EditText[] codeBoxes = new EditText[6];
    private Button btnVerifyCode;
    private TextView tvResendCode;
    private ProgressDialog loadingDialog;
    private String email;
    private static final String VERIFY_URL = ApiConstants.BASE_URL + "/api/users/verify-reset-code";
    private static final String RESEND_URL = ApiConstants.BASE_URL + "/api/users/reset-password-request";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        email = getIntent().getStringExtra("email");
        if (email == null) {
            Toast.makeText(this, "Email not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupCodeBoxes();
        setupButtons();
        
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Verifying code...");
        loadingDialog.setCancelable(false);
    }

    private void initializeViews() {
        codeBoxes[0] = findViewById(R.id.code1);
        codeBoxes[1] = findViewById(R.id.code2);
        codeBoxes[2] = findViewById(R.id.code3);
        codeBoxes[3] = findViewById(R.id.code4);
        codeBoxes[4] = findViewById(R.id.code5);
        codeBoxes[5] = findViewById(R.id.code6);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        tvResendCode = findViewById(R.id.tvResendCode);
    }

    private void setupCodeBoxes() {
        for (int i = 0; i < codeBoxes.length; i++) {
            final int currentIndex = i;
            codeBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < codeBoxes.length - 1) {
                        codeBoxes[currentIndex + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace
            codeBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && 
                    event.getAction() == KeyEvent.ACTION_DOWN && 
                    currentIndex > 0 && 
                    codeBoxes[currentIndex].getText().toString().isEmpty()) {
                    
                    codeBoxes[currentIndex - 1].requestFocus();
                    codeBoxes[currentIndex - 1].setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private void setupButtons() {
        btnVerifyCode.setOnClickListener(v -> verifyCode());
        tvResendCode.setOnClickListener(v -> resendCode());
    }

    private void verifyCode() {
        StringBuilder code = new StringBuilder();
        for (EditText codeBox : codeBoxes) {
            String digit = codeBox.getText().toString().trim();
            if (digit.isEmpty()) {
                Toast.makeText(this, "Please enter complete code", Toast.LENGTH_SHORT).show();
                return;
            }
            code.append(digit);
        }

        // Ensure code is exactly 6 digits
        String finalCode = code.toString();
        if (finalCode.length() != 6 || !finalCode.matches("\\d{6}")) {
            Toast.makeText(this, "Invalid code format", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email.trim());
            requestBody.put("code", finalCode);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomJsonRequest request = new CustomJsonRequest(
            Request.Method.POST,
            VERIFY_URL,
            requestBody,
            response -> {
                loadingDialog.dismiss();
                Intent intent = new Intent(VerifyCodeActivity.this, ResetPasswordActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("code", finalCode);
                startActivity(intent);
            },
            error -> {
                loadingDialog.dismiss();
                handleError(error);
            }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, 1.0f));
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void resendCode() {
        loadingDialog.setMessage("Resending code...");
        loadingDialog.show();
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (JSONException e) {
            loadingDialog.dismiss();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        CustomJsonRequest request = new CustomJsonRequest(
            Request.Method.POST,
            RESEND_URL,
            requestBody,
            response -> {
                loadingDialog.dismiss();
                Toast.makeText(this, "New code sent to your email", Toast.LENGTH_LONG).show();
                // Clear existing code
                for (EditText codeBox : codeBoxes) {
                    codeBox.setText("");
                }
                codeBoxes[0].requestFocus();
            },
            error -> {
                loadingDialog.dismiss();
                handleError(error);
            }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, 1.0f));
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void handleError(VolleyError error) {
        String errorMessage = "Verification failed";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String errorData = new String(error.networkResponse.data, "UTF-8");
                System.out.println("Error Data: " + errorData); // Debug log
                System.out.println("Status Code: " + error.networkResponse.statusCode); // Debug log
                
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
                System.out.println("Error parsing response: " + e.getMessage()); // Debug log
            }
        } else {
            System.out.println("Network Response: null"); // Debug log
            System.out.println("Error: " + error.toString()); // Debug log
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
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
