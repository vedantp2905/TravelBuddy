package TravelBuddy.repositories;

import TravelBuddy.model.TravelDocument;
import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPostRepository extends JpaRepository<TravelPost, Long> {

    Page<TravelPostProjection> findByCategory(String category, Pageable pageable);

    Page<TravelPostProjection> findAllProjectedBy(Pageable pageable);

    List<TravelPostProjection> findByUser(User user);

    List<TravelPost> findAllByOrderByCreatedAtDesc();
}
