package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TriviaResultsActivity extends AppCompatActivity {
    private TextView tvWinner1, tvWinner2, tvWinner3;
    private Button btnBackToHome;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trivia_results);

        tvWinner1 = findViewById(R.id.tvWinner1);
        tvWinner2 = findViewById(R.id.tvWinner2);
        tvWinner3 = findViewById(R.id.tvWinner3);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        requestQueue = Volley.newRequestQueue(this);

        String winnersJson = getIntent().getStringExtra("winners");
        Log.d("TriviaResults", "Received winners JSON: " + winnersJson);

        try {
            JSONArray winners = new JSONArray(winnersJson);
            displayWinners(winners);
        } catch (JSONException e) {
            Log.e("TriviaResults", "Error parsing winners JSON", e);
            e.printStackTrace();
        }

        btnBackToHome.setOnClickListener(v -> goToHome());
        cleanupConnections();
    }

    private void displayWinners(JSONArray winners) throws JSONException {
        for (int i = 0; i < Math.min(winners.length(), 3); i++) {
            JSONObject winner = winners.getJSONObject(i);
            long userId = winner.getLong("userId");
            int score = winner.getInt("score");
            fetchAndDisplayUsername(userId, score, i + 1);
        }
    }

    private void fetchAndDisplayUsername(long userId, int score, int rank) {
        String url = "http://coms-3090-010.class.las.iastate.edu:8080/api/users/" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    String username = response.getString("username");
                    displayWinnerInfo(username, score, rank);
                } catch (JSONException e) {
                    Log.e("TriviaResults", "Error parsing user response", e);
                    displayWinnerInfo("Player " + rank, score, rank);
                }
            },
            error -> {
                Log.e("TriviaResults", "Error fetching username for userId: " + userId, error);
                displayWinnerInfo("Player " + rank, score, rank);
            }
        );
        
        requestQueue.add(request);
    }

    private void displayWinnerInfo(String username, int score, int rank) {
        TextView winnerView;
        String medal;
        switch (rank) {
            case 1:
                winnerView = tvWinner1;
                medal = "🥇";
                break;
            case 2:
                winnerView = tvWinner2;
                medal = "🥈";
                break;
            case 3:
                winnerView = tvWinner3;
                medal = "🥉";
                break;
            default:
                return;
        }
        
        winnerView.setText(String.format("%s %s - %d points", medal, username, score));
        winnerView.setVisibility(View.VISIBLE);
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void cleanupConnections() {
        if (TriviaRoomActivity.activeStompClient != null) {
            TriviaRoomActivity.activeStompClient.disconnect();
            TriviaRoomActivity.activeStompClient = null;
        }
    }

    @Override
    public void onBackPressed() {
        goToHome();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupConnections();
    }
} 