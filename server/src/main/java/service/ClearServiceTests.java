package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDAO;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the ClearService functionality.
 */
public class ClearServiceTests {

    private DataAccess dataAccess;
    private ClearService clearService;

    @BeforeEach
    public void setup() throws Exception {

        dataAccess = new MemoryDAO();

        clearService = new ClearService(dataAccess);

        // pre-populate test data
        dataAccess.insertUser(new UserData("testuser", "password123", "test@example.com"));
        dataAccess.createAuth("testuser");
        dataAccess.createGame("TestGame");
    }

    /**
     * Positive Test: Ensure the clear method removes all data.
     */
    @Test
    void clearPositive() throws Exception {

        clearService.clear();

        // check if all data is gone, user is null
        assertNull(dataAccess.getUser("testuser"), "User data should be cleared.");

        // Auth token  is null (MemoryDAO's getAuth should be meoty)
        assertNull(dataAccess.getAuth("some-auth-token"),
                "Auth token storage should be cleared.");

        // Game list is empty
        assertEquals(0, dataAccess.listGames().size(), "Game data should be cleared.");

    }
}