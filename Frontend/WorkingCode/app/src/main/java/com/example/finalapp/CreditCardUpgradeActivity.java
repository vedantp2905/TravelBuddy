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

public class CreditCardUpgradeActivity extends AppCompatActivity {
    private PaymentSheet paymentSheet;
    private Button upgradeButton;
    private String userId;
    private String clientSecret;
    private RadioGroup planRadioGroup;
    private String selectedPlan = "monthly";
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_card_upgrade);

        userId = getUserId();
        int userRole = getUserRole();

        if (userId.equals("-1") || userRole == 3) {
            Toast.makeText(this, "You are already a premium user!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        requestQueue = Volley.newRequestQueue(this);
        
        PaymentConfiguration.init(getApplicationContext(), "pk_test_51QBOcp05ijGEGObCtM7k9AeZnPHYDYmiwaZ8CPijjGqafuzkLq3cfgyIwczl315SbtSALzd9lFag8A6CPWqbct5E00UVUnwCMs");
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        setupViews();
    }

    private void setupViews() {
        upgradeButton = findViewById(R.id.upgradeButton);
        planRadioGroup = findViewById(R.id.planRadioGroup);
        userId = getUserId();

        planRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.monthlyPlanRadio) {
                selectedPlan = "monthly";
            } else if (checkedId == R.id.annualPlanRadio) {
                selectedPlan = "annual";
            }
        });

        upgradeButton.setOnClickListener(v -> startUpgradeProcess());
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
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + 
                    "/upgrade-to-premium?plan=" + selectedPlan + 
                    "&paymentMethod=card";

        JSONObject jsonObject = new JSONObject();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    try {
                        clientSecret = response.getString("clientSecret");
                        presentPaymentSheet(clientSecret);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "Network error";
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
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void presentPaymentSheet(String clientSecret) {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Travel Buddy")
                .build();
        paymentSheet.presentWithPaymentIntent(clientSecret, configuration);
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentResult) {
        if (paymentResult instanceof PaymentSheetResult.Completed) {
            confirmUpgrade();
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_LONG).show();
        } else if (paymentResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment canceled!", Toast.LENGTH_LONG).show();
        } else if (paymentResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, "Payment failed: " + ((PaymentSheetResult.Failed) paymentResult).getError(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmUpgrade() {
        String[] parts = clientSecret.split("_secret_");
        String extractedValue = parts[0];
        
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("paymentIntentId", extractedValue);
            jsonObject.put("plan", selectedPlan);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/confirm-premium-upgrade";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    try {
                        String message = response.getString("message");
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        createPremiumSubscription();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show());

        requestQueue.add(jsonObjectRequest);
    }

    private void createPremiumSubscription() {
        updateUserRole();
        Intent intent = new Intent(CreditCardUpgradeActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void updateUserRole() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("role", 3);  // Set role to premium (3)
        editor.apply();
        
        Intent intent = new Intent(CreditCardUpgradeActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
} 