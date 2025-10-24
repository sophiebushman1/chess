package exception;

/**
 * Thrown when authentication is required or the token is invalid (maps to 401 Unauthorized).
 */
public class UnauthorizedException extends ResponseException {
    public UnauthorizedException(String message) {
        // Use 401 Unauthorized for bad auth tokens or unauthorized access
        super(401, "Error: " + message);
    }

    // Default constructor for simple throws (Fixes 'Expected 1 argument' error)
    public UnauthorizedException() {
        this("unauthorized");
    }
}