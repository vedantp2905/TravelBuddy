package TravelBuddy.repositories;


import TravelBuddy.controller.SpaceMessageController;
import TravelBuddy.model.SpaceMessage;
import TravelBuddy.model.TravelSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceMessageRepository extends JpaRepository<SpaceMessage, Long> {

    List<SpaceMessageProjection> findByTravelSpaceId(Long travelSpaceId);
    List<SpaceMessage> findByTravelSpace(TravelSpace travelSpace);
    List<SpaceMessage> findAllByTravelSpaceId(Long id);
    List<SpaceMessage> findByTravelSpaceOrderByTimestamp(TravelSpace travelSpace);




}
