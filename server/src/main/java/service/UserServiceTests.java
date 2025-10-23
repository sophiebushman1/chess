package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDAO;
import model.UserData; // <-- FIX: Added model import
import model.AuthData; // <-- FIX: Added model import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.ServiceExceptions.AlreadyTakenException;
import service.ServiceExceptions.BadRequestException;
import service.ServiceExceptions.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Service Tests")
public class UserServiceTests {

    private DataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    void setUp() throws DataAccessException {
        // Use a fresh in-memory DAO for each test
        dataAccess = new MemoryDAO();
        userService = new UserService(dataAccess);
        dataAccess.clear();

        // Pre-register a user for login/logout tests
        dataAccess.createUser(new UserData("ExistingUser", "pass123", "exist@mail.com"));
    }

    // --- Register Tests ---

    @Test
    @DisplayName("Positive Register Test")
    void registerSuccess() {
        RegisterRequest req = new RegisterRequest("newUser", "newPass", "new@mail.com");
        assertDoesNotThrow(() -> {
            AuthResult result = userService.register(req);
            assertNotNull(result.authToken(), "Auth token should be generated.");
            assertEquals("newUser", result.username(), "Username should match request.");
        }, "Registration should succeed for a new user.");

        // Verify user was actually created
        assertDoesNotThrow(() -> assertNotNull(dataAccess.getUser("newUser"), "User should be stored in DAO."));
    }

    @Test
    @DisplayName("Negative Register (Already Taken) Test")
    void registerFailureAlreadyTaken() {
        RegisterRequest req = new RegisterRequest("ExistingUser", "pass456", "test@mail.com");
        assertThrows(AlreadyTakenException.class, () -> userService.register(req),
                "Should throw AlreadyTakenException if username exists.");
    }

    @Test
    @DisplayName("Negative Register (Bad Request) Test")
    void registerFailureBadRequest() {
        RegisterRequest req = new RegisterRequest("user", null, "test@mail.com");
        assertThrows(BadRequestException.class, () -> userService.register(req),
                "Should throw BadRequestException if required fields are missing.");
    }

    // --- Login Tests ---

    @Test
    @DisplayName("Positive Login Test")
    void loginSuccess() {
        LoginRequest req = new LoginRequest("ExistingUser", "pass123");
        assertDoesNotThrow(() -> {
            AuthResult result = userService.login(req);
            assertNotNull(result.authToken(), "A new auth token should be created on successful login.");
            assertEquals("ExistingUser", result.username(), "Username should match.");
        }, "Login should succeed with correct credentials.");
    }

    @Test
    @DisplayName("Negative Login (Unauthorized) Test")
    void loginFailureUnauthorized() {
        LoginRequest req = new LoginRequest("ExistingUser", "wrongPass");
        assertThrows(UnauthorizedException.class, () -> userService.login(req),
                "Should throw UnauthorizedException for bad password.");

        LoginRequest req2 = new LoginRequest("nonExistentUser", "anyPass");
        assertThrows(UnauthorizedException.class, () -> userService.login(req2),
                "Should throw UnauthorizedException for non-existent user.");
    }

    // --- Logout Tests ---

    @Test
    @DisplayName("Positive Logout Test")
    void logoutSuccess() throws DataAccessException {
        // Create an active session
        dataAccess.createAuth(new AuthData("logoutToken", "ExistingUser"));

        assertDoesNotThrow(() -> userService.logout("logoutToken"), "Logout should succeed for a valid token.");

        // Verify token was deleted
        assertNull(dataAccess.getAuth("logoutToken"), "Auth token should be deleted from DAO.");
    }

    @Test
    @DisplayName("Negative Logout (Unauthorized) Test")
    void logoutFailureUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> userService.logout("invalidToken"),
                "Should throw UnauthorizedException for an invalid/non-existent token.");
    }
}