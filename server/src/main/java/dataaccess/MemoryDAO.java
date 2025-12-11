package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.*;

public class MemoryDAO implements DataAccess {

    private final Map<String, UserData> users = new HashMap<>();
    private final Map<String, AuthData> authTokens = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();

    private int nextGameID = 1;

    // ---- Users ----
    @Override
    public void createUser(UserData user) {
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) {
        return users.get(username);
    }

    @Override
    public Collection<UserData> getAllUsers() {
        return users.values();
    }

    // ---- Auth ----
    @Override
    public AuthData createAuth(AuthData auth) {
        authTokens.put(auth.authToken(), auth);
        return auth;
    }

    @Override
    public AuthData getAuth(String authToken) {
        return authTokens.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) {
        authTokens.remove(authToken);
    }

    // ---- Games ----
    @Override
    public GameData createGame(GameData game) {
        int id = nextGameID++;
        GameData newGame = new GameData(id,
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                game.game());
        games.put(id, newGame);
        return newGame;
    }

    @Override
    public GameData getGame(int gameID) {
        return games.get(gameID);
    }

    @Override
    public GameData getGameByName(String gameName) {
        for (GameData g : games.values()) {
            if (g.gameName().equalsIgnoreCase(gameName)) {
                return g;
            }
        }
        return null;
    }

    @Override
    public Collection<GameData> listGames() {
        return games.values();
    }

    @Override
    public void updateGame(GameData game) {
        games.put(game.gameID(), game);
    }

    @Override
    public void clear() {
        users.clear();
        authTokens.clear();
        games.clear();
        nextGameID = 1;
    }
}
