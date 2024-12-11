package TravelBuddy.service;

import TravelBuddy.config.DatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.sql.*;

@Service
public class RewardService {

    @Autowired
    private DatabaseConfig databaseConfig;


    public BigDecimal getBalance(Long userId) {
        String sql = "SELECT balance FROM reward_balances WHERE user_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                return resultSet.getBigDecimal("balance");
            } else {
                // If no reward balance exists, create one with 0 balance
                createRewardBalance(userId);
                return BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get reward balance: " + e.getMessage(), e);
        }
    }

    public void updateBalance(Long userId, BigDecimal newBalance) {
        String sql = "UPDATE reward_balances SET balance = ? WHERE user_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setBigDecimal(1, newBalance);
            preparedStatement.setLong(2, userId);
            int rowsAffected = preparedStatement.executeUpdate();
            
            if (rowsAffected == 0) {
                createRewardBalance(userId);
                updateBalance(userId, newBalance);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reward balance: " + e.getMessage(), e);
        }
    }

    private void createRewardBalance(Long userId) {
        String sql = "INSERT INTO reward_balances (user_id, balance) VALUES (?, 0.00)";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setLong(1, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reward balance: " + e.getMessage(), e);
        }
    }

    public void addPostReward(Long userId) {
        BigDecimal currentBalance = getBalance(userId);
        BigDecimal postReward = new BigDecimal("50.00");
        updateBalance(userId, currentBalance.add(postReward));
    }

    @Transactional
    public void addPoints(Long userId, int points) {
        String sql = "UPDATE reward_balances SET balance = balance + ? WHERE user_id = ?";
        
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            // Convert points to BigDecimal for the reward_balances table
            BigDecimal pointsToAdd = BigDecimal.valueOf(points);
            
            preparedStatement.setBigDecimal(1, pointsToAdd);
            preparedStatement.setLong(2, userId);
            
            int rowsAffected = preparedStatement.executeUpdate();
            
            // If user doesn't have a reward balance record yet, create one
            if (rowsAffected == 0) {
                createRewardBalance(userId);
                // Try update again
                preparedStatement.setBigDecimal(1, pointsToAdd);
                preparedStatement.setLong(2, userId);
                preparedStatement.executeUpdate();
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add points: " + e.getMessage(), e);
        }
    }
} 