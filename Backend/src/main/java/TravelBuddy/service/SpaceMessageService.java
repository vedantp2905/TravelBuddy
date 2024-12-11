package TravelBuddy.service;

import TravelBuddy.model.SpaceMessage;
import TravelBuddy.model.TravelSpace;
import TravelBuddy.model.User;
import TravelBuddy.repositories.SpaceMessageRepository;
import TravelBuddy.repositories.TravelSpaceRepository;
import TravelBuddy.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SpaceMessageService {

    private final SpaceMessageRepository spaceMessageRepository;
    private final TravelSpaceRepository travelSpaceRepository;
    private final UserRepository userRepository;

    public SpaceMessageService(SpaceMessageRepository spaceMessageRepository, TravelSpaceRepository travelSpaceRepository, UserRepository userRepository) {
        this.spaceMessageRepository = spaceMessageRepository;
        this.travelSpaceRepository = travelSpaceRepository;
        this.userRepository = userRepository;
    }

    public SpaceMessage processMessage(Long travelSpaceId, SpaceMessage message) {

        TravelSpace travelSpace = travelSpaceRepository.findById(travelSpaceId)
                .orElseThrow(() -> new IllegalArgumentException("TravelSpace not found"));

        User sender = userRepository.findById(message.getSender().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        message.setTravelSpace(travelSpace);
        message.setSender(sender);
        message.setTimestamp(LocalDateTime.now());

        return spaceMessageRepository.save(message);
    }
}
