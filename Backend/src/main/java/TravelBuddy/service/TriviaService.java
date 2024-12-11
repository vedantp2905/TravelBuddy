package TravelBuddy.service;

import TravelBuddy.model.TriviaRoom;
import TravelBuddy.model.Winner;
import TravelBuddy.repositories.TriviaPlayerRepository;
import TravelBuddy.repositories.TriviaRoomRepository;
import TravelBuddy.model.QuestionsResponse;
import TravelBuddy.model.messages.GameOverMessage;
import TravelBuddy.model.messages.ProgressMessage;
import TravelBuddy.model.messages.QuestionMessage;
import TravelBuddy.model.messages.TransitionMessage;
import TravelBuddy.model.TriviaQuestion;
import TravelBuddy.model.User;
import TravelBuddy.model.TriviaRoomEntity;
import TravelBuddy.model.TriviaPlayerEntity;
import TravelBuddy.model.TriviaPlayerKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TriviaService {
    private static final int QUESTION_TIME_LIMIT = 10; // 10 seconds time limit
    private static final int BASE_POINTS = 1000;
    private static final int STREAK_BONUS = 100;
    private static final long TIME_LIMIT = 10000; // 10 seconds
    
    @Value("${trivia.service.url}")
    private String triviaServiceUrl;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TriviaRoomRepository triviaRoomRepository;
    
    @Autowired
    private TriviaPlayerRepository triviaPlayerRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger log = LoggerFactory.getLogger(TriviaService.class);
    
    private Map<String, TriviaRoom> activeRooms = new ConcurrentHashMap<>();
    
    private Map<String, Map<Long, Integer>> roomTotalScores = new ConcurrentHashMap<>();
    
    public TriviaRoom createRoom(Long userId) {
        String roomCode = generateRoomCode();
        User host = userService.findById(userId);
        
        // First create and save the room entity
        TriviaRoomEntity roomEntity = new TriviaRoomEntity();
        roomEntity.setRoomCode(roomCode);
        roomEntity.setHostId(userId);
        roomEntity.setHostName(host.getUsername());
        roomEntity.setActive(false);
        roomEntity = triviaRoomRepository.save(roomEntity);  // Save and get the managed entity
        
        // Now create the player entity with the saved room
        TriviaPlayerEntity player = new TriviaPlayerEntity();
        player.setId(new TriviaPlayerKey(roomCode, userId));
        player.setScore(0);
        player.setRoom(roomEntity);  // Set the room reference
        triviaPlayerRepository.save(player);
        
        // Create in-memory room with host information
        TriviaRoom room = new TriviaRoom(roomCode, userId, host.getUsername(), userService);
        activeRooms.put(roomCode, room);
        
        return room;
    }
    
    public TriviaRoom joinRoom(String roomCode, Long userId) {
        TriviaRoom room = getRoomByCode(roomCode);
        if (room == null) {
            log.error("Room {} not found", roomCode);
            return null;
        }
        
        if (room.isActive()) {
            log.error("Room {} is active, cannot join", roomCode);
            return null;
        }
        
        // Check if player is already in room
        if (!room.getPlayers().contains(userId)) {
            room.addPlayer(userId);
        }
        return room;
    }
    
    public List<TriviaQuestion> generateQuestions() {
        List<TriviaQuestion> questions = new ArrayList<>();
        try {
            ResponseEntity<QuestionsResponse> response = 
                restTemplate.getForEntity(triviaServiceUrl + "/api/trivia/questions", QuestionsResponse.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                questions = response.getBody().getQuestions();
                System.out.println("Received " + questions.size() + " questions from trivia service");
                for (TriviaQuestion question : questions) {
                    question.setTimeLimit(QUESTION_TIME_LIMIT);
                    // Map the correct answer to the corresponding option (A, B, C, D)
                    List<String> options = question.getOptions();
                    for (int i = 0; i < options.size(); i++) {
                        if (options.get(i).equals(question.getCorrectAnswer())) {
                            question.setCorrectOption(String.valueOf((char)('A' + i)));
                            break;
                        }
                    }
                }
                return questions;
            }
            throw new RuntimeException("Failed to generate questions");
        } catch (Exception e) {
            log.error("Error generating questions", e);
            throw new RuntimeException("Error calling trivia service: " + e.getMessage());
        }
    }
    
    public void processAnswer(String roomCode, Long userId, String selectedOption, long timeRemaining) {
        TriviaRoom room = activeRooms.get(roomCode);
        if (room != null) {
            // Get active players BEFORE processing the answer
            List<Long> activePlayers = room.getActivePlayers();
            
            // Skip if player is not active
            if (!activePlayers.contains(userId)) {
                log.info("Room {}: Ignoring answer from inactive player {}", roomCode, userId);
                return;
            }

            TriviaQuestion currentQuestion = room.getCurrentQuestion();
            String correctOption = currentQuestion.getCorrectOption();
            if (correctOption == null) {
                // If correctOption is not set, fall back to comparing with full answer
                List<String> options = currentQuestion.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i).equals(currentQuestion.getCorrectAnswer())) {
                        correctOption = String.valueOf((char)('A' + i));
                        currentQuestion.setCorrectOption(correctOption);
                        break;
                    }
                }
            }

            boolean isCorrect = selectedOption.equalsIgnoreCase(correctOption);
            if (isCorrect) {
                // Calculate score based on time remaining
                int score = calculateScore(timeRemaining);
                Map<Long, Integer> totalScores = roomTotalScores.get(roomCode);
                totalScores.merge(userId, score, Integer::sum);
            }

            room.addPlayerAnswer(userId);
            
            roomTotalScores.putIfAbsent(roomCode, new ConcurrentHashMap<>());
            Map<Long, Integer> totalScores = roomTotalScores.get(roomCode);
            totalScores.putIfAbsent(userId, 0);
            
            // Count answers only from active players
            int answeredCount = (int) room.getAnsweredPlayers().stream()
                .filter(activePlayers::contains)
                .count();
                
            log.info("Room {}: Active players: {}, Answered players: {}, Active player list: {}", 
                roomCode, activePlayers.size(), answeredCount, activePlayers);
            
            // Send progress update
            messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, 
                new ProgressMessage("WAITING", answeredCount, activePlayers.size()));
            
            // Force progress if all remaining active players have answered
            if (answeredCount >= activePlayers.size()) {
                log.info("Room {}: All remaining active players ({}) have answered, proceeding to next question", 
                    roomCode, activePlayers.size());
                messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, 
                    new TransitionMessage());
                moveToNextQuestion(roomCode);
            }
        }
    }
    
    public List<Winner> getWinners(String roomCode) {
        TriviaRoom room = activeRooms.get(roomCode);
        if (room == null) return Collections.emptyList();
        
        Map<Long, Integer> finalScores = roomTotalScores.get(roomCode);
        if (finalScores == null) return Collections.emptyList();
        
        return finalScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(3)
            .map(entry -> new Winner(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
    
    public void startGame(String roomCode, List<TriviaQuestion> questions) {
        TriviaRoom room = activeRooms.get(roomCode);
        if (room != null) {
            room.setQuestions(questions);
            room.setCurrentQuestionIndex(0);
            room.setActive(true);
        }
    }
    
    public boolean isLastQuestion(String roomCode) {
        TriviaRoom room = activeRooms.get(roomCode);
        if (room == null) return true;
        
        int currentIndex = room.getCurrentQuestionIndex();
        int totalQuestions = room.getQuestions().size();
        //log.info("Checking if last question: current={}, total={}", currentIndex, totalQuestions);
        
        return currentIndex >= totalQuestions - 1;
    }
    
    private String generateRoomCode() {
        String code;
        do {
            code = String.format("%04d", new Random().nextInt(10000));
        } while (activeRooms.containsKey(code));
        return code;
    }
    
    public TriviaRoom getRoomByCode(String roomCode) {
        return activeRooms.get(roomCode);
    }
    
    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupInactiveRooms() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<TriviaRoomEntity> inactiveRooms = triviaRoomRepository.findByCreatedAtBeforeAndActiveIsFalse(oneHourAgo);
        
        for (TriviaRoomEntity room : inactiveRooms) {
            activeRooms.remove(room.getRoomCode());
            triviaPlayerRepository.deleteByIdRoomCode(room.getRoomCode());
            triviaRoomRepository.delete(room);
        }
    }
    
    public TriviaQuestion getNextQuestion(String roomCode) {
        TriviaRoom room = activeRooms.get(roomCode);
        if (room != null) {
            int nextIndex = room.getCurrentQuestionIndex() + 1;
            room.setCurrentQuestionIndex(nextIndex);
            room.clearPlayerAnswers();
            return room.getQuestions().get(nextIndex);
        }
        return null;
    }
    
    @Transactional
    public void cleanupRoom(String roomCode) {
        // Remove from all in-memory collections
        activeRooms.remove(roomCode);
        roomTotalScores.remove(roomCode);
        
        try {
            // Delete from database
            triviaPlayerRepository.deleteByIdRoomCode(roomCode);
            triviaRoomRepository.deleteByRoomCode(roomCode);
            log.info("Cleaned up room {} from database and memory", roomCode);
        } catch (Exception e) {
            log.error("Error cleaning up room {} from database", roomCode, e);
        }
    }
    
    public int calculateScore(long timeRemaining) {
        // Base score of 100 points for correct answer
        // Plus up to 50 bonus points based on how quickly they answered
        int baseScore = 100;
        int timeBonus = (int)((timeRemaining / (float)QUESTION_TIME_LIMIT) * 50);
        return baseScore + timeBonus;
    }
    
    private void moveToNextQuestion(String roomCode) {
        TriviaRoom room = getRoomByCode(roomCode);
        if (room == null) return;

        room.resetForNextQuestion();
        
        if (isLastQuestion(roomCode)) {
            // Game is over, send winners
            List<Winner> winners = getWinners(roomCode);
            room.setActive(false);
            messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, 
                new GameOverMessage(winners));
            return;
        }

        // Get and send next question after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1 second transition delay
                TriviaQuestion nextQuestion = getNextQuestion(roomCode);
                if (nextQuestion != null) {
                    messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode,
                        new QuestionMessage(nextQuestion));
                }
            } catch (InterruptedException e) {
                log.error("Error during question transition delay", e);
            }
        }).start();
    }
    
    public void handlePlayerLeave(String roomCode, String userId) {
        TriviaRoom room = getRoomByCode(roomCode);
        if (room != null) {
            Long userIdLong = Long.parseLong(userId);
            boolean wasHost = userIdLong.equals(room.getHostId());
            
            // Update active players list before removing the player
            List<Long> activePlayers = room.getActivePlayers();
            activePlayers.remove(userIdLong);
            
            room.removePlayer(userId);
            
            // Remove player from any game-specific collections
            room.getAnsweredPlayers().remove(userIdLong);
            room.getTimedOutPlayers().remove(userIdLong);
            
            // Update host name if needed
            if (wasHost && !room.getPlayers().isEmpty()) {
                Long newHostId = room.getHostId();
                String newHostName = userService.findById(newHostId).getUsername();
                room.setHostName(newHostName);
            }
        }
    }
    
    public void updateRoom(TriviaRoom room) {
        activeRooms.put(room.getRoomCode(), room);
    }
} 
