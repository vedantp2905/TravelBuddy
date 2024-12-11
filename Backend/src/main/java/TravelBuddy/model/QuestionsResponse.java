package TravelBuddy.model;

import java.util.List;
import java.util.ArrayList;

public class QuestionsResponse {
    private List<TriviaQuestion> questions;

    public QuestionsResponse() {
        questions = new ArrayList<>();
    }

    public List<TriviaQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<TriviaQuestion> questions) {
        this.questions = questions;
    }
} 