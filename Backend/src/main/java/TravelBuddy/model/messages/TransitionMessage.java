package TravelBuddy.model.messages;

public class TransitionMessage {
    private String type = "TRANSITION";
    private String message = "Next Question Coming Up!";
    
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
