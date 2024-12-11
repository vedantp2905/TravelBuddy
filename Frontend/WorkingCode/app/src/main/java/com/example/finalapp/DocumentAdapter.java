package com.example.finalapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private final List<Document> documents;
    private final Context context;

    public DocumentAdapter(Context context, List<Document> documents) {
        this.context = context;
        this.documents = documents;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.document_list_item, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);
        holder.documentTypeView.setText(document.getDocumentType());
        holder.documentNumberView.setText(document.getDocumentNumber());
        // You can set other fields here as needed
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        TextView documentTypeView;
        TextView documentNumberView;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            documentTypeView = itemView.findViewById(R.id.documentTypeView);
            documentNumberView = itemView.findViewById(R.id.documentNumberView);
        }
    }
}
