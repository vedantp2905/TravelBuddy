package TravelBuddy.model;

import java.time.LocalDateTime;

public class FriendRequestDTO {

    private long senderId;

    private long receiverId;

    private LocalDateTime sentAt;

    public FriendRequestDTO(long senderId, long receiverId, LocalDateTime sentAt) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.sentAt = sentAt;
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
