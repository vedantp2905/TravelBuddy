package TravelBuddy.service;

import TravelBuddy.config.DatabaseConfig;
import TravelBuddy.model.Itinerary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.DocumentException;

@Service
public class ItineraryService {

    @Autowired
    private DatabaseConfig databaseConfig;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private PDFService pdfService;

    public void saveItinerary(Itinerary itinerary) {
        String sql = "INSERT INTO itineraries (user_id, country, cities, start_date, end_date, " +
                    "number_of_adults, number_of_children, user_location, generated_itinerary, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
        try (Connection conn = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, itinerary.getUserId());
            pstmt.setString(2, itinerary.getCountry());
            pstmt.setString(3, itinerary.getCities());
            pstmt.setTimestamp(4, Timestamp.valueOf(itinerary.getStartDate()));
            pstmt.setTimestamp(5, Timestamp.valueOf(itinerary.getEndDate()));
            pstmt.setInt(6, itinerary.getNumberOfAdults());
            pstmt.setInt(7, itinerary.getNumberOfChildren());
            pstmt.setString(8, itinerary.getUserLocation());
            pstmt.setString(9, itinerary.getGeneratedItinerary());
            pstmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    itinerary.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save itinerary: " + e.getMessage(), e);
        }
    }

    public void sendItineraryEmail(String userEmail, Itinerary itinerary, String subject) {
        String htmlContent = "<html><body>" +
                           "<h1>Your Travel Itinerary</h1>" +
                           "<h2>Trip Details:</h2>" +
                           "<p>Destination: " + itinerary.getCountry() + "</p>" +
                           "<p>Cities: " + itinerary.getCities() + "</p>" +
                           "<p>Date: " + itinerary.getStartDate() + " to " + itinerary.getEndDate() + "</p>" +
                           "<h2>Generated Itinerary:</h2>" +
                           itinerary.getGeneratedItinerary() +
                           "</body></html>";

        try {
            byte[] pdfContent = pdfService.generateItineraryPDF(itinerary);
            emailService.sendItineraryEmail(userEmail, htmlContent, subject, pdfContent);
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    public List<Itinerary> getUserItineraries(Long userId) {
        List<Itinerary> itineraries = new ArrayList<>();
        String sql = "SELECT * FROM itineraries WHERE user_id = ? ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Itinerary itinerary = new Itinerary();
                itinerary.setId(rs.getLong("id"));
                itinerary.setUserId(rs.getLong("user_id"));
                itinerary.setCountry(rs.getString("country"));
                itinerary.setCities(rs.getString("cities"));
                itinerary.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                itinerary.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                itinerary.setNumberOfAdults(rs.getInt("number_of_adults"));
                itinerary.setNumberOfChildren(rs.getInt("number_of_children"));
                itinerary.setUserLocation(rs.getString("user_location"));
                itinerary.setGeneratedItinerary(rs.getString("generated_itinerary"));
                itinerary.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                itineraries.add(itinerary);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user itineraries: " + e.getMessage(), e);
        }
        return itineraries;
    }

    public void updateItinerary(Itinerary itinerary) {
        String sql = "UPDATE itineraries SET country = ?, cities = ?, start_date = ?, end_date = ?, " +
                    "number_of_adults = ?, number_of_children = ?, user_location = ?, " +
                    "generated_itinerary = ? WHERE id = ?";
                    
        try (Connection conn = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, itinerary.getCountry());
            pstmt.setString(2, itinerary.getCities());
            pstmt.setTimestamp(3, Timestamp.valueOf(itinerary.getStartDate()));
            pstmt.setTimestamp(4, Timestamp.valueOf(itinerary.getEndDate()));
            pstmt.setInt(5, itinerary.getNumberOfAdults());
            pstmt.setInt(6, itinerary.getNumberOfChildren());
            pstmt.setString(7, itinerary.getUserLocation());
            pstmt.setString(9, itinerary.getGeneratedItinerary());
            pstmt.setLong(10, itinerary.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update itinerary: " + e.getMessage(), e);
        }
    }

    public void deleteItinerary(Long id) {
        String sql = "DELETE FROM itineraries WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete itinerary: " + e.getMessage(), e);
        }
    }

    public List<Itinerary> getAllItineraries() {
        List<Itinerary> itineraries = new ArrayList<>();
        String sql = "SELECT * FROM itineraries ORDER BY created_at DESC";
        
        try (Connection conn = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Itinerary itinerary = new Itinerary();
                itinerary.setId(rs.getLong("id"));
                itinerary.setUserId(rs.getLong("user_id"));
                itinerary.setCountry(rs.getString("country"));
                itinerary.setCities(rs.getString("cities"));
                itinerary.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                itinerary.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                itinerary.setNumberOfAdults(rs.getInt("number_of_adults"));
                itinerary.setNumberOfChildren(rs.getInt("number_of_children"));
                itinerary.setUserLocation(rs.getString("user_location"));
                itinerary.setGeneratedItinerary(rs.getString("generated_itinerary"));
                itinerary.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                itineraries.add(itinerary);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all itineraries: " + e.getMessage(), e);
        }
        return itineraries;
    }

    public Itinerary getItineraryById(Long id) {
        String sql = "SELECT * FROM itineraries WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Itinerary itinerary = new Itinerary();
                itinerary.setId(rs.getLong("id"));
                itinerary.setUserId(rs.getLong("user_id"));
                itinerary.setCountry(rs.getString("country"));
                itinerary.setCities(rs.getString("cities"));
                itinerary.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                itinerary.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                itinerary.setNumberOfAdults(rs.getInt("number_of_adults"));
                itinerary.setNumberOfChildren(rs.getInt("number_of_children"));
                itinerary.setUserLocation(rs.getString("user_location"));
                itinerary.setGeneratedItinerary(rs.getString("generated_itinerary"));
                itinerary.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return itinerary;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch itinerary by ID: " + e.getMessage(), e);
        }
    }
} 