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
    public void createAuthSuccess() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        database.createAuth(auth);
        AuthData found = database.getAuth("token123");
        assertNotNull(found);
        assertEquals("sophia", found.username());
    }

    @Test
    public void createAuthDuplicateTokenFails() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        database.createAuth(auth);
        assertThrows(DataAccessException.class, () -> database.createAuth(auth));
    }

    @Test
    public void getAuthNotFoundReturnsNull() throws DataAccessException {
        assertNull(database.getAuth("missingToken"));
    }

    @Test
    public void deleteAuthSuccess() throws DataAccessException {
        AuthData auth = new AuthData("token123", "sophia");
        database.createAuth(auth);
        database.deleteAuth("token123");
        assertNull(database.getAuth("token123"));
    }

    @Test
    public void deleteAuthInvalidTokenNoError() throws DataAccessException {
        assertDoesNotThrow(() -> database.deleteAuth("doesNotExist"));
    }

    @Test
    public void clearRemovesAllAuths() throws DataAccessException {
        database.createAuth(new AuthData("token123", "sophia"));
        database.clear();
        assertNull(database.getAuth("token123"));
    }
}
