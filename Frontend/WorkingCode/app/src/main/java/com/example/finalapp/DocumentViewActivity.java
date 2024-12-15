package com.example.finalapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.github.barteksc.pdfviewer.PDFView;
import androidx.appcompat.app.AlertDialog;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.finalapp.InputStreamVolleyRequest;

/**
 * Activity for viewing, updating, downloading, and deleting document details.
 */
public class DocumentViewActivity extends AppCompatActivity {

    // UI elements
    private TextView documentTypeText, documentNumberText, expiryDateText;
    private Button downloadButton, updateButton, deleteButton;
    private PDFView pdfView;

    // Constants and member variables
    private static final String BASE_URL = ApiConstants.BASE_URL + "/api/document";
    private static final int PERMISSION_REQUEST_CODE = 123;
    private int documentId;
    private int userId;
    private String filePath;

    /**
     * Initializes the activity and sets up UI elements and listeners.
     *
     * @param savedInstanceState the saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_view);

        // Initialize UI elements
        documentTypeText = findViewById(R.id.documentTypeText);
        documentNumberText = findViewById(R.id.documentNumberText);
        expiryDateText = findViewById(R.id.expiryDateText);
        downloadButton = findViewById(R.id.downloadButton);
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        pdfView = findViewById(R.id.pdfView);

        // Retrieve document and user IDs
        documentId = getIntent().getIntExtra("documentId", -1);
        userId = getIntent().getIntExtra("userId", -1);

        // Load document data if documentId is valid
        if (documentId != -1) {
            loadDocumentData();
        }

        // Set up button listeners
        downloadButton.setOnClickListener(v -> {
            String pdfUrl = BASE_URL + "/file/" + filePath.substring(filePath.lastIndexOf("/") + 1);
            checkPermissionAndDownload(pdfUrl);
        });

        updateButton.setOnClickListener(v -> editDocument());
        deleteButton.setOnClickListener(v -> deleteDocument());
    }

    /**
     * Fetches and displays document details from the server.
     */
    private void loadDocumentData() {
        String url = BASE_URL + "/read-by-id/" + documentId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Extract document details from the response
                        String documentType = response.getString("documentType");
                        String documentNumber = response.getString("documentNumber");
                        String expiryDate = response.getString("expiryDate").substring(0, 10);
                        filePath = response.getString("filePath");

                        // Update UI elements
                        documentTypeText.setText("Document Type: " + documentType);

                        String numberLabel;
                        switch (documentType) {
                            case "Passport":
                                numberLabel = "Passport Number: ";
                                break;
                            case "Driving License":
                                numberLabel = "License Number: ";
                                break;
                            case "Tickets":
                                numberLabel = "Ticket Number: ";
                                break;
                            default:
                                numberLabel = "Document Number: ";
                        }
                        documentNumberText.setText(numberLabel + documentNumber);
                        expiryDateText.setText("Expiry Date: " + expiryDate);

                        // Load the PDF
                        String pdfUrl = BASE_URL + "/file/" + filePath.substring(filePath.lastIndexOf("/") + 1);
                        loadPdfFromUrl(pdfUrl);

                    } catch (JSONException e) {
                        Toast.makeText(this, "Error parsing document data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error loading document", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    /**
     * Downloads and displays the PDF using PDFView.
     *
     * @param pdfUrl the URL of the PDF file.
     */
    private void loadPdfFromUrl(String pdfUrl) {
        InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, pdfUrl,
                response -> {
                    pdfView.fromBytes(response)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .defaultPage(0)
                            .load();
                },
                error -> Toast.makeText(this, "Error loading PDF", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    /**
     * Checks for necessary permissions and downloads the PDF.
     *
     * @param pdfUrl the URL of the PDF file.
     */
    private void checkPermissionAndDownload(String pdfUrl) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                downloadPdf(pdfUrl);
            } else {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                downloadPdf(pdfUrl);
            }
        }
    }

    /**
     * Downloads the PDF file and saves it locally.
     *
     * @param pdfUrl the URL of the PDF file.
     */
    private void downloadPdf(String pdfUrl) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Document_" + documentId + "_" + timeStamp + ".pdf";
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName);

            InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, pdfUrl,
                    response -> {
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(response);
                            Toast.makeText(this, "PDF downloaded to Downloads folder", Toast.LENGTH_LONG).show();

                            // Open the PDF
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri uri = FileProvider.getUriForFile(this,
                                    "com.example.finalapp.fileprovider", file);
                            intent.setDataAndType(uri, "application/pdf");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } catch (IOException e) {
                            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> Toast.makeText(this, "Error downloading PDF", Toast.LENGTH_SHORT).show());

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigates to the AddDocumentActivity for editing the document.
     */
    private void editDocument() {
        Intent intent = new Intent(DocumentViewActivity.this, AddDocumentActivity.class);
        intent.putExtra("documentId", documentId);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    /**
     * Deletes the document file and database entry after confirmation.
     */
    private void deleteDocument() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete this document?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Extract filename from filePath
                    String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                    String fileDeleteUrl = String.format(BASE_URL + "/file/%s", fileName);
                    
                    StringRequest fileDeleteRequest = new StringRequest(Request.Method.DELETE, fileDeleteUrl,
                            fileResponse -> {
                                String dbDeleteUrl = String.format(BASE_URL + "/delete/%d", documentId);
                                StringRequest dbDeleteRequest = new StringRequest(Request.Method.DELETE, dbDeleteUrl,
                                        response -> {
                                            Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        },
                                        error -> {
                                            Toast.makeText(this, "Error deleting document record", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                                Volley.newRequestQueue(this).add(dbDeleteRequest);
                            },
                            error -> {
                                Toast.makeText(this, "Error deleting document file", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                    Volley.newRequestQueue(this).add(fileDeleteRequest);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Handles runtime permission request results.
     *
     * @param requestCode the request code for the permission request.
     * @param permissions the requested permissions.
     * @param grantResults the results of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String pdfUrl = BASE_URL + "/file/" + filePath.substring(filePath.lastIndexOf("/") + 1);
                downloadPdf(pdfUrl);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
