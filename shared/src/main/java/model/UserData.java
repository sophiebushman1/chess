package model;

/**
 * Represents a user's data: username, password (hashed), and email.
 * @param username The user's unique identifier.
 * @param password The user's password.
 * @param email The user's email address.
 */
public record UserData(
        String username,
        String password,
        String email) {
}