package TravelBuddy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import TravelBuddy.model.TravelHistory;
import TravelBuddy.model.User;
import TravelBuddy.service.TravelHistoryService;
import TravelBuddy.service.UserService;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/travel-history")
@Tag(name = "Travel History", description = "APIs for managing user travel history")
public class TravelHistoryController {

    @Autowired
    private TravelHistoryService travelHistoryService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Add travel history",
            description = "Adds a new travel history entry for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Travel history added successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/user/{userId}")
    public ResponseEntity<String> addTravelHistory(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "Travel history details") @RequestBody TravelHistory travelHistory
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            travelHistory.setUserId(userId);
            travelHistoryService.saveTravelHistory(travelHistory);
            return ResponseEntity.ok("Travel history added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get user travel history",
            description = "Retrieves all travel history entries for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved travel history"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTravelHistory(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            List<TravelHistory> travelHistories = travelHistoryService.getUserTravelHistory(userId);
            return ResponseEntity.ok(travelHistories);
        } catch (Exception e) {
            e.printStackTrace(); // Add this line to print the full stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Update travel history",
            description = "Updates an existing travel history entry")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Travel history updated successfully"),
        @ApiResponse(responseCode = "403", description = "Travel history does not belong to user"),
        @ApiResponse(responseCode = "404", description = "User or travel history not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{travelHistoryId}/user/{userId}")
    public ResponseEntity<String> updateTravelHistory(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "ID of the travel history") @PathVariable Long travelHistoryId,
        @Parameter(description = "Updated fields") @RequestBody Map<String, Object> updates
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            
            TravelHistory existingTravelHistory = travelHistoryService.findById(travelHistoryId);
            if (existingTravelHistory == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel history not found with ID: " + travelHistoryId);
            }
            
            if (!existingTravelHistory.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This travel history does not belong to the specified user");
            }
            
            travelHistoryService.patchTravelHistory(travelHistoryId, userId, updates);
            return ResponseEntity.ok("Travel history updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete travel history",
            description = "Deletes an existing travel history entry")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Travel history deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Travel history does not belong to user"),
        @ApiResponse(responseCode = "404", description = "User or travel history not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{travelHistoryId}/user/{userId}")
    public ResponseEntity<String> deleteTravelHistory(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "ID of the travel history") @PathVariable Long travelHistoryId
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            
            TravelHistory existingTravelHistory = travelHistoryService.findById(travelHistoryId);
            if (existingTravelHistory == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Travel history not found with ID: " + travelHistoryId);
            }
            
            if (!existingTravelHistory.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This travel history does not belong to the specified user");
            }
            
            travelHistoryService.deleteTravelHistory(travelHistoryId);
            return ResponseEntity.ok("Travel history deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get user travel history IDs",
            description = "Retrieves all travel history IDs for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved travel history IDs"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user/{userId}/travel-history-ids")
    public ResponseEntity<?> getUserTravelHistoryIds(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            List<Long> travelHistoryIds = travelHistoryService.getUserTravelHistoryIds(userId);
            return ResponseEntity.ok(travelHistoryIds);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
