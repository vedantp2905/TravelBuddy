package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import org.json.JSONException;
import org.json.JSONObject;

public class UpgradeActivity extends AppCompatActivity {
    private PaymentSheet paymentSheet;
    private Button upgradeButton;
    private String userId;
    private String clientSecret;  // Declare clientSecret as a global variable
    private ImageView logo;
    private RadioGroup planRadioGroup;
    private String selectedPlan = "monthly"; // Default plan
    private TextView rewardsBalanceText;
    private RadioGroup paymentMethodGroup;
    private double rewardsBalance = 0.0;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        // Initialize RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        // Get userId and check role
        userId = getUserId();
        int userRole = getUserRole();

        if (userId.equals("-1") || userRole == 3) {
            Toast.makeText(this, "You are already a premium user!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize Stripe SDK with your publishable key
        PaymentConfiguration.init(getApplicationContext(), "XXX"); // TODO: change this to your stripe publishable key
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
        logo = findViewById(R.id.logo);

        upgradeButton = findViewById(R.id.upgradeButton);

        // Initialize radio group
        planRadioGroup = findViewById(R.id.planRadioGroup);
        planRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.monthlyPlanRadio) {
                selectedPlan = "monthly";
            } else if (checkedId == R.id.annualPlanRadio) {
                selectedPlan = "annual";
            }
        });

        rewardsBalanceText = findViewById(R.id.rewardsBalanceText);
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);

        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startUpgradeProcess();
            }
        });

        fetchRewardsBalance(); // Fetch initial rewards balance

        androidx.cardview.widget.CardView creditCardUpgradeCard = findViewById(R.id.creditCardUpgradeCard);
        creditCardUpgradeCard.setOnClickListener(v -> {
            Intent intent = new Intent(UpgradeActivity.this, CreditCardUpgradeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return String.valueOf(sharedPreferences.getInt("userId", -1));
    }

    private int getUserRole() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("role", 1);
    }

    private void startUpgradeProcess() {
        if (paymentMethodGroup.getCheckedRadioButtonId() == R.id.rewardsPaymentRadio) {
            processRewardsUpgrade();
        } else {
            // Launch credit card upgrade activity
            Intent intent = new Intent(UpgradeActivity.this, CreditCardUpgradeActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void processRewardsUpgrade() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/reward/" + userId + "/use-for-premium";
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("plan", selectedPlan);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        String message = response.getString("message");
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        updateUserRole();
                        Intent intent = new Intent(UpgradeActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error processing upgrade", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "Error upgrading membership";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String errorData = new String(error.networkResponse.data, "UTF-8");
                            JSONObject errorJson = new JSONObject(errorData);
                            errorMessage = errorJson.getString("error");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
        );
        requestQueue.add(request);
    }

    private void presentPaymentSheet(String clientSecret) {
        // Configure the PaymentSheet
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Travel Buddy")
                .build();

        // Present the PaymentSheet with the client secret
        paymentSheet.presentWithPaymentIntent(clientSecret, configuration);
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentResult) {
        if (paymentResult instanceof PaymentSheetResult.Completed) {
            confirmUpgrade();
            updateUserRole();
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();
        } else if (paymentResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment canceled!", Toast.LENGTH_LONG).show();
        } else if (paymentResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, "Payment failed: " + ((PaymentSheetResult.Failed) paymentResult).getError(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmUpgrade() {
        String userId = getUserId();
        String selectedPlan = "monthly"; // Since we're removing the spinner

        // Split the string at '_secret_'
        String[] parts = clientSecret.split("_secret_");

        // The first part will contain 'pi_3NJcXXXXXXXXXXXX'
        String extractedValue = parts[0];
        // Build the JSON object for confirming the upgrade
        JSONObject jsonObject = new JSONObject();
        try {
            // No need for paymentIntentId, just send plan
            jsonObject.put("paymentIntentId", extractedValue);
            jsonObject.put("plan", selectedPlan);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // URL for confirming the upgrade
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/" + userId + "/confirm-premium-upgrade";

        // Create a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String message = response.getString("message");
                            Toast.makeText(UpgradeActivity.this, message, Toast.LENGTH_SHORT).show();
                            finish();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(UpgradeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(UpgradeActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Add the request to the queue
        requestQueue.add(jsonObjectRequest);
    }

    private void updateUserRole() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("role", 3);  // Set role to premium (3)
        editor.apply();
        
        // Return to HomeActivity
        Intent intent = new Intent(UpgradeActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void fetchRewardsBalance() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/reward/" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        rewardsBalance = response.getDouble("balance");
                        rewardsBalanceText.setText(String.format("Rewards Balance: %.0f points", rewardsBalance));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing rewards balance", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error fetching rewards balance", Toast.LENGTH_SHORT).show()
        );
        
        // Add retry policy
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000,  // 30 seconds timeout
            0,      // no retries
            1.0f    // no backoff multiplier
        ));
        
        requestQueue.add(request);
    }
}

