package TravelBuddy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import TravelBuddy.model.CityInfo;
import TravelBuddy.repositories.CityInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@Service
public class SerpApiService {

    private static final Logger logger = LoggerFactory.getLogger(SerpApiService.class);

    @Value("${serp.api.key}")
    private String serpApiKey;

    @Autowired
    private CityInfoRepository cityInfoRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> getLocalInfo(String city) {
        try {
            Optional<CityInfo> existingInfo = cityInfoRepository.findByCityName(city);
            if (existingInfo.isPresent() && existingInfo.get().getLastUpdated().plusDays(7).isAfter(LocalDateTime.now())) {
                return convertToMap(existingInfo.get()); // Use helper method for consistent conversion
            }

            Map<String, Object> results = searchLocalPlaces(city, null, null, null);

            logger.info("Results types - attractions: {}, restaurants: {}, historical: {}",
                results.get("attractions").getClass().getSimpleName(),
                results.get("restaurants").getClass().getSimpleName(),
                results.get("historical").getClass().getSimpleName());

            saveCityInfo(city, results);
            return results; // Directly return the results, no need for formatting
        } catch (Exception e) {
            logger.error("Error fetching local information for city: " + city, e);
            // Return empty JSONArrays in case of error
            return createEmptyResultsMap(); // Use helper method for consistency
        }
    }

    private Map<String, Object> createEmptyResultsMap() {  // Helper function for empty map
        Map<String, Object> emptyResults = new HashMap<>();
        emptyResults.put("attractions", new JSONArray());
        emptyResults.put("restaurants", new JSONArray());
        emptyResults.put("historical", new JSONArray());
        return emptyResults;
    }

    private JSONArray searchCategory(String location, String category) {
        try {
            String searchUrl = String.format(
                "https://serpapi.com/search.json?engine=google_maps&q=%s+%s&api_key=%s",
                location, category, serpApiKey
            );
            
            logger.debug("Making request to SerpAPI: {}", searchUrl);
            String response = restTemplate.getForObject(searchUrl, String.class);
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray results = new JSONArray();
            
            if (jsonResponse.has("local_results")) {
                JSONArray places = jsonResponse.getJSONArray("local_results");

                // Convert JSONArray to List<JSONObject> for sorting
                List<JSONObject> placesList = new ArrayList<>();
                for (int i = 0; i < places.length(); i++) {
                    placesList.add(places.getJSONObject(i));
                }

                // Sort placesList based on rating in descending order
                placesList.sort((a, b) -> {
                    double ratingA = a.optDouble("rating", 0); // Extract rating, handle missing values
                    double ratingB = b.optDouble("rating", 0);
                    return Double.compare(ratingB, ratingA); // Descending order
                });

                // Clear the original JSONArray and add sorted elements back
                results = new JSONArray(); // reinitialize results
                for (JSONObject place : placesList) {
                    JSONObject placeInfo = processPlaceInfo(place);
                    if (placeInfo != null) {
                        results.put(placeInfo);
                    }
                }
            }
            
            logger.debug("Found {} places for category {}", results.length(), category);
            return results;
        } catch (Exception e) {
            logger.error("Error searching category {} for location {}: {}", 
                        category, location, e.getMessage());
            return new JSONArray(); // Return empty array instead of null
        }
    }

    private String buildSearchUrl(String city) {
        // Implement the logic to build the search URL based on the city
        // For example, you can use a third-party service or a custom algorithm
        // to determine the best search URL for the given city.
        // For now, we'll use a simple placeholder.
        return "https://serpapi.com/search.json?engine=google_maps&q=" + city + "&api_key=" + serpApiKey;
    }

    private void saveCityInfo(String city, Map<String, Object> data) {
        try {
            CityInfo cityInfo = cityInfoRepository.findByCityName(city)
                .orElse(new CityInfo());
            
            cityInfo.setCityName(city);
            
            // Convert JSONArray objects directly to strings
            JSONObject jsonData = new JSONObject(data);
            cityInfo.setAttractions(jsonData.getJSONArray("attractions").toString());
            cityInfo.setRestaurants(jsonData.getJSONArray("restaurants").toString());
            cityInfo.setHistoricalSites(jsonData.getJSONArray("historical").toString());
            
            cityInfo.setLastUpdated(LocalDateTime.now());
            
            // Log before saving
            logger.info("Saving to database - city: {}, data: {}", city, jsonData.toString());
            
            cityInfoRepository.save(cityInfo);
            
            // Verify save
            Optional<CityInfo> saved = cityInfoRepository.findByCityName(city);
            if (saved.isPresent()) {
                logger.info("Successfully saved and retrieved - city: {}, data: {}", 
                    city, saved.get().getRestaurants());
            }
        } catch (Exception e) {
            logger.error("Error saving city info: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving city info", e);
        }
    }

    public Map<String, Object> searchLocalPlaces(String location, String category, Double latitude, Double longitude) {
        logger.info("Searching local places with params - location: {}, category: {}", location, category);
        
        try {
            Map<String, Object> results = new HashMap<>();
            logger.info("Type of variable results is {} ",results.getClass().getSimpleName());

            // Search for each category and log results
            JSONArray attractions = searchCategory(location, "tourist attractions");
            JSONArray restaurants = searchCategory(location, "restaurants");
            JSONArray historical = searchCategory(location, "historical sites");

            logger.info("Raw search results - attractions: {}", attractions.toString());
            logger.info("Raw search results - restaurants: {}", restaurants.toString());
            logger.info("Raw search results - historical: {}", historical.toString());
            
            results.put("attractions", attractions != null ? attractions : new JSONArray());
            results.put("restaurants", restaurants != null ? restaurants : new JSONArray());
            results.put("historical", historical != null ? historical : new JSONArray());
            
            // Save to database
            saveCityInfo(location, results);
            
            // Log final response
            logger.info("Final response being sent to frontend: {}", new JSONObject(results).toString());
            return results;
        } catch (Exception e) {
            logger.error("Error searching local places: {}", e.getMessage(), e);
            Map<String, Object> emptyResults = new HashMap<>();
            emptyResults.put("attractions", new JSONArray());
            emptyResults.put("restaurants", new JSONArray());
            emptyResults.put("historical", new JSONArray());
            return emptyResults;
        }
    }

    private String buildUrl(String baseUrl, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl + "?");
        params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
        return url.toString();
    }

    private Map<String, Object> convertToMap(CityInfo cityInfo) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Convert stored JSON strings back to JSONArrays directly
            result.put("attractions", new JSONArray(cityInfo.getAttractions()));
            result.put("restaurants", new JSONArray(cityInfo.getRestaurants()));
            result.put("historical", new JSONArray(cityInfo.getHistoricalSites()));
            return result;
        } catch (Exception e) {
            logger.error("Error converting city info to map: {}", e.getMessage());
            throw new RuntimeException("Failed to convert city info", e);
        }
    }

    public Map<String, Object> getPlaceDetails(String placeId) {
        String url = UriComponentsBuilder.fromHttpUrl("https://serpapi.com/search")
                .queryParam("engine", "google_maps")
                .queryParam("place_id", placeId)
                .queryParam("api_key", serpApiKey)
                .toUriString();
                
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();
    }

    private JSONObject processPlaceInfo(JSONObject place) {
        JSONObject placeInfo = new JSONObject();
        try {
            placeInfo.put("name", place.getString("title"));
            placeInfo.put("address", place.getString("address"));
            placeInfo.put("rating", place.optString("rating", "N/A"));
            
            // Extract thumbnail URL if available
            if (place.has("thumbnail")) {
                placeInfo.put("thumbnail", place.getString("thumbnail"));
            }
            
            // Get description or build one from available data
            String description = place.optString("description");
            if (description == null || description.isEmpty()) {
                StringBuilder autoDescription = new StringBuilder();
                
                if (place.has("type")) {
                    autoDescription.append(place.getString("type")).append(". ");
                }
                if (place.has("open_state")) {
                    autoDescription.append(place.getString("open_state")).append(". ");
                }
                if (place.has("service_options")) {
                    JSONObject services = place.getJSONObject("service_options");
                    if (services.optBoolean("dine_in")) autoDescription.append("Dine-in available. ");
                    if (services.optBoolean("takeout")) autoDescription.append("Takeout available. ");
                    if (services.optBoolean("delivery")) autoDescription.append("Delivery available. ");
                }
                if (place.has("price")) {
                    autoDescription.append("Price: ").append(place.getString("price")).append(". ");
                }
                if (place.has("snippet")) {
                    autoDescription.append(place.getString("snippet"));
                }
                
                description = autoDescription.length() > 0 ? 
                             autoDescription.toString() : 
                             "No description available";
            }
            placeInfo.put("description", description);
            
            // Handle coordinates
            if (place.has("gps_coordinates")) {
                JSONObject coordinates = place.getJSONObject("gps_coordinates");
                placeInfo.put("latitude", coordinates.getDouble("latitude"));
                placeInfo.put("longitude", coordinates.getDouble("longitude"));
            } else {
                placeInfo.put("latitude", 0.0);
                placeInfo.put("longitude", 0.0);
            }
            
            return placeInfo;
        } catch (JSONException e) {
            logger.error("Error processing place info: {}", e.getMessage());
            return null;
        }
    }
} 
