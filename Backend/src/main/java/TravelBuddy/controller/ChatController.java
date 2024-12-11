package TravelBuddy.controller;

import TravelBuddy.model.ChatMessage;
import TravelBuddy.model.Conversation;
import TravelBuddy.repositories.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import TravelBuddy.model.User;
import TravelBuddy.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import TravelBuddy.service.ChatService;
import java.util.List;
import java.util.Optional;
import java.sql.SQLException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import TravelBuddy.model.MessageReaction;
import org.springframework.messaging.handler.annotation.Payload;

@RestController
@RequestMapping("/api")
@Tag(name = "Chat Management", description = "APIs for managing chat functionality")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    

    @Operation(summary = "Send chat message",
            description = "Sends a new chat message in a conversation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @MessageMapping("/chat")
    public void sendMessage(
        @Parameter(description = "Chat message details") ChatMessage chatMessage
    ) {
        try {
            ChatMessage savedMessage;
            if (chatMessage.getReplyToId() != null) {
                savedMessage = chatService.saveReplyMessage(chatMessage);
            } else {
                savedMessage = chatService.saveMessage(chatMessage);
            }
            
            // Send the message to the specific conversation topic
            messagingTemplate.convertAndSend("/topic/conversations/" + chatMessage.getConversationId(), savedMessage);
            
            // Get both users involved in the conversation
            Long otherUserId = chatService.getOtherUserId(chatMessage.getConversationId(), chatMessage.getUserId());
            
            // Send conversation update notifications to both users
            messagingTemplate.convertAndSend("/user/" + chatMessage.getUserId() + "/conversations", "update");
            messagingTemplate.convertAndSend("/user/" + otherUserId + "/conversations", "update");
            
            // Send message notifications
            messagingTemplate.convertAndSend("/topic/user/" + chatMessage.getUserId(), savedMessage);
            messagingTemplate.convertAndSend("/topic/user/" + otherUserId, savedMessage);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Operation(summary = "Get user conversations",
            description = "Retrieves all conversations for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved conversations"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getConversations(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            System.out.println("Getting conversations for user: " + userId);
            
            List<Conversation> conversations = conversationRepository.findNonDeletedConversationsForUser(userId);
            System.out.println("Found " + conversations.size() + " conversations for user " + userId);
            
            List<Map<String, Object>> conversationsWithUsernames = new ArrayList<>();
            for (Conversation conv : conversations) {
                try {
                    System.out.println(String.format(
                        "Processing conversation ID: %d, User1: %d, User2: %d",
                        conv.getId(), conv.getUser1Id(), conv.getUser2Id()
                    ));
                    
                    Map<String, Object> convMap = new HashMap<>();
                    convMap.put("id", conv.getId());
                    Long otherUserId = conv.getUser1Id().equals(userId) ? conv.getUser2Id() : conv.getUser1Id();
                    User otherUser = userService.findById(otherUserId);
                    
                    if (otherUser == null) {
                        System.out.println("Warning: Could not find user with ID: " + otherUserId);
                        continue;
                    }
                    
                    convMap.put("otherUserId", otherUserId);
                    convMap.put("otherUsername", otherUser.getUsername());
                    conversationsWithUsernames.add(convMap);
                } catch (Exception e) {
                    System.err.println("Error processing conversation " + conv.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return ResponseEntity.ok(conversationsWithUsernames);
        } catch (Exception e) {
            System.err.println("Error getting conversations for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    @Operation(summary = "Create conversation",
            description = "Creates a new conversation between two users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversation created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or cannot chat with self")
    })
    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(
        @Parameter(description = "User IDs for conversation") @RequestBody Map<String, Long> request
    ) {
        Long userId = request.get("userId");
        Long otherUserId = request.get("otherUserId");
        
        if (userId == null || otherUserId == null) {
            return ResponseEntity.badRequest().body("userId and otherUserId must not be null");
        }
        
        // Prevent chatting with self
        if (userId.equals(otherUserId)) {
            return ResponseEntity.badRequest().body("Cannot start a conversation with yourself");
        }
        
        Optional<Conversation> existingConversation = conversationRepository.findConversationBetweenUsers(userId, otherUserId);
        if (existingConversation.isPresent()) {
            Conversation conversation = existingConversation.get();
            return ResponseEntity.ok(createConversationResponse(conversation, userId));
        }
        
        Conversation newConversation = new Conversation();
        newConversation.setUser1Id(userId);
        newConversation.setUser2Id(otherUserId);
        newConversation.setDeletedForUser1(false);
        newConversation.setDeletedForUser2(false);
        
        Conversation savedConversation = conversationRepository.save(newConversation);
        return ResponseEntity.ok(createConversationResponse(savedConversation, userId));
    }

    @Operation(summary = "Get conversation messages",
            description = "Retrieves all messages in a conversation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved messages"),
        @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<?> getConversationMessages(@PathVariable Long conversationId) {
        try {
            List<ChatMessage> messages = chatService.getConversationMessages(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Get single message",
            description = "Retrieves a specific message by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved message"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    @GetMapping("/messages/single/{messageId}")
    public ResponseEntity<ChatMessage> getMessage(
        @Parameter(description = "ID of the message") @PathVariable Long messageId
    ) {
        try {
            ChatMessage message = chatService.getMessageById(messageId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, Object> createConversationResponse(Conversation conversation, Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", conversation.getId());
        Long otherUserId = conversation.getUser1Id().equals(userId) ? conversation.getUser2Id() : conversation.getUser1Id();
        User otherUser = userService.findById(otherUserId);
        response.put("otherUserId", otherUserId);
        response.put("otherUsername", otherUser.getUsername());
        return response;
    }

    @Operation(summary = "Delete conversation",
            description = "Marks a conversation as deleted for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversation marked as deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Conversation not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<?> deleteConversation(
        @Parameter(description = "ID of the conversation") @PathVariable Long conversationId,
        @Parameter(description = "ID of the user") @RequestParam Long userId
    ) {
        try {
            chatService.markConversationAsDeleted(conversationId, userId);
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error marking conversation as deleted: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Undelete conversation",
            description = "Unmarks a conversation as deleted for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conversation unmarked as deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Conversation not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/conversations/{conversationId}/undelete")
    public ResponseEntity<?> undeleteConversation(
        @Parameter(description = "ID of the conversation") @PathVariable Long conversationId,
        @Parameter(description = "ID of the user") @RequestParam Long userId
    ) {
        try {
            chatService.unmarkConversationAsDeleted(conversationId, userId);
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error unmarking conversation as deleted: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete message",
            description = "Marks a message as deleted")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Message deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Message not found"),
        @ApiResponse(responseCode = "403", description = "User not authorized to delete message")
    })
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
        @Parameter(description = "ID of the message") @PathVariable Long messageId,
        @Parameter(description = "ID of the user") @RequestParam Long userId
    ) {
        try {
            ChatMessage deletedMessage = chatService.deleteMessage(messageId, userId);
            
            // Send notification to both users in the conversation
            Long otherUserId = chatService.getOtherUserId(deletedMessage.getConversationId(), userId);
            
            // Send to conversation topic
            messagingTemplate.convertAndSend("/topic/conversations/" + deletedMessage.getConversationId(), deletedMessage);
            
            // Send to individual user topics
            messagingTemplate.convertAndSend("/topic/user/" + userId, deletedMessage);
            messagingTemplate.convertAndSend("/topic/user/" + otherUserId, deletedMessage);
            
            return ResponseEntity.ok(deletedMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @MessageMapping("/chat/react")
    public void handleReaction(MessageReaction reaction) {
        try {
            MessageReaction savedReaction = chatService.saveReaction(reaction);
            ChatMessage message = chatService.getMessageById(reaction.getMessageId());
            Long conversationId = message.getConversationId();
            
            // Create update with full message state
            Map<String, Object> update = createReactionUpdate(savedReaction, message);
            
            // Broadcast to conversation topic
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, update);
            
            // Send to both users
            Long otherUserId = chatService.getOtherUserId(conversationId, reaction.getUserId());
            messagingTemplate.convertAndSend("/topic/user/" + reaction.getUserId(), update);
            messagingTemplate.convertAndSend("/topic/user/" + otherUserId, update);
        } catch (RuntimeException e) {
            // Handle error
            e.printStackTrace();
        }
    }

    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<?> removeReaction(
        @PathVariable Long messageId,
        @RequestParam Long userId,
        @RequestParam MessageReaction.ReactionType reactionType
    ) {
        try {
            chatService.removeReaction(messageId, userId, reactionType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private Map<String, Object> createReactionUpdate(MessageReaction reaction, ChatMessage message) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "REACTION");
        update.put("messageId", reaction.getMessageId());
        update.put("userId", reaction.getUserId());
        update.put("reactionType", reaction.getReactionType());
        update.put("conversationId", message.getConversationId());
        return update;
    }

    @MessageMapping("/chat/remove-reaction")
    public void handleRemoveReaction(@Payload Map<String, Object> request) {
        Long messageId = Long.parseLong(request.get("messageId").toString());
        Long userId = Long.parseLong(request.get("userId").toString());
        MessageReaction.ReactionType reactionType = MessageReaction.ReactionType.valueOf(request.get("reactionType").toString());
        
        chatService.removeReaction(messageId, userId, reactionType);
        ChatMessage message = chatService.getMessageWithReactions(messageId); // Get updated message with reactions
        Long conversationId = message.getConversationId();
        
        // Create update with full message state
        Map<String, Object> update = new HashMap<>();
        update.put("type", "REMOVE_REACTION");
        update.put("messageId", messageId);
        update.put("userId", userId);
        update.put("reactionType", reactionType);
        update.put("message", message);  // Add the full message
        
        // Send only to user topics
        Long otherUserId = chatService.getOtherUserId(conversationId, userId);
        messagingTemplate.convertAndSend("/topic/user/" + userId, update);
        messagingTemplate.convertAndSend("/topic/user/" + otherUserId, update);
    }
}
