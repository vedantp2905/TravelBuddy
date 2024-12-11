package TravelBuddy.service;

import TravelBuddy.model.ChatMessage;
import TravelBuddy.model.Conversation;
import TravelBuddy.model.User;
import TravelBuddy.repositories.ChatMessageRepository;
import TravelBuddy.repositories.ConversationRepository;
import TravelBuddy.repositories.MessageReactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import TravelBuddy.model.MessageReaction;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MessageReactionRepository reactionRepository;

    @Transactional
    public ChatMessage saveMessage(ChatMessage chatMessage) throws SQLException {
        chatMessage.setTimestamp(LocalDateTime.now());
        User sender = userService.findById(chatMessage.getUserId());
        chatMessage.setSender(sender.getUsername());

        // Undelete the conversation for both users before saving the message
        unmarkConversationAsDeletedForBoth(chatMessage.getConversationId());

        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getMessagesByConversationId(Long conversationId) {
        return chatMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    @Transactional
    public Conversation createOrGetConversation(Long userId1, Long userId2) {
        Optional<Conversation> existingConversation = conversationRepository.findConversationBetweenUsers(userId1, userId2);
        if (existingConversation.isPresent()) {
            return existingConversation.get();
        }

        Conversation newConversation = new Conversation();
        newConversation.setUser1Id(userId1);
        newConversation.setUser2Id(userId2);
        newConversation.setDeletedForUser1(false);
        newConversation.setDeletedForUser2(false);
        return conversationRepository.save(newConversation);
    }

    public List<Conversation> getNonDeletedConversationsForUser(Long userId) {
        return conversationRepository.findNonDeletedConversationsForUser(userId);
    }

    @Transactional
    public void markConversationAsDeleted(Long conversationId, Long userId) throws SQLException {
        String sql = "UPDATE conversations SET deleted_for_user1 = CASE WHEN user1_id = ? THEN TRUE ELSE deleted_for_user1 END, " +
                     "deleted_for_user2 = CASE WHEN user2_id = ? THEN TRUE ELSE deleted_for_user2 END " +
                     "WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql, userId, userId, conversationId);
        if (updatedRows == 0) {
            throw new RuntimeException("Conversation not found or user is not part of this conversation");
        }
    }

    @Transactional
    public void unmarkConversationAsDeleted(Long conversationId, Long userId) throws SQLException {
        String sql = "UPDATE conversations SET deleted_for_user1 = CASE WHEN user1_id = ? THEN FALSE ELSE deleted_for_user1 END, " +
                     "deleted_for_user2 = CASE WHEN user2_id = ? THEN FALSE ELSE deleted_for_user2 END " +
                     "WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql, userId, userId, conversationId);
        if (updatedRows == 0) {
            throw new RuntimeException("Conversation not found or user is not part of this conversation");
        }
    }

    @Transactional
    public void unmarkConversationAsDeletedForBoth(Long conversationId) throws SQLException {
        String sql = "UPDATE conversations SET deleted_for_user1 = FALSE, deleted_for_user2 = FALSE WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql, conversationId);
        if (updatedRows == 0) {
            throw new RuntimeException("Conversation not found");
        }
    }

    public Long getOtherUserId(Long conversationId, Long userId) {
        String sql = "SELECT CASE WHEN user1_id = ? THEN user2_id ELSE user1_id END AS other_user_id " +
                     "FROM conversations WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId, conversationId);
    }

    @Transactional(readOnly = true)
    public ChatMessage getMessageById(Long messageId) {
        return chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    @Transactional
    public ChatMessage saveReplyMessage(ChatMessage chatMessage) throws SQLException {
        // If this is a reply message, fetch and set the original message content
        if (chatMessage.getReplyToId() != null) {
            ChatMessage originalMessage = getMessageById(chatMessage.getReplyToId());
            chatMessage.setReplyToContent(originalMessage.getContent());
        }
        
        return saveMessage(chatMessage);
    }

    @Transactional
    public ChatMessage deleteMessage(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        
        if (!message.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this message");
        }
        
        // Delete all reactions for this message
        reactionRepository.deleteByMessageId(messageId);
        
        message.setContent("[Message deleted]");
        message.setDeleted(true);
        message.setReactions(null); // Clear reactions from the message object
        
        return chatMessageRepository.save(message);
    }

    @Transactional
    public MessageReaction saveReaction(MessageReaction reaction) {
        // Delete ALL existing reactions from this user for this message
        reactionRepository.deleteByMessageIdAndUserId(reaction.getMessageId(), reaction.getUserId());
        
        // Check if the message is deleted
        ChatMessage message = getMessageById(reaction.getMessageId());
        if (Boolean.TRUE.equals(message.getDeleted())) {
            throw new RuntimeException("Cannot react to deleted message");
        }
        
        // Save the new reaction
        return reactionRepository.save(reaction);
    }

    public void removeReaction(Long messageId, Long userId, MessageReaction.ReactionType type) {
        reactionRepository.findByMessageIdAndUserIdAndReactionType(messageId, userId, type)
            .ifPresent(reactionRepository::delete);
    }

    public List<MessageReaction> getMessageReactions(Long messageId) {
        return reactionRepository.findByMessageId(messageId);
    }

    public ChatMessage getMessageWithReactions(Long messageId) {
        ChatMessage message = getMessageById(messageId);
        if (message != null) {
            List<MessageReaction> reactions = reactionRepository.findByMessageId(messageId);
            Map<String, List<MessageReaction>> reactionMap = new HashMap<>();
            
            for (MessageReaction reaction : reactions) {
                String type = reaction.getReactionType().toString();
                reactionMap.computeIfAbsent(type, k -> new ArrayList<>()).add(reaction);
            }
            
            message.setReactions(reactionMap);
        }
        return message;
    }

    public List<ChatMessage> getConversationMessages(Long conversationId) {
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        
        for (ChatMessage message : messages) {
            List<MessageReaction> reactions = reactionRepository.findByMessageId(message.getId());
            Map<String, List<MessageReaction>> reactionMap = new HashMap<>();
            
            for (MessageReaction reaction : reactions) {
                String type = reaction.getReactionType().toString();
                reactionMap.computeIfAbsent(type, k -> new ArrayList<>()).add(reaction);
            }
            
            message.setReactions(reactionMap);
        }
        
        return messages;
    }
}
