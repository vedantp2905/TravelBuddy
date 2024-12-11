package TravelBuddy.repositories;

import TravelBuddy.model.TravelDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelDocumentsRepository extends JpaRepository<TravelDocument, Long> {

    List<TravelDocument> findByUserId(Long userId);
}
