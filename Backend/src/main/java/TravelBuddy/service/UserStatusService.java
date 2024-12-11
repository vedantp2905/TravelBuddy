package TravelBuddy.service;

import TravelBuddy.model.User;
import TravelBuddy.model.UserStatus;
import TravelBuddy.repositories.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserStatusService {

    @Autowired
    private UserStatusRepository userStatusRepository;

    public UserStatusService(UserStatusRepository userStatusRepository) {
        this.userStatusRepository = userStatusRepository;
    }

    public void saveStatus(User user, String prompt) {

        UserStatus status = new UserStatus();
        status.setUser(user);
        status.setStatus(prompt);
        status.setCreatedAt(LocalDateTime.now());

        userStatusRepository.save(status);

    }

    public UserStatus getStatus(User user) {

        return userStatusRepository.findByUser(user);

    }

    public boolean statusExists(User user) {

        return userStatusRepository.existsByUser(user);

    }

    @Scheduled(fixedRate = 3600000)
    public void deleteExpiredStatuses() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<UserStatus> expiredStatuses = userStatusRepository.findByCreatedAtBefore(cutoffTime);

        if (!expiredStatuses.isEmpty()) {
            userStatusRepository.deleteAll(expiredStatuses);
            System.out.println("Deleted expired TravelSpaces: " + expiredStatuses.size());
        }
    }


}
