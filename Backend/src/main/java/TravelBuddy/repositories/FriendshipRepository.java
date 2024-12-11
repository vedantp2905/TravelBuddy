package TravelBuddy.repositories;

import TravelBuddy.model.Friendship;
import TravelBuddy.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship,Long> {

    boolean existsByUserAndFriend(User user, User friend);

    List<Friendship> findByUser(User user);

    @Transactional
    void deleteByUserAndFriend(User user, User friend);
}
