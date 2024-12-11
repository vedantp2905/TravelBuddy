package TravelBuddy.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class SpaceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String message;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "travel_space_id", nullable = false)
    private TravelSpace travelSpace;

    @ManyToOne
    @JoinColumn(name = "parent_message_id")
    private SpaceMessage parentMessage;

    @OneToMany(mappedBy = "parentMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpaceMessage> replies = new ArrayList<>();

    @Column(name = "message_type", nullable = false)
    private String messageType;

    public SpaceMessage() {

    }

    public SpaceMessage(User user, String message, TravelSpace space) {
        this.sender = user;
        this.message = message;
        this.travelSpace = space;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TravelSpace getTravelSpace() {
        return travelSpace;
    }

    public void setTravelSpace(TravelSpace travelSpace) {
        this.travelSpace = travelSpace;
    }

    public SpaceMessage getParentMessage() {
        return parentMessage;
    }

    public void setParentMessage(SpaceMessage parentMessage) {
        this.parentMessage = parentMessage;
    }

    public List<SpaceMessage> getReplies() {
        return replies;
    }

    public void setReplies(List<SpaceMessage> replies) {
        this.replies = replies;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
