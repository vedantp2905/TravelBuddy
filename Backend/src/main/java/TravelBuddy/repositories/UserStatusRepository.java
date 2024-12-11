package TravelBuddy.repositories;


import TravelBuddy.model.TravelSpace;
import TravelBuddy.model.User;
import TravelBuddy.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    boolean existsByUser(User user);

    List<UserStatus> findByCreatedAtBefore(LocalDateTime now);
    UserStatus findByUser(User user);

}
