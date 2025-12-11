package requests;
public class JoinGameRequest {
    private String playerColor;
    private String gameID;
    private String authToken;

    public JoinGameRequest(String playerColor, String gameID, String authToken) {
        this.playerColor = playerColor;
        this.gameID = gameID;
        this.authToken = authToken;
    }

    public String getPlayerColor() {

        return playerColor;
    }

    public String getGameID() {

        return gameID;
    }

    public String getAuthToken() {

        return authToken;
    }

    public void setAuthToken(String authToken) {

        this.authToken = authToken;
    }

}

