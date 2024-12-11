package TravelBuddy.model.messages;

import TravelBuddy.model.Winner;
import java.util.List;

public class GameOverMessage {
    private String type = "GAME_OVER";
    private List<Winner> winners;

    public GameOverMessage(List<Winner> winners) {
        this.winners = winners;
        // Set ranks for winners
        for (int i = 0; i < winners.size(); i++) {
            winners.get(i).setRank(i + 1);
        }
    }

    public String getType() {
        return type;
    }

    public List<Winner> getWinners() {
        return winners;
    }
} 