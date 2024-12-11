package TravelBuddy.service;

import TravelBuddy.config.DatabaseConfig;
import TravelBuddy.model.User;
import TravelBuddy.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException; // Add this import
import java.util.Date;

import java.util.UUID; // Add this import

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService; // Add this import

    @Autowired
    private DatabaseConfig databaseConfig;

    @PostConstruct
    public void checkDatabaseConnection() {
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword())) {
            if (connection != null) {
                System.out.println("Connected to the MySQL database!");
            }
        } catch (SQLException e) {
            System.err.println("Connection to the MySQL database failed: " + e.getMessage());
        }
    }

    public void save(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        String sql = "INSERT INTO users (email, first_name, last_name, username, age, gender, password, is_email_verified, email_verification_token) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String token = generateVerificationToken();
            user.setEmailVerificationToken(token);
            user.setEmailVerified(false);

            preparedStatement.setString(1, user.getEmail());
            preparedStatement.setString(2, user.getFirstName());
            preparedStatement.setString(3, user.getLastName());
            preparedStatement.setString(4, user.getUsername());
            preparedStatement.setInt(5, user.getAge());
            preparedStatement.setString(6, user.getGender());
            preparedStatement.setString(7, encodedPassword);
            preparedStatement.setBoolean(8, user.isEmailVerified());
            preparedStatement.setString(9, user.getEmailVerificationToken());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) {
        User user = null;
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                user = new User();
                user.setId(resultSet.getLong("id"));
                user.setUsername(resultSet.getString("username"));
                user.setPassword(resultSet.getString("password"));
                user.setFirstName(resultSet.getString("first_name"));
                user.setLastName(resultSet.getString("last_name"));
                user.setAge(resultSet.getInt("age"));
                user.setGender(resultSet.getString("gender"));
                user.setEmail(resultSet.getString("email"));
                user.setEmailVerified(resultSet.getBoolean("is_email_verified"));
                user.setRole(resultSet.getInt("role")); // Ensure this line is present
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find user by username: " + e.getMessage(), e);
        }
        return user;
    }

    public boolean checkPassword(String rawPassword, String storedPassword) {
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    public void deleteUser(Long id) {
        if (findById(id) == null) {
            throw new NoSuchElementException("User not found with ID: " + id);
        }
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting user failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    public User findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userRepository.findById(id).orElse(null);
    }

    public void updateUser(User user) {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        if (user.getPassword() != null) {
            sql.append("password = ?, ");
            params.add(user.getPassword());
        }
        if (user.getFirstName() != null) {
            sql.append("first_name = ?, ");
            params.add(user.getFirstName());
        }
        if (user.getLastName() != null) {
            sql.append("last_name = ?, ");
            params.add(user.getLastName());
        }
        if (user.getAge() != null) {
            sql.append("age = ?, ");
            params.add(user.getAge());
        }
        if (user.getGender() != null) {
            sql.append("gender = ?, ");
            params.add(user.getGender());
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            sql.append("email = ?, ");
            params.add(user.getEmail());
        }
        sql.append("is_email_verified = ?, ");
        params.add(user.isEmailVerified());
        if (user.getEmailVerificationToken() != null) {
            sql.append("email_verification_token = ?, ");
            params.add(null); // Always set to null when updating
        }
        
        // Add role update
        sql.append("role = ?, ");
        params.add(user.getRole());

        // Remove the trailing comma and space
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(user.getId());

        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                preparedStatement.setObject(i + 1, params.get(i));
            }
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Failed to update user");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    //This method updates the password of a user based on their id
    public void updateUserPassword(Long id, String newPassword) {
        //Use the findById method to identify the user
        User user = findById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        //Encode the password and set the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        //Use JPA hibernate to save the user to remote database
        userRepository.save(user);

    }

    public void deleteUserById(Long id) {
        //Check if the user exists using existsById method
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        //Use existing JpaRepository method
        userRepository.deleteById(id);
    }

    public boolean userExists(Long id) {

        if (!userRepository.existsById(id)) {
            return false;
        }
        return true;
    }

    public void changeUserStatus(Long id, int status) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        if (status > 2 || status < 0) {
            throw new RuntimeException("Status does not exist");
        }
        User user = userRepository.getReferenceById(id);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) {
        emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    public void verifyEmail(String token) {
        User user = findByVerificationToken(token);
        if (user != null) {
            String sql = "UPDATE users SET email = COALESCE(pending_email, email), is_email_verified = TRUE, email_verification_token = NULL, pending_email = NULL WHERE id = ?";
            try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, user.getId());
                int updatedRows = preparedStatement.executeUpdate();
                if (updatedRows == 0) {
                    throw new RuntimeException("Failed to verify email for user ID: " + user.getId());
                }
            } catch (SQLException e) {
                throw new RuntimeException("Database error while verifying email: " + e.getMessage(), e);
            }
        } else {
            throw new RuntimeException("Invalid verification token");
        }
    }

    public User findByVerificationToken(String token) {
        User user = null;
        String sql = "SELECT * FROM users WHERE email_verification_token = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, token);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                user = new User();
                user.setId(resultSet.getLong("id"));
                user.setUsername(resultSet.getString("username"));
                user.setPassword(resultSet.getString("password"));
                user.setFirstName(resultSet.getString("first_name"));
                user.setLastName(resultSet.getString("last_name"));
                user.setAge(resultSet.getInt("age"));
                user.setGender(resultSet.getString("gender"));
                user.setEmail(resultSet.getString("email"));
                user.setEmailVerified(resultSet.getBoolean("is_email_verified"));
                user.setEmailVerificationToken(resultSet.getString("email_verification_token"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by verification token: " + e.getMessage(), e);
        }
        return user;
    }

    public void changeEmail(User user, String newEmail) {
        String token = generateVerificationToken();
        user.setEmail(newEmail);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(token);
        updateUser(user);
        emailService.sendVerificationEmail(newEmail, token);
    }

    public void initiateEmailChange(User user, String newEmail) {
        String token = generateVerificationToken();
        String sql = "UPDATE users SET pending_email = ?, email_verification_token = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, newEmail);
            preparedStatement.setString(2, token);
            preparedStatement.setLong(3, user.getId());
            preparedStatement.executeUpdate();
            emailService.sendVerificationEmail(newEmail, token);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initiate email change: " + e.getMessage(), e);
        }
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                User user = new User();
                user.setId(resultSet.getLong("id"));
                user.setUsername(resultSet.getString("username"));
                user.setPassword(resultSet.getString("password"));
                user.setFirstName(resultSet.getString("first_name"));
                user.setLastName(resultSet.getString("last_name"));
                user.setAge(resultSet.getInt("age"));
                user.setGender(resultSet.getString("gender"));
                user.setEmail(resultSet.getString("email"));
                user.setEmailVerified(resultSet.getBoolean("is_email_verified"));
                user.setNewsletterSubscribed(resultSet.getBoolean("newsletter_subscribed"));
                users.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get all users: " + e.getMessage(), e);
        }
        return users;
    }

    public void updateNewsletterPreference(Long userId, boolean subscribed) {
        String sql = "UPDATE users SET newsletter_subscribed = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setBoolean(1, subscribed);
            preparedStatement.setLong(2, userId);
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No user found for ID: " + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update newsletter preference: " + e.getMessage(), e);
        }
    }

    public void downgradeFromPremium(User user) {
        user.setRole(2); // Set role back to normal user
        user.setPremiumPlan(null);
        user.setPremiumExpiryDate(null);
        updateUserPremiumStatus(user);
    }

    public void updateUserPremiumStatus(User user) {
        String sql = "UPDATE users SET role = ?, premium_plan = ?, premium_expiry_date = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            
            preparedStatement.setInt(1, user.getRole());
            preparedStatement.setString(2, user.getPremiumPlan());
            preparedStatement.setTimestamp(3, new java.sql.Timestamp(user.getPremiumExpiryDate().getTime()));
            preparedStatement.setLong(4, user.getId());
            
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No user found for ID: " + user.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user premium status: " + e.getMessage(), e);
        }
    }

    public void checkAndDowngradePremiumUsers() {
        List<User> premiumUsers = userRepository.findByRole(3);
        Date currentDate = new Date();
        for (User user : premiumUsers) {
            if (user.getPremiumExpiryDate() != null && user.getPremiumExpiryDate().before(currentDate)) {
                downgradeFromPremium(user);
            }
        }
    }

    public boolean checkIfSamePassword(Long userId, String newPassword) {
        String sql = "SELECT password FROM users WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), 
                 databaseConfig.getUsername(), databaseConfig.getPassword());
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        
            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
        
            if (resultSet.next()) {
                String currentHashedPassword = resultSet.getString("password");
                return passwordEncoder.matches(newPassword, currentHashedPassword);
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check password: " + e.getMessage(), e);
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            try (Connection conn = databaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking email existence", e);
        }
    }
}
