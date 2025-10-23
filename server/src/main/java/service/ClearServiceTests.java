package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDAO;
import model.AuthData; // <-- FIX: Added model imports
import model.GameData; // <-- FIX: Added model imports
import model.UserData; // <-- FIX: Added model imports
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Clear Service Tests")
public class ClearServiceTests {

    private DataAccess dataAccess;
    private ClearService clearService;

    @BeforeEach
    void setUp() throws DataAccessException {
        // Initialize MemoryDAO for in-memory testing
        dataAccess = new MemoryDAO();
        clearService = new ClearService(dataAccess);

        // Populate with some data before each test to verify clear() works
        dataAccess.clear(); // Ensure clean start

        // Use model objects directly
        dataAccess.createUser(new UserData("testUser", "password", "test@example.com"));
        dataAccess.createAuth(new AuthData("validToken", "testUser"));
        dataAccess.createGame(new GameData(1, null, null, "Game1", null));
    }

    @Test
    @DisplayName("Positive Clear Test")
    void clearSuccess() {
        assertDoesNotThrow(() -> clearService.clear(), "Clear service should execute without throwing an exception.");

        // Verify that all data has been cleared
        assertAll(
                () -> assertNull(dataAccess.getUser("testUser"), "User data should be cleared."),
                () -> assertNull(dataAccess.getAuth("validToken"), "Auth data should be cleared."),
                () -> assertTrue(dataAccess.listGames().isEmpty(), "Game data should be cleared.")
        );
    }
}