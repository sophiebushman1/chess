package results;

public class CreateGameResult {
    private boolean success;
    private String message;
    private String gameID; // Changed to String
    public CreateGameResult(boolean success, String message, String gameID) {
        this.success = success;
        this.message = message;
        this.gameID = gameID;

    }

    public boolean isSuccess() {

        return success;
    }

    public String getMessage() {

        return message;
    }

    public String getGameID() {

        return gameID;
    }

}
