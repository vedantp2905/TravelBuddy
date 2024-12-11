package TravelBuddy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public Map<String, String> welcomeMessage() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to TravelBuddy API");
        response.put("version", "1.0.0");
        response.put("status", "operational");
        return response;
    }
}
