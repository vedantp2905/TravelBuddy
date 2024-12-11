package TravelBuddy.controller;

import TravelBuddy.model.FriendRequestDTO;
import TravelBuddy.model.User;
import TravelBuddy.service.FriendRequestService;
import TravelBuddy.service.FriendshipService;
import TravelBuddy.service.UserService;
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
@RequestMapping("/api/friend-request/")
@Tag(name="Friend Requests", description = "APIs for Friend Requests")
public class FriendRequestController {

    @Autowired
    private FriendRequestService friendRequestService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserService userService;

    @PostMapping("/send-request/")
    @Operation(summary = "Send request",
            description = "Sends a friend request to a receiver from a sender.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request sent successfully."),
            @ApiResponse(responseCode = "400", description = "Sender or receiver not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> sendRequest(@RequestParam long senderId, @RequestParam long receiverId) {

        try {
            if (!userService.userExists(senderId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender not found.");
            }

            if (!userService.userExists(receiverId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Receiver not found.");
            }

            User sender = userService.findById(senderId);
            User receiver = userService.findById(receiverId);

            if (friendRequestService.friendRequestExists(sender, receiver)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request already exists.");
            }

            if (friendRequestService.friendRequestExists(receiver, sender)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Receiver has already sent a request to sender.");
            }

            friendRequestService.createFriendRequest(sender, receiver);
            return ResponseEntity.ok("Request sent successfully.");
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/get/{userId}")
    @Operation(summary = "Get requests",
            description = "Returns a list of all the friend requests the user has pending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Requests returned successfully."),
            @ApiResponse(responseCode = "400", description = "Receiver not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> getRequests(@PathVariable long userId) {

       try {

           if (!userService.userExists(userId)) {
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found.");
           }

           User receiver = userService.findById(userId);

           List<FriendRequestDTO> requests = friendRequestService.getFriendRequests(receiver);
           return ResponseEntity.ok(requests);

       }
       catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
       }
    }

    @DeleteMapping("/accept-request/")
    @Operation(summary = "Accept request",
            description = "Creates a friendship between the sender and receiver and removes the request from the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted."),
            @ApiResponse(responseCode = "400", description = "User not found or request doesn't exist."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> acceptRequest(@RequestParam long senderId, @RequestParam long receiverId) {

        try {

            if (!userService.userExists(senderId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender not found.");
            }

            if (!userService.userExists(receiverId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Receiver not found.");
            }

            User sender = userService.findById(senderId);
            User receiver = userService.findById(receiverId);

            if (!friendRequestService.friendRequestExists(sender, receiver)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request doesn't exist.");
            }

            friendshipService.createFriendship(sender, receiver);
            friendRequestService.deleteRequest(friendRequestService.getRequest(sender,receiver));

            return ResponseEntity.ok("Request accepted successfully.");

        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }

    @DeleteMapping("/reject-request/")
    @Operation(summary = "Reject request",
            description = "Removes the request from the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected."),
            @ApiResponse(responseCode = "400", description = "User not found or request doesn't exist."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> rejectRequest(@RequestParam long senderId, @RequestParam long receiverId) {

        try {

            if (!userService.userExists(senderId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender not found.");
            }

            if (!userService.userExists(receiverId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Receiver not found.");
            }

            User sender = userService.findById(senderId);
            User receiver = userService.findById(receiverId);

            if (!friendRequestService.friendRequestExists(sender, receiver)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request doesn't exist.");
            }

            friendRequestService.deleteRequest(friendRequestService.getRequest(sender,receiver));

            return ResponseEntity.ok("Request rejected successfully.");

        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }


}
