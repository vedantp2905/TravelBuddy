package TravelBuddy.repositories;

import TravelBuddy.model.TripTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripTaskRepository extends JpaRepository<TripTask, Long> {
    List<TripTask> findByUserId(Long userId);
    List<TripTask> findByCompletedFalse();
} 