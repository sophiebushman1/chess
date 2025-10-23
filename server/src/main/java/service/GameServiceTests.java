package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDAO;
import model.AuthData;
import model.GameData; // <-- FIX: Added model import
import model.UserData; // <-- FIX: Added model import
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.ServiceExceptions.AlreadyTakenException;
import service.ServiceExceptions.BadRequestException;
import service.ServiceExceptions.UnauthorizedException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Game Service Tests")
public class GameServiceTests { // <-- This name must match the filename!

    private DataAccess dataAccess;
    private UserService userService; // Added to help generate token for userB
    private GameService gameService;
    private String validTokenA;
    private String validTokenB;
    private final String userA = "playerA";
    private final String userB = "playerB";

    @BeforeEach
    void setUp() throws DataAccessException, AlreadyTakenException, BadRequestException, UnauthorizedException {
        dataAccess = new MemoryDAO();
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);
        dataAccess.clear();

        // Setup User A and get Auth Token
        userService.register(new RegisterRequest(userA, "passA", "a@mail.com"));
        AuthResult authResultA = userService.login(new LoginRequest(userA, "passA"));
        validTokenA = authResultA.authToken();

        // Setup User B and get Auth Token (Fixes the listAuths() issue)
        userService.register(new RegisterRequest(userB, "passB", "b@mail.com"));
        AuthResult authResultB = userService.login(new LoginRequest(userB, "passB"));
        validTokenB = authResultB.authToken();
    }

    // --- Create Game Tests ---

    @Test
    @DisplayName("Positive Create Game Test")
    void createGameSuccess() {
        CreateGameRequest req = new CreateGameRequest("NewChessGame");
        assertDoesNotThrow(() -> {
            CreateGameResult result = gameService.createGame(validTokenA, req);
            assertTrue(result.gameID() > 0, "Game ID should be greater than zero.");

            // Verify the game was created in the DAO
            GameData game = dataAccess.getGame(result.gameID());
            assertNotNull(game, "Game should be stored in DAO.");
            assertEquals("NewChessGame", game.gameName(), "Game name should match.");
            assertNotNull(game.game(), "Game object should be initialized.");
        }, "Game creation should succeed with a valid token and name.");
    }

    @Test
    @DisplayName("Negative Create Game (Unauthorized) Test")
    void createGameFailureUnauthorized() {
        CreateGameRequest req = new CreateGameRequest("BadGame");
        assertThrows(UnauthorizedException.class, () -> gameService.createGame("invalidToken", req),
                "Should throw UnauthorizedException for invalid token.");
    }

    @Test
    @DisplayName("Negative Create Game (Bad Request) Test")
    void createGameFailureBadRequest() {
        CreateGameRequest req = new CreateGameRequest(null);
        assertThrows(BadRequestException.class, () -> gameService.createGame(validTokenA, req),
                "Should throw BadRequestException if game name is missing.");
    }

    // --- List Games Tests ---

    @Test
    @DisplayName("Positive List Games Test")
    void listGamesSuccess() throws DataAccessException {
        // Pre-create two games
        dataAccess.createGame(new GameData(0, null, null, "G1", new ChessGame()));
        dataAccess.createGame(new GameData(0, null, null, "G2", new ChessGame()));

        assertDoesNotThrow(() -> {
            ListGamesResult result = gameService.listGames(validTokenA);
            assertNotNull(result.games(), "List should not be null.");
            assertEquals(2, result.games().size(), "Should return the correct number of games.");
        }, "Listing games should succeed with a valid token.");
    }

    @Test
    @DisplayName("Negative List Games (Unauthorized) Test")
    void listGamesFailureUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> gameService.listGames("invalidToken"),
                "Should throw UnauthorizedException for invalid token.");
    }

    // --- Join Game Tests ---

    @Test
    @DisplayName("Positive Join Game as White Test")
    void joinGameWhiteSuccess() throws DataAccessException {
        // Create a game with ID 1
        GameData game = dataAccess.createGame(new GameData(0, null, null, "TestGame", new ChessGame()));
        JoinGameRequest req = new JoinGameRequest("WHITE", game.gameID());

        assertDoesNotThrow(() -> gameService.joinGame(validTokenA, req), "Joining white should succeed.");

        GameData updatedGame = dataAccess.getGame(game.gameID());
        assertEquals(userA, updatedGame.whiteUsername(), "UserA should be white.");
        assertNull(updatedGame.blackUsername(), "Black should be null.");
    }

    @Test
    @DisplayName("Positive Join Game as Observer Test")
    void joinGameObserverSuccess() throws DataAccessException {
        // Create a game with ID 1
        GameData game = dataAccess.createGame(new GameData(0, userA, null, "TestGame", new ChessGame()));
        JoinGameRequest req = new JoinGameRequest(null, game.gameID()); // Null color for observer

        assertDoesNotThrow(() -> gameService.joinGame(validTokenB, req), "Joining as observer should succeed.");

        GameData updatedGame = dataAccess.getGame(game.gameID());
        // Verify no change to player spots, userB is an observer
        assertEquals(userA, updatedGame.whiteUsername(), "White player should remain UserA.");
    }

    @Test
    @DisplayName("Negative Join Game (Bad Request - Missing ID) Test")
    void joinGameFailureBadRequestID() {
        JoinGameRequest req = new JoinGameRequest("WHITE", 0);
        assertThrows(BadRequestException.class, () -> gameService.joinGame(validTokenA, req),
                "Should throw BadRequestException for missing game ID.");
    }

    @Test
    @DisplayName("Negative Join Game (Bad Request - Game Not Found) Test")
    void joinGameFailureGameNotFound() {
        JoinGameRequest req = new JoinGameRequest("WHITE", 999);
        assertThrows(BadRequestException.class, () -> gameService.joinGame(validTokenA, req),
                "Should throw BadRequestException if game ID does not exist.");
    }

    @Test
    @DisplayName("Negative Join Game (Unauthorized) Test")
    void joinGameFailureUnauthorized() throws DataAccessException {
        GameData game = dataAccess.createGame(new GameData(0, null, null, "TestGame", new ChessGame()));
        JoinGameRequest req = new JoinGameRequest("WHITE", game.gameID());

        assertThrows(UnauthorizedException.class, () -> gameService.joinGame("invalidToken", req),
                "Should throw UnauthorizedException for invalid token.");
    }

    @Test
    @DisplayName("Negative Join Game (Already Taken) Test")
    void joinGameFailureAlreadyTaken() throws DataAccessException {
        // Create a game where playerA is already white
        GameData game = dataAccess.createGame(new GameData(0, userA, null, "TestGame", new ChessGame()));

        // Try to join as white using playerB's token
        JoinGameRequest req = new JoinGameRequest("WHITE", game.gameID());

        assertThrows(AlreadyTakenException.class, () -> gameService.joinGame(validTokenB, req),
                "Should throw AlreadyTakenException if position is already taken by another user.");
    }
}