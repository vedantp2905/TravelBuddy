package TravelBuddy.controller;

import TravelBuddy.model.FutureTrip;
import TravelBuddy.service.FutureTripsService;
import TravelBuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/future-trips")
@Tag(name = "Future Trips", description = "APIs for managing future trip plans")
public class FutureTripsController {

    @Autowired
    private FutureTripsService futureTripsService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Add future trip",
            description = "Adds a new future trip for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trip added successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/user/{userId}")
    public ResponseEntity<String> addFutureTrip(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "Future trip details") @RequestBody FutureTrip futureTrip
    ) {
        try {
            if (userService.findById(userId) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            futureTrip.setUserId(userId);
            futureTripsService.saveFutureTrip(futureTrip); // Calls service method
            return ResponseEntity.ok("Future trip added successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get user's future trips",
            description = "Retrieves all future trips for a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserFutureTrips(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            if (userService.findById(userId) == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            List<FutureTrip> futureTrips = futureTripsService.getUserFutureTrips(userId); // Calls service method
            return ResponseEntity.ok(futureTrips);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Update future trip",
            description = "Updates an existing future trip details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Future trip updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Future trip not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{tripId}")
    public ResponseEntity<String> updateFutureTrip(
        @Parameter(description = "ID of the trip to update") @PathVariable Long tripId,
        @Parameter(description = "Updated trip details") @RequestBody FutureTrip futureTrip
    ) {
        try {
            FutureTrip existingTrip = futureTripsService.getFutureTripById(tripId);
            if (existingTrip == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Future trip not found with ID: " + tripId);
            }
            futureTrip.setId(tripId);
            futureTripsService.updateFutureTrip(futureTrip);
            return ResponseEntity.ok("Future trip updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete future trip",
            description = "Deletes an existing future trip")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Future trip deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Future trip not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{tripId}")
    public ResponseEntity<String> deleteFutureTrip(
        @Parameter(description = "ID of the trip to delete") @PathVariable Long tripId
    ) {
        try {
            FutureTrip existingTrip = futureTripsService.getFutureTripById(tripId);
            if (existingTrip == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Future trip not found with ID: " + tripId);
            }
            futureTripsService.deleteFutureTrip(tripId);
            return ResponseEntity.ok("Future trip deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

}
