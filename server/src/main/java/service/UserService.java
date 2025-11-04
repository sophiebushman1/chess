package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import exception.AlreadyTakenException;
import exception.BadRequestException;
import exception.UnauthorizedException;
import org.mindrot.jbcrypt.BCrypt;
import java.util.UUID;

public class UserService {
    private final DataAccess dataAccess;

    public record RegisterRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResult(String username, String authToken) {}

    public UserService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public AuthResult register(RegisterRequest req)
            throws BadRequestException, AlreadyTakenException, DataAccessException {
        requireNonEmpty(req.username(), req.password(), req.email());

        if (dataAccess.getUser(req.username()) != null) {
            throw new AlreadyTakenException("already taken");
        }

        String hashedPassword = BCrypt.hashpw(req.password(), BCrypt.gensalt());
        dataAccess.createUser(new UserData(req.username(), hashedPassword, req.email()));

        String token = UUID.randomUUID().toString();
        dataAccess.createAuth(new AuthData(token, req.username()));

        return new AuthResult(req.username(), token);
    }

    public AuthResult login(LoginRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {
        requireNonEmpty(req.username(), req.password());

        UserData user = dataAccess.getUser(req.username());
        if (user == null || !BCrypt.checkpw(req.password(), user.password())) {
            throw new UnauthorizedException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        dataAccess.createAuth(new AuthData(token, user.username()));

        return new AuthResult(user.username(), token);
    }

    public void logout(String token)
            throws UnauthorizedException, DataAccessException {
        requireValidAuth(token);
        dataAccess.deleteAuth(token);
    }

    private void requireNonEmpty(String... fields) throws BadRequestException {
        for (String f : fields) {
            if (f == null || f.isEmpty()) {
                throw new BadRequestException("bad request");
            }
        }
    }

    private void requireValidAuth(String token)
            throws UnauthorizedException, DataAccessException {
        if (token == null || token.isEmpty() || dataAccess.getAuth(token) == null) {
            throw new UnauthorizedException("unauthorized");
        }
    }
}
