package TravelBuddy.repositories;

import TravelBuddy.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByMessageIdAndUserIdAndReactionType(Long messageId, Long userId, MessageReaction.ReactionType reactionType);
    List<MessageReaction> findByMessageId(Long messageId);
    void deleteByMessageId(Long messageId);
    @Modifying
    @Query("DELETE FROM MessageReaction r WHERE r.messageId = :messageId AND r.userId = :userId")
    void deleteByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);
}

