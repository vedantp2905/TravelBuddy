package TravelBuddy.model.messages;

public class RoomMessage {
    private String type;
    private String message;

    public RoomMessage(String type) {
        this.type = type;
    }

    public RoomMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 