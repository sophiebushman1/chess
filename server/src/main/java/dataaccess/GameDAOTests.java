package dataaccess;

import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;

public class GameDAOTests {
    private SQLDataAccess database;

    @BeforeEach
    public void setup() throws DataAccessException {
        database = new SQLDataAccess();
        database.clear();
        database.createUser(new UserData("white", "pw", "w@e.com"));
        database.createUser(new UserData("black", "pw", "b@e.com"));
    }

    @Test
    public void createGame_success() throws DataAccessException {
        GameData game = new GameData(0, "white", "black", "CoolGame", new chess.ChessGame());
        GameData created = database.createGame(game);
        assertNotNull(created);
        assertTrue(created.gameID() > 0);
    }

    @Test
    public void createGame_missingName_fails() {
        assertThrows(DataAccessException.class, () -> {
            GameData bad = new GameData(0, "white", "black", null, new chess.ChessGame());
            database.createGame(bad);
        });
    }

    @Test
    public void getGame_invalidId_returnsNull() throws DataAccessException {
        assertNull(database.getGame(999));
    }

    @Test
    public void updateGame_success() throws DataAccessException {
        GameData created = database.createGame(new GameData(0, null, null, "MyGame", new chess.ChessGame()));
        GameData updated = new GameData(created.gameID(), "white", null, "MyGame", created.game());
        database.updateGame(updated);
        assertEquals("white", database.getGame(created.gameID()).whiteUsername());
    }

    @Test
    public void updateGame_nonexistent_doesNothing() throws DataAccessException {
        GameData fake = new GameData(999, "white", null, "Nope", new chess.ChessGame());
        assertDoesNotThrow(() -> database.updateGame(fake));
    }

    @Test
    public void listGames_returnsAll() throws DataAccessException {
        database.createGame(new GameData(0, "white", "black", "Game1", new chess.ChessGame()));
        database.createGame(new GameData(0, "white", "black", "Game2", new chess.ChessGame()));
        Collection<GameData> games = database.listGames();
        assertEquals(2, games.size());
    }

    @Test
    public void listGames_empty_returnsEmpty() throws DataAccessException {
        Collection<GameData> games = database.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    public void clear_removesAllGames() throws DataAccessException {
        database.createGame(new GameData(0, "white", "black", "Game1", new chess.ChessGame()));
        database.clear();
        assertTrue(database.listGames().isEmpty());
    }
}
