package results;

public class LoginResult {
    private boolean success;
    private String username;
    private String authToken;
    private String message;


    public LoginResult(boolean success, String username, String authToken, String message) {
        this.success = success;
        this.username = username;
        this.authToken = authToken;
        this.message = message;
    }

    public boolean isSuccess() {

        return success;
    }

    public String getUsername() {

        return username;
    }

    public String getAuthToken() {

        return authToken;
    }

    public String getMessage() {

        return message;
    }
}

