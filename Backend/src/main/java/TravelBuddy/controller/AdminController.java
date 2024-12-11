package TravelBuddy.controller;

import TravelBuddy.service.UserService;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import TravelBuddy.model.Newsletter;
import TravelBuddy.model.User;
import TravelBuddy.service.NewsletterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Management", description = "APIs for administrative operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Get all newsletters",
            description = "Retrieves all newsletters from the system")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved newsletters"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/newsletters")
    public ResponseEntity<List<Newsletter>> getAllNewsletters() {
        List<Newsletter> newsletters = newsletterService.getAllNewsletters();
        return ResponseEntity.ok(newsletters);
    }
    @Operation(summary = "Get all users",
            description = "Retrieves all users registered in the system")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users list"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Send test newsletter",
            description = "Sends a test newsletter to all verified users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Newsletter sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/test-send-newsletter")
    public ResponseEntity<String> testSendNewsletter(@RequestBody Map<String, String> requestBody) {
        String topic = requestBody.get("topic");
        if (topic == null || topic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Topic is required.");
        }
        try {
            newsletterService.sendNewsletterToAllUsers(topic);
            return ResponseEntity.ok("Newsletter sent successfully to all verified users.");
        } catch (Exception e) {
            logger.error("Error sending test newsletter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
