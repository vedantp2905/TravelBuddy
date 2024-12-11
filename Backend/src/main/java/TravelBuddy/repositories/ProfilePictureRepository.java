package TravelBuddy.repositories;

import TravelBuddy.model.ProfilePicture;
import TravelBuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfilePictureRepository extends JpaRepository<ProfilePicture, Long> {

    ProfilePicture findByUser(User user);

    boolean existsByUser(User user);
}
