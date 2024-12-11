package TravelBuddy.repositories;

import TravelBuddy.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findByActiveTrue();
    List<Poll> findByActiveTrueAndExpiresAtBefore(LocalDateTime dateTime);
} 