package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

public interface DataAccess {

    // ---- Users ----
    void createUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    Collection<UserData> getAllUsers() throws DataAccessException;

    // ---- Auth ----
    AuthData createAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    // ---- Games ----
    GameData createGame(GameData game) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    GameData getGameByName(String gameName) throws DataAccessException;
    Collection<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;

    // ---- Utils ----
    void clear() throws DataAccessException;
}
