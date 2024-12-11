package TravelBuddy.model.messages;

public class RoomCreatedMessage {
    private String type;
    private String roomCode;
    private String hostName;

    public RoomCreatedMessage(String type, String roomCode, String hostName) {
        this.type = type;
        this.roomCode = roomCode;
        this.hostName = hostName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public String toString() {
        return String.format("RoomCreatedMessage{type='%s', roomCode='%s', hostName='%s'}", type, roomCode, hostName);
    }
} 