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


    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }


    public AuthResult register(RegisterRequest req)
            throws BadRequestException, AlreadyTakenException, DataAccessException {


        if (req.username() == null || req.password() == null || req.email() == null ||
                req.username().isEmpty() || req.password().isEmpty() || req.email().isEmpty()) {
            throw new BadRequestException("bad request");
        }

        // 403
        if (dataAccess.getUser(req.username()) != null) {
            throw new AlreadyTakenException("already taken");
        }

        // register user
        UserData newUser = new UserData(req.username(), req.password(), req.email());
        dataAccess.createUser(newUser);

        String authToken = UUID.randomUUID().toString();
        AuthData newAuth = new AuthData(authToken, req.username());
        dataAccess.createAuth(newAuth);

        return new AuthResult(req.username(), authToken);
    }


    public AuthResult login(LoginRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {

        if (req.username() == null || req.password() == null ||
                req.username().isEmpty() || req.password().isEmpty()) {
            throw new BadRequestException("bad request");
        }

        UserData existingUser = dataAccess.getUser(req.username());

        if (existingUser == null || !existingUser.password().equals(req.password())) {
            throw new UnauthorizedException("unauthorized");
        }

        String authToken = UUID.randomUUID().toString();
        AuthData newAuth = new AuthData(authToken, existingUser.username());
        dataAccess.createAuth(newAuth);

        return new AuthResult(existingUser.username(), authToken);
    }

    public void logout(String authToken)
            throws UnauthorizedException, DataAccessException {

        if (authToken == null || authToken.isEmpty() || dataAccess.getAuth(authToken) == null) {
            throw new UnauthorizedException("unauthorized");
        }

        dataAccess.deleteAuth(authToken);
    }
}