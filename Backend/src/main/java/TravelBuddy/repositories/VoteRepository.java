package TravelBuddy.repositories;

import TravelBuddy.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByPoll_IdAndUserId(Long pollId, Long userId);
    
    Vote findByPoll_IdAndUserId(Long pollId, Long userId);
    
    @Query("SELECT v.selectedOption as option, COUNT(v) as count FROM Vote v " +
           "WHERE v.poll.id = :pollId GROUP BY v.selectedOption")
    List<Map<String, Object>> countVotesByPollIdGroupByOption(@Param("pollId") Long pollId);

    List<Vote> findByPoll_Id(Long pollId);
} 