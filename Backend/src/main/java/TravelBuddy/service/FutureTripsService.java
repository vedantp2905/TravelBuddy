package TravelBuddy.service;

import TravelBuddy.model.FutureTrip;
import TravelBuddy.config.DatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FutureTripsService {

    @Autowired
    private DatabaseConfig databaseConfig;

    public void saveFutureTrip(FutureTrip futureTrip) {
        String sql = "INSERT INTO future_trips (user_id, destination, start_date, end_date, status, budget, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, futureTrip.getUserId());
            preparedStatement.setString(2, futureTrip.getDestination());
            preparedStatement.setDate(3, Date.valueOf(futureTrip.getStartDate()));
            preparedStatement.setDate(4, Date.valueOf(futureTrip.getEndDate()));
            preparedStatement.setString(5, futureTrip.getStatus().name());
            preparedStatement.setBigDecimal(6, futureTrip.getBudget());
            preparedStatement.setString(7, futureTrip.getNotes());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating future trip failed, no rows affected.");
            }

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    futureTrip.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating future trip failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save future trip: " + e.getMessage(), e);
        }
    }

    public List<FutureTrip> getUserFutureTrips(Long userId) {
        List<FutureTrip> futureTrips = new ArrayList<>();
        String sql = "SELECT * FROM future_trips WHERE user_id = ? ORDER BY id DESC";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    FutureTrip futureTrip = new FutureTrip();
                    futureTrip.setId(resultSet.getLong("id"));
                    futureTrip.setUserId(resultSet.getLong("user_id"));
                    futureTrip.setDestination(resultSet.getString("destination"));
                    futureTrip.setStartDate(resultSet.getDate("start_date").toLocalDate());
                    futureTrip.setEndDate(resultSet.getDate("end_date").toLocalDate());
                    futureTrip.setStatus(FutureTrip.Status.valueOf(resultSet.getString("status").toUpperCase()));
                    futureTrip.setBudget(resultSet.getBigDecimal("budget"));
                    futureTrip.setNotes(resultSet.getString("notes"));
                    futureTrips.add(futureTrip);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user future trips: " + e.getMessage(), e);
        }
        return futureTrips;
    }

    public void updateFutureTrip(FutureTrip futureTrip) {
        StringBuilder sql = new StringBuilder("UPDATE future_trips SET ");
        List<Object> parameters = new ArrayList<>();
        
        if (futureTrip.getDestination() != null) {
            sql.append("destination = ?, ");
            parameters.add(futureTrip.getDestination());
        }
        if (futureTrip.getStartDate() != null) {
            sql.append("start_date = ?, ");
            parameters.add(Date.valueOf(futureTrip.getStartDate()));
        }
        if (futureTrip.getEndDate() != null) {
            sql.append("end_date = ?, ");
            parameters.add(Date.valueOf(futureTrip.getEndDate()));
        }
        if (futureTrip.getStatus() != null) {
            sql.append("status = ?, ");
            parameters.add(futureTrip.getStatus().name());
        }
        if (futureTrip.getBudget() != null) {
            sql.append("budget = ?, ");
            parameters.add(futureTrip.getBudget());
        }
        if (futureTrip.getNotes() != null) {
            sql.append("notes = ?, ");
            parameters.add(futureTrip.getNotes());
        }
        
        // Remove the last comma and space
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        parameters.add(futureTrip.getId());

        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < parameters.size(); i++) {
                preparedStatement.setObject(i + 1, parameters.get(i));
            }

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating future trip failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update future trip: " + e.getMessage(), e);
        }
    }

    public void deleteFutureTrip(Long tripId) {
        String sql = "DELETE FROM future_trips WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, tripId);

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting future trip failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete future trip: " + e.getMessage(), e);
        }
    }

    public FutureTrip getFutureTripById(Long tripId) {
        String sql = "SELECT * FROM future_trips WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, tripId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    FutureTrip futureTrip = new FutureTrip();
                    futureTrip.setId(resultSet.getLong("id"));
                    futureTrip.setUserId(resultSet.getLong("user_id"));
                    futureTrip.setDestination(resultSet.getString("destination"));
                    futureTrip.setStartDate(resultSet.getDate("start_date").toLocalDate());
                    futureTrip.setEndDate(resultSet.getDate("end_date").toLocalDate());
                    futureTrip.setStatus(FutureTrip.Status.valueOf(resultSet.getString("status").toUpperCase()));
                    futureTrip.setBudget(resultSet.getBigDecimal("budget"));
                    futureTrip.setNotes(resultSet.getString("notes"));
                    return futureTrip;
                } else {
                    throw new SQLException("No future trip found with ID: " + tripId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get future trip by ID: " + e.getMessage(), e);
        }
    }

    // Additional methods for updating and deleting future trips can be added here
}
