package TravelBuddy.model.messages;

public class WaitingMessage {
    private String type;
    private int answered;
    private int total;

    public WaitingMessage(int answered, int total) {
        this.type = "WAITING";
        this.answered = answered;
        this.total = total;
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAnswered() {
        return answered;
    }

    public void setAnswered(int answered) {
        this.answered = answered;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
} 