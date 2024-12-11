package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.dto.StompHeader;

import java.util.ArrayList;
import java.util.List;



public class TriviaGameActivity extends AppCompatActivity {
    private TextView tvQuestion;
    private TextView tvTimer;
    private LinearLayout optionsContainer;
    private StompClient stompClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private CountDownTimer countDownTimer;
    private String roomCode;
    private String userId;
    private long timeRemaining;
    private View loadingContainer;
    private View gameContainer;
    private TextView tvLoadingMessage;

    public static StompClient activeStompClient;

    private int currentQuestionNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trivia_game);

        loadingContainer = findViewById(R.id.loadingContainer);
        gameContainer = findViewById(R.id.gameContainer);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvTimer = findViewById(R.id.tvTimer);
        optionsContainer = findViewById(R.id.optionsContainer);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);

        // Show loading initially with waiting message
        loadingContainer.setVisibility(View.VISIBLE);
        gameContainer.setVisibility(View.GONE);
        tvLoadingMessage.setText("Generating questions and getting ready...");

        roomCode = getIntent().getStringExtra("roomCode");
        userId = getIntent().getStringExtra("userId");

        setupWebSocket();
    }

    private void setupWebSocket() {
        if (TriviaRoomActivity.activeStompClient != null) {
            stompClient = TriviaRoomActivity.activeStompClient;
            // Subscribe to game messages
            compositeDisposable.add(stompClient.topic("/topic/trivia/room/" + roomCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.d("STOMP", "Received game message: " + topicMessage.getPayload());
                    JSONObject message = new JSONObject(topicMessage.getPayload());
                    handleGameMessage(message);
                }, throwable -> {
                    Log.e("STOMP", "Error on subscription", throwable);
                }));
        }
    }

    private void handleGameMessage(JSONObject message) {
        try {
            String type = message.getString("type");
            Log.d("STOMP", "Received message type: " + type);
            
            switch (type) {
                case "GAME_STARTED":
                    hasAnswered = false;
                    runOnUiThread(() -> {
                        loadingContainer.setVisibility(View.VISIBLE);
                        gameContainer.setVisibility(View.GONE);
                        tvLoadingMessage.setText("Game is starting...");
                        tvLoadingMessage.setTextSize(28);
                        tvLoadingMessage.setTextColor(getResources().getColor(android.R.color.black));
                    });
                    break;
                    
                case "NEXT_QUESTION":
                    hasAnswered = false;
                    runOnUiThread(() -> {
                        // Show transition screen for 1 second before showing question
                        loadingContainer.setVisibility(View.VISIBLE);
                        gameContainer.setVisibility(View.GONE);
                        tvLoadingMessage.setText("Get Ready! Next Question Coming Up!");
                        tvLoadingMessage.setTextSize(28);
                        tvLoadingMessage.setTextColor(getResources().getColor(android.R.color.black));
                        
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            loadingContainer.setVisibility(View.GONE);
                            gameContainer.setVisibility(View.VISIBLE);
                            try {
                                setupQuestion(message);
                            } catch (Exception e) {
                                Log.e("STOMP", "Error setting up question", e);
                            }
                        }, 1000); // 1 second delay
                    });
                    break;
                    
                case "WAITING":
                    int answered = message.getInt("answered");
                    int total = message.getInt("total");
                    
                    runOnUiThread(() -> {
                        if (hasAnswered) {
                            loadingContainer.setVisibility(View.VISIBLE);
                            gameContainer.setVisibility(View.GONE);
                            tvLoadingMessage.setText("Waiting for other players... " + answered + "/" + total);
                        } else {
                            loadingContainer.setVisibility(View.GONE);
                            gameContainer.setVisibility(View.VISIBLE);
                        }
                    });
                    break;
                    
                case "TRANSITION":
                    runOnUiThread(() -> {
                        loadingContainer.setVisibility(View.VISIBLE);
                        gameContainer.setVisibility(View.GONE);
                        tvLoadingMessage.setText("Next Question Coming Up!");
                        tvLoadingMessage.setTextSize(28);
                        tvLoadingMessage.setTextColor(getResources().getColor(android.R.color.black));
                    });
                    break;
                    
                case "GAME_OVER":
                    JSONArray winners = message.getJSONArray("winners");
                    showGameResults(winners);
                    break;
            }
        } catch (JSONException e) {
            Log.e("STOMP", "Error handling game message", e);
        }
    }

    private void startTimer(int timeLimit) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        timeRemaining = timeLimit;
        countDownTimer = new CountDownTimer(timeLimit * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished / 1000;
                runOnUiThread(() -> {
                    // Just show the number, no text
                    tvTimer.setText(String.valueOf(timeRemaining));
                });
            }

            @Override
            public void onFinish() {
                timeRemaining = 0;
                tvTimer.setText("0");
                handleTimeout();
            }
        }.start();
    }

    private boolean hasAnswered = false;

    private void submitAnswer(String answer) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (hasAnswered) {
            return; // Prevent multiple submissions
        }
        
        hasAnswered = true;
        
        try {
            JSONObject request = new JSONObject();
            request.put("userId", userId);
            request.put("answer", answer != null ? answer : "X"); // Use X for timeouts
            request.put("timeRemaining", timeRemaining);
            request.put("timedOut", answer == null);

            Log.d("STOMP", "Submitting answer: " + request.toString());
            stompClient.send("/app/trivia/answer/" + roomCode, request.toString()).subscribe();
        } catch (JSONException e) {
            Log.e("STOMP", "Error submitting answer", e);
        }
    }

    private void showGameResults(JSONArray winners) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        // Cleanup STOMP client before transitioning
        if (stompClient != null) {
            stompClient.disconnect();
            stompClient = null;
        }
        
        // Clear the static reference
        TriviaRoomActivity.activeStompClient = null;
        
        // Ensure we're not already transitioning
        if (!isFinishing()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, TriviaResultsActivity.class);
                intent.putExtra("winners", winners.toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isFinishing() && stompClient != null && stompClient.isConnected()) {
            try {
                JSONObject leaveMessage = new JSONObject();
                leaveMessage.put("userId", userId);
                
                // Send leave message and wait for confirmation before disconnecting
                stompClient.send("/app/trivia/leave/" + roomCode, leaveMessage.toString())
                    .subscribe(
                        () -> {
                            Log.d("TriviaGame", "Leave message sent successfully in onDestroy");
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            if (stompClient != null) {
                                stompClient.disconnect();
                                stompClient = null;
                            }
                        },
                        error -> {
                            Log.e("TriviaGame", "Error sending leave message in onDestroy", error);
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            if (stompClient != null) {
                                stompClient.disconnect();
                                stompClient = null;
                            }
                        }
                    );
            } catch (JSONException e) {
                Log.e("TriviaGame", "Error creating leave message in onDestroy", e);
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                if (stompClient != null) {
                    stompClient.disconnect();
                    stompClient = null;
                }
            }
        } else {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            if (stompClient != null) {
                stompClient.disconnect();
                stompClient = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!isFinishing() && stompClient != null && stompClient.isConnected()) {
            try {
                JSONObject leaveMessage = new JSONObject();
                leaveMessage.put("userId", userId);
                
                // Send leave message and wait for confirmation before closing
                stompClient.send("/app/trivia/leave/" + roomCode, leaveMessage.toString())
                    .subscribe(
                        () -> {
                            Log.d("TriviaGame", "Leave message sent successfully");
                            runOnUiThread(() -> {
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                }
                                if (stompClient != null) {
                                    stompClient.disconnect();
                                    stompClient = null;
                                }
                                // Navigate to TriviaActivity instead of just going back
                                Intent intent = new Intent(this, TriviaActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        },
                        error -> {
                            Log.e("TriviaGame", "Error sending leave message", error);
                            runOnUiThread(() -> {
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                }
                                if (stompClient != null) {
                                    stompClient.disconnect();
                                    stompClient = null;
                                }
                                // Navigate to TriviaActivity on error as well
                                Intent intent = new Intent(this, TriviaActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        }
                    );
            } catch (JSONException e) {
                Log.e("TriviaGame", "Error creating leave message", e);
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                if (stompClient != null) {
                    stompClient.disconnect();
                    stompClient = null;
                }
                // Navigate to TriviaActivity on exception
                Intent intent = new Intent(this, TriviaActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        } else {
            // Navigate to TriviaActivity if no cleanup needed
            Intent intent = new Intent(this, TriviaActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setupQuestion(JSONObject questionData) {
        try {
            // Reset state for new question
            hasAnswered = false;
            
            // Set question text
            tvQuestion.setText(questionData.getString("question"));
            
            // Clear previous options
            optionsContainer.removeAllViews();
            
            // Add option buttons
            JSONArray options = questionData.getJSONArray("options");
            int timeLimit = questionData.getInt("timeLimit");
            
            for (int i = 0; i < options.length(); i++) {
                Button optionButton = new Button(this);
                String optionText = options.getString(i);
                optionButton.setText((char)('A' + i) + ". " + optionText);
                
                // Set button styling
                optionButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                
                // Add margin bottom to each button except the last one
                if (i < options.length() - 1) {
                    ((LinearLayout.LayoutParams) optionButton.getLayoutParams()).bottomMargin = 
                        (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                }
                
                // Set different background colors for each option
                switch(i) {
                    case 0:
                        optionButton.setBackgroundColor(getResources().getColor(R.color.option_color_1));
                        break;
                    case 1:
                        optionButton.setBackgroundColor(getResources().getColor(R.color.option_color_2));
                        break;
                    case 2:
                        optionButton.setBackgroundColor(getResources().getColor(R.color.option_color_3));
                        break;
                    case 3:
                        optionButton.setBackgroundColor(getResources().getColor(R.color.option_color_4));
                        break;
                }
                
                optionButton.setTextColor(getResources().getColor(android.R.color.black));
                optionButton.setTextSize(18);
                optionButton.setPadding(32, 24, 32, 24);
                
                final String answer = String.valueOf((char)('A' + i));
                optionButton.setOnClickListener(v -> {
                    if (!hasAnswered) {
                        hasAnswered = true;
                        try {
                            JSONObject request = new JSONObject();
                            request.put("userId", userId);
                            request.put("answer", answer);
                            request.put("timeRemaining", timeRemaining);
                            request.put("timedOut", false);
                            
                            Log.d("STOMP", "Sending answer: " + answer + " for user: " + userId);
                            stompClient.send("/app/trivia/answer/" + roomCode, request.toString())
                                .subscribe(() -> {
                                    Log.d("STOMP", "Answer sent successfully");
                                }, throwable -> {
                                    Log.e("STOMP", "Error sending answer", throwable);
                                });
                        } catch (JSONException e) {
                            Log.e("STOMP", "Error creating answer request", e);
                        }
                        disableAllOptions();
                    }
                });
                
                optionsContainer.addView(optionButton);
            }
            
            // Start the timer
            startTimer(timeLimit);
            
        } catch (JSONException e) {
            Log.e("STOMP", "Error setting up question", e);
        }
    }

    private void handleTimeout() {
        if (!hasAnswered) {
            hasAnswered = true;
            disableAllOptions();
            
            try {
                JSONObject request = new JSONObject();
                request.put("userId", userId);
                request.put("answer", "X"); // Send "X" instead of null for timeouts
                request.put("timeRemaining", 0);
                request.put("timedOut", true);
                
                stompClient.send("/app/trivia/answer/" + roomCode, request.toString()).subscribe();
            } catch (JSONException e) {
                Log.e("STOMP", "Error submitting timeout", e);
            }
        }
    }

    private void disableAllOptions() {
        runOnUiThread(() -> {
            // Disable all buttons in the options container
            for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                View child = optionsContainer.getChildAt(i);
                if (child instanceof Button) {
                    child.setEnabled(false);
                }
            }
        });
    }

    private void displayQuestion(JSONObject questionData) {
        try {
            // Increment question number before displaying
            currentQuestionNumber++;
            
            // Update question number display
            TextView tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
            tvQuestionNumber.setText(String.format("Question %d/10", currentQuestionNumber));
            
            String questionText = questionData.getString("question");
            JSONArray options = questionData.getJSONArray("options");
            
            // Set question text
            tvQuestion.setText(questionText);

            // Clear previous options
            optionsContainer.removeAllViews();
            
            // Add new options
            for (int i = 0; i < options.length(); i++) {
                Button optionButton = new Button(this);
                optionButton.setText(options.getString(i));
                // ... rest of your option button setup code ...
            }
            
            int timeLimit = questionData.optInt("timeLimit", 10); // Default 10 seconds if not specified
            startTimer(timeLimit);
        } catch (JSONException e) {
            Log.e("TriviaGame", "Error displaying question", e);
        }
    }
} 