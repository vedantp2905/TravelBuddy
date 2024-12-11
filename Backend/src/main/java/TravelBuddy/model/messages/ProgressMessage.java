package TravelBuddy.model.messages;

public class ProgressMessage {
    private String type;
    private int answered;
    private int total;

    public ProgressMessage(String type, int answered, int total) {
        this.type = type;
        this.answered = answered;
        this.total = total;
    }

    public String getType() {
        return type;
    }

    public int getAnswered() {
        return answered;
    }

    public int getTotal() {
        return total;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAnswered(int answered) {
        this.answered = answered;
    }

    public void setTotal(int total) {
        this.total = total;
    }
} 