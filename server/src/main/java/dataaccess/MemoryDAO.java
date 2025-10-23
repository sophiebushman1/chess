package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessGame;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryDAO implements DataAccess {
    private final Map<String, UserData> users = new ConcurrentHashMap<>();
    private final Map<String, AuthData> auths = new ConcurrentHashMap<>();
    private final Map<Integer, GameData> games = new ConcurrentHashMap<>();
    private final AtomicInteger nextGameID = new AtomicInteger(1000); // Start IDs at 1000

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("Error: already taken");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        UserData user = users.get(username);
        if (user == null) {
            return null; // Return null if not found
        }
        return user;
    }

    @Override
    public AuthData createAuth(AuthData auth) throws DataAccessException {
        auths.put(auth.authToken(), auth);
        return auth;
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return auths.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        auths.remove(authToken);
    }

    @Override
    public GameData createGame(GameData game) throws DataAccessException {
        int newID = nextGameID.getAndIncrement();
        // Creates a new GameData with the assigned ID
        GameData newGame = new GameData(
                newID,
                null,
                null,
                game.gameName(),
                new ChessGame() // New game starts with a new ChessGame object
        );
        games.put(newID, newGame);
        return newGame;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID);
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        return games.values();
    }

    @Override
    public void updateGame(GameData updatedGame) throws DataAccessException {
        if (!games.containsKey(updatedGame.gameID())) {
            throw new DataAccessException("Error: Game not found");
        }
        games.put(updatedGame.gameID(), updatedGame);
    }

    @Override
    public void clear() throws DataAccessException {
        users.clear();
        auths.clear();
        games.clear();
        nextGameID.set(1000);
    }
}