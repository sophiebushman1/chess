package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOTests {
    private SQLDataAccess db;

    @BeforeEach
    public void setup() throws DataAccessException {
        db = new SQLDataAccess();
        db.clear();
        db.createUser(new UserData("sophia", "pw", "s@e.com"));
    }

    @Test
    public void createAuth_success() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        db.createAuth(auth);
        AuthData found = db.getAuth("token123");
        assertNotNull(found);
        assertEquals("sophia", found.username());
    }

    @Test
    public void createAuth_duplicateToken_fails() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        db.createAuth(auth);
        assertThrows(DataAccessException.class, () -> db.createAuth(auth));
    }

    @Test
    public void getAuth_notFound_returnsNull() throws DataAccessException {
        assertNull(db.getAuth("missingToken"));
    }

    @Test
    public void deleteAuth_success() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        db.createAuth(auth);
        db.deleteAuth("token123");
        assertNull(db.getAuth("token123"));
    }

    @Test
    public void deleteAuth_invalidToken_noError() throws DataAccessException {
        assertDoesNotThrow(() -> db.deleteAuth("doesNotExist"));
    }
}
