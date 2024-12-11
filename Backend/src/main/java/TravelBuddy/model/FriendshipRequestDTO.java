package TravelBuddy.model;

import java.time.LocalDateTime;

public class FriendshipRequestDTO {

    private long friendId;

    private String friendUsername;
    private LocalDateTime createdAt;

    public FriendshipRequestDTO(long friendId, String friendUsername, LocalDateTime createdAt) {
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.createdAt = createdAt;
    }

    public long getFriendId() {
        return friendId;
    }

    public void setFriendId(long friendId) {
        this.friendId = friendId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }
}
