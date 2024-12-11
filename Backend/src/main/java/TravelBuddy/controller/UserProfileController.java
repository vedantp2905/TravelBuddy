package TravelBuddy.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import TravelBuddy.model.User;
import TravelBuddy.model.UserProfile;
import TravelBuddy.service.UserService;
import TravelBuddy.service.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "APIs for managing user profiles")
public class UserProfileController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserProfileService userProfileService;

    @Operation(summary = "Save user profile",
            description = "Creates or updates a user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid profile data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/profile/{userId}")
    public ResponseEntity<Map<String, String>> saveUserProfile(@PathVariable Long userId, 
                                              @RequestBody(required = false) UserProfile profile,
                                              @RequestPart(value = "profile", required = false) String profileJson) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "User not found with ID: " + userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            if (profile == null && profileJson != null) {
                ObjectMapper mapper = new ObjectMapper();
                profile = mapper.readValue(profileJson, UserProfile.class);
            }
            
            if (profile == null) {
                Map<String, String> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Profile data is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            profile.setId(userId);
                        
            userProfileService.saveUserProfile(profile);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User profile saved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "An error occurred while saving the profile");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(summary = "Update user profile",
            description = "Update an existing user's profile information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/profile/{userId}")
    public ResponseEntity<String> updateUserProfile(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "Profile update details") @RequestBody Map<String, Object> updates
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            
            // Ensure the updates are processed in the new order
            Map<String, Object> orderedUpdates = new LinkedHashMap<>();
            String[] orderedFields = {"aboutMe", "profilePictureUrl", "preferredLanguage", "currencyPreference", 
                                      "travelBudget", "travelStyle", "travelExperienceLevel", "maxTripDuration", 
                                      "preferredDestinations", "interests", "preferredAirlines", 
                                      "preferredAccommodationType", "dietaryRestrictions", "passportCountry", 
                                      "frequentFlyerPrograms"};
            
            for (String field : orderedFields) {
                if (updates.containsKey(field)) {
                    orderedUpdates.put(field, updates.get(field));
                }
            }
            
            userProfileService.updateUserProfile(userId, orderedUpdates);
            return ResponseEntity.ok("User profile updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get user profile",
            description = "Retrieves a user's profile details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved profile"),
        @ApiResponse(responseCode = "404", description = "User or profile not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found for ID: " + userId);
            }
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete user profile",
            description = "Deletes a user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User or profile not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/profile/{userId}")
    public ResponseEntity<String> deleteUserProfile(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            
            userProfileService.deleteUserProfile(userId);
            return ResponseEntity.ok("User profile deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PatchMapping("/profile/{userId}")
    @Operation(summary = "Partially update user profile",
            description = "Updates specific fields of a user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> patchUserProfile(
        @PathVariable Long userId,
        @RequestBody Map<String, Object> updates
    ) {
        try {
            userProfileService.updateUserProfile(userId, updates);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unable to update profile");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}


