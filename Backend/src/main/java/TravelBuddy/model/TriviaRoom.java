package TravelBuddy.model;

import java.util.*;

import TravelBuddy.service.UserService;

public class TriviaRoom {
    private String roomCode;
    private Long hostId;
    private String hostName;
    private List<Long> players;
    private Map<Long, Integer> scores;
    private List<TriviaQuestion> questions;
    private int currentQuestionIndex;
    private boolean isActive = false;
    private Set<Long> answeredPlayers = new HashSet<>();
    private Set<Long> timedOutPlayers = new HashSet<>();
    private Map<Long, Integer> answerStreaks = new HashMap<>();
    private Map<Long, String> playerStatus = new HashMap<>();  // "ACTIVE", "DISCONNECTED"
    private Map<String, Integer> currentQuestionAnswerDistribution = new HashMap<>();
    private Long fastestAnswerPlayer;
    private long fastestAnswerTime = Long.MAX_VALUE;
    private UserService userService;
    private Set<Long> activePlayers = new HashSet<>();

    public TriviaRoom(String roomCode) {
        this.roomCode = roomCode;
        this.players = new ArrayList<>();
        this.scores = new HashMap<>();
        this.isActive = false;
        this.answeredPlayers = new HashSet<>();
    }

    public TriviaRoom(String roomCode, Long hostId, String hostName, UserService userService) {
        this.roomCode = roomCode;
        this.hostId = hostId;
        this.hostName = hostName;
        this.userService = userService;
        this.players = new ArrayList<>();
        this.scores = new HashMap<>();
        this.players.add(hostId);
        this.scores.put(hostId, 0);
        this.isActive = false;
        this.answeredPlayers = new HashSet<>();
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public List<Long> getPlayers() {
        return new ArrayList<>(players);
    }

    public void setPlayers(List<Long> players) {
        this.players = players;
    }

    public Map<Long, Integer> getScores() {
        return new HashMap<>(scores);
    }

    public void setScores(Map<Long, Integer> scores) {
        this.scores = scores;
    }

    public List<TriviaQuestion> getQuestions() {
        return new ArrayList<>(questions);
    }

    public void setQuestions(List<TriviaQuestion> questions) {
        this.questions = questions;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public TriviaQuestion getCurrentQuestion() {
        return questions.get(currentQuestionIndex);
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int index) {
        this.currentQuestionIndex = index;
    }

    public Set<Long> getAnsweredPlayers() {
        return new HashSet<>(answeredPlayers);
    }

    public void addPlayerAnswer(Long userId) {
        answeredPlayers.add(userId);
    }

    public boolean haveAllPlayersAnswered() {
        return answeredPlayers.size() >= players.size();
    }

    public void clearPlayerAnswers() {
        answeredPlayers.clear();
    }

    public int getAnsweredCount() {
        return answeredPlayers.size();
    }

    public int getPlayerCount() {
        return players.size();
    }

    public void addPlayer(Long userId) {
        players.add(userId);
        scores.putIfAbsent(userId, 0);  // Initialize score to 0 when player joins
    }

    public void markPlayerTimedOut(Long userId) {
        timedOutPlayers.add(userId);
    }

    public boolean haveAllPlayersTimedOut() {
        return timedOutPlayers.size() + answeredPlayers.size() == players.size();
    }

    public void resetForNextQuestion() {
        answeredPlayers.clear();
        timedOutPlayers.clear();
    }

    public void updateStreak(Long userId, boolean correct) {
        if (correct) {
            answerStreaks.merge(userId, 1, Integer::sum);
        } else {
            answerStreaks.put(userId, 0);
        }
    }

    public int getStreak(Long userId) {
        return answerStreaks.getOrDefault(userId, 0);
    }

    public void markPlayerDisconnected(Long userId) {
        playerStatus.put(userId, "DISCONNECTED");
        markPlayerTimedOut(userId);
    }

    public void markPlayerReconnected(Long userId) {
        playerStatus.put(userId, "ACTIVE");
    }

    public boolean isPlayerActive(Long userId) {
        return players.contains(userId) && 
               !playerStatus.getOrDefault(userId, "ACTIVE").equals("DISCONNECTED");
    }

    public void recordAnswer(String answer, Long userId, long timeRemaining) {
        currentQuestionAnswerDistribution.merge(answer, 1, Integer::sum);
        
        if (timeRemaining < fastestAnswerTime) {
            fastestAnswerTime = timeRemaining;
            fastestAnswerPlayer = userId;
        }
    }

    public void resetQuestionStats() {
        currentQuestionAnswerDistribution.clear();
        fastestAnswerPlayer = null;
        fastestAnswerTime = Long.MAX_VALUE;
    }

    public void removePlayer(String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            players.remove(userIdLong);
            scores.remove(userIdLong);
            answerStreaks.remove(userIdLong);
            answeredPlayers.remove(userIdLong);
            timedOutPlayers.remove(userIdLong);
            playerStatus.remove(userIdLong);
            activePlayers.remove(userIdLong);
            
            if (!players.isEmpty() && userIdLong.equals(hostId)) {
                hostId = players.get(0);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error converting userId to Long: " + userId);
        }
    }

    public List<Long> getActivePlayers() {
        players.removeIf(playerId -> !playerStatus.getOrDefault(playerId, "ACTIVE").equals("ACTIVE"));
        return new ArrayList<>(players);
    }

    public Set<Long> getTimedOutPlayers() {
        return timedOutPlayers;
    }
} 