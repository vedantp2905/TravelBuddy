package TravelBuddy.repositories;

import TravelBuddy.model.Comment;
import TravelBuddy.model.Like;
import TravelBuddy.model.User;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TravelPostProjection {
    Long getId();
    String getDescription();

    User getUser();

    LocalDateTime getCreatedAt();

    Like getLikes();

    Comment getComments();

    String getCategory();

    int getRating();

    LocalDateTime getStartDate();

    LocalDateTime getEndDate();

    String getDestination();

    int getLikeCount();


    List<ImageIdOnly> getImages();

    interface ImageIdOnly {
        Long getId();
    }
}
