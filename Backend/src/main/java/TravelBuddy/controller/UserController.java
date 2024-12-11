package TravelBuddy.controller;

import TravelBuddy.model.User;
import TravelBuddy.service.UserService;
import TravelBuddy.service.PaymentService;
import TravelBuddy.service.EmailService;
import TravelBuddy.config.DatabaseConfig;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import com.stripe.model.PaymentIntent;

import java.util.Date;
import java.util.Calendar;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.DriverManager;

import java.util.Random;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing user accounts and authentication")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseConfig databaseConfig;

    @Operation(summary = "User signup",
            description = "Register a new user account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            if (user.getUsername() == null || user.getPassword() == null || user.getFirstName() == null || 
                user.getLastName() == null || user.getAge() == null || user.getGender() == null || user.getEmail() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All fields are required.");
            }
            if (userService.findByUsername(user.getUsername()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists! Use a different username.");
            }
            
            // Set default role to normal (2)
            user.setRole(2);
            
            userService.save(user);
            userService.sendVerificationEmail(user);
            return ResponseEntity.ok("User created successfully. Please check your email for verification.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Verify email",
            description = "Verifies a user's email address using the verification token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid verification token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(
        @Parameter(description = "Email verification token") @RequestParam String token
    ) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok("Email verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "User login",
            description = "Authenticate user and return access token")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            if (user.getUsername() == null || user.getPassword() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username and password are required.");
            }
            User existingUser = userService.findByUsername(user.getUsername());
            if (existingUser != null) {
                if (!existingUser.isEmailVerified()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is not verified. Please check your email for verification.");
                }
                boolean passwordMatch = userService.checkPassword(user.getPassword(), existingUser.getPassword());
                if (passwordMatch) {
                    // Return user ID and role instead of a success message
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Login successful");
                    response.put("userId", existingUser.getId());
                    response.put("role", existingUser.getRole()); // Include the role in the response
                    return ResponseEntity.ok(response);
                }
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    @Operation(summary = "Update user details",
            description = "Updates a user's basic information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User details updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or attempt to change username/email"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateUserDetails(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "Updated user details") @RequestBody User updatedUser
    ) {
        try {
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            if (!existingUser.getUsername().equals(updatedUser.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username cannot be changed.");
            }

            if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email cannot be changed through this endpoint. Use the change-email endpoint instead.");
            }

            // Password encoding should be done in the UserService
            existingUser.setPassword(updatedUser.getPassword());
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());
            existingUser.setAge(updatedUser.getAge());
            existingUser.setGender(updatedUser.getGender());

            userService.updateUser(existingUser);
            return ResponseEntity.ok("User details updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Update password",
            description = "Updates a user's password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or empty password"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/update-password/{id}")
    public ResponseEntity<String> updateUserPassword(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "New password details") @RequestBody Map<String, Object> body
    ) {
        try {
            String newPassword = (String) body.get("password");
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password cannot be empty");
            }
            userService.updateUserPassword(id, newPassword);
            return ResponseEntity.ok("Password successfully changed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Delete user",
            description = "Deletes a user account from the system")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
        @Parameter(description = "ID of the user to delete") @PathVariable Long id
    ) {
        try {
            userService.deleteUserById(id);
            return ResponseEntity.ok("Successfully deleted the user by their id.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Update user status",
            description = "Updates a user's status (Default/Premium/Admin)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/update-status/{id}")
    public ResponseEntity<?> updateUserStatus(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "New status details") @RequestBody Map<String,Object> body
    ) {
        try {
            String status = (String) body.get("status");
            int statusInt = Integer.parseInt(status);
            userService.changeUserStatus(id, statusInt);
            String statusName = "";
            if (statusInt == 0) {
                statusName = "Default";
            }
            else if (statusInt == 1) {
                statusName = "Premium";
            }
            else {
                statusName = "Admin";
            }
            return ResponseEntity.ok("Successfully updated the user's status to " + statusName);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Partial update user",
            description = "Updates specific fields of a user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/update/{id}")
    public ResponseEntity<String> partialUpdateUser(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "Fields to update") @RequestBody Map<String, Object> updates
    ) {
        try {
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            if (updates.containsKey("username")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username cannot be changed.");
            }

            if (updates.containsKey("email")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email cannot be changed through this endpoint. Use the change-email endpoint instead.");
            }

            updates.forEach((key, value) -> {
                switch (key) {
                    case "password":
                        existingUser.setPassword((String) value);
                        break;
                    case "firstName":
                    case "first_name":
                        existingUser.setFirstName((String) value);
                        break;
                    case "lastName":
                    case "last_name":
                        existingUser.setLastName((String) value);
                        break;
                    case "age":
                        existingUser.setAge((Integer) value);
                        break;
                    case "gender":
                        existingUser.setGender((String) value);
                        break;
                }
            });

            userService.updateUser(existingUser);
            return ResponseEntity.ok("User details updated successfully");
        } catch (NullPointerException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        } catch (ClassCastException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data type for one or more fields");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Change email",
            description = "Initiates email change process for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email change initiated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email or unverified current email"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/change-email/{id}")
    public ResponseEntity<String> changeEmail(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "New email details") @RequestBody Map<String, String> emailMap
    ) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            String newEmail = emailMap.get("email");
            if (newEmail == null || newEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New email cannot be empty");
            }

            // Check if new email is same as current email
            if (newEmail.equals(user.getEmail())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New email cannot be same as current email");
            }

            // Check if email exists for another user
            if (userService.emailExists(newEmail)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists for another user");
            }

            // Use the initiateEmailChange method instead of direct update
            userService.initiateEmailChange(user, newEmail);
            
            return ResponseEntity.ok("Email change initiated. Please verify your new email.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Update newsletter preference",
            description = "Updates user's newsletter subscription preference")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Newsletter preference updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/newsletter-preference/{id}")
    public ResponseEntity<String> updateNewsletterPreference(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "Subscription preference") @RequestBody Map<String, Boolean> preference
    ) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            Boolean subscribed = preference.get("subscribed");
            if (subscribed == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("'subscribed' field is required");
            }

            userService.updateNewsletterPreference(id, subscribed);
            return ResponseEntity.ok("Newsletter preference updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get newsletter preference",
            description = "Retrieves user's current newsletter subscription status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved newsletter preference"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/newsletter-preference/{id}")
    public ResponseEntity<?> getNewsletterPreference(
        @Parameter(description = "ID of the user") @PathVariable Long id
    ) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }
            
            boolean subscribed = user.isNewsletterSubscribed();
            Map<String, Boolean> response = new HashMap<>();
            response.put("subscribed", subscribed);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Update user role",
            description = "Updates a user's role in the system")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User role updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role value"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/update-role/{id}")
    public ResponseEntity<String> updateUserRole(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "New role details") @RequestBody Map<String, Integer> roleMap
    ) {
        try {
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            Integer newRole = roleMap.get("role");
            if (newRole == null || (newRole < 1 || newRole > 3)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid role. Must be 1 (admin), 2 (normal), or 3 (premium).");
            }

            existingUser.setRole(newRole);
            userService.updateUser(existingUser);
            return ResponseEntity.ok("User role updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Get current user",
            description = "Retrieves the currently authenticated user's details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user details"),
        @ApiResponse(responseCode = "401", description = "No authenticated user"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No authenticated user");
        }
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Get user by ID",
            description = "Retrieves a specific user's details by their ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        User user = userService.findById(userId);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Upgrade to premium",
            description = "Upgrade user account to premium status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User upgraded to premium successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{userId}/upgrade-to-premium")
    public ResponseEntity<?> upgradeToPremium(@PathVariable Long userId, @RequestParam String plan) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found with ID: " + userId));
            }

            if (user.getRole() == 3) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "User is already a premium member"));
            }

            long amount;
            if ("monthly".equalsIgnoreCase(plan)) {
                amount = 999; // $9.99 for monthly plan
            } else if ("annual".equalsIgnoreCase(plan)) {
                amount = 9900; // $99.00 for annual plan
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid plan. Choose 'monthly' or 'annual'."));
            }

            PaymentIntent paymentIntent = paymentService.createPaymentIntent(amount, "usd");

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("plan", plan);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    @Operation(summary = "Confirm premium upgrade",
            description = "Confirms and processes premium membership upgrade after successful payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Premium upgrade confirmed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid payment data or payment unsuccessful"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{userId}/confirm-premium-upgrade")
    public ResponseEntity<?> confirmPremiumUpgrade(
        @Parameter(description = "ID of the user") @PathVariable Long userId,
        @Parameter(description = "Payment confirmation details") @RequestBody Map<String, String> payload
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "User not found with ID: " + userId,
                    "role", 0
                ));
            }

            String paymentIntentId = payload.get("paymentIntentId");
            String plan = payload.get("plan");
            if (paymentIntentId == null || plan == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Payment intent ID and plan are required",
                    "role", user.getRole()
                ));
            }

            // Verify the payment status with Stripe
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            if ("succeeded".equals(paymentIntent.getStatus())) {
                // Update user's premium status immediately
                user.setRole(3);
                user.setPremiumPlan(plan);
                user.setPremiumExpiryDate(calculateExpiryDate(plan));
                userService.updateUserPremiumStatus(user);
                return ResponseEntity.ok(Map.of(
                    "message", "Payment successful. You have been upgraded to premium " + plan + " plan.",
                    "role", user.getRole()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Payment not successful",
                    "role", user.getRole()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error: " + e.getMessage(),
                "role", 0
            ));
        }
    }

    private Date calculateExpiryDate(String plan) {
        Calendar calendar = Calendar.getInstance();
        if ("monthly".equalsIgnoreCase(plan)) {
            calendar.add(Calendar.MONTH, 1);
        } else if ("annual".equalsIgnoreCase(plan)) {
            calendar.add(Calendar.YEAR, 1);
        }
        calendar.add(Calendar.SECOND, -1); // Set to 23:59:59
        return calendar.getTime();
    }

    @Operation(summary = "Downgrade from premium",
            description = "Removes premium membership status from a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully downgraded from premium"),
        @ApiResponse(responseCode = "400", description = "User is not a premium member"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{userId}/downgrade-from-premium")
    public ResponseEntity<?> downgradeFromPremium(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }

            if (user.getRole() != 3) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not a premium member");
            }

            userService.downgradeFromPremium(user);
            return ResponseEntity.ok("User has been successfully downgraded from premium");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Check password",
            description = "Verifies if provided password matches user's current password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password check completed"),
        @ApiResponse(responseCode = "400", description = "Password not provided"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/check-password/{id}")
    public ResponseEntity<?> checkPassword(
        @Parameter(description = "ID of the user") @PathVariable Long id,
        @Parameter(description = "Password to check") @RequestBody Map<String, String> body
    ) {
        try {
            String password = body.get("password");
            if (password == null || password.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password is required");
            }

            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            boolean isSamePassword = userService.checkIfSamePassword(id, password);
            Map<String, Boolean> response = new HashMap<>();
            response.put("isSame", isSamePassword);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error checking password: " + e.getMessage());
        }
    }

    @Operation(summary = "Request password reset",
            description = "Sends a password reset link to the user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset link sent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reset-password-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }

            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No account found with this email");
            }

            // Generate 6-digit code
            String resetCode = String.format("%06d", new Random().nextInt(999999));
            String updateSql = "UPDATE users SET reset_code = ?, reset_code_expiry = ? WHERE id = ?";
            try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), 
                    databaseConfig.getUsername(), databaseConfig.getPassword());
                 PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                
                stmt.setString(1, resetCode);
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)));
                stmt.setLong(3, user.getId());
                stmt.executeUpdate();
                
                // Send verification code email
                emailService.sendPasswordResetCode(user.getEmail(), resetCode);
                
                return ResponseEntity.ok("Verification code has been sent to your email");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        }
    }

    @Operation(summary = "Verify reset code",
            description = "Verifies the reset code sent via email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Code verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/verify-reset-code")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            
            System.out.println("Received code: " + code);
            
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            if (user.getResetCode() == null) {
                return ResponseEntity.badRequest().body("No reset code found");
            }
            if (!user.getResetCode().equals(code)) {
                return ResponseEntity.badRequest().body("Invalid code");
            }
            if (user.getResetCodeExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("Code has expired");
            }
            System.out.println("Code verified successfully. Sending response ...");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error verifying code: " + e.getMessage());
        }
    }

    @Operation(summary = "Reset password with code",
            description = "Resets user's password using the code sent via email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reset-password-with-code")
    public ResponseEntity<?> resetPasswordWithCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");
            String newPassword = request.get("password");

            // Validate inputs
            if (email == null || code == null || newPassword == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            // First verify the code is valid
            String verifyCodeSql = "SELECT id FROM users WHERE email = ? AND reset_code = ? AND reset_code_expiry > NOW()";
            String updatePasswordSql = "UPDATE users SET password = ?, reset_code = NULL, reset_code_expiry = NULL WHERE email = ?";

            try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), 
                    databaseConfig.getUsername(), databaseConfig.getPassword())) {
                
                // First verify the code
                try (PreparedStatement verifyStmt = connection.prepareStatement(verifyCodeSql)) {
                    verifyStmt.setString(1, email);
                    verifyStmt.setString(2, code);
                    ResultSet rs = verifyStmt.executeQuery();
                    
                    if (!rs.next()) {
                        return ResponseEntity.badRequest().body("Invalid or expired code");
                    }
                }

                // If code is valid, update password
                try (PreparedStatement updateStmt = connection.prepareStatement(updatePasswordSql)) {
                    String hashedPassword = passwordEncoder.encode(newPassword);
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setString(2, email);
                    updateStmt.executeUpdate();
                }

                return ResponseEntity.ok().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        }
    }
}
