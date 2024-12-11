package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;

public class RewardsActivity extends AppCompatActivity {
    private TextView rewardsBalanceTextLarge;
    private Button btnUpgradePremium;
    private RequestQueue requestQueue;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        // Initialize views
        rewardsBalanceTextLarge = findViewById(R.id.rewardsBalanceTextLarge);
        btnUpgradePremium = findViewById(R.id.btnUpgradePremium);

        // Get userId from SharedPreferences
        userId = String.valueOf(getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                .getInt("userId", -1));

        requestQueue = Volley.newRequestQueue(this);

        // Set click listener for upgrade button
        btnUpgradePremium.setOnClickListener(v -> {
            Intent intent = new Intent(RewardsActivity.this, UpgradeActivity.class);
            startActivity(intent);
        });

        // Fetch rewards balance
        fetchRewardsBalance();
    }

    private void fetchRewardsBalance() {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/reward/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double rewardsBalance = response.getDouble("balance");
                        rewardsBalanceTextLarge.setText(String.format("%.0f points", rewardsBalance));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing rewards balance", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error fetching rewards balance", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRewardsBalance();
    }
} 