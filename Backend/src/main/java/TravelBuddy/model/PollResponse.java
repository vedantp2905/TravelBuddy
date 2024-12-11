package TravelBuddy.model;

public class PollResponse {
    private Poll poll;
    private String message;
    private boolean success;

    public PollResponse(Poll poll, String message, boolean success) {
        this.poll = poll;
        this.message = message;
        this.success = success;
    }

    public Poll getPoll() {
        return poll;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }
} 