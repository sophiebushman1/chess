package requests;

public class CreateGameRequest {
    private String gameName;
    private String authToken;
    public CreateGameRequest(String gameName, String authToken) {
        this.gameName = gameName;
        this.authToken = authToken;
    }

    public String getGameName() {

        return gameName;
    }

    public String getAuthToken() {

        return authToken;
    }

    public void setAuthToken(String authToken) {

        this.authToken = authToken;
    }
}

