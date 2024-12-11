package TravelBuddy.model;

import jakarta.persistence.*;

@Entity
@Table(name =  "user_travel_space", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "travel_space_id"})})
public class UserTravelSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "travel_space_id", nullable = false)
    private TravelSpace travelSpace;

    private String color;

    public UserTravelSpace() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TravelSpace getTravelSpace() {
        return travelSpace;
    }

    public void setTravelSpace(TravelSpace travelSpace) {
        this.travelSpace = travelSpace;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
