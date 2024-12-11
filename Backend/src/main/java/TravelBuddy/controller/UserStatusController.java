package TravelBuddy.controller;


import TravelBuddy.model.User;
import TravelBuddy.model.UserStatus;
import TravelBuddy.service.UserService;
import TravelBuddy.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user-status/")
@Tag(name="User Statuses", description = "APIs for the Status feature")
public class UserStatusController {

    @Autowired
    UserStatusService userStatusService;

    @Autowired
    UserService userService;

    @PostMapping("/add/{userId}")
    @Operation(summary = "Add status",
            description = "Saves the status based on the user id provided. Statuses are deleted automatically roughly 24 hours after they are posted.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status saved successfully."),
            @ApiResponse(responseCode = "400", description = "User not found or prompt not supplied."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> createStatus(@PathVariable long userId, @RequestBody Map<String, Object> promptData) {

        try {

            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The user was not found.");
            }

            User user = userService.findById(userId);

            if (userStatusService.statusExists(user)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Status already exists.");
            }

            if (!promptData.containsKey("prompt")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Prompt not supplied.");
            }
            String prompt = (String) promptData.get("prompt");

            userStatusService.saveStatus(user, prompt);
            return ResponseEntity.ok("Status saved.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }

    @GetMapping("/get/{userId}")
    @Operation(summary = "Get status",
            description = "Returns the status based on the userId provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status returned."),
            @ApiResponse(responseCode = "400", description = "User or status not found."),
            @ApiResponse(responseCode = "500", description = "Error occurred internally.")
    })
    public ResponseEntity<?> getStatus(@PathVariable long userId) {

        try {

            if (!userService.userExists(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The user was not found.");
            }

            User user = userService.findById(userId);

            if (!userStatusService.statusExists(user)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Status not found.");
            }

            UserStatus status = userStatusService.getStatus(user);
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }

    }

}
