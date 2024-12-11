package TravelBuddy.controller;

import TravelBuddy.model.Like;
import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import TravelBuddy.service.LikeService;
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
@RequestMapping("/api/like/")
@Tag(name="Travel Feed Likes", description = "APIs for managing Travel Feed likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @Autowired
    private TravelPostService travelPostService;

    @Operation(summary = "Creating Like",
            description = "Creates a Like associated with a TravelPost and saves it to the server.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like created successfully"),
            @ApiResponse(responseCode = "404", description = "User or Post not found"),
            @ApiResponse(responseCode = "400", description = "Like already exists")
    })
    @PostMapping("/create/")
    public ResponseEntity<String> createLike(@RequestBody Map<String, Object> likeBody) {
        try {
            User user = userService.findById(Long.parseLong((String) likeBody.get("userId")));
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            TravelPost post = travelPostService.findById(Long.parseLong((String) likeBody.get("postId")));
            if (post == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }

            if (likeService.likeAlreadyExists(user, post)) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This like already exists");
            }

            LocalDateTime createdAt = LocalDateTime.now();
            Like newLike = new Like();
            newLike.setUser(user);
            newLike.setPost(post);
            newLike.setCreatedAt(createdAt);

            likeService.createLike(newLike);
            travelPostService.incrementLikeCount(post.getId());
            return ResponseEntity.ok("Like created successfully.");
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Deleting Like",
            description = "Deletes a like from the server.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Like not found")

    })
    @DeleteMapping("/delete/")
    public ResponseEntity<?> deleteLike(@RequestParam Long userId, @RequestParam Long travelPostId) {

        boolean deleted = likeService.deleteLike(userId, travelPostId);
        if (deleted) {
            return ResponseEntity.ok("Like deleted successfully.");
        }
        else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Like not found.");
        }
    }

//    @DeleteMapping("/delete/{id}")
//    public ResponseEntity<String> deleteLike(@PathVariable Long id) {
//        try {
//            boolean likeExists = likeService.deleteLike(id);
//            if (likeExists) {
//                //likeService.find
//                //travelPostService.decrementLikeCount(post.getId());
//                return ResponseEntity.ok("Successfully deleted like.");
//            }
//            else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Like not found");
//            }
//        }
//        catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
//        }
//    }
}
