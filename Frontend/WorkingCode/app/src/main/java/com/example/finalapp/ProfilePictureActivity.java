package com.example.finalapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.core.content.FileProvider;

public class ProfilePictureActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_PICK_IMAGE = 1;
    private Uri photoUri;
    private ImageView profileImageView;
    private Button uploadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_picture); // Link to layout

        profileImageView = findViewById(R.id.profileImageView);
        uploadButton = findViewById(R.id.uploadButton);

        // Set an onClickListener to upload button
        uploadButton.setOnClickListener(v -> openImagePicker()); // Open image picker
    }

    private void openImagePicker() {
        // Intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void openCamera() {
        // Intent to capture image with the camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Create image file
            } catch (IOException ex) {
                // Handle error
            }

            if (photoFile != null) {
                // Get a URI for the image file
                photoUri = FileProvider.getUriForFile(this, "com.example.finalapp.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); // Set the URI as output
                startActivityForResult(cameraIntent, REQUEST_CAMERA); // Start camera
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE) {
                Uri sourceUri = data.getData();
                startCrop(sourceUri); // Crop the selected image
            } else if (requestCode == REQUEST_CAMERA) {
                // Load the image from the URI and show it in the ImageView
                Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .into(profileImageView);
            } else if (requestCode == UCrop.REQUEST_CROP) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    loadCroppedImage(resultUri); // Load cropped image
                }
            }
        }
    }

    private void loadCroppedImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(profileImageView);
    }

    private void startCrop(Uri sourceUri) {
        // Define the destination file for the cropped image
        File destinationFile = new File(getCacheDir(), "croppedImage.jpg");
        UCrop.of(sourceUri, Uri.fromFile(destinationFile))
                .withAspectRatio(1, 1) // Aspect ratio for a square crop
                .withMaxResultSize(500, 500) // Max size of the resulting image
                .start(this);
    }

    private File createImageFile() throws IOException {
        // Create a temporary image file with a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}
