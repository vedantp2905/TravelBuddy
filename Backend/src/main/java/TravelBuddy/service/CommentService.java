package TravelBuddy.service;

import TravelBuddy.model.Comment;
import TravelBuddy.repositories.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public void createComment(Comment comment) {

        commentRepository.save(comment);
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public boolean deleteComment(Long id) {
        if (commentRepository.existsById(id)) {

            commentRepository.deleteById(id);
            return true; //successfully deleted
        }
        else {
            return false; //failed to delete
        }
    }

}
