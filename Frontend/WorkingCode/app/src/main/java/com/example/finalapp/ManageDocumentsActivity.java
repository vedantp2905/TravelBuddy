package com.example.finalapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.example.finalapp.models.Document;
import com.example.finalapp.adapters.DocumentAdapter;

public class ManageDocumentsActivity extends AppCompatActivity {

    private ListView documentListView;
    private FloatingActionButton addDocumentButton;
    private TextView noDocumentsText;
    private static final String BASE_URL = ApiConstants.BASE_URL + "/api/document";
    private int userId;
    private List<Document> documentList; // Use List<Document> instead of List<Map<String, String>>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_documents);

        documentListView = findViewById(R.id.documentListView);
        addDocumentButton = findViewById(R.id.addDocumentButton);
        noDocumentsText = findViewById(R.id.noDocumentsText);
        documentList = new ArrayList<>(); // Initialize the document list

        userId = getUserId();

        addDocumentButton.setOnClickListener(v -> {
            Intent intent = new Intent(ManageDocumentsActivity.this, AddDocumentActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        documentListView.setOnItemClickListener((parent, view, position, id) -> {
            Document document = documentList.get(position);
            Intent intent = new Intent(ManageDocumentsActivity.this, DocumentViewActivity.class);
            intent.putExtra("documentId", document.getId().intValue());
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        loadDocuments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }

    private void loadDocuments() {
        String url = BASE_URL + "/read/" + userId;
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    documentList.clear();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            Document doc = new Document(
                                obj.getLong("id"),
                                obj.getLong("userId"),
                                obj.getString("documentType"),
                                obj.getString("documentNumber"),
                                LocalDate.parse(obj.getString("expiryDate").substring(0, 10)),
                                obj.getString("filePath"),
                                LocalDateTime.parse(obj.getString("createdAt")),
                                LocalDateTime.parse(obj.getString("updatedAt"))
                            );
                            documentList.add(doc);
                        }
                        updateDocumentListView();
                    } catch (JSONException e) {
                        Log.e("ManageDocuments", "Error parsing JSON: " + e.getMessage());
                        Toast.makeText(this, "Error parsing document data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        // No documents found - this is actually OK
                        documentList.clear();
                        updateDocumentListView();
                    } else {
                        Log.e("ManageDocuments", "Error loading documents: " + error.toString());
                        Toast.makeText(this, "Error loading documents", Toast.LENGTH_SHORT).show();
                    }
                });

        Volley.newRequestQueue(this).add(request);
    }

    private void updateDocumentListView() {
        if (documentList.isEmpty()) {
            documentListView.setVisibility(View.GONE);
            noDocumentsText.setVisibility(View.VISIBLE);
        } else {
            DocumentAdapter adapter = new DocumentAdapter(this, documentList);
            documentListView.setAdapter(adapter);
            documentListView.setVisibility(View.VISIBLE);
            noDocumentsText.setVisibility(View.GONE);
        }
    }

    private void showDocumentOptions(int documentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Document Options")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        editDocument(documentId);
                    } else if (which == 1) {
                        deleteDocument(documentId);
                    }
                });
        builder.create().show();
    }

    private void editDocument(int documentId) {
        Intent intent = new Intent(ManageDocumentsActivity.this, AddDocumentActivity.class);
        intent.putExtra("documentId", documentId);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void deleteDocument(int documentId) {
        String url = BASE_URL + "/api/document/delete/" + documentId;
        StringRequest deleteRequest = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show();
                    loadDocuments(); // Refresh the list
                },
                error -> {
                    Log.e("ManageDocuments", "Error deleting document: " + error.toString());
                    Toast.makeText(this, "Error deleting document", Toast.LENGTH_SHORT).show();
                });

        Volley.newRequestQueue(this).add(deleteRequest);
    }

    private int getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        return sharedPreferences.getInt("userId", -1);
    }
}
