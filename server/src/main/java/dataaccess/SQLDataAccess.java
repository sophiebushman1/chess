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
import java.util.List;

public class SQLDataAccess implements DataAccess {
    private final Gson gson = new Gson();

    public SQLDataAccess() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection()) {
            createTables(conn);
        } catch (Exception e) {
            throw new DataAccessException("Unable to initialize database", e);
        }
    }

    // ------------------- Table Creation -------------------
    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Users table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY NOT NULL,
                    hashed_password VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Auth tokens table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth_tokens (
                    auth_token_id INT AUTO_INCREMENT PRIMARY KEY,
                    auth_token VARCHAR(100) NOT NULL UNIQUE,
                    username VARCHAR(50),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                );
            """);

            // Games table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS games (
                    game_id INT AUTO_INCREMENT PRIMARY KEY,
                    game_name VARCHAR(100) NOT NULL,
                    white_player_id VARCHAR(50),
                    black_player_id VARCHAR(50),
                    game_state JSON,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (white_player_id) REFERENCES users(username) ON DELETE SET NULL,
                    FOREIGN KEY (black_player_id) REFERENCES users(username) ON DELETE SET NULL
                );
            """);
        }
    }

    // User
    @Override
    public void createUser(UserData user) throws DataAccessException {
        String hashed = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        String sql = "INSERT INTO users (username, hashed_password, email) VALUES (?, ?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, hashed);
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to create user", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, hashed_password, email FROM users WHERE username=?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("hashed_password"),
                            rs.getString("email")
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get user", e);
        }
    }

    public List<UserData> getAllUsers() throws DataAccessException {
        String sql = "SELECT username, hashed_password, email FROM users;";
        List<UserData> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(new UserData(
                        rs.getString("username"),
                        rs.getString("hashed_password"),
                        rs.getString("email")
                ));
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to list users", e);
        }
    }

    // Auth
    @Override
    public AuthData createAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO auth_tokens (auth_token, username) VALUES (?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();
            return auth;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to create auth token", e);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT auth_token, username FROM auth_tokens WHERE auth_token=?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("auth_token"),
                            rs.getString("username")
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get auth token", e);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth_tokens WHERE auth_token=?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to delete auth token", e);
        }
    }

    // Game
    @Override
    public GameData createGame(GameData game) throws DataAccessException {
        String sql = "INSERT INTO games (game_name, white_player_id, black_player_id, game_state) VALUES (?, ?, ?, ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, game.gameName());
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            stmt.setString(4, gson.toJson(game.game()));
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int gameID = keys.getInt(1);
                    return new GameData(gameID, game.whiteUsername(), game.blackUsername(), game.gameName(), game.game());
                } else {
                    throw new DataAccessException("Game creation failed, no ID returned");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Unable to create game", e);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT * FROM games WHERE game_id=?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChessGame game = gson.fromJson(rs.getString("game_state"), ChessGame.class);
                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_player_id"),
                            rs.getString("black_player_id"),
                            rs.getString("game_name"),
                            game
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get game", e);
        }
    }

    public GameData getGameByName(String gameName) throws DataAccessException {
        String sql = "SELECT * FROM games WHERE game_name=?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, gameName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChessGame game = gson.fromJson(rs.getString("game_state"), ChessGame.class);
                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_player_id"),
                            rs.getString("black_player_id"),
                            rs.getString("game_name"),
                            game
                    );
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Unable to get game by name", e);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        String sql = "SELECT * FROM games;";
        List<GameData> games = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ChessGame game = gson.fromJson(rs.getString("game_state"), ChessGame.class);
                games.add(new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_player_id"),
                        rs.getString("black_player_id"),
                        rs.getString("game_name"),
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
        String sql = "UPDATE games SET white_player_id=?, black_player_id=?, game_name=?, game_state=? WHERE game_id=?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gson.toJson(game.game()));
            stmt.setInt(5, game.gameID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Unable to update game", e);
        }
    }

    // Clear
    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auth_tokens;");
            stmt.executeUpdate("DELETE FROM games;");
            stmt.executeUpdate("DELETE FROM users;");
        } catch (SQLException e) {
            throw new DataAccessException("Unable to clear database", e);
        }
    }
}
