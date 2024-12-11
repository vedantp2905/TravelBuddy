package TravelBuddy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import TravelBuddy.service.TriviaService;
import TravelBuddy.service.RewardService;
import TravelBuddy.service.UserService;
import TravelBuddy.model.TriviaRoom;
import TravelBuddy.model.TriviaQuestion;
import TravelBuddy.model.requests.CreateRoomRequest;
import TravelBuddy.model.messages.RoomUpdateMessage;
import TravelBuddy.model.messages.GameStartMessage;
import TravelBuddy.model.messages.QuestionMessage;
import TravelBuddy.model.messages.GameOverMessage;
import TravelBuddy.model.requests.AnswerRequest;
import TravelBuddy.model.Winner;
import TravelBuddy.model.messages.RoomCreatedMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import TravelBuddy.model.messages.ProgressMessage;
import org.json.JSONObject;

import TravelBuddy.model.messages.TransitionMessage;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.SendTo;
import org.json.JSONException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TriviaController {
    private static final Logger log = LoggerFactory.getLogger(TriviaController.class);
    
    @Autowired
    private TriviaService triviaService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RewardService rewardService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/trivia/create")
    public void createRoom(@Payload CreateRoomRequest request) {
        log.info("Received create room request from userId: {}", request.getUserId());
        TriviaRoom room = triviaService.createRoom(request.getUserId());
        
        RoomCreatedMessage message = new RoomCreatedMessage("ROOM_CREATED", room.getRoomCode(), userService.findById(request.getUserId()).getUsername());
        log.info("Sending to frontend - ROOM_CREATED: {}", message);
        log.debug("Sending to destination: /topic/trivia/room/create");
        messagingTemplate.convertAndSend("/topic/trivia/room/create", message);
        log.debug("Message sent successfully");
    }
    
    @MessageMapping("/trivia/join/{roomCode}")
    @SendTo("/topic/trivia/room/{roomCode}")
    public ResponseEntity<?> joinRoom(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JSONObject request = new JSONObject(payload);
            Long userId = request.getLong("userId");
            
            // Check if room exists and get current players
            TriviaRoom room = triviaService.getRoomByCode(roomCode);
            if (room == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Room not found");
            }
            
            List<Long> currentPlayers = room.getPlayers();
            if (currentPlayers.size() >= 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject()
                        .put("type", "ROOM_FULL")
                        .put("message", "Room is full (maximum 6 players)")
                        .toString());
            }
            
            // Add player to room
            currentPlayers.add(userId);
            room.setPlayers(currentPlayers);
            triviaService.updateRoom(room);
            
            // Broadcast updated player list
            List<String> usernames = currentPlayers.stream()
                .map(playerId -> userService.findById(playerId).getUsername())
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(new JSONObject()
                .put("type", "JOIN")
                .put("players", usernames)
                .toString());
                
        } catch (JSONException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid request format");
        }
    }
    
    @MessageMapping("/trivia/start/{roomCode}")
    public void startGame(@DestinationVariable String roomCode, @Payload(required = false) String payload) {
        log.info("Received start game request for room: {} with payload: {}", roomCode, payload);
        
        if (roomCode == null || roomCode.equals("null")) {
            log.error("Invalid room code received in start game request");
            return;
        }
        
        // Validate room exists and is not active
        TriviaRoom room = triviaService.getRoomByCode(roomCode);
        if (room == null) {
            log.error("Attempt to start non-existent room: {}", roomCode);
            return;
        }
        
        if (room.isActive()) {
            log.error("Attempt to start already active room: {}", roomCode);
            return;
        }
        
        // Activate the room
        room.setActive(true);
        
        // Send GAME_STARTED message immediately
        GameStartMessage startMessage = new GameStartMessage("GAME_STARTED");
        messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, startMessage);
        
        List<TriviaQuestion> questions = triviaService.generateQuestions();
        triviaService.startGame(roomCode, questions);
        
        // Send first question after a delay
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
                QuestionMessage questionMessage = new QuestionMessage(questions.get(0));
                //log.info("Sending to frontend - First Question: {}", questionMessage);
                messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, questionMessage);
            } catch (InterruptedException e) {
                log.error("Error during question delay", e);
            }
        }).start();
    }
    
    @MessageMapping("/trivia/answer/{roomCode}")
    public void handleAnswer(@DestinationVariable String roomCode, @Payload AnswerRequest request) {
        TriviaRoom room = triviaService.getRoomByCode(roomCode);
        if (room == null) return;

        // Get the current question
        TriviaQuestion currentQuestion = room.getCurrentQuestion();
        
        // Log the comparison for debugging
        log.info("Room {}: User {} submitted option: {}", roomCode, request.getUserId(), request.getAnswer());
        log.info("Room {}: Question '{}' - Correct option: {} ({})", 
            roomCode,
            currentQuestion.getQuestion(),
            currentQuestion.getCorrectOption(),
            currentQuestion.getCorrectAnswer());
        
        // Process the answer - pass both the selected option and correct option
        triviaService.processAnswer(roomCode, request.getUserId(), request.getAnswer(), 
            request.getTimeRemaining());
        
        // Get active players count
        List<Long> activePlayers = room.getActivePlayers();
        int activeCount = activePlayers.size();
        int answeredCount = (int) room.getAnsweredPlayers().stream()
            .filter(activePlayers::contains)
            .count();
        
        log.info("Room {}: Processing answer - Active players: {}, Answered: {}, Active list: {}", 
            roomCode, activeCount, answeredCount, activePlayers);

        log.info("User {} answered: {}", request.getUserId(), request.getAnswer());
        log.info("Correct answer: {}", room.getCurrentQuestion().getCorrectAnswer());
        
        // Send progress update to all players
        messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, 
            new ProgressMessage("WAITING", answeredCount, activeCount));
        
        // Check if all active players have answered
        if (answeredCount >= activeCount) {
            if (triviaService.isLastQuestion(roomCode)) {
                // Game is over, send winners immediately
                List<Winner> winners = triviaService.getWinners(roomCode);
                GameOverMessage gameOverMessage = new GameOverMessage(winners);
                messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, gameOverMessage);
                return;
            }
            // Send transition message first
            messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, 
                new TransitionMessage());
            // Move to next question after delay
            moveToNextQuestion(roomCode);
        }
    }

    private void moveToNextQuestion(String roomCode) {
        TriviaRoom room = triviaService.getRoomByCode(roomCode);
        if (room == null) return;

        room.resetForNextQuestion();
        int nextIndex = room.getCurrentQuestionIndex() + 1;

        if (nextIndex >= room.getQuestions().size()) {
            List<Winner> winners = triviaService.getWinners(roomCode);
            room.setActive(false); // Deactivate room when game ends
            messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, 
                new GameOverMessage(winners));
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(1000); // Show transition for 1 second
                room.setCurrentQuestionIndex(nextIndex);
                QuestionMessage nextQuestion = new QuestionMessage(room.getCurrentQuestion());
                messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, nextQuestion);
            } catch (InterruptedException e) {
                log.error("Error during question delay", e);
            }
        }).start();
    }

    @MessageMapping("/trivia/leave/{roomCode}")
    public void handlePlayerLeave(@DestinationVariable String roomCode, @Payload String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            String userId = json.getString("userId");
            TriviaRoom room = triviaService.getRoomByCode(roomCode);
            
            if (room != null) {
                // Check if the leaving player is the host
                if (room.getHostId().toString().equals(userId)) {
                    // Host is leaving, notify all players and cleanup room
                    messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode,
                        new RoomUpdateMessage("HOST_LEFT", null, "Host has left, room closed"));
                    triviaService.cleanupRoom(roomCode);
                    return;
                }
                
                // Regular player leaving logic
                triviaService.handlePlayerLeave(roomCode, userId);
                
                List<String> remainingPlayers = room.getPlayers().stream()
                    .map(playerId -> userService.findById(playerId).getUsername())
                    .collect(Collectors.toList());
                    
                // Send update to all players with remaining players list
                RoomUpdateMessage message = new RoomUpdateMessage("PLAYER_LEFT", remainingPlayers, "");
                messagingTemplate.convertAndSend("/topic/trivia/room/" + roomCode, message);
                
                // If no players left, cleanup the room
                if (remainingPlayers.isEmpty()) {
                    triviaService.cleanupRoom(roomCode);
                }
            }
        } catch (Exception e) {
            log.error("Error handling player leave", e);
        }
    }
    @GetMapping("/api/trivia/room/{roomCode}")
    public ResponseEntity<?> checkRoomExists(@PathVariable String roomCode) {
        log.info("Checking if room exists: {}", roomCode);
        TriviaRoom room = triviaService.getRoomByCode(roomCode);
        
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        // If room exists but is active, return a different status
        if (room.isActive()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Room is already active");
        }
        
        return ResponseEntity.ok().build();
    }
} 
