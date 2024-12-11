package TravelBuddy.service;

import TravelBuddy.config.DatabaseConfig;
import TravelBuddy.model.TravelHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TravelHistoryService {

    @Autowired
    private DatabaseConfig databaseConfig;

    public void saveTravelHistory(TravelHistory travelHistory) {
        String sql = "INSERT INTO travel_history (user_id, destination, trip_duration, travel_month, itinerary, airline, flight_number, hotel, flight_review, hotel_review, overall_trip_review, rating, photos) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, travelHistory.getUserId());
            preparedStatement.setString(2, travelHistory.getDestination());
            preparedStatement.setInt(3, travelHistory.getTripDuration());
            preparedStatement.setString(4, travelHistory.getTravelMonth().toString());
            preparedStatement.setString(5, travelHistory.getItinerary());
            preparedStatement.setString(6, travelHistory.getAirline());
            preparedStatement.setString(7, travelHistory.getFlightNumber());
            preparedStatement.setString(8, travelHistory.getHotel());
            preparedStatement.setString(9, travelHistory.getFlightReview());
            preparedStatement.setString(10, travelHistory.getHotelReview());
            preparedStatement.setString(11, travelHistory.getOverallTripReview());
            preparedStatement.setInt(12, travelHistory.getRating());
            preparedStatement.setString(13, travelHistory.getPhotos());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating travel history failed, no rows affected.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    travelHistory.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating travel history failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save travel history: " + e.getMessage(), e);
        }
    }

    public List<TravelHistory> getUserTravelHistory(Long userId) {
        List<TravelHistory> travelHistories = new ArrayList<>();
        String sql = "SELECT * FROM travel_history WHERE user_id = ? ORDER BY id DESC";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    TravelHistory travelHistory = new TravelHistory();
                    travelHistory.setId(resultSet.getLong("id"));
                    travelHistory.setUserId(resultSet.getLong("user_id"));
                    travelHistory.setDestination(resultSet.getString("destination"));
                    travelHistory.setTripDuration(resultSet.getInt("trip_duration"));
                    travelHistory.setTravelMonth(Month.valueOf(resultSet.getString("travel_month")));
                    travelHistory.setItinerary(resultSet.getString("itinerary"));
                    travelHistory.setAirline(resultSet.getString("airline"));
                    travelHistory.setFlightNumber(resultSet.getString("flight_number"));
                    travelHistory.setHotel(resultSet.getString("hotel"));
                    travelHistory.setFlightReview(resultSet.getString("flight_review"));
                    travelHistory.setHotelReview(resultSet.getString("hotel_review"));
                    travelHistory.setOverallTripReview(resultSet.getString("overall_trip_review"));
                    travelHistory.setRating(resultSet.getInt("rating"));
                    travelHistory.setPhotos(resultSet.getString("photos"));
                    travelHistories.add(travelHistory);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user travel history: " + e.getMessage(), e);
        }
        return travelHistories;
    }

    public void updateTravelHistory(TravelHistory travelHistory) {
        String sql = "UPDATE travel_history SET destination = ?, trip_duration = ?, travel_month = ?, itinerary = ?, airline = ?, flight_number = ?, hotel = ?, flight_review = ?, hotel_review = ?, overall_trip_review = ?, rating = ?, photos = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, travelHistory.getDestination());
            preparedStatement.setInt(2, travelHistory.getTripDuration());
            preparedStatement.setString(3, travelHistory.getTravelMonth().toString());
            preparedStatement.setString(4, travelHistory.getItinerary());
            preparedStatement.setString(5, travelHistory.getAirline());
            preparedStatement.setString(6, travelHistory.getFlightNumber());
            preparedStatement.setString(7, travelHistory.getHotel());
            preparedStatement.setString(8, travelHistory.getFlightReview());
            preparedStatement.setString(9, travelHistory.getHotelReview());
            preparedStatement.setString(10, travelHistory.getOverallTripReview());
            preparedStatement.setInt(11, travelHistory.getRating());
            preparedStatement.setString(12, travelHistory.getPhotos());
            preparedStatement.setLong(13, travelHistory.getId());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating travel history failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update travel history: " + e.getMessage(), e);
        }
    }

    public void deleteTravelHistory(Long id) {
        String sql = "DELETE FROM travel_history WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting travel history failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete travel history: " + e.getMessage(), e);
        }
    }

    public void patchTravelHistory(Long travelHistoryId, Long userId, Map<String, Object> updates) {
        TravelHistory existingHistory = findById(travelHistoryId);
        if (existingHistory == null || !existingHistory.getUserId().equals(userId)) {
            throw new RuntimeException("Travel history not found or does not belong to the user");
        }

        // Apply updates
        if (updates.containsKey("destination")) {
            existingHistory.setDestination((String) updates.get("destination"));
        }
        
        if (updates.containsKey("tripDuration")) {
            existingHistory.setTripDuration((Integer) updates.get("tripDuration"));
        }
        if (updates.containsKey("travelMonth")) {
            existingHistory.setTravelMonth((Month) updates.get("travelMonth"));
        }
        if (updates.containsKey("itinerary")) {
            existingHistory.setItinerary((String) updates.get("itinerary"));
        }
        if (updates.containsKey("airline")) {
            existingHistory.setAirline((String) updates.get("airline"));
        }
        if (updates.containsKey("flightNumber")) {
            existingHistory.setFlightNumber((String) updates.get("flightNumber"));
        }
        if (updates.containsKey("hotel")) {
            existingHistory.setHotel((String) updates.get("hotel"));
        }
        if (updates.containsKey("flightReview")) {
            existingHistory.setFlightReview((String) updates.get("flightReview"));
        }
        if (updates.containsKey("hotelReview")) {
            existingHistory.setHotelReview((String) updates.get("hotelReview"));
        }
        if (updates.containsKey("overallTripReview")) {
            existingHistory.setOverallTripReview((String) updates.get("overallTripReview"));
        }
        if (updates.containsKey("rating")) {
            existingHistory.setRating((Integer) updates.get("rating"));
        }
        if (updates.containsKey("photos")) {
            existingHistory.setPhotos((String) updates.get("photos"));
        }

        saveTravelHistory(existingHistory);
    }

    public TravelHistory findById(Long id) {
        String sql = "SELECT * FROM travel_history WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    TravelHistory travelHistory = new TravelHistory();
                    travelHistory.setId(resultSet.getLong("id"));
                    travelHistory.setUserId(resultSet.getLong("user_id"));
                    travelHistory.setDestination(resultSet.getString("destination"));
                    travelHistory.setTripDuration(resultSet.getInt("trip_duration"));
                    travelHistory.setTravelMonth(Month.valueOf(resultSet.getString("travel_month")));
                    travelHistory.setItinerary(resultSet.getString("itinerary"));
                    travelHistory.setAirline(resultSet.getString("airline"));
                    travelHistory.setFlightNumber(resultSet.getString("flight_number"));
                    travelHistory.setHotel(resultSet.getString("hotel"));
                    travelHistory.setFlightReview(resultSet.getString("flight_review"));
                    travelHistory.setHotelReview(resultSet.getString("hotel_review"));
                    travelHistory.setOverallTripReview(resultSet.getString("overall_trip_review"));
                    travelHistory.setRating(resultSet.getInt("rating"));
                    travelHistory.setPhotos(resultSet.getString("photos"));
                    return travelHistory;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find travel history by ID: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Long> getUserTravelHistoryIds(Long userId) {
        String sql = "SELECT id FROM travel_history WHERE user_id = ? ORDER BY id DESC";
        List<Long> travelHistoryIds = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    travelHistoryIds.add(resultSet.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user travel history IDs: " + e.getMessage(), e);
        }
        return travelHistoryIds;
    }
}