package com.example.finalapp;

public class Message {
    private String id;
    private String message;
    private String sender;
    private String timestamp;
    private String parentMessage;  // Add parentMessage to store reply details if available
    private String messageType;    // Add messageType to handle the type of message (HTML or TEXT)

    // Constructor for normal messages (without parentMessage)
    public Message(String id, String message, String sender, String timestamp, String messageType) {
        this.id = id;
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
        this.parentMessage = null;  // Normal message has no parentMessage
        this.messageType = messageType;  // Store the message type
    }

    // Constructor for reply messages (with parentMessage)
    public Message(String id, String message, String sender, String timestamp, String parentMessage, String messageType) {
        this.id = id;
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
        this.parentMessage = parentMessage;  // Set parentMessage for replies
        this.messageType = messageType;  // Store the message type
    }

    // Getters and setters for all fields
    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParentMessage() {
        return parentMessage;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setParentMessage(String parentMessage) {
        this.parentMessage = parentMessage;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
