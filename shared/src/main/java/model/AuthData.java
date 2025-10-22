package model;

/**
 * Represents an authentication token and the associated user.
 * @param authToken The unique token used to authenticate requests.
 * @param username The username associated with the token.
 */
public record AuthData(
        String authToken,
        String username) {
}