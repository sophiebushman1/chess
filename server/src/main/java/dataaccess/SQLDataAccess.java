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

}
