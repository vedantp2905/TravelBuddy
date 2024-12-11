package TravelBuddy.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dm_reactions")
public class MessageReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reaction_type")
    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    public enum ReactionType {
        THUMBS_UP,
        THUMBS_DOWN,
        HEART,
        EXCLAMATION,
        LAUGH
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ReactionType getReactionType() {
        return reactionType;
    }

    public void setReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }
}
