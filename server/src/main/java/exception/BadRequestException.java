package exception;

/**
 * Corresponds to HTTP 400 Bad Request.
 * Thrown when required input is missing or malformed.
 */
public class BadRequestException extends ResponseException {
    public BadRequestException(String message) {
        // Use 400 Bad Request for missing required fields or invalid input
        super(400, "Error: " + message);
    }

    // Default constructor for simple throws
    public BadRequestException() {
        this("bad request");
    }
}