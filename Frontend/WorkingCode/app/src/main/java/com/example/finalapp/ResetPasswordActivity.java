package com.example.finalapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;

public class ResetPasswordActivity extends AppCompatActivity {
    private EditText etNewPassword, etConfirmPassword;
    private Button btnUpdatePassword;
    private ProgressDialog loadingDialog;
    private String email, code;
    private static final String RESET_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/reset-password-with-code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");
        code = getIntent().getStringExtra("code");
        
        if (email == null || code == null) {
            Toast.makeText(this, "Invalid reset attempt", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        // Style EditTexts to match existing theme
        etNewPassword.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.buttonBackground)));
        etConfirmPassword.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.buttonBackground)));
        
        btnUpdatePassword.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.buttonBackground)));
        btnUpdatePassword.setTextColor(getResources().getColor(R.color.buttonTextColor));

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("Updating password...");
        loadingDialog.setCancelable(false);
    }

    private void setupListeners() {
        btnUpdatePassword.setOnClickListener(v -> handlePasswordUpdate());
    }

    private void handlePasswordUpdate() {
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validation logic similar to ChangePassActivity
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

        updatePassword(newPassword);
    }

    private void updatePassword(String newPassword) {
        loadingDialog.show();
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("code", code);
            requestBody.put("password", newPassword);
        } catch (JSONException e) {
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
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
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
        String errorMessage = "Failed to update password";
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
}
