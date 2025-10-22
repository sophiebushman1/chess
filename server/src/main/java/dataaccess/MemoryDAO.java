package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of the DataAccess interface for Phase 3.
 */
public class MemoryDAO implements DataAccess {

    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final Map<String, AuthData> authTokens = new ConcurrentHashMap<>();
    private final Map<Integer, GameData> games = new ConcurrentHashMap<>();

    // auto-incrementing game ID counter
    private final AtomicInteger nextGameID = new AtomicInteger(1);


    @Override
    public AuthData createAuth(String username) throws DataAccessException {
        // generate a random token
        String authToken = UUID.randomUUID().toString();

        AuthData authData = new AuthData(authToken, username);
        authTokens.put(authToken, authData);
        return authData;
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        AuthData authData = authTokens.get(authToken);
        if (authData == null) {
            // a failed auth lookup is usually treated as Unauthorized
            // null/missing data as an error.
            return null;
        }
        return authData;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (authTokens.remove(authToken) == null) {
            // if token doesn't exist it's an Unauthorized failure.
            throw new DataAccessException("AuthToken not found");
        }
    }

    @Override
    public void insertUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            // Per Register endpoint specs, this is an 'already taken' failure.
            throw new DataAccessException("User already exists");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        return users.get(username);
    }

    @Override
    public GameData createGame(String gameName) throws DataAccessException {
        int gameID = nextGameID.getAndIncrement();

        // start a new game
        GameData newGame = new GameData(gameID, null, null, gameName, new chess.ChessGame());
        games.put(gameID, newGame);
        return newGame;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID);
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (!games.containsKey(game.gameID())) {
            throw new DataAccessException("Game ID not found for update");
        }
        // Overwrite the old game data with the updated game data
        games.put(game.gameID(), game);
    }


    @Override
    public void clear() throws DataAccessException {
        users.clear();
        authTokens.clear();
        games.clear();
        nextGameID.set(1); // Reset ID count
    }
}