package dataaccess;

import model.GameData;
import chess.ChessGame;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameDAOTests {
    private SQLDataAccess db;

    @BeforeEach
    public void setup() throws DataAccessException {
        db = new SQLDataAccess();
        db.clear();
    }



    @Test
    public void getGame_invalidId_returnsNull() throws DataAccessException {
        assertNull(db.getGame(999));
    }

    @Test
    public void createGame_success() throws DataAccessException {
        db.createUser(new model.UserData("white", "pw", "w@e.com"));
        db.createUser(new model.UserData("black", "pw", "b@e.com"));

        GameData game = new GameData(0, "white", "black", "CoolGame", new chess.ChessGame());
        GameData created = db.createGame(game);

        assertNotNull(created);
        assertTrue(created.gameID() > 0);

        GameData found = db.getGame(created.gameID());
        assertNotNull(found);
        assertEquals("CoolGame", found.gameName());
    }

    @Test
    public void updateGame_success() throws DataAccessException {
        // ✅ Add user so FK check passes
        db.createUser(new model.UserData("whiteUser", "pw", "w@e.com"));

        GameData game = new GameData(0, null, null, "MyGame", new chess.ChessGame());
        GameData created = db.createGame(game);

        GameData updated = new GameData(created.gameID(), "whiteUser", null, "MyGame", created.game());
        db.updateGame(updated);

        GameData found = db.getGame(created.gameID());
        assertEquals("whiteUser", found.whiteUsername());
    }

}
