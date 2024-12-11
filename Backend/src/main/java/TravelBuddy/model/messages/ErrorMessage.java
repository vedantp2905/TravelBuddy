package TravelBuddy.model.messages;

public class ErrorMessage {
    private String type;
    private String message;

    public ErrorMessage(String type) {
        this.type = type;
        this.message = type; // Use type as message for simple cases
    }

    public ErrorMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
} 