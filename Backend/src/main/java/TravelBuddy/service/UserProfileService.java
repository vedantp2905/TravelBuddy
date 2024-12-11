package TravelBuddy.service;

import TravelBuddy.config.DatabaseConfig;
import TravelBuddy.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class UserProfileService {

    @Autowired
    private DatabaseConfig databaseConfig;

    public void saveUserProfile(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (id, about_me, preferred_language, currency_preference, travel_budget, travel_style, travel_experience_level, max_trip_duration, preferred_destinations, interests, preferred_airlines, preferred_accommodation_type, dietary_restrictions, passport_country, frequent_flyer_programs) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setLong(1, profile.getId());
            preparedStatement.setString(2, profile.getAboutMe());
            preparedStatement.setString(3, profile.getPreferredLanguage());
            preparedStatement.setString(4, profile.getCurrencyPreference());
            preparedStatement.setDouble(5, profile.getTravelBudget());
            preparedStatement.setString(6, profile.getTravelStyle());
            preparedStatement.setString(7, profile.getTravelExperienceLevel());
            preparedStatement.setInt(8, profile.getMaxTripDuration());
            preparedStatement.setString(9, String.join(",", profile.getPreferredDestinations()));
            preparedStatement.setString(10, String.join(",", profile.getInterests()));
            preparedStatement.setString(11, String.join(",", profile.getPreferredAirlines()));
            preparedStatement.setString(12, profile.getPreferredAccommodationType());
            preparedStatement.setString(13, String.join(",", profile.getDietaryRestrictions()));
            preparedStatement.setString(14, profile.getPassportCountry());
            preparedStatement.setString(15, String.join(",", profile.getFrequentFlyerPrograms()));
            
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user profile: " + e.getMessage(), e);
        }
    }

    public void updateUserProfile(Long userId, Map<String, Object> updates) {
        if (updates.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("UPDATE user_profiles SET ");
        List<String> setStatements = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String columnName = convertToSnakeCase(entry.getKey());
            setStatements.add(columnName + " = ?");
            params.add(entry.getValue().toString());
        }

        sql.append(String.join(", ", setStatements));
        sql.append(" WHERE id = ?");
        params.add(userId);

        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), 
                databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                preparedStatement.setString(i + 1, params.get(i).toString());
            }
            
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to update profile");
        }
    }

    private String convertToSnakeCase(String camelCase) {
        switch (camelCase) {
            case "aboutMe": return "about_me";
            case "preferredLanguage": return "preferred_language";
            case "currencyPreference": return "currency_preference";
            case "travelBudget": return "travel_budget";
            case "travelStyle": return "travel_style";
            case "travelExperienceLevel": return "travel_experience_level";
            case "maxTripDuration": return "max_trip_duration";
            case "preferredDestinations": return "preferred_destinations";
            case "interests": return "interests";
            case "preferredAirlines": return "preferred_airlines";
            case "accommodationType": return "preferred_accommodation_type";
            case "dietaryRestrictions": return "dietary_restrictions";
            case "passportCountry": return "passport_country";
            case "frequentFlyerPrograms": return "frequent_flyer_programs";
            default: return camelCase;
        }
    }

    public UserProfile getUserProfile(Long userId) {
        String sql = "SELECT * FROM user_profiles WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                UserProfile profile = new UserProfile();
                profile.setId(resultSet.getLong("id"));
                profile.setAboutMe(resultSet.getString("about_me"));
                profile.setPreferredLanguage(resultSet.getString("preferred_language"));
                profile.setCurrencyPreference(resultSet.getString("currency_preference"));
                profile.setTravelBudget(resultSet.getInt("travel_budget"));
                profile.setTravelStyle(resultSet.getString("travel_style"));
                profile.setTravelExperienceLevel(resultSet.getString("travel_experience_level"));
                profile.setMaxTripDuration(resultSet.getInt("max_trip_duration"));
                profile.setPreferredDestinations(Arrays.asList(resultSet.getString("preferred_destinations").split(",")));
                profile.setInterests(Arrays.asList(resultSet.getString("interests").split(",")));
                profile.setPreferredAirlines(Arrays.asList(resultSet.getString("preferred_airlines").split(",")));
                profile.setPreferredAccommodationType(resultSet.getString("preferred_accommodation_type"));
                profile.setDietaryRestrictions(Arrays.asList(resultSet.getString("dietary_restrictions").split(",")));
                profile.setPassportCountry(resultSet.getString("passport_country"));
                profile.setFrequentFlyerPrograms(Arrays.asList(resultSet.getString("frequent_flyer_programs").split(",")));
                return profile;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch user profile: " + e.getMessage(), e);
        }
        return null;
    }

    public void deleteUserProfile(Long userId) {
        String sql = "DELETE FROM user_profiles WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("No user profile found for user ID: " + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user profile: " + e.getMessage(), e);
        }
    }
}