package com.example.finalapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private Button btnGoToSignUp, btnGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        btnGoToSignUp = findViewById(R.id.btnGoToSignUp);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        // Sign Up button click
        btnGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SignupActivity.class));
        });

        // Login button click
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });
    }
}
