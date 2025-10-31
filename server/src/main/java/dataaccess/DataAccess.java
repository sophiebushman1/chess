package dataaccess;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.Collection;

public interface DataAccess {

    //user
    void createUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;

    //auth
    AuthData createAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

  //gamedata
    GameData createGame(GameData game) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    Collection<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException; // Used for joining

    void clear() throws DataAccessException;
}