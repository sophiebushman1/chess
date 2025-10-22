package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;
import java.util.List;

/**
 * Interface defining the required database operations for the Chess server.
 * All methods throw DataAccessException on failure.
 */
public interface DataAccess {


    AuthData createAuth(String username) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    void insertUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;


    GameData createGame(String gameName) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    List<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;


    void clear() throws DataAccessException;
}