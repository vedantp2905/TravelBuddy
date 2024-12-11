package TravelBuddy.repositories;

import java.time.LocalDateTime;

public interface TravelSpaceProjection {

    Long getId();
    String getTitle();
    String getDescription();
    LocalDateTime getExpirationDate();
}
