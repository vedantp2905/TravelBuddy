package TravelBuddy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiService {

    @Autowired
    private final RestTemplate restTemplate;

    //API Key for SerpAPI
    private final String serpApiKey = "1cc2020c247c0f653eecd2555ac271cea234ab401db025cd2ee30188bda30ae2";
    private final String openWeatherKey = "91943c2eaf8a54fdd6c386e2af583ec7";

    public ApiService(RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    public String fetchData() {

        String url = "https://jsonplaceholder.typicode.com/posts/1";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return response.getBody();
    }

    public Map<String,Object> fetchFlightDetails(String origin, String destination) {

        String query = String.format("flights from %s to %s", origin, destination);
        String url = String.format("https://serpapi.com/search.json?q=%s&api_key=%s", query, serpApiKey);

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        return response.getBody();

    }

    public List<Map<String, Object>> fetchFlightDetails2(String departureID, String arrivalID, String outboundDate, String returnDate) {

        String url = UriComponentsBuilder.fromHttpUrl("https://serpapi.com/search")
                .queryParam("engine", "google_flights")
                //.queryParam("q", String.format("flights from %s to %s", departureCity, destinationCity))
                .queryParam("departure_id", departureID)
                .queryParam("arrival_id", arrivalID)
                .queryParam("outbound_date", outboundDate)
                .queryParam("return_date", returnDate)
                .queryParam("api_key", serpApiKey)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        //return response.getBody();
        Map<String,Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("best_flights")) {
            return ((List<Map<String,Object>>) responseBody.get("best_flights"));
        }
        else {
            return List.of();
        }
    }


    public List<Map<String,Object>> fetchHotelData(String city, String checkInDate, String checkOutDate) {

        String url = UriComponentsBuilder.fromHttpUrl("https://serpapi.com/search")
                .queryParam("engine", "google_hotels")
                .queryParam("q", String.format("hotels in %s",city))
                .queryParam("check_in_date", checkInDate)
                .queryParam("check_out_date", checkOutDate)
                .queryParam("api_key", serpApiKey)
                .toUriString();

        //for testing purposes
        //System.out.println(url);

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        //return response.getBody();
        Map<String,Object> responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("properties")) {
            return ((List<Map<String,Object>>) responseBody.get("properties"));
        }
        else {
            return List.of();
        }
    }

    public Map<String,Object> convertCityToCoord(String city) {

        String url = String.format("http://api.openweathermap.org/geo/1.0/direct?q=%s&limit=5&appid=%s", city, openWeatherKey);

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

        List<Map<String,Object>> result = response.getBody();
        if (result != null && !result.isEmpty()) {

            Map<String,Object> firstVal = result.get(0);
            Map<String,Object> coords = new HashMap<>();
            coords.put("lat", firstVal.get("lat"));
            coords.put("lon", firstVal.get("lon"));
            return (coords);
        }
        else {
            throw new RuntimeException("City not found");
        }
    }
    public Map<String,Object> fetchWeatherData(String city) {
        Map<String,Object> coords = convertCityToCoord(city);
        String lat = coords.get("lat").toString();
        String lon = coords.get("lon").toString();

        String url = String.format("https://api.openweathermap.org/data/2.5/forecast?lat=%s&lon=%s&appid=%s", lat, lon, openWeatherKey);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        return (response.getBody());
    }

}

