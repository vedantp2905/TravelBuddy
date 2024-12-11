package TravelBuddy.model.messages;

import java.util.List;

public class RoomUpdateMessage {
    private String type;
    private List<String> players;
    private String playerName;

    public RoomUpdateMessage(String type, List<String> players, String playerName) {
        this.type = type;
        this.players = players;
        this.playerName = playerName;
    }

    public String getType() {
        return type;
    }

    public List<String> getPlayers() {
        return players;
    }

    public String getPlayerName() {
        return playerName;
    }
} 