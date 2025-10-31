//document the schema so I have my design in mind:

//CREATE TABLE users (
//        username VARCHAR(255) PRIMARY KEY,
//password VARCHAR(255) NOT NULL,
//email VARCHAR(255) NOT NULL
//);
//
//CREATE TABLE auths (
//        token VARCHAR(255) PRIMARY KEY,
//username VARCHAR(255) NOT NULL,
//FOREIGN KEY (username) REFERENCES users(username)
//        );
//
//CREATE TABLE games (
//        gameID INT AUTO_INCREMENT PRIMARY KEY,
//        whiteUsername VARCHAR(255),
//blackUsername VARCHAR(255),
//gameName VARCHAR(255) NOT NULL,
//gameState TEXT NOT NULL,
//FOREIGN KEY (whiteUsername) REFERENCES users(username),
//FOREIGN KEY (blackUsername) REFERENCES users(username)
//        );

//getConnection()
//run queries
//steralization
//hashing passwords

package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SQLDataAccess implements DataAccess {
    private final Gson gson = new Gson();

    public SQLDataAccess() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            createTables(conn);
        } catch (Exception e) {
            throw new DataAccessException("Unable to initialize database", e);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS user (
                    username VARCHAR(255) NOT NULL PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth (
                    authToken VARCHAR(255) NOT NULL PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    FOREIGN KEY (username) REFERENCES user(username) ON DELETE CASCADE
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS game (
                    gameID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255) NOT NULL,
                    gameJSON TEXT NOT NULL,
                    FOREIGN KEY (whiteUsername) REFERENCES user(username) ON DELETE SET NULL,
                    FOREIGN KEY (blackUsername) REFERENCES user(username) ON DELETE SET NULL
                )
            """);
        }
    }
    //override functions: user, auth, game, clear
    // 1: user functions

    @Override
    public void createUser(UserData user) throws DataAccessException {
        String hashed = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO user (username, password, email) VALUES (?, ?, ?)")) {
            stmt.setString(1, user.username());
            stmt.setString(2, hashed);
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        }
            catch (SQLException e) {
            throw new DataAccessException("Unable to create user", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT username, password, email FROM user WHERE username=?")) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));

                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get user", e);
        }
    }
    // Auth

    @Override
    public AuthData createAuth(AuthData auth) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("INSERT INTO auth (authToken, username) VALUES (?, ?)")) {
            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();
            return auth;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to create auth", e);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT authToken, username FROM auth WHERE authToken=?")) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(rs.getString("authToken"), rs.getString("username"));
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get auth", e);
        }
    }



    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth WHERE authToken=?")) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to delete auth", e);
        }
    }

    // Game
    @Override
    public GameData createGame(GameData game) throws DataAccessException {
        String gameJSON = gson.toJson(game.game());
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT INTO game (whiteUsername, blackUsername, gameName, gameJSON) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJSON);
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int gameID = rs.getInt(1);
                    return new GameData(gameID, game.whiteUsername(), game.blackUsername(), game.gameName(), game.game());
                }
            }
            throw new DataAccessException("Game creation failed, no ID returned");
        } catch (SQLException e) {
            throw new DataAccessException("Unable to create game", e);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM game WHERE gameID=?")) {
            stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChessGame game = gson.fromJson(rs.getString("gameJSON"), ChessGame.class);
                    return new GameData(
                            rs.getInt("gameID"),
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            game
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get game", e);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        Collection<GameData> games = new ArrayList<>();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM game");
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                ChessGame game = gson.fromJson(rs.getString("gameJSON"), ChessGame.class);
                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        game
                ));
            }
            return games;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to list games", e);
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        String gameJSON = gson.toJson(game.game());
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(
                     "UPDATE game SET whiteUsername=?, blackUsername=?, gameName=?, gameJSON=? WHERE gameID=?")) {
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJSON);
            stmt.setInt(5, game.gameID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to update game", e);
        }
    }

    // Clear

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auth");
            stmt.executeUpdate("DELETE FROM game");
            stmt.executeUpdate("DELETE FROM user");
        } catch (SQLException e) {
            throw new DataAccessException("Unable to clear database", e);
        }
    }

}
