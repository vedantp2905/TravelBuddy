package TravelBuddy.service;

import TravelBuddy.model.Like;
import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import TravelBuddy.repositories.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LikeService {

    private final LikeRepository likeRepository;

    @Autowired
    private TravelPostService travelPostService;

    @Autowired
    public LikeService(LikeRepository likeRepository, TravelPostService travelPostService) {
        this.likeRepository = likeRepository;
        this.travelPostService = travelPostService;
    }

    public void createLike(Like like) {

        likeRepository.save(like);
    }

    public boolean deleteLikeById(Long id) {
        if (likeRepository.existsById(id)) {

            likeRepository.deleteById(id);
            return true; //successfully deleted
        }
        else {
            return false; //failed to delete
        }
    }

    public boolean deleteLike(Long userId, Long postId) {
        Optional<Like> likeOptional = likeRepository.findByUserIdAndPostId(userId, postId);

        if (likeOptional.isPresent()) {
            travelPostService.decrementLikeCount(postId);
            likeRepository.delete(likeOptional.get());

            return true; //successfully deleted
        } else {
            return false; //did not delete or didn't exist
        }
    }

    public boolean likeAlreadyExists(User user, TravelPost post) {

        if (likeRepository.existsByUserAndPost(user,post)) {
            return true;
        }
        else {
            return false;
        }

    }
}
