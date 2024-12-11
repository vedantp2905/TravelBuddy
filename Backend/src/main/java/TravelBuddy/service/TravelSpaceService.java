package TravelBuddy.service;

import TravelBuddy.model.TravelSpace;
import TravelBuddy.model.User;
import TravelBuddy.model.UserTravelSpace;
import TravelBuddy.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TravelSpaceService {

    @Autowired
    private final TravelSpaceRepository travelSpaceRepository;

    @Autowired
    private final SpaceMessageRepository spaceMessageRepository;
    @Autowired
    private UserTravelSpaceRepository userTravelSpaceRepository;

    public TravelSpaceService(TravelSpaceRepository travelSpaceRepository, SpaceMessageRepository spaceMessageRepository) {
        this.travelSpaceRepository = travelSpaceRepository;
        this.spaceMessageRepository = spaceMessageRepository;
    }


    public TravelSpace createTravelSpace(TravelSpace travelSpace) {
        return travelSpaceRepository.save(travelSpace);
    }

//    public List<TravelSpace> getAllTravelSpaces() {
//        return travelSpaceRepository.findAll();
//    }

    public List<TravelSpaceProjection> getAllTravelSpaces() {
        return travelSpaceRepository.findAllProjectedBy();
    }

    public TravelSpace getSpaceById(Long id) {
        return travelSpaceRepository.getReferenceById(id);
    }

    public UserTravelSpace joinTravelSpace(User user, TravelSpace travelSpace, String color) {

        if (userTravelSpaceRepository.existsByUserAndTravelSpace(user,travelSpace)) {
            return null; //already exists
        }

        UserTravelSpace newUserTravelSpace = new UserTravelSpace();
        newUserTravelSpace.setUser(user);
        newUserTravelSpace.setTravelSpace(travelSpace);
        newUserTravelSpace.setColor(color);
        return userTravelSpaceRepository.save(newUserTravelSpace);
    }

    public List<UserTravelSpace> getUsersFromTravelSpace(TravelSpace travelSpace) {

        List<UserTravelSpace> users = userTravelSpaceRepository.findByTravelSpaceId(travelSpace.getId());
        return users != null ? users : Collections.emptyList();
    }

    public List<SpaceMessageProjection> getMessages(Long spaceId) {

        return spaceMessageRepository.findByTravelSpaceId(spaceId);
    }

//    public void addUser
}

