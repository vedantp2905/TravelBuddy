package TravelBuddy.controller;

import TravelBuddy.model.User;
import TravelBuddy.repositories.TravelPostProjection;

import TravelBuddy.repositories.UserRepository;
import TravelBuddy.service.TravelPostService;


import TravelBuddy.service.FriendshipService;
import TravelBuddy.service.UserService;
import TravelBuddy.service.TravelPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friend/")
@Tag(name="Friends", description = "APIs for the Friendship functionality")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserService userService;

    @Autowired
    private TravelPostService travelPostService;

    @PostMapping("/add/")
    @Operation(summary = "Add friend",
            description = "Adds a friend based on their id. To the friend the user is also added as a friend.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friend added successfully."),
            @ApiResponse(responseCode = "400", description = "User or friend not found, or friendship already exists"),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> addFriend(@RequestParam Long userId, @RequestParam Long friendId) {

        try {

            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The user was not found.");
            }
            if (!userService.userExists(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The friend was not found.");
            }

            User user = userService.findById(userId);
            User friend = userService.findById(friendId);

            if (friendshipService.friendshipExists(user,friend)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Friendship already exists.");
            }

            friendshipService.createFriendship(user, friend);
            return ResponseEntity.ok("Added friend successfully.");
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/get/{userId}")
    @Operation(summary = "Get friends",
            description = "Lists the friends associated with this user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friends listed successfully."),
            @ApiResponse(responseCode = "400", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> listFriends(@PathVariable long userId) {

        try {
            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found.");
            }

            User user = userService.findById(userId);

            return ResponseEntity.ok(friendshipService.getFriendships(user));
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove/")
    @Operation(summary = "Remove friend",
            description = "Removes a friend from a user's friends. Also removes the user from the friend's friend-list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friend removed successfully."),
            @ApiResponse(responseCode = "400", description = "User or friend not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> removeFriend(@RequestParam long userId, @RequestParam long friendId) {

        try {

            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The user was not found.");
            }
            if (!userService.userExists(friendId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The friend was not found.");
            }

            User user = userService.findById(userId);
            User friend = userService.findById(friendId);

            if (!friendshipService.friendshipExists(user,friend)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Friendship doesn't exist.");
            }

            friendshipService.removeFriend(user,friend);
            return ResponseEntity.ok("Removed friend successfully.");

        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }

    @GetMapping("/search/")
    @Operation(summary = "Search users",
            description = "Supplies the users whose usernames start with the provided prompt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search returned."),
            @ApiResponse(responseCode = "400", description = "Searcher not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> searchUsers(@RequestParam long searcherId, @RequestParam String prompt) {

        try {

            if (!userService.userExists(searcherId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Searcher not found.");
            }

            User searcher = userService.findById(searcherId);

            return (ResponseEntity.ok(friendshipService.searchUsers(searcher, prompt)));

        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }

    @GetMapping("/get-posts/{userId}")
    @Operation(summary = "Get posts",
            description = "Supplies the posts created by the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts returned."),
            @ApiResponse(responseCode = "400", description = "User not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> getUserPosts(@PathVariable long userId) {

        try {

            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found.");
            }

            User user = userService.findById(userId);

            List<TravelPostProjection> posts = travelPostService.getUserPosts(user);

            return ResponseEntity.ok(posts);

        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }


}
