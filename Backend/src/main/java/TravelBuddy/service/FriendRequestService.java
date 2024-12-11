package TravelBuddy.service;

import TravelBuddy.model.FriendRequest;
import TravelBuddy.model.FriendRequestDTO;
import TravelBuddy.model.User;
import TravelBuddy.repositories.FriendRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendRequestService {

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    public FriendRequestService(FriendRequestRepository friendRequestRepository) {
        this.friendRequestRepository = friendRequestRepository;
    }

    public void createFriendRequest(User sender, User receiver) {

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setSentAt(LocalDateTime.now());
        friendRequestRepository.save(friendRequest);
    }

    public boolean friendRequestExists(User sender, User receiver) {

        return friendRequestRepository.existsBySenderAndReceiver(sender, receiver);

    }

    public List<FriendRequestDTO> getFriendRequests(User receiver) {

        List<FriendRequest> requests = friendRequestRepository.findByReceiver(receiver);

        return (requests.stream().map(
                friendRequest -> new FriendRequestDTO(
                        friendRequest.getSender().getId(),
                        friendRequest.getReceiver().getId(),
                        friendRequest.getSentAt())).collect(Collectors.toList()));

    }

    public FriendRequest getRequest(User sender, User receiver) {

        return friendRequestRepository.findBySenderAndReceiver(sender, receiver);

    }

    public void deleteRequest(FriendRequest request) {

        friendRequestRepository.delete(request);

    }


}
