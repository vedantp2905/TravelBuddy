package TravelBuddy.model;

import java.util.List;

public class TriviaQuestion {
    private String question;
    private List<String> options;
    private String correctAnswer;
    private String correctOption;
    private String explanation;
    private int timeLimit = 15; // seconds

    public TriviaQuestion() {
    }

    public TriviaQuestion(String question, List<String> options, String correctAnswer, String correctOption, String explanation, int timeLimit) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.correctOption = correctOption;
        this.explanation = explanation;
        this.timeLimit = timeLimit;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }
} 