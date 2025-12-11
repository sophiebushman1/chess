
package results;

import model.GameData;
import java.util.List;

public class ListGamesResult {
    private boolean success;
    private String message;
    private List<GameData> games;  // Assuming it's a list of game IDs or descriptions

    public ListGamesResult(boolean success, String message, List<GameData> games) {
        this.success = success;
        this.message = message;
        this.games = games;

    }

    public boolean isSuccess() {

        return success;
    }

    public String getMessage() {

        return message;
    }

    public List<GameData> getGames() {

        return games;
    }
}

