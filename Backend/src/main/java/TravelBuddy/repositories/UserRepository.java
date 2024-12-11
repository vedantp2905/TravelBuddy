package TravelBuddy.repositories;

import TravelBuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(int role);

    List<User> findByUsernameStartingWithIgnoreCase(String prompt);

    User findByEmail(String email);

}
