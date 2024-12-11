package TravelBuddy.repositories;

import TravelBuddy.model.TravelSpace;
import TravelBuddy.model.User;
import TravelBuddy.model.UserTravelSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserTravelSpaceRepository extends JpaRepository<UserTravelSpace, Long> {

    boolean existsByUserAndTravelSpace(User user, TravelSpace space);

    List<UserTravelSpace> findByTravelSpaceId(Long spaceId);
}
