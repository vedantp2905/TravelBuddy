package TravelBuddy.controller;

import TravelBuddy.model.Comment;
import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import TravelBuddy.service.CommentService;
import TravelBuddy.service.TravelPostService;
import TravelBuddy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/comment/")
@Tag(name="Travel Feed Comments", description = "APIs for managing the comments within the Travel Feed ")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private TravelPostService travelPostService;

    @Operation(summary = "Creating comments",
            description = "Creates a comment which has relations to a user and a travel post.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment created successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid input")
    })
    @PostMapping("/create/")
    public ResponseEntity<String> createComment(@RequestBody Map<String, Object> commentBody) {

        try {
            User user = userService.findById(Long.parseLong((String) commentBody.get("userId")));
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            TravelPost post = travelPostService.findById(Long.parseLong((String) commentBody.get("postId")));
            if (post == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }
            String description = (String) commentBody.get("description");

            LocalDateTime createdAt = LocalDateTime.now();
            Comment newComment = new Comment();
            newComment.setUser(user);
            newComment.setPost(post);
            newComment.setCreatedAt(createdAt);
            newComment.setDescription(description);

            commentService.createComment(newComment);
            return ResponseEntity.ok("Comment created successfully.");
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Deleting comments",
            description = "Deletes a comment from the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment created successfully"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "500", description = "Error occurred on server"),
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteComment(@PathVariable Long id) {
        try {
            boolean commentExists = commentService.deleteComment(id);
            if (commentExists) {
                return ResponseEntity.ok("Successfully deleted comment.");
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Comment not found");
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

}
