package TravelBuddy.service;

import TravelBuddy.model.TravelSpace;
import TravelBuddy.repositories.TravelSpaceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TravelSpaceCleanupService {

    private final TravelSpaceRepository travelSpaceRepository;

    public TravelSpaceCleanupService(TravelSpaceRepository travelSpaceRepository) {
        this.travelSpaceRepository = travelSpaceRepository;
    }

    @Scheduled(fixedRate = 3600000)
    public void deleteExpiredTravelSpaces() {
        LocalDateTime now = LocalDateTime.now();
        List<TravelSpace> expiredSpaces = travelSpaceRepository.findByExpirationDateBefore(now);

        if (!expiredSpaces.isEmpty()) {
            travelSpaceRepository.deleteAll(expiredSpaces);
            System.out.println("Deleted expired TravelSpaces: " + expiredSpaces.size());
        }
    }
}

