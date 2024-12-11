package com.example.finalapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.example.finalapp.R;
import com.example.finalapp.models.Document;
import java.util.List;

public class DocumentAdapter extends ArrayAdapter<Document> {
    public DocumentAdapter(Context context, List<Document> documents) {
        super(context, 0, documents);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Document document = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_document, parent, false);
        }

        TextView typeText = convertView.findViewById(R.id.tv_document_type);
        TextView numberText = convertView.findViewById(R.id.tv_document_number);
        TextView expiryText = convertView.findViewById(R.id.tv_expiry_date);

        typeText.setText(document.getDocumentType());
        numberText.setText("Document Number: " + document.getDocumentNumber());
        expiryText.setText("Expires: " + document.getExpiryDate().toString());

        return convertView;
    }
} 