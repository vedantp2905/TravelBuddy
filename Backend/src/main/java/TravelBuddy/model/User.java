package TravelBuddy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private String email;
    private String username;
    private String password;
    private Integer age;
    private String gender;

    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "is_email_verified")
    private boolean isEmailVerified;
    @Column(name = "email_verification_token")
    private String emailVerificationToken;
    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(name = "newsletter_subscribed")
    private Boolean newsletterSubscribed;

//    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<SpaceMessage> messages;
    // New role field
    private int role; // 1 for admin, 2 for normal, 3 for premium

    private String premiumPlan;
    private Date premiumExpiryDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<UserTravelSpace> userTravelSpaces = new HashSet<>();

    @Column(nullable = false)
    private Boolean profileCompleted = false;  // defaults to false for new users

    @Column(length = 6)
    private String resetCode;

    @Column
    private LocalDateTime resetCodeExpiry;

    public Boolean getNewsletterSubscribed() {
        return newsletterSubscribed;
    }

    public String privacySetting;

    // Getters and setters with error handling
    public Long getId() {
        if (id == null) {
            throw new NullPointerException("User ID is not initialized.");
        }
        return id;
    }

    public void setId(Long id) {
        if (id == null) {
            throw new NullPointerException("User ID cannot be null.");
        }
        this.id = id;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        if (username == null) {
            throw new NullPointerException("Username is not initialized.");
        }
        return username;
    }

    public void setUsername(String username) {
        if (username == null) {
            throw new NullPointerException("Username cannot be null.");
        }
        this.username = username;
    }

    public String getPassword() {
        if (password == null) {
            throw new NullPointerException("Password is not initialized.");
        }
        return password;
    }

    public void setPassword(String password) {
        if (password == null) {
            throw new NullPointerException("Password cannot be null.");
        }
        this.password = password;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public String getPendingEmail() {
        return pendingEmail;
    }

    public void setPendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
    }

    public Boolean isNewsletterSubscribed() {
        return newsletterSubscribed;
    }

    public void setNewsletterSubscribed(Boolean newsletterSubscribed) {
        this.newsletterSubscribed = newsletterSubscribed;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getPremiumPlan() {
        return premiumPlan;
    }

    public void setPremiumPlan(String premiumPlan) {
        this.premiumPlan = premiumPlan;
    }

    public Date getPremiumExpiryDate() {
        return premiumExpiryDate;
    }

    public void setPremiumExpiryDate(Date premiumExpiryDate) {
        this.premiumExpiryDate = premiumExpiryDate;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public LocalDateTime getResetCodeExpiry() {
        return resetCodeExpiry;
    }

    public void setResetCodeExpiry(LocalDateTime resetCodeExpiry) {
        this.resetCodeExpiry = resetCodeExpiry;
    }

    public Set<UserTravelSpace> getUserTravelSpaces() {
        return userTravelSpaces;
    }

    public void setUserTravelSpaces(Set<UserTravelSpace> userTravelSpaces) {
        this.userTravelSpaces = userTravelSpaces;
    }

    public String getPrivacySetting() {
        return privacySetting;
    }

    public void setPrivacySetting(String privacySetting) {
        this.privacySetting = privacySetting;
    }
}
