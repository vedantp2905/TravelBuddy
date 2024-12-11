package TravelBuddy.model.messages;

import TravelBuddy.model.TriviaQuestion;

public class GameStartMessage {
    private String type;
    private TriviaQuestion firstQuestion;

    public GameStartMessage(String type) {
        this.type = type;
    }

    public GameStartMessage(TriviaQuestion firstQuestion) {
        this.type = "NEXT_QUESTION";
        this.firstQuestion = firstQuestion;
    }

    public String getType() {
        return type;
    }

    public TriviaQuestion getFirstQuestion() {
        return firstQuestion;
    }
} 