package com.example.finalapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.example.finalapp.InputStreamVolleyRequest;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;

public class AddDocumentActivity extends AppCompatActivity {
    private static final int PICK_PDF_FILE = 1;
    private Spinner documentTypeSpinner;
    private EditText documentNumberEditText;
    private Button expiryDateButton, uploadPdfButton, submitButton;
    private TextView selectedFileTextView;
    private Uri selectedPdfUri;
    private Calendar calendar;
    private static final String BASE_URL = "http://coms-3090-010.class.las.iastate.edu:8080/api/document";
    private int userId;
    private int documentId;
    private boolean isUpdateMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        // Initialize views
        documentTypeSpinner = findViewById(R.id.spinner_document_type);
        documentNumberEditText = findViewById(R.id.et_document_number);
        expiryDateButton = findViewById(R.id.btn_expiry_date);
        uploadPdfButton = findViewById(R.id.btn_upload_pdf);
        selectedFileTextView = findViewById(R.id.tv_selected_file);
        submitButton = findViewById(R.id.btn_submit);

        calendar = Calendar.getInstance();
        userId = getUserId();
        documentId = getDocumentId();

        isUpdateMode = documentId != -1;
        if (isUpdateMode) {
            // Update UI for update mode
            documentTypeSpinner.setEnabled(false);
            submitButton.setText("Update Document");
            loadDocumentData();
        }

        setupSpinner();
        setupDatePicker();
        setupPdfPicker();
        
        submitButton.setOnClickListener(v -> {
            if (isUpdateMode) {
                updateDocument();
            } else {
                addDocument();
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.document_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        documentTypeSpinner.setAdapter(adapter);

        documentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals("Select Document Type")) {
                    ((TextView) parent.getChildAt(0)).setTextColor(getResources().getColor(R.color.titleTextColor));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void setupDatePicker() {
        expiryDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateDisplay();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupPdfPicker() {
        uploadPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_PDF_FILE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedPdfUri = data.getData();
                try {
                    String fileName = getFileNameFromUri(selectedPdfUri);
                    if (fileName != null) {
                        selectedFileTextView.setText("Selected file: " + fileName);
                    } else {
                        Toast.makeText(this, "Error: Could not get file name", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("PDF_SELECTION", "Error getting file name: " + e.getMessage());
                    Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return null;
        
        String result = null;
        try {
            if (uri.getScheme().equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index != -1) {
                            result = cursor.getString(index);
                        }
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        } catch (Exception e) {
            Log.e("FILE_NAME", "Error getting file name: " + e.getMessage());
            return null;
        }
        return result;
    }

    private void updateDateDisplay() {
        String myFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        expiryDateButton.setText(sdf.format(calendar.getTime()));
    }

    private int getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1);
    }

    private int getDocumentId() {
        return getIntent().getIntExtra("documentId", -1);
    }

    private void loadDocumentData() {
        String url = BASE_URL + "/read-by-id/" + documentId;
        JsonObjectRequest request = new JsonObjectRequest(com.android.volley.Request.Method.GET, url, null,
                response -> {
                    try {
                        String documentType = response.getString("documentType");
                        String documentNumber = response.getString("documentNumber");
                        String expiryDate = response.getString("expiryDate");

                        // Set spinner selection
                        ArrayAdapter adapter = (ArrayAdapter) documentTypeSpinner.getAdapter();
                        int position = adapter.getPosition(documentType);
                        documentTypeSpinner.setSelection(position);

                        // Handle ticket type if present
                        if (documentType.equals("Tickets")) {
                            String[] parts = documentNumber.split("-", 2);
                            if (parts.length == 2) {
                                documentNumberEditText.setText(parts[1]);
                            }
                        } else {
                            documentNumberEditText.setText(documentNumber);
                        }

                        // Set expiry date
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        calendar.setTime(sdf.parse(expiryDate));
                        updateDateDisplay();

                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading document data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error loading document", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    private void addDocument() {
        if (!validateInputs()) return;

        String url = BASE_URL + "/create";
        
        String documentNumber = documentNumberEditText.getText().toString().trim();
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userId", String.valueOf(userId))
                .addFormDataPart("documentType", documentTypeSpinner.getSelectedItem().toString())
                .addFormDataPart("documentNumber", documentNumber)
                .addFormDataPart("expiryDate", expiryDateButton.getText().toString());

        if (selectedPdfUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedPdfUri);
                byte[] fileBytes = getBytes(inputStream);
                String fileName = getFileNameFromUri(selectedPdfUri);
                builder.addFormDataPart("file", fileName,
                        RequestBody.create(MediaType.parse("application/pdf"), fileBytes));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error reading PDF file", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AddDocumentActivity.this, 
                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddDocumentActivity.this, 
                            "Document added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddDocumentActivity.this, 
                            "Error: " + responseBody, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private boolean validateInputs() {
        String documentType = documentTypeSpinner.getSelectedItem().toString();
        String documentNumber = documentNumberEditText.getText().toString().trim();
        String expiryDateText = expiryDateButton.getText().toString();

        if (documentNumber.isEmpty() || expiryDateText.equals("Select Expiry Date")) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedPdfUri == null) {
            Toast.makeText(this, "Please select a PDF document", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private JSONObject createDocumentJson() {
        try {
            JSONObject jsonBody = new JSONObject();
            String documentType = documentTypeSpinner.getSelectedItem().toString();
            String documentNumber = documentNumberEditText.getText().toString().trim();

            jsonBody.put("userId", userId);
            jsonBody.put("documentType", documentType);
            jsonBody.put("documentNumber", documentNumber);
            jsonBody.put("expiryDate", expiryDateButton.getText().toString());
            jsonBody.put("filePath", getFileNameFromUri(selectedPdfUri));

            return jsonBody;
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void updateDocument() {
        if (!validateUpdateInputs()) return;

        String url = BASE_URL + "/update/" + documentId;
        Log.d("UpdateDocument", "Update URL: " + url);
        
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // Only add these if they've been changed
        String documentNumber = documentNumberEditText.getText().toString().trim();
        String expiryDate = expiryDateButton.getText().toString();
        
        if (!documentNumber.isEmpty()) {
            builder.addFormDataPart("documentNumber", documentNumber);
        }
        
        if (!expiryDate.equals("Select Expiry Date")) {
            builder.addFormDataPart("expiryDate", expiryDate);
        }

        // PDF is mandatory
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedPdfUri);
            byte[] fileBytes = getBytes(inputStream);
            String fileName = getFileNameFromUri(selectedPdfUri);
            Log.d("UpdateDocument", "File Name: " + fileName);
            builder.addFormDataPart("file", fileName,
                    RequestBody.create(MediaType.parse("application/pdf"), fileBytes));
        } catch (IOException e) {
            Log.e("UpdateDocument", "Error reading PDF: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error reading PDF file", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .patch(requestBody)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UpdateDocument", "Update failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(AddDocumentActivity.this, 
                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                Log.d("UpdateDocument", "Response code: " + response.code());
                Log.d("UpdateDocument", "Response body: " + responseBody);
                
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddDocumentActivity.this, 
                            "Document updated successfully", Toast.LENGTH_SHORT).show();
                        // Create intent to ManageDocumentsActivity
                        Intent intent = new Intent(AddDocumentActivity.this, ManageDocumentsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the activity stack
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AddDocumentActivity.this, 
                            "Error: " + responseBody, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private boolean validateUpdateInputs() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, "Please select a PDF document", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
