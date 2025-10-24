package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import exception.AlreadyTakenException;
import exception.BadRequestException;
import exception.UnauthorizedException;

import java.util.UUID;

/**
 * Service class responsible for user registration, login, and logout.
 */
public class UserService {
    private final DataAccess dataAccess;

    // --- Inner Records for Request/Response (Can be moved to separate files if needed) ---
    public record RegisterRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResult(String username, String authToken) {}
    // --------------------------------------------------------------------------------------

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * Registers a new user, creates an auth token, and logs them in.
     */
    public AuthResult register(RegisterRequest req)
            throws BadRequestException, AlreadyTakenException, DataAccessException {

        // 1. Validate Request (400 Bad Request)
        if (req.username() == null || req.password() == null || req.email() == null ||
                req.username().isEmpty() || req.password().isEmpty() || req.email().isEmpty()) {
            throw new BadRequestException("bad request");
        }

        // 2. Check for Already Taken (403 Forbidden)
        if (dataAccess.getUser(req.username()) != null) {
            throw new AlreadyTakenException("already taken");
        }

        // 3. Register user
        UserData newUser = new UserData(req.username(), req.password(), req.email());
        dataAccess.createUser(newUser);

        // 4. Create and store initial auth token
        String authToken = UUID.randomUUID().toString();
        AuthData newAuth = new AuthData(authToken, req.username());
        dataAccess.createAuth(newAuth);

        return new AuthResult(req.username(), authToken);
    }

    /**
     * Logs in a user by verifying credentials and generating a new auth token.
     */
    public AuthResult login(LoginRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {

        // 1. Validate Request (400 Bad Request)
        if (req.username() == null || req.password() == null ||
                req.username().isEmpty() || req.password().isEmpty()) {
            throw new BadRequestException("bad request");
        }

        // 2. Check credentials (401 Unauthorized)
        UserData existingUser = dataAccess.getUser(req.username());

        if (existingUser == null || !existingUser.password().equals(req.password())) {
            throw new UnauthorizedException("unauthorized");
        }

        // 3. Successful login: Create and store new AuthToken
        String authToken = UUID.randomUUID().toString();
        AuthData newAuth = new AuthData(authToken, existingUser.username());
        dataAccess.createAuth(newAuth);

        return new AuthResult(existingUser.username(), authToken);
    }

    /**
     * Logs out a user by deleting their auth token.
     */
    public void logout(String authToken)
            throws UnauthorizedException, DataAccessException {

        // 1. Check for token existence (401 Unauthorized)
        if (authToken == null || authToken.isEmpty() || dataAccess.getAuth(authToken) == null) {
            throw new UnauthorizedException("unauthorized");
        }

        // 2. Invalidate (delete) the token
        dataAccess.deleteAuth(authToken);
    }
}