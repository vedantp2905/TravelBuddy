package TravelBuddy.repositories;

import java.time.LocalDateTime;
import java.util.List;

public interface SpaceMessageProjection {

    Long getId();
    String getMessage();
    LocalDateTime getTimestamp();
    SenderSummary getSender();
    //SpaceMessageProjection getParentMessage();
    //List<SpaceMessageProjection> getReplies();
    SimpleSpaceMessage getParentMessage();
    List<SimpleSpaceMessage> getReplies();

    interface SenderSummary {
        Long getId();
        String getUsername();
    }

    interface SimpleSpaceMessage {
        Long getId();
        String getMessage();
    }


}
