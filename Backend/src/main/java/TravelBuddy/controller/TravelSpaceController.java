package TravelBuddy.controller;

import TravelBuddy.model.SpaceMessage;
import TravelBuddy.model.TravelSpace;
import TravelBuddy.model.User;
import TravelBuddy.model.UserTravelSpace;
import TravelBuddy.repositories.*;
import TravelBuddy.service.TravelSpaceService;
import TravelBuddy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/travelspace")
@Tag(name="Travel Spaces", description = "APIs for the Travel Spaces feature")
public class TravelSpaceController {

    private final TravelSpaceService travelSpaceService;
    private final UserService userService;

    @Autowired
    private SpaceMessageRepository spaceMessageRepository;

    @Autowired
    private TravelSpaceRepository travelSpaceRepository;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    public TravelSpaceController(TravelSpaceService travelSpaceService, UserService userService) {
        this.travelSpaceService = travelSpaceService;
        this.userService = userService;
    }

    @Operation(summary = "Create TravelSpace",
            description = "Creates a TravelSpace and saves it to the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TravelSpace created successfully."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @PostMapping("/create/")
    public TravelSpace createTravelSpace(@RequestBody TravelSpace travelSpace) {
        return travelSpaceService.createTravelSpace(travelSpace);
    }

//    @GetMapping("/get/")
//    public List<TravelSpace> getAllTravelSpaces() {
//        return travelSpaceService.getAllTravelSpaces();
//    }

    @Operation(summary = "Get all TravelSpaces",
            description = "Returns all TravelSpaces")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Spaces returned successfully."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @GetMapping("/get/")
    public List<TravelSpaceProjection> getAllTravelSpaces() {
        return travelSpaceService.getAllTravelSpaces();
    }

    @Operation(summary = "Get TravelSpace Messages",
            description = "Returns all existing messages in a TravelSpace.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages returned successfully."),
            @ApiResponse(responseCode = "404", description = "TravelSpace not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @GetMapping("/get-messages/{travelSpaceId}")
    public ResponseEntity<List<Map<String, Object>>> getMessages(@PathVariable Long travelSpaceId) {
        List<SpaceMessage> messages = spaceMessageRepository.findAllByTravelSpaceId(travelSpaceId);
        List<Map<String, Object>> formattedMessages = messages.stream().map(message -> {
            Map<String, Object> formattedMessage = new HashMap<>();
            formattedMessage.put("message", message.getMessage());
            formattedMessage.put("id", message.getId());
            formattedMessage.put("timestamp", message.getTimestamp());
            formattedMessage.put("messageType", message.getMessageType());
            
            Map<String, Object> sender = new HashMap<>();
            sender.put("id", message.getSender().getId());
            sender.put("username", message.getSender().getUsername());
            formattedMessage.put("sender", sender);

            if (message.getParentMessage() != null) {
                Map<String, Object> parentMessage = new HashMap<>();
                parentMessage.put("id", message.getParentMessage().getId());
                formattedMessage.put("parentMessage", parentMessage);
            } else {
                formattedMessage.put("parentMessage", null);
            }

            formattedMessage.put("replies", new ArrayList<>());
            return formattedMessage;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(formattedMessages);
    }

    @Operation(summary = "Join TravelSpace",
            description = "Associates a User with a TravelSpace and their preferred color.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User joined successfully."),
            @ApiResponse(responseCode = "400", description = "Provided color was empty."),
            @ApiResponse(responseCode = "404", description = "User or TravelSpace not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @PostMapping("/join/{travelSpaceId}")
    public ResponseEntity<?> joinTravelSpace(@PathVariable Long travelSpaceId, @RequestParam Long userId, @RequestParam String color) {
        try {
            User user = userService.findById(userId);
            TravelSpace space = travelSpaceService.getSpaceById(travelSpaceId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            if (space == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TravelSpace not found");
            }
            if (color.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Color cannot be empty");
            }

            UserTravelSpace userTravelSpace = travelSpaceService.joinTravelSpace(user, space, color);
            if (userTravelSpace == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("This user has already joined the travel space.");
            }

            return ResponseEntity.ok("Registered User for this TravelSpace.");
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }

//    @GetMapping("/get-users/{travelSpaceId}/")
//    public ResponseEntity<?> getUsersInTravelSpace(@PathVariable Long travelSpaceId) {
//        try {
//            TravelSpace space = travelSpaceService.getSpaceById(travelSpaceId);
//            if (space == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TravelSpace not found");
//            }
//            List<UserTravelSpace> users = travelSpaceService.getUsersFromTravelSpace(space);
//            if (users == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("There are no users in this TravelSpace");
//            }
//            return ResponseEntity.ok(users);
//        }
//        catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
//        }
//    }

    @Operation(summary = "Return TravelSpace Users",
            description = "Returns all of the users that have joined a TravelSpace.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned successfully."),
            @ApiResponse(responseCode = "404", description = "TravelSpace not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    @GetMapping("/get-users/{travelSpaceId}")
    public ResponseEntity<?> getUsersInTravelSpace(@PathVariable Long travelSpaceId) {
        try {
            TravelSpace space = travelSpaceService.getSpaceById(travelSpaceId);
            if (space == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("TravelSpace not found");
            }

            // Fetch users in the TravelSpace
            List<UserTravelSpace> userTravelSpaces = travelSpaceService.getUsersFromTravelSpace(space);

            // Map to a simplified response structure
            List<Map<String, Object>> response = userTravelSpaces.stream().map(uts -> {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", uts.getUser().getId());
                map.put("username", uts.getUser().getUsername());
                map.put("color", uts.getColor());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "TravelSpace Message reply",
            description = "Posts a reply to another user's message within a TravelSpace.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reply posted successfully."),
            @ApiResponse(responseCode = "404", description = "TravelSpace or User not found.")

    })
    @PostMapping("/{travelSpaceId}/messages/{parentMessageId}/reply")
    public ResponseEntity<?> replyToMessage(
            @PathVariable Long travelSpaceId,
            @PathVariable Long parentMessageId,
            @RequestParam Long userId,
            @RequestBody String messageContent) {

        // Fetch the travel space, user, and parent message
        TravelSpace travelSpace = travelSpaceRepository.findById(travelSpaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TravelSpace not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        SpaceMessage parentMessage = spaceMessageRepository.findById(parentMessageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent message not found"));

        // Create and save the reply message
        SpaceMessage replyMessage = new SpaceMessage();
        replyMessage.setMessage(messageContent);
        replyMessage.setTimestamp(LocalDateTime.now());
        replyMessage.setSender(user);
        replyMessage.setTravelSpace(travelSpace);
        replyMessage.setParentMessage(parentMessage);

        SpaceMessage savedReply = spaceMessageRepository.save(replyMessage);

        return ResponseEntity.status(HttpStatus.CREATED).body("Saved reply.");
    }



//    @PatchMapping("/add-user/")
//    public TravelSpace addUser(@RequestParam Long userId, @RequestParam Long travelSpaceId) {
//
//        TravelSpace space = travelSpaceService.getSpaceById(travelSpaceId);
//
//    }
}

