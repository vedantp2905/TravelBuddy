package TravelBuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import TravelBuddy.service.RewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import TravelBuddy.service.UserService;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;
import TravelBuddy.model.User;

@RestController
@RequestMapping("/api/reward")
@Tag(name = "Reward Management", description = "APIs for managing user rewards and points")
public class RewardController {

    @Autowired
    private RewardService rewardService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Get user's reward balance",
            description = "Retrieves the current reward points balance for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved balance"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<?> getBalance(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            BigDecimal balance = rewardService.getBalance(userId);
            return ResponseEntity.ok().body(Map.of("balance", balance));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Update reward balance",
            description = "Updates the reward points balance for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{userId}/update")
    public ResponseEntity<?> updateBalance(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "New balance details") @RequestBody Map<String, Object> body
    ) {
        try {
            BigDecimal newBalance = new BigDecimal(body.get("balance").toString());
            rewardService.updateBalance(userId, newBalance);
            return ResponseEntity.ok().body(Map.of("message", "Balance updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Use rewards for premium upgrade",
            description = "Converts reward points to premium membership")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully upgraded to premium"),
        @ApiResponse(responseCode = "400", description = "Insufficient points or invalid plan"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{userId}/use-for-premium")
    public ResponseEntity<?> useRewardsForPremium(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "Premium plan details") @RequestBody Map<String, String> body
    ) {
        try {
            String plan = body.get("plan");
            int requiredPoints = plan.equals("monthly") ? 1000 : 7500;
            
            BigDecimal currentBalance = rewardService.getBalance(userId);
            if (currentBalance.compareTo(BigDecimal.valueOf(requiredPoints)) < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Insufficient rewards points"));
            }
            
            // Calculate premium expiry date based on plan
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, plan.equals("monthly") ? 1 : 12);
            
            // Create User object with updated premium details
            User user = new User();
            user.setId(userId);
            user.setRole(3); // Premium role
            user.setPremiumPlan(plan);
            user.setPremiumExpiryDate(calendar.getTime());
            
            // Update user's premium status
            userService.updateUserPremiumStatus(user);
            
            // Deduct points after successful premium upgrade
            rewardService.updateBalance(userId, currentBalance.subtract(BigDecimal.valueOf(requiredPoints)));
            
            return ResponseEntity.ok().body(Map.of(
                "message", "Successfully upgraded to premium using rewards",
                "plan", plan,
                "expiryDate", calendar.getTime().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 