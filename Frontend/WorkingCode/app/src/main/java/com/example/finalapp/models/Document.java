package com.example.finalapp.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Document {
    private Long id;
    private Long userId;
    private String documentType;
    private String documentNumber;
    private LocalDate expiryDate;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Document(Long id, Long userId, String documentType, String documentNumber, 
                   LocalDate expiryDate, String filePath, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.expiryDate = expiryDate;
        this.filePath = filePath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
} 