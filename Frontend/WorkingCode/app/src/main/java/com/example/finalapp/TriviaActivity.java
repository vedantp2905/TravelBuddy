package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import android.app.ProgressDialog;
import android.util.Log;

public class TriviaActivity extends AppCompatActivity {
    private EditText[] codeBoxes = new EditText[4];
    private CardView createRoomCard;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trivia);

        // Initialize views
        createRoomCard = findViewById(R.id.createRoomCard);

        // Initialize code boxes
        codeBoxes[0] = findViewById(R.id.code1);
        codeBoxes[1] = findViewById(R.id.code2);
        codeBoxes[2] = findViewById(R.id.code3);
        codeBoxes[3] = findViewById(R.id.code4);

        // Get userId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        int userIdInt = prefs.getInt("userId", -1);
        userId = String.valueOf(userIdInt);

        setupCodeBoxes();
        
        createRoomCard.setOnClickListener(v -> createRoom());
    }

    private void setupCodeBoxes() {
        for (int i = 0; i < codeBoxes.length; i++) {
            final int currentIndex = i;
            codeBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        if (currentIndex < codeBoxes.length - 1) {
                            codeBoxes[currentIndex + 1].requestFocus();
                        } else if (currentIndex == codeBoxes.length - 1) {
                            // This is the last box and a digit was entered
                            joinRoom();
                        }
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

    private void joinRoom() {
        StringBuilder roomCode = new StringBuilder();
        for (EditText codeBox : codeBoxes) {
            String digit = codeBox.getText().toString().trim();
            if (digit.isEmpty()) {
                Toast.makeText(this, "Please enter complete room code", Toast.LENGTH_SHORT).show();
                return;
            }
            roomCode.append(digit);
        }

        String finalRoomCode = roomCode.toString();
        Log.d("TriviaActivity", "Attempting to join room with code: " + finalRoomCode);
        
        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Checking room...");
        progressDialog.show();

        // Check if room exists first
        new Thread(() -> {
            try {
                String urlString = getString(R.string.base_url) + "/api/trivia/room/" + finalRoomCode;
                Log.d("TriviaActivity", "Making request to: " + urlString);
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                Log.d("TriviaActivity", "Response code: " + responseCode);
                
                // Read the response body for error cases
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Log.e("TriviaActivity", "Error response: " + response.toString());
                }
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Room exists, proceed to join via WebSocket
                        Intent intent = new Intent(this, TriviaRoomActivity.class);
                        intent.putExtra("isHost", false);
                        intent.putExtra("userId", userId);
                        intent.putExtra("roomCode", finalRoomCode);
                        startActivityForResult(intent, 1);
                    } else {
                        // Room doesn't exist
                        Toast.makeText(this, "Room does not exist", Toast.LENGTH_SHORT).show();
                        // Clear the code boxes
                        for (EditText codeBox : codeBoxes) {
                            codeBox.setText("");
                        }
                        codeBoxes[0].requestFocus();
                    }
                });
            } catch (Exception e) {
                Log.e("TriviaActivity", "Network error", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error checking room: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_CANCELED) {
            // Room doesn't exist or error occurred
            String error = data.getStringExtra("error");
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            // Clear the code boxes
            for (EditText codeBox : codeBoxes) {
                codeBox.setText("");
            }
            codeBoxes[0].requestFocus();
        }
    }

    private void createRoom() {
        Intent intent = new Intent(this, TriviaRoomActivity.class);
        intent.putExtra("isHost", true);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
} 