package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTests {
    private SQLDataAccess database;

    @BeforeEach
    public void setup() throws DataAccessException {
        database = new SQLDataAccess();
        database.clear();
    }

    @Test
    public void createUserSuccess() throws DataAccessException {
        UserData user = new UserData("sophia", "pass123", "soph@example.com");
        database.createUser(user);
        UserData found = database.getUser("sophia");
        assertNotNull(found);
        assertEquals("sophia", found.username());
    }

    @Test
    public void createUserDuplicateUsernameFails() throws DataAccessException {
        UserData user = new UserData("sophia", "pass123", "soph@example.com");
        database.createUser(user);
        assertThrows(DataAccessException.class, () -> database.createUser(user));
    }

    @Test
    public void createUserNullUsernameFails() {
        assertThrows(DataAccessException.class, () -> {
            database.createUser(new UserData(null, "pw", "email"));
        });
    }

    @Test
    public void getUserNotFoundReturnsNull() throws DataAccessException {
        assertNull(database.getUser("missing"));
    }

    @Test
    public void clearRemovesAllUsers() throws DataAccessException {
        database.createUser(new UserData("sophia", "pw", "s@e.com"));
        database.clear();
        assertNull(database.getUser("sophia"));
    }
}
