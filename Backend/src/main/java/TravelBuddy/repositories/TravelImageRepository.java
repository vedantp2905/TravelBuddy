package TravelBuddy.repositories;

import TravelBuddy.model.TravelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelImageRepository extends JpaRepository<TravelImage, Long> {
}
