package TravelBuddy.controller;

import TravelBuddy.model.Comment;
import TravelBuddy.model.TravelPost;
import TravelBuddy.model.User;
import TravelBuddy.repositories.TravelPostProjection;
import TravelBuddy.repositories.TravelPostRepository;
import TravelBuddy.service.CommentService;
import TravelBuddy.service.TravelPostService;
import TravelBuddy.service.UserService;
import TravelBuddy.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/post/")
@Tag(name="Travel Posts", description = "APIs for the Travel Feed's TravelPosts")
public class TravelPostController {

    @Autowired
    private TravelPostService travelPostService;
    @Autowired
    private UserService userService;

    @Autowired
    private TravelPostRepository travelPostRepository;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RewardService rewardService;

    @Operation(summary = "Create TravelPost",
            description = "Creates a TravelPost and saves it to the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TravelPost created successfully."),
            @ApiResponse(responseCode = "404", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @PostMapping("/create/")
    public ResponseEntity<?> createPost(@RequestBody Map<String,Object> postData) {

        try {
            User user = userService.findById(Long.parseLong((String) postData.get("userId")));
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            String description = (String) postData.get("description");
            String category = (String) postData.get("category");
            int rating = Integer.parseInt((String) postData.get("rating"));
            LocalDateTime startDate = LocalDateTime.parse((String) postData.get("startDate"));
            LocalDateTime endDate = LocalDateTime.parse((String) postData.get("endDate"));
            String destination = (String) postData.get("destination");
            TravelPost newPost = new TravelPost();
            newPost.setUser(user);
            newPost.setCreatedAt(LocalDateTime.now());
            newPost.setDescription(description);
            newPost.setCategory(category);
            newPost.setRating(rating);
            newPost.setStartDate(startDate);
            newPost.setEndDate(endDate);
            newPost.setDestination(destination);
            newPost.setLikeCount(0);
            travelPostService.createPost(newPost);

            Long userId = Long.parseLong(postData.get("userId").toString());
            BigDecimal currentBalance = rewardService.getBalance(userId);
            BigDecimal postReward = new BigDecimal("50.00");
            rewardService.updateBalance(userId, currentBalance.add(postReward));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Post created successfully");
            response.put("postId", newPost.getId());
            response.put("rewardPoints", "50");

            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "List TravelPosts",
            description = "List all TravelPosts in a paginated manner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts returned successfully."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @GetMapping("/get-posts/")
    public Object getPosts(@RequestParam(required = false) String category,
                                      @RequestParam(required = false) boolean newest,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        try {
            Page<TravelPostProjection> paginatedPosts = travelPostService.getPosts(category, newest, page, size);
            //return ResponseEntity.ok(paginatedPosts);
            return paginatedPosts;
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Return TravelPost Comments",
            description = "Returns all comments associated with a TravelPost.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comments returned successfully."),
            @ApiResponse(responseCode = "404", description = "Post not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @GetMapping("/get-comments/{travelPostId}")
    public ResponseEntity<?> getComments(@PathVariable Long travelPostId) {
        try {
            boolean postExists = travelPostRepository.existsById(travelPostId);
            if (!postExists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }
            List<Comment> comments = commentService.getCommentsByPostId(travelPostId);
            return ResponseEntity.ok(comments);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete TravelPost",
            description = "Deletes a TravelPost by the id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post deleted successfully."),
            @ApiResponse(responseCode = "404", description = "Post not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @DeleteMapping("/delete/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        try {
            if (!travelPostRepository.existsById(postId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
            }

            travelPostRepository.deleteById(postId);
            return ResponseEntity.ok("Post deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/getAllPosts")
    public ResponseEntity<?> getAllPosts() {
        List<TravelPost> posts = travelPostRepository.findAll();
        return ResponseEntity.ok(posts);
    }

}
