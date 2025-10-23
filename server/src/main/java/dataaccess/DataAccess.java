package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

public interface DataAccess {

    // --- User DAO ---
    void createUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;

    // --- Auth DAO ---
    AuthData createAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    // --- Game DAO ---
    GameData createGame(GameData game) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    Collection<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException; // Used for joining

    // --- Clear DAO ---
    void clear() throws DataAccessException;
}