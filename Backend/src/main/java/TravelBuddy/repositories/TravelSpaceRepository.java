package TravelBuddy.repositories;

import TravelBuddy.model.SpaceMessage;
import TravelBuddy.model.TravelSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TravelSpaceRepository extends JpaRepository<TravelSpace, Long> {

    List<TravelSpace> findByExpirationDateBefore(LocalDateTime now);

    List<TravelSpaceProjection> findAllProjectedBy();


}
