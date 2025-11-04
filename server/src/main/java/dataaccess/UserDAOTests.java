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
    public void createUser_success() throws DataAccessException {
        UserData user = new UserData("sophia", "pass123", "soph@example.com");
        database.createUser(user);
        UserData found = database.getUser("sophia");
        assertNotNull(found);
        assertEquals("sophia", found.username());
    }

    @Test
    public void createUser_duplicateUsername_fails() throws DataAccessException {
        UserData user = new UserData("sophia", "pass123", "soph@example.com");
        database.createUser(user);
        assertThrows(DataAccessException.class, () -> database.createUser(user));
    }

    @Test
    public void createUser_nullUsername_fails() {
        assertThrows(DataAccessException.class, () -> {
            database.createUser(new UserData(null, "pw", "email"));
        });
    }

    @Test
    public void getUser_notFound_returnsNull() throws DataAccessException {
        assertNull(database.getUser("missing"));
    }

    @Test
    public void clear_removesAllUsers() throws DataAccessException {
        database.createUser(new UserData("sophia", "pw", "s@e.com"));
        database.clear();
        assertNull(database.getUser("sophia"));
    }
}
