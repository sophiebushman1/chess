package service;

/**
 * Base class for exceptions that should result in a specific HTTP response.
 * All custom exceptions used by the Service layer should extend this.
 */
public class ServiceExceptions extends Exception {
    private final int statusCode;

    public ServiceExceptions(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    // Maps to HTTP 400 Bad Request
    public static class BadRequestException extends ServiceExceptions {
        public BadRequestException() {
            super(400, "Error: bad request");
        }

        public BadRequestException(String message) {
            super(400, "Error: " + message);
        }
    }

    // Maps to HTTP 401 Unauthorized
    public static class UnauthorizedException extends ServiceExceptions {
        public UnauthorizedException() {
            super(401, "Error: unauthorized");
        }

        public UnauthorizedException(String message) {
            super(401, "Error: " + message);
        }
    }

    // Maps to HTTP 403 Forbidden
    public static class AlreadyTakenException extends ServiceExceptions {
        public AlreadyTakenException() {
            super(403, "Error: already taken");
        }

        public AlreadyTakenException(String message) {
            super(403, "Error: " + message);
        }
    }
}
