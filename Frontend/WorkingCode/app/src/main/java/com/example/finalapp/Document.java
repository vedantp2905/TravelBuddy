package com.example.finalapp;

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

    // Constructor
    public Document(Long id, Long userId, String documentType, String documentNumber,
                    LocalDate expiryDate, String filePath, LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.expiryDate = expiryDate;
        this.filePath = filePath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", userId=" + userId +
                ", documentType='" + documentType + '\'' +
                ", documentNumber='" + documentNumber + '\'' +
                ", expiryDate=" + expiryDate +
                ", filePath='" + filePath + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
