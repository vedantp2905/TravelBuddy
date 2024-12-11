package com.example.finalapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class ImageViewActivity extends AppCompatActivity {
    private ImageView fullscreenImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        fullscreenImage = findViewById(R.id.fullscreen_image);
        String imageUrl = getIntent().getStringExtra("imageUrl");

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .into(fullscreenImage);
        }

        // Set system UI flags for fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        fullscreenImage.setOnClickListener(v -> finish());
    }
}
