package TravelBuddy.service;

import TravelBuddy.model.Friendship;
import TravelBuddy.model.FriendshipRequestDTO;
import TravelBuddy.model.User;
import TravelBuddy.repositories.FriendshipRepository;
import TravelBuddy.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository) {

        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    public void createFriendship(User user, User friend) {

        Friendship friendship1 = new Friendship();
        friendship1.setUser(user);
        friendship1.setFriend(friend);
        friendship1.setCreatedAt(LocalDateTime.now());

        Friendship friendship2 = new Friendship();
        friendship2.setUser(friend);
        friendship2.setFriend(user);
        friendship2.setCreatedAt(LocalDateTime.now());

        friendshipRepository.save(friendship1);
        friendshipRepository.save(friendship2);
    }

    public boolean friendshipExists(User user, User friend) {

        if (friendshipRepository.existsByUserAndFriend(user,friend) || friendshipRepository.existsByUserAndFriend(friend,user)) {
            return true;
        }
        return false;

    }

    public List<FriendshipRequestDTO> getFriendships(User user) {

        List<Friendship> friendships = friendshipRepository.findByUser(user);

        return (friendships.stream().map(
                friendship -> new FriendshipRequestDTO(
                        friendship.getFriend().getId(),
                        friendship.getFriend().getUsername(),
                        friendship.getCreatedAt()))
                .collect(Collectors.toList()
                )
        );

    }

    public void removeFriend(User user, User friend) {

        friendshipRepository.deleteByUserAndFriend(user,friend);
        friendshipRepository.deleteByUserAndFriend(friend,user);

    }

    public List<User> searchUsers(User searcher, String prompt) {

        prompt = prompt.toLowerCase();
        List<User> users =userRepository.findByUsernameStartingWithIgnoreCase(prompt);
        if (users.contains(searcher)) {
            users.remove(searcher);
        }
        return users;

    }

}
