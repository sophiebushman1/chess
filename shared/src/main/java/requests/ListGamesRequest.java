package requests;

public class ListGamesRequest {
    private String authToken;

    public ListGamesRequest(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {

        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}

