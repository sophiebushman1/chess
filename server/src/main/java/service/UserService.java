package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

public class UserService {
    private final DataAccess dataAccess;

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthResult register(RegisterRequest req) throws DataAccessException, AlreadyTakenException, BadRequestException {
        // in input valid?
        if (req.username() == null || req.password() == null || req.email() == null ||
                req.username().isEmpty() || req.password().isEmpty()) {
            throw new BadRequestException("Missing required fields");
        }

        // already exist?
        if (dataAccess.getUser(req.username()) != null) {
            throw new AlreadyTakenException("username already taken");
        }

        // insert user
        UserData newUser = new UserData(req.username(), req.password(), req.email());
        dataAccess.insertUser(newUser);

        // make auth token
        AuthData authData = dataAccess.createAuth(req.username());

        return new AuthResult(authData.username(), authData.authToken());
    }

    public AuthResult login(LoginRequest req) throws DataAccessException, UnauthorizedException, BadRequestException {
        // is input valid?
        if (req.username() == null || req.password() == null) {
            throw new BadRequestException("Missing required fields");
        }

        // get user
        UserData user = dataAccess.getUser(req.username());

        // check password
        if (user == null || !user.password().equals(req.password())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        // make auth token
        AuthData authData = dataAccess.createAuth(req.username());

        return new AuthResult(authData.username(), authData.authToken());
    }



    public void logout(String authToken) throws DataAccessException, UnauthorizedException {
        // not found error
        if (dataAccess.getAuth(authToken) == null) {
            throw new UnauthorizedException("AuthToken not found");
        }

        // delete token
        dataAccess.deleteAuth(authToken);
    }

}