package TravelBuddy.repositories;

import TravelBuddy.model.FriendRequest;
import TravelBuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsBySenderAndReceiver(User sender, User receiver);

    FriendRequest findBySenderAndReceiver(User sender, User receiver);

    List<FriendRequest> findByReceiver(User receiver);
}
