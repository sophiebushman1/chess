package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class MemoryDAO implements DataAccess {

    private final Map<String, UserData> userMap = new HashMap<>();
    private final Map<String, AuthData> authMap = new HashMap<>();
    private final Map<Integer, GameData> gameMap = new HashMap<>();
    private final AtomicInteger nextGameID = new AtomicInteger(1);

    /**
     * Clears all stored data (users, auth tokens, games).
     * @throws DataAccessException if there's an issue clearing data (not applicable for memory DAO).
     */
    @Override
    public void clear() throws DataAccessException {
        userMap.clear();
        authMap.clear();
        gameMap.clear();
        nextGameID.set(1); // Reset the game ID counter
    }

    @Override
    public void createUser(UserData userData) throws DataAccessException {
        if (userMap.containsKey(userData.username())) {
            throw new DataAccessException("Error: already taken");
        }
        userMap.put(userData.username(), userData);
    }


    @Override
    public UserData getUser(String username) {
        return userMap.get(username);
    }


    @Override
    public AuthData createAuth(AuthData authData) {
        authMap.put(authData.authToken(), authData);
        return authData; // FIX: Changed return type from void to AuthData to match interface
    }

    @Override
    public AuthData getAuth(String authToken) {
        return authMap.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) {
        authMap.remove(authToken);
    }


    @Override
    public GameData createGame(GameData gameData) {
        int gameID = nextGameID.getAndIncrement();
        // Create a new GameData with the generated ID and a new ChessGame
        GameData newGame = new GameData(
                gameID,
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                new ChessGame() // Ensure a fresh game state
        );
        gameMap.put(gameID, newGame);
        return newGame;
    }


    @Override
    public GameData getGame(int gameID) {
        return gameMap.get(gameID);
    }


    @Override
    public Collection<GameData> listGames() {
        // Returns a copy of the values in the map
        return gameMap.values();
    }


    @Override
    public void updateGame(GameData updatedGame) throws DataAccessException {
        // use the game ID to put the new object
        // into the map, replacing the old one.
        if (!gameMap.containsKey(updatedGame.gameID())) {
            throw new DataAccessException("Error: Game not found for update.");
        }
        gameMap.put(updatedGame.gameID(), updatedGame);
    }
}
