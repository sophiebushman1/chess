package web;

/**
 * Represents the structure of a JSON error response returned to the client.
 * e.g., { "message": "Error: Game name already taken." }
 */
public record ErrorResponse(String message) {
}