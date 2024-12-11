package com.example.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class CommentActivity extends AppCompatActivity {
    private static final String TAG = "CommentActivity";
    private ListView commentListView;
    private EditText commentEditText;
    private Button addCommentButton;
    private CommentAdapter commentAdapter;
    private TravelApiService apiService;
    private Long postId;
    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_feed);
        Log.d(TAG, "CommentActivity onCreate");

        // Initialize back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Get data from intent
        getIntentData();

        // Validate postId and userId
        if (postId == -1 || userId == -1) {
            Toast.makeText(this, "Invalid Post or User ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Initialize API service
        apiService = new TravelApiService(this);

        // Load comments
        loadComments();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        commentListView = findViewById(R.id.comment_list_view);
        commentEditText = findViewById(R.id.comment_edit_text);
        addCommentButton = findViewById(R.id.add_comment_button);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        postId = Long.valueOf(intent.getIntExtra("postId", -1));
        userId = Long.valueOf(intent.getIntExtra("userId", -1));

        Log.d(TAG, "PostId: " + postId + ", UserId: " + userId);
    }

    private void setupClickListeners() {
        addCommentButton.setOnClickListener(v -> {
            String description = commentEditText.getText().toString().trim();
            if (!description.isEmpty()) {
                createComment(description);
            } else {
                Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadComments() {
        apiService.getComments(postId, new TravelApiService.ApiCallback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> comments) {
                Log.d(TAG, "Comments loaded successfully: " + comments.size() + " comments");
                runOnUiThread(() -> {
                    commentAdapter = new CommentAdapter(CommentActivity.this, comments);
                    commentListView.setAdapter(commentAdapter);
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading comments: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(CommentActivity.this, 
                        "Error loading comments: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createComment(String description) {
        if (description == null || description.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        addCommentButton.setEnabled(false);
        
        apiService.createComment(userId, postId, description.trim(), new TravelApiService.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    commentEditText.setText("");
                    addCommentButton.setEnabled(true);
                    // Refresh comments immediately
                    loadComments();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    addCommentButton.setEnabled(true);
                    Toast.makeText(CommentActivity.this, 
                        "Failed to post comment", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
