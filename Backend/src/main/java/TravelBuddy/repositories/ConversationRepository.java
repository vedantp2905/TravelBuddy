package TravelBuddy.repositories;

import TravelBuddy.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser1IdOrUser2Id(Long userId, Long userId2);
    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);
    List<Conversation> findByUser1IdOrUser2IdAndDeletedForUser1FalseAndDeletedForUser2False(Long userId1, Long userId2);

    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.user1Id = :user1Id AND c.user2Id = :user2Id) OR " +
           "(c.user1Id = :user2Id AND c.user2Id = :user1Id)")
    Optional<Conversation> findConversationBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT c FROM Conversation c WHERE " +
           "((c.user1Id = :userId AND c.deletedForUser1 = false) OR " +
           "(c.user2Id = :userId AND c.deletedForUser2 = false))")
    List<Conversation> findNonDeletedConversationsForUser(@Param("userId") Long userId);
}
