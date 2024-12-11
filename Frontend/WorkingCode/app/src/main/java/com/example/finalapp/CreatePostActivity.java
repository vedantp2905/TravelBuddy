package com.example.finalapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import java.time.LocalDateTime;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import android.app.DatePickerDialog;
import android.util.Log;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

public class CreatePostActivity extends AppCompatActivity {
    private TextInputEditText destinationInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText categoryInput;
    private TextInputEditText ratingInput;
    private Button submitButton;
    private TravelApiService apiService;
    private LocalDate selectedStartDate;
    private LocalDate selectedEndDate;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView selectedImagePreview;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Initialize views
        destinationInput = findViewById(R.id.destination_input);
        descriptionInput = findViewById(R.id.description_input);
        categoryInput = findViewById(R.id.category_input);
        ratingInput = findViewById(R.id.rating_input);
        submitButton = findViewById(R.id.submit_button);
        
        TextView startDateText = findViewById(R.id.start_date_text);
        TextView endDateText = findViewById(R.id.end_date_text);
        Button startDateButton = findViewById(R.id.start_date_button);
        Button endDateButton = findViewById(R.id.end_date_button);
        selectedImagePreview = findViewById(R.id.selected_image_preview);
        Button selectImageButton = findViewById(R.id.select_image_button);

        // Initialize API service
        apiService = new TravelApiService(this);

        // Initialize with current date
        selectedStartDate = LocalDate.now();
        selectedEndDate = LocalDate.now().plusDays(1);

        // Format and display initial dates
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        startDateText.setText(selectedStartDate.format(displayFormatter));
        endDateText.setText(selectedEndDate.format(displayFormatter));

        startDateButton.setOnClickListener(v -> showDatePicker(true, startDateText));
        endDateButton.setOnClickListener(v -> showDatePicker(false, endDateText));

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
        });

        // Setup submit button
        submitButton.setOnClickListener(v -> createPost());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK 
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            selectedImagePreview.setImageURI(selectedImageUri);
            selectedImagePreview.setVisibility(View.VISIBLE);
        }
    }

    private void createPost() {
        if (!validateInputs()) {
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = String.valueOf(sharedPreferences.getInt("userId", -1));

        if (userId.equals("-1")) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateDates()) {
            return;
        }

        // Create post data
        try {
            JSONObject postData = new JSONObject();
            postData.put("userId", userId);
            postData.put("description", descriptionInput.getText().toString().trim());
            postData.put("category", categoryInput.getText().toString().trim());
            postData.put("rating", ratingInput.getText().toString().trim());
            postData.put("destination", destinationInput.getText().toString().trim());
            
            // Format dates properly with time component
            LocalDateTime startDateTime = selectedStartDate.atTime(0, 0, 0);
            LocalDateTime endDateTime = selectedEndDate.atTime(0, 0, 0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            postData.put("startDate", startDateTime.format(formatter));
            postData.put("endDate", endDateTime.format(formatter));

            // Send the post creation request
            apiService.createPost(postData, new TravelApiService.ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject response) {
                    try {
                        long postId = response.getLong("postId");
                        if (selectedImageUri != null) {
                            uploadImage(postId);
                        } else {
                            runOnUiThread(() -> {
                                Intent intent = new Intent(CreatePostActivity.this, TravelFeedActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("CreatePost", "Error parsing post ID", e);
                        runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, 
                            "Error creating post", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, 
                        "Failed to create post: " + error, 
                        Toast.LENGTH_SHORT).show());
                }
            });
        } catch (JSONException e) {
            Log.e("CreatePost", "Error creating post JSON", e);
            runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, 
                "Error creating post", Toast.LENGTH_SHORT).show());
        }
    }

    private boolean validateInputs() {
        String destination = destinationInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();
        String ratingStr = ratingInput.getText().toString().trim();

        if (destination.isEmpty()) {
            destinationInput.setError("Destination is required");
            return false;
        }

        if (description.isEmpty()) {
            descriptionInput.setError("Description is required");
            return false;
        }

        if (category.isEmpty()) {
            categoryInput.setError("Category is required");
            return false;
        }

        if (ratingStr.isEmpty()) {
            ratingInput.setError("Rating is required");
            return false;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedStartDate == null || selectedEndDate == null) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            int rating = Integer.parseInt(ratingStr);
            if (rating < 1 || rating > 5) {
                ratingInput.setError("Rating must be between 1 and 5");
                return false;
            }
        } catch (NumberFormatException e) {
            ratingInput.setError("Please enter a valid number");
            return false;
        }

        return true;
    }

    private void showDatePicker(boolean isStartDate, TextView dateText) {
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
            if (isStartDate) {
                selectedStartDate = selectedDate;
            } else {
                selectedEndDate = selectedDate;
            }
            dateText.setText(selectedDate.format(displayFormatter));
        };

        LocalDate currentSelection = isStartDate ? selectedStartDate : selectedEndDate;
        DatePickerDialog dialog = new DatePickerDialog(
            this,
            R.style.DatePickerTheme,
            dateSetListener,
            currentSelection.getYear(),
            currentSelection.getMonthValue() - 1,
            currentSelection.getDayOfMonth()
        );
        dialog.show();
    }

    private boolean validateDates() {
        if (selectedStartDate == null || selectedEndDate == null) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedEndDate.isBefore(selectedStartDate)) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void uploadImage(long postId) {
        if (selectedImageUri == null) {
            Log.e("CreatePost", "No image selected!");
            return;
        }

        try {
            Log.d("CreatePost", "Starting image upload process for post ID: " + postId);
            Log.d("CreatePost", "Selected image URI: " + selectedImageUri);
            
            InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
            byte[] imageBytes = IOUtils.toByteArray(imageStream);
            
            if (imageBytes == null || imageBytes.length == 0) {
                Log.e("CreatePost", "Image bytes are null or empty!");
                return;
            }
            
            Log.d("CreatePost", "Image bytes size: " + imageBytes.length);
            
            apiService.uploadImage(postId, imageBytes, new TravelApiService.ApiCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    Log.d("CreatePost", "Image upload success: " + response);
                    runOnUiThread(() -> {
                        // Navigate to feed activity
                        Intent intent = new Intent(CreatePostActivity.this, TravelFeedActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    Log.e("CreatePost", "Image upload error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(CreatePostActivity.this, 
                            "Failed to upload image: " + error, 
                            Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (IOException e) {
            Log.e("CreatePost", "Error reading image file", e);
            Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
        }
    }
}