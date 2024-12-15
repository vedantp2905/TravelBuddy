package TravelBuddy.controller;

import TravelBuddy.service.ApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/external-api/")
@Tag(name="External APIs", description = "Controller for managing access to external APIs, such as  weather, hotel, and flight details APIs.")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }


    @GetMapping("/fetch-data")
    public String fetchData() {

        return apiService.fetchData();
    }

    //Example in Postman: localhost:8080/fetch-flight-details?origin=Los%20Angeles&destination=New%20York
    @GetMapping("/test-fetch-flight-details")
    public Map<String,Object> fetchFlightDetails(@RequestParam String origin, @RequestParam String destination) {

        Map<String,Object> data = apiService.fetchFlightDetails(origin, destination);

       return data;
    }

    @Operation(summary = "Fetching flight details",
            description = "Returns details on flights within a certain time period.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flight details returned"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    @GetMapping("/fetch-flight-details")
    public List<Map<String,Object>> testFlight(@RequestParam String departureID, @RequestParam String arrivalID, @RequestParam String outboundDate, @RequestParam String returnDate) {
        List<Map<String,Object>> data  = apiService.fetchFlightDetails2(departureID,arrivalID,outboundDate,returnDate);
        return (data);
    }

    @Operation(summary = "Fetching hotel details",
            description = "Returns details on hotels within a certain time period.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel details returned"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    @GetMapping("/fetch-hotel-details")
    public List<Map<String,Object>> hotelDetails(@RequestParam String city, @RequestParam String checkIn, @RequestParam String checkOut) {
        List<Map<String,Object>> data = apiService.fetchHotelData(city, checkIn, checkOut);
        return data;
    }

    @Operation(summary = "Fetching coordinate details",
            description = "Returns the coordinates for a city based on the city name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel details returned"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    @GetMapping("/fetch-city-by-coords")
    public Map<String,Object> coordDetails(@RequestParam String city) {
        Map<String,Object> data = apiService.convertCityToCoord(city);
        return data;
    }

    @Operation(summary = "Fetching weather details",
            description = "Returns weather data based on provided coordinates.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Weather details returned"),
            @ApiResponse(responseCode = "404", description = "Endpoint not found")
    })
    @GetMapping("fetch-weather-data")
    public Map<String,Object> weatherDetails(@RequestParam String city) {
        Map<String,Object> data = apiService.fetchWeatherData(city);
        return data;
    }

}
