package TravelBuddy.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
@Embeddable
public class TriviaPlayerKey implements Serializable {
    @Column(name = "room_code")
    private String roomCode;
    
    @Column(name = "user_id")
    private Long userId;
    
    public TriviaPlayerKey() {}
    
    public TriviaPlayerKey(String roomCode, Long userId) {
        this.roomCode = roomCode;
        this.userId = userId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriviaPlayerKey that = (TriviaPlayerKey) o;
        return Objects.equals(roomCode, that.roomCode) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomCode, userId);
    }
} 