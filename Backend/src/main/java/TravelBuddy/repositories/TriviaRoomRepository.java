package TravelBuddy.repositories;

import TravelBuddy.model.TriviaRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TriviaRoomRepository extends JpaRepository<TriviaRoomEntity, String> {
    TriviaRoomEntity findByRoomCode(String roomCode);
    List<TriviaRoomEntity> findByCreatedAtBeforeAndActiveIsFalse(LocalDateTime dateTime);
    Optional<TriviaRoomEntity> findByRoomCodeAndActiveTrue(String roomCode);
    
    @Query("SELECT r FROM TriviaRoomEntity r WHERE r.active = true AND r.createdAt < :cutoffTime")
    List<TriviaRoomEntity> findStaleActiveRooms(@Param("cutoffTime") LocalDateTime cutoffTime);
    void deleteByRoomCode(String roomCode);
} 