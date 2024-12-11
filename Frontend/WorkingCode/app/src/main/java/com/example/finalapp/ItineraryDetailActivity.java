package com.example.finalapp;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.finalapp.ShareItineraryActivity;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItineraryDetailActivity extends AppCompatActivity {
    private static final String TAG = "ItineraryDetailActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private String generatedItinerary;
    private String country;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itinerary_detail);

        // Get data from intent
        country = getIntent().getStringExtra("country");
        String cities = getIntent().getStringExtra("cities");
        String startDate = getIntent().getStringExtra("startDate");
        String endDate = getIntent().getStringExtra("endDate");
        int adults = getIntent().getIntExtra("adults", 0);
        int children = getIntent().getIntExtra("children", 0);
        String location = getIntent().getStringExtra("location");
        String postID = getIntent().getStringExtra("postID");
        generatedItinerary = getIntent().getStringExtra("generatedItinerary");

        // Set up TextViews and WebView
        TextView countryText = findViewById(R.id.countryText);
        TextView citiesText = findViewById(R.id.citiesText);
        TextView datesText = findViewById(R.id.datesText);
        TextView travelersText = findViewById(R.id.travelersText);
        TextView locationText = findViewById(R.id.locationText);
        WebView itineraryWebView = findViewById(R.id.itineraryWebView);

        // Display the basic information
        countryText.setText("Country: " + country);
        citiesText.setText("Cities: " + cities);
        datesText.setText(String.format("Dates: %s to %s", startDate, endDate));
        travelersText.setText(String.format("Travelers: %d adults, %d children", adults, children));
        locationText.setText("Starting from: " + location);

        // Configure WebView
        itineraryWebView.getSettings().setJavaScriptEnabled(true);
        itineraryWebView.getSettings().setBuiltInZoomControls(true);
        itineraryWebView.getSettings().setDisplayZoomControls(false);

        // Format HTML content with styling
        String htmlContent = "<html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; padding: 16px; line-height: 1.6; }" +
                "strong { color: #2196F3; }" +
                "ul { padding-left: 20px; }" +
                "li { margin-bottom: 10px; }" +
                "</style></head><body>" +
                (generatedItinerary != null ? generatedItinerary : "No itinerary available") +
                "</body></html>";

        // Load the content into WebView
        itineraryWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);

        Button downloadButton = findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(v -> checkPermissionAndDownload());

        // Add the new button to share itinerary
        Button shareButton = findViewById(R.id.shareButton); // Assuming you added this button to the layout XML
        shareButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareItineraryActivity.class);
            intent.putExtra("postID", postID);
            startActivity(intent);
        });
    }

    private void checkPermissionAndDownload() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For Android 11 and above
            if (Environment.isExternalStorageManager()) {
                downloadItinerary();
            } else {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            // For Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                downloadItinerary();
            }
        }
    }

    private void downloadItinerary() {
        try {
            Document document = new Document();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Itinerary_" + country + "_" + timeStamp + ".pdf";
            
            // Use the Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);
            
            FileOutputStream fos = new FileOutputStream(file);
            PdfWriter.getInstance(document, fos);
            
            document.open();
            
            // Add content to PDF
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            
            Paragraph title = new Paragraph("Travel Itinerary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));
            
            document.add(new Paragraph("Country: " + country, normalFont));
            document.add(new Paragraph(generatedItinerary, normalFont));
            
            document.close();
            
            // Show success message
            Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show();
            
            // Try to open the PDF
            try {
                Uri uri = FileProvider.getUriForFile(this, 
                    "com.example.finalapp.fileprovider", file);
                    
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                // Check if there's an app that can handle PDF viewing
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, 
                        "Please install a PDF viewer to open the file", 
                        Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                // If opening fails, at least inform the user where the file is saved
                Toast.makeText(this, 
                    "PDF saved to Downloads folder: " + fileName, 
                    Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error opening PDF: " + e.getMessage());
            }
            
        } catch (DocumentException | IOException e) {
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error creating PDF: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadItinerary();
            } else {
                Toast.makeText(this, "Permission denied to save itinerary", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
