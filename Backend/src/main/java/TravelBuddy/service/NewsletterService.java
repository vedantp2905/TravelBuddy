package TravelBuddy.service;

import TravelBuddy.config.DatabaseConfig;
import TravelBuddy.model.Newsletter;
import TravelBuddy.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;

@Service
public class NewsletterService {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private DatabaseConfig databaseConfig;

    private final String newsletterApiUrl = "http://localhost:5005/generate-newsletter";

    public String generateNewsletter(String topic) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("topic", topic);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                newsletterApiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            @SuppressWarnings("null")
            String htmlContent = (String) responseBody.get("formatted_html");
            System.out.println(htmlContent);

            // Save the generated newsletter
            saveNewsletter(topic, htmlContent);

            return htmlContent;
        } catch (Exception e) {
            // Log the error and return a default message
            System.err.println("Failed to generate newsletter: " + e.getMessage());
            return "<html><body><h1>Default newsletter content</h1></body></html>";
        }
    }

    public void sendNewsletterToAllUsers(String topic) {
        String newsletter = generateNewsletter(topic);
        List<User> users = userService.getAllUsers();
        for (User user : users) {
            if (user.isNewsletterSubscribed()) {
                // System.out.println("Sending newsletter to user: " + user.getEmail());
                emailService.sendNewsletter(user.getEmail(), newsletter);
            } else {
                System.out.println("Skipping unsubscribed email: " + user.getEmail());
            }
        }
    }

    public void saveNewsletter(String topic, String htmlContent) {
        String sql = "INSERT INTO newsletters (topic, content) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, topic);
            preparedStatement.setString(2, htmlContent);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save newsletter: " + e.getMessage(), e);
        }
    }

    public List<Newsletter> getAllNewsletters() {
        List<Newsletter> newsletters = new ArrayList<>();
        String sql = "SELECT * FROM newsletters ORDER BY created_at DESC";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Newsletter newsletter = new Newsletter();
                newsletter.setId(resultSet.getLong("id"));
                newsletter.setTopic(resultSet.getString("topic"));
                newsletter.setContent(resultSet.getString("content"));
                newsletter.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                newsletters.add(newsletter);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch newsletters: " + e.getMessage(), e);
        }
        return newsletters;
    }
    
}
