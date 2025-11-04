package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOTests {
    private SQLDataAccess database;

    @BeforeEach
    public void setup() throws DataAccessException {
        database = new SQLDataAccess();
        database.clear();
        database.createUser(new UserData("sophia", "pw", "s@e.com"));
    }

    @Test
    public void createAuth_success() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        database.createAuth(auth);
        AuthData found = database.getAuth("token123");
        assertNotNull(found);
        assertEquals("sophia", found.username());
    }

    @Test
    public void createAuth_duplicateToken_fails() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        database.createAuth(auth);
        assertThrows(DataAccessException.class, () -> database.createAuth(auth));
    }

    @Test
    public void getAuth_notFound_returnsNull() throws DataAccessException {
        assertNull(database.getAuth("missingToken"));
    }

    @Test
    public void deleteAuth_success() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        database.createAuth(auth);
        database.deleteAuth("token123");
        assertNull(database.getAuth("token123"));
    }

    @Test
    public void deleteAuth_invalidToken_noError() throws DataAccessException {
        assertDoesNotThrow(() -> database.deleteAuth("doesNotExist"));
    }

    @Test
    public void clear_removesAllAuths() throws DataAccessException {
        database.createAuth(new AuthData("token123", "sophia"));
        database.clear();
        assertNull(database.getAuth("token123"));
    }
}
