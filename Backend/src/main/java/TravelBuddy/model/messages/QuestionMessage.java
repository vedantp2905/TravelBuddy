package TravelBuddy.model.messages;

import TravelBuddy.model.TriviaQuestion;
import java.util.List;

public class QuestionMessage {
    private String type;
    private String question;
    private List<String> options;
    private int timeLimit;

    public QuestionMessage(TriviaQuestion question) {
        this.type = "NEXT_QUESTION";
        this.question = question.getQuestion();
        this.options = question.getOptions();
        this.timeLimit = 10; // 10 seconds per question
    }

    public String getType() {
        return type;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getTimeLimit() {
        return timeLimit;
    }
} 