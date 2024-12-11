package TravelBuddy.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
public class TravelSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private LocalDateTime expirationDate;

    @ManyToMany
    Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "travelSpace", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SpaceMessage> messages;

    @OneToMany(mappedBy = "travelSpace", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserTravelSpace> userTravelSpaces = new HashSet<>();

    public TravelSpace() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Set<SpaceMessage> getMessages() {
        return messages;
    }

    public void setMessages(Set<SpaceMessage> messages) {
        this.messages = messages;
    }
}
