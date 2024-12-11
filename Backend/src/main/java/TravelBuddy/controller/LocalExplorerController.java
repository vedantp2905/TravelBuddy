package TravelBuddy.controller;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import TravelBuddy.service.SerpApiService;
import java.util.Map;

import org.json.JSONObject;


@RestController
@RequestMapping("/api/local-explorer/")
@Tag(name="Local Explorer", description = "APIs for discovering local attractions and places")
public class LocalExplorerController {

    private static final Logger logger = LoggerFactory.getLogger(LocalExplorerController.class);

    @Autowired
    private SerpApiService serpApiService;

    @GetMapping("/search")
    public ResponseEntity<?> searchLocalPlaces(
        @RequestParam String location
    ) {
        try {
            Map<String, Object> results = serpApiService.getLocalInfo(location);
            JSONObject jsonResponse = new JSONObject(results);
            logger.info("Final JSON response being sent: {}", jsonResponse.toString());
            return ResponseEntity.ok(jsonResponse.toString());
        } catch (Exception e) {
            logger.error("Error processing search request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        }
    }

    @GetMapping("/details/{placeId}")
    @Operation(summary = "Get place details",
            description = "Returns detailed information about a specific place")
    public ResponseEntity<?> getPlaceDetails(@PathVariable String placeId) {
        try {
            Map<String, Object> details = serpApiService.getPlaceDetails(placeId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
} 