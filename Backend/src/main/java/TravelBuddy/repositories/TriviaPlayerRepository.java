package TravelBuddy.repositories;

import TravelBuddy.model.TriviaPlayerEntity;
import TravelBuddy.model.TriviaPlayerKey;
import TravelBuddy.model.TriviaRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriviaPlayerRepository extends JpaRepository<TriviaPlayerEntity, TriviaPlayerKey> {
    void deleteByIdRoomCode(String roomCode);
    List<TriviaPlayerEntity> findByRoom(TriviaRoomEntity room);
    TriviaPlayerEntity findByIdRoomCodeAndIdUserId(String roomCode, Long userId);
} 