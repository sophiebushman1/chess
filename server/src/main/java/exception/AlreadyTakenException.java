package exception;

/**
 * Thrown when a unique resource (like a username or a spot in a game)
 * is already taken (maps to 403 Forbidden).
 */
public class AlreadyTakenException extends ResponseException {

    // Constructor requiring a specific message
    public AlreadyTakenException(String message) {
        // Use 403 Forbidden for conflicts like resource already taken
        super(403, "Error: " + message);
    }

    // Default constructor for simple throws (Fixes 'Expected 1 argument' error)
    public AlreadyTakenException() {
        this("already taken");
    }
}