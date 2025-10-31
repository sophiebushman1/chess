package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTests {
    private SQLDataAccess db;

    @BeforeEach
    public void setup() throws DataAccessException {
        db = new SQLDataAccess();
        db.clear();
    }

    @Test
    public void createUser_success() throws DataAccessException {
        UserData user = new UserData("sophia", "pass123", "soph@example.com");
        db.createUser(user);
        UserData found = db.getUser("sophia");
        assertNotNull(found);
        assertEquals("sophia", found.username());
    }

    @Test
    public void createUser_duplicateUsername_fails() throws DataAccessException {
        UserData user = new UserData("sophia", "pass123", "soph@example.com");
        db.createUser(user);
        assertThrows(DataAccessException.class, () -> db.createUser(user));
    }

    @Test
    public void createUser_nullUsername_fails() {
        assertThrows(DataAccessException.class, () -> {
            db.createUser(new UserData(null, "pw", "email"));
        });
    }

    @Test
    public void getUser_notFound_returnsNull() throws DataAccessException {
        assertNull(db.getUser("missing"));
    }
}
