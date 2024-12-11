package TravelBuddy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trivia_players")
public class TriviaPlayerEntity {
    @EmbeddedId
    private TriviaPlayerKey id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomCode")
    @JoinColumn(name = "room_code")
    private TriviaRoomEntity room;
    
    @Column(nullable = false)
    private Integer score = 0;
    
    @Column(nullable = false)
    private LocalDateTime joinedAt;
    
    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
    
    // Add getters and setters
    public TriviaPlayerKey getId() {
        return id;
    }
    
    public void setId(TriviaPlayerKey id) {
        this.id = id;
    }
    
    public Integer getScore() {
        return score;
    }
    
    public void setScore(Integer score) {
        this.score = score;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public TriviaRoomEntity getRoom() {
        return room;
    }
    
    public void setRoom(TriviaRoomEntity room) {
        this.room = room;
    }
} 