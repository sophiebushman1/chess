package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of the DataAccess interface.
 * Stores all data (users, auth, games) in Maps.
 */
public class MemoryDAO implements DataAccess {

    // Maps to store application data
    private final Map<String, UserData> userMap = new HashMap<>();
    private final Map<String, AuthData> authMap = new HashMap<>();
    // Use a sequential ID generator for games
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

    // --- UserData Methods ---

    /**
     * Creates a new user.
     * @param userData The user data to store.
     * @throws DataAccessException if a user with that username already exists.
     */
    @Override
    public void createUser(UserData userData) throws DataAccessException {
        if (userMap.containsKey(userData.username())) {
            throw new DataAccessException("Error: already taken");
        }
        userMap.put(userData.username(), userData);
    }

    /**
     * Retrieves a user by username.
     * @param username The username to look up.
     * @return The UserData object, or null if not found.
     */
    @Override
    public UserData getUser(String username) {
        return userMap.get(username);
    }

    // --- AuthData Methods ---

    /**
     * Creates a new authentication token record.
     * @param authData The AuthData to store.
     * @return The AuthData object that was stored.
     */
    @Override
    public AuthData createAuth(AuthData authData) {
        authMap.put(authData.authToken(), authData);
        return authData; // FIX: Changed return type from void to AuthData to match interface
    }

    /**
     * Retrieves an AuthData object by token.
     * @param authToken The token to look up.
     * @return The AuthData object, or null if not found.
     */
    @Override
    public AuthData getAuth(String authToken) {
        return authMap.get(authToken);
    }

    /**
     * Deletes an authentication token record.
     * @param authToken The token to remove.
     */
    @Override
    public void deleteAuth(String authToken) {
        authMap.remove(authToken);
    }

    // --- GameData Methods ---

    /**
     * Creates a new game and assigns a unique ID.
     * @param gameData The GameData template (ID is ignored and overwritten).
     * @return The newly created GameData with the assigned ID.
     */
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

    /**
     * Retrieves a game by ID.
     * @param gameID The ID of the game to retrieve.
     * @return The GameData object, or null if not found.
     */
    @Override
    public GameData getGame(int gameID) {
        return gameMap.get(gameID);
    }

    /**
     * Retrieves all games currently stored.
     * @return A collection of all GameData objects.
     */
    @Override
    public Collection<GameData> listGames() {
        // Returns a copy of the values in the map
        return gameMap.values();
    }

    /**
     * Updates an existing game with new data (used for joining/leaving a game).
     * The game is identified by the gameID within the provided GameData object.
     * @param updatedGame The new GameData to replace the old one.
     * @throws DataAccessException if the game ID does not exist.
     */
    @Override
    public void updateGame(GameData updatedGame) throws DataAccessException {
        // This is the critical fix: we must use the game ID to put the new object
        // into the map, replacing the old one.
        if (!gameMap.containsKey(updatedGame.gameID())) {
            throw new DataAccessException("Error: Game not found for update.");
        }
        gameMap.put(updatedGame.gameID(), updatedGame);
    }
}
