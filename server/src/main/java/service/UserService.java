package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import service.ServiceExceptions.AlreadyTakenException;
import service.ServiceExceptions.BadRequestException;
import service.ServiceExceptions.UnauthorizedException;

import java.util.UUID;

public class UserService {

    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthResult register(RegisterRequest req) throws BadRequestException, AlreadyTakenException, DataAccessException {
        if (req.username() == null || req.username().isEmpty() ||
                req.password() == null || req.password().isEmpty() ||
                req.email() == null || req.email().isEmpty()) {
            throw new BadRequestException("Missing required fields.");
        }

        if (dataAccess.getUser(req.username()) != null) {
            throw new AlreadyTakenException("Username already taken.");
        }

        UserData newUser = new UserData(req.username(), req.password(), req.email());
        dataAccess.createUser(newUser);

        String authToken = UUID.randomUUID().toString();
        AuthData authData = new AuthData(authToken, req.username());
        dataAccess.createAuth(authData);

        return new AuthResult(req.username(), authToken);
    }

    public AuthResult login(LoginRequest req) throws BadRequestException, UnauthorizedException, DataAccessException {
        if (req.username() == null || req.username().isEmpty() ||
                req.password() == null || req.password().isEmpty()) {
            throw new BadRequestException("Missing required fields.");
        }

        UserData user = dataAccess.getUser(req.username());

        if (user == null || !user.password().equals(req.password())) {
            throw new UnauthorizedException("Invalid username or password.");
        }

        String authToken = UUID.randomUUID().toString();
        AuthData authData = new AuthData(authToken, req.username());
        dataAccess.createAuth(authData);

        return new AuthResult(req.username(), authToken);
    }

    public void logout(String authToken) throws UnauthorizedException, DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Invalid authToken.");
        }

        dataAccess.deleteAuth(authToken);
    }

    // <--- ADD THIS METHOD ---
    public void clear() throws DataAccessException {
        dataAccess.clear();
    }
}
