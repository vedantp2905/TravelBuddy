package TravelBuddy.service;

import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import TravelBuddy.repositories.TravelPostProjection;
import TravelBuddy.repositories.TravelPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class TravelPostService {

    private final TravelPostRepository travelPostRepository;

    @Autowired
    public TravelPostService(TravelPostRepository travelPostRepository) {
        this.travelPostRepository = travelPostRepository;
    }

    public TravelPost createPost(TravelPost post) {
        return (travelPostRepository.save(post));
    }

    public TravelPost findById(Long id) {

        return travelPostRepository.findById(id).orElse(null);
    }

    public Page<TravelPostProjection> getPosts(String category, boolean newest, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, newest ? Sort.by("createdAt").descending() : Sort.by("createdAt").ascending());
        if (category != null && !category.isEmpty()) {
            return travelPostRepository.findByCategory(category, pageable);
        } else {
            return travelPostRepository.findAllProjectedBy(pageable);
        }

//        if (category == null || category.isEmpty()) {
//            if (newest) {
//                return (travelPostRepository.findAllByOrderByCreatedAtDesc());
//            }
//            return (travelPostRepository.findAll());
//        }
//        List<TravelPost> posts = travelPostRepository.findByCategory(category);
//        if (newest) {
//            Collections.reverse(posts);
//        }
//        return posts;
    }

    public List<TravelPostProjection> getUserPosts(User user) {

        return travelPostRepository.findByUser(user);

    }

    public boolean incrementLikeCount(Long postId) {

        if (travelPostRepository.existsById(postId)) {
            TravelPost post = travelPostRepository.getReferenceById(postId);
            int newLikeCount = post.getLikeCount() + 1;
            post.setLikeCount(newLikeCount);
            travelPostRepository.save(post);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean decrementLikeCount(Long postId) {

        if (travelPostRepository.existsById(postId)) {
            TravelPost post = travelPostRepository.getReferenceById(postId);
            if (post.getLikeCount() > 0) {
                int newLikeCount = post.getLikeCount() - 1;
                post.setLikeCount(newLikeCount);
                travelPostRepository.save(post);
                return true;
            }
        }

        return false;
    }

}
