package TravelBuddy.repositories;

import TravelBuddy.model.Like;
import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUserAndPost(User user, TravelPost post);
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

}
