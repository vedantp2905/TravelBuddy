package TravelBuddy.controller;

import TravelBuddy.model.SpaceMessage;
import TravelBuddy.model.TravelSpace;
import TravelBuddy.model.User;
import TravelBuddy.repositories.SpaceMessageRepository;
import TravelBuddy.repositories.TravelSpaceRepository;
import TravelBuddy.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@ServerEndpoint(value = "/travelspace/{userId}/{travelSpaceId}")
public class TravelSpaceSocket {

    private static Map<Session, Long> sessionUserIdMap = new Hashtable<>(); // Only track userId by session
    private static Map<Long, Session> userIdSessionMap = new Hashtable<>(); // Track session by userId

    private final Logger logger = LoggerFactory.getLogger(TravelSpaceSocket.class);

    private static SpaceMessageRepository messageRepository;
    private static UserRepository userRepository;
    private static TravelSpaceRepository travelSpaceRepository;

    @Autowired
    public void setSpaceMessageRepository(SpaceMessageRepository repo) {
        messageRepository = repo;
    }

    @Autowired
    public void setUserRepository(UserRepository repo) {
        userRepository = repo;
    }

    @Autowired
    public void setTravelSpaceRepository(TravelSpaceRepository repo) {
        travelSpaceRepository = repo;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId, @PathParam("travelSpaceId") Long travelSpaceId) {
        logger.info("User {} connected to TravelSpace {}", userId, travelSpaceId);

        // Track session and user associations
        sessionUserIdMap.put(session, userId);
        userIdSessionMap.put(userId, session);

        //Send welcome message or chat history
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));
        String name = user.getUsername();

        sendMessageToParticularUser(userId, getChatHistory(travelSpaceId));

        String welcomeMessage = "Welcome " + name + " to the space!";
        broadcast(welcomeMessage);
    }

    @OnMessage
    public void onMessage(Session session, String messageJson, @PathParam("travelSpaceId") Long travelSpaceId) throws IOException {
        Long userId = sessionUserIdMap.get(session);
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));
            String username = user.getUsername();

            TravelSpace space = travelSpaceRepository.findById(travelSpaceId)
                    .orElseThrow(() -> new RuntimeException("TravelSpace not found for ID: " + travelSpaceId));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(messageJson);
            String message = jsonNode.get("message").asText();
            Long parentMessageId = jsonNode.has("parentMessageId") ? jsonNode.get("parentMessageId").asLong() : null;

            SpaceMessage spaceMessage = new SpaceMessage();
            spaceMessage.setMessage(message);
            spaceMessage.setSender(user);
            spaceMessage.setTimestamp(LocalDateTime.now());
            spaceMessage.setTravelSpace(space);

            // Set messageType from JSON if present, default to "TEXT"
            String messageType = jsonNode.has("messageType") ? 
                jsonNode.get("messageType").asText() : "TEXT";
            spaceMessage.setMessageType(messageType);

            if (parentMessageId != null) {
                SpaceMessage parentMessage = messageRepository.findById(parentMessageId)
                        .orElseThrow(() -> new RuntimeException("Parent message not found for ID: " + parentMessageId));
                spaceMessage.setParentMessage(parentMessage);  // Set the parent message
            }


            messageRepository.save(spaceMessage);
            String parentUsername = "";
            if (parentMessageId != null) {
                SpaceMessage parentMessage = messageRepository.findById(parentMessageId).get();
                parentUsername = parentMessage.getSender().getUsername();
            }

            String formattedMessage = parentMessageId != null
                    ? String.format("{\"type\":\"%s\", \"content\":\"Reply to %s from %s: %s\"}", 
                        spaceMessage.getMessageType(), parentUsername, username, message)
                    : String.format("{\"type\":\"%s\", \"content\":\"%s: %s\"}", 
                        spaceMessage.getMessageType(), username, message);

            broadcast(formattedMessage);
        }
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = sessionUserIdMap.remove(session);
        userIdSessionMap.remove(userId);
        logger.info("User {} disconnected from TravelSpace", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));
        String name = user.getUsername();
        broadcast(name + " has left the space.");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Error in WebSocket session", throwable);
    }

    private void sendMessageToParticularUser(@PathParam("userId") Long userId, String message) {
        try {
            userIdSessionMap.get(userId).getBasicRemote().sendText(message);
        }
        catch (IOException e) {
            logger.info("Exception: " + e.getMessage().toString());
            e.printStackTrace();
        }
    }


    private void broadcast(String message) {
        userIdSessionMap.forEach((userId, session) -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.error("Error broadcasting message to TravelSpace");
            }
        });
    }

    private String getChatHistory(@PathParam("travelSpaceId") Long travelSpaceId) {
        List<SpaceMessage> messages = messageRepository.findAllByTravelSpaceId(travelSpaceId);
        StringBuilder sb = new StringBuilder();
        if(messages != null && messages.size() != 0) {
            for (SpaceMessage message : messages) {
                sb.append(String.format("{\"messageType\":\"%s\", \"content\":\"%s: %s\"}\n",
                    message.getMessageType(),
                    message.getSender().getUsername(),
                    message.getMessage()));
            }
        }
        return sb.toString();
    }


}


