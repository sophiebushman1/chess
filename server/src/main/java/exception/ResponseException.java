package exception;

/**
 * Base class for exceptions that should result in a specific HTTP response.
 * This class carries the HTTP status code (4xx) and the error message.
 * All specific application exceptions (Unauthorized, BadRequest, etc.) must extend this.
 */
public class ResponseException extends Exception {
    private final int statusCode;

    /**
     * Creates a new ResponseException.
     * @param statusCode The HTTP status code to return (e.g., 400, 401, 403).
     * @param message The error message to include in the response.
     */
    public ResponseException(int statusCode, String message) {
        // Calls the parent Exception constructor to set the message
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Public getter method to safely retrieve the HTTP status code.
     * This is what the Server.java handler calls to set the HTTP response status.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
