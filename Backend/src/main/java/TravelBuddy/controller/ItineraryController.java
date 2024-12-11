package TravelBuddy.controller;

import TravelBuddy.model.Itinerary;
import TravelBuddy.model.User;
import TravelBuddy.service.ItineraryService;
import TravelBuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import java.time.LocalDateTime;
import java.util.Map;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/itineraries")
@Tag(name = "Itinerary Management", description = "APIs for generating travel itineraries using AI")
public class ItineraryController {

    @Autowired
    private ItineraryService itineraryService;

    @Autowired
    private UserService userService;

    // Get all itineraries for a user
    @Operation(summary = "Get user itineraries",
            description = "Retrieves all itineraries for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved itineraries"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserItineraries(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found with ID: " + userId);
            }

            List<Itinerary> itineraries = itineraryService.getUserItineraries(userId);
            return ResponseEntity.ok(itineraries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching itineraries: " + e.getMessage());
        }
    }

    @Operation(summary = "Generate itinerary",
            description = "Generates a new travel itinerary based on provided parameters")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Itinerary generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/generate")
    public ResponseEntity<?> generateItinerary(@RequestBody Map<String, Object> requestBody) {
        try {
            System.out.println("Received request body: " + requestBody);
            
            // Validate required fields
            if (!requestBody.containsKey("userId") || requestBody.get("userId") == null) {
                return ResponseEntity.badRequest().body("userId is required");
            }
            
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://localhost:5000/generate-itinerary",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("itinerary")) {
                String generatedItinerary = responseBody.get("itinerary").toString();
                System.out.println("Generated itinerary received successfully");
                
                try {
                    Itinerary itinerary = new Itinerary();
                    Long userId = Long.parseLong(String.valueOf(requestBody.get("userId")));
                    itinerary.setUserId(userId);
                    
                    // Safely get values with null checks
                    itinerary.setCountry(String.valueOf(requestBody.getOrDefault("country", "")));
                    
                    @SuppressWarnings("unchecked")
                    List<String> citiesList = (List<String>) requestBody.getOrDefault("cities", List.of());
                    String citiesString = String.join(", ", citiesList);
                    itinerary.setCities(citiesString);
                    
                    // Parse dates safely
                    String startDate = (String) requestBody.getOrDefault("start_date", LocalDateTime.now().toString());
                    String endDate = (String) requestBody.getOrDefault("end_date", LocalDateTime.now().plusDays(1).toString());
                    itinerary.setStartDate(LocalDateTime.parse(startDate + "T00:00:00"));
                    itinerary.setEndDate(LocalDateTime.parse(endDate + "T00:00:00"));
                    
                    itinerary.setNumberOfAdults(Integer.parseInt(String.valueOf(requestBody.getOrDefault("number_of_adults", 1))));
                    itinerary.setNumberOfChildren(Integer.parseInt(String.valueOf(requestBody.getOrDefault("number_of_children", 0))));
                    itinerary.setUserLocation(String.valueOf(requestBody.getOrDefault("user_location", "")));
                    itinerary.setGeneratedItinerary(generatedItinerary);
                    
                    itineraryService.saveItinerary(itinerary);
                    System.out.println("Itinerary saved successfully");
                    
                    // Send email if user exists
                    User user = userService.findById(userId);
                    if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                        System.out.println("Sending email to: " + user.getEmail());
                        itineraryService.sendItineraryEmail(
                            user.getEmail(), 
                            itinerary,
                            String.format("Your Trip to %s (%s to %s)", 
                                itinerary.getCountry(),
                                startDate,
                                endDate)
                        );
                        System.out.println("Email sent successfully");
                        return ResponseEntity.ok(Map.of("message", "Itinerary generated and email sent successfully"));
                    } else {
                        return ResponseEntity.ok(Map.of("message", "Itinerary generated but no email was sent (user email not found)"));
                    }
                } catch (Exception e) {
                    System.err.println("Error processing itinerary: " + e.getMessage());
                    e.printStackTrace();
                }
                
                return ResponseEntity.ok(responseBody);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate itinerary");
            }
        } catch (Exception e) {
            System.err.println("Error in generate endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error generating itinerary: " + e.getMessage());
        }
    }

    @Operation(summary = "Get itinerary by ID",
            description = "Retrieves a specific itinerary by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved itinerary"),
        @ApiResponse(responseCode = "404", description = "Itinerary not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getItineraryById(
        @Parameter(description = "ID of the itinerary") @PathVariable Long id
    ) {
        Itinerary itinerary = itineraryService.getItineraryById(id);
        return ResponseEntity.ok(itinerary);
    }

    @Operation(summary = "Delete itinerary",
            description = "Deletes an existing itinerary")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Itinerary deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Itinerary not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItinerary(
        @Parameter(description = "ID of the itinerary to delete") @PathVariable Long id
    ) {
        try {
            Itinerary itinerary = itineraryService.getItineraryById(id);
            if (itinerary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Itinerary not found with ID: " + id);
            }

            itineraryService.deleteItinerary(id);
            return ResponseEntity.ok("Itinerary deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting itinerary: " + e.getMessage());
        }
    }

    @Operation(summary = "Get all itineraries",
            description = "Retrieves all itineraries in the system")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all itineraries"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<?> getAllItineraries() {
        try {
            List<Itinerary> itineraries = itineraryService.getAllItineraries();
            return ResponseEntity.ok(itineraries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching itineraries: " + e.getMessage());
        }
    }
} 