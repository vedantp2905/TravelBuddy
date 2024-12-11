package TravelBuddy.repositories;

import TravelBuddy.model.CityInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CityInfoRepository extends JpaRepository<CityInfo, Long> {
    Optional<CityInfo> findByCityName(String cityName);
} 