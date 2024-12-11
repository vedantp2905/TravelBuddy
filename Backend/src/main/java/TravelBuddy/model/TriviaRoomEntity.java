package TravelBuddy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trivia_rooms")
public class TriviaRoomEntity {
    @Id
    private String roomCode;
    
    @Column(nullable = false)
    private Long hostId;
    
    @Column(nullable = false)
    private String hostName;
    
    @Column(nullable = false)
    private boolean active;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    private List<TriviaPlayerEntity> players;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<TriviaPlayerEntity> getPlayers() {
        return players;
    }

    public void setPlayers(List<TriviaPlayerEntity> players) {
        this.players = players;
    }
} 