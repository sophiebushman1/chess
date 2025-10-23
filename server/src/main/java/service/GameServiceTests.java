package service;


import dataaccess.DataAccess;
import dataaccess.MemoryDAO;
import model.*;
import model.GameData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class GameServiceTests {

    private DataAccess dataAccess;
    private GameService gameService;
    private String validAuthToken;

    private int existingGameID;

    @BeforeEach
    public void setup() throws Exception {
        dataAccess = new MemoryDAO();
        gameService = new GameService(dataAccess);

        // logged in
        AuthData auth = dataAccess.createAuth("testPlayer");
        validAuthToken = auth.authToken();

        // game exits
        GameData game = dataAccess.createGame("InitialGame");
        existingGameID = game.gameID();
    }

    //list tests
    @Test
    void listGamesPositive() throws Exception {
        // add a second game
        dataAccess.createGame("SecondGame");

        ListGamesResult result = gameService.listGames(validAuthToken);

        // correctness
        assertEquals(2, result.games().size(), "Should return exactly two games.");
        assertTrue(result.games().stream().anyMatch(g -> g.gameID() == existingGameID),
                "The initial game should be in the list.");
    }


    @Test
    void listGamesNegative_Unauthorized() {
        assertThrows(UnauthorizedException.class, () ->
                        gameService.listGames("invalid-token"),
                "Should throw UnauthorizedException for invalid token.");
    }

    //create game tests
    @Test
    void createGamePositive() throws Exception {
        CreateGameRequest req = new CreateGameRequest("NewGameTitle");
        CreateGameResult result = gameService.createGame(validAuthToken, req);

        // correctness
        assertTrue(result.gameID() > 0, "Game ID must be a positive integer.");

        assertNotNull(dataAccess.getGame(result.gameID()), "The new game must be stored in the DAO.");
    }

    @Test
    void createGameNegative_BadRequest() {
        CreateGameRequest req = new CreateGameRequest(null); // Missing game name

        assertThrows(BadRequestException.class, () ->
                        gameService.createGame(validAuthToken, req),
                "Should throw BadRequestException for missing game name.");
    }



    @Test
    void createGameNegative_Unauthorized() {
        CreateGameRequest req = new CreateGameRequest("ValidName");

        assertThrows(UnauthorizedException.class, () ->
                        gameService.createGame("bad-auth", req),
                "Should throw UnauthorizedException for invalid token.");

    }

    // joining tests


    @Test
    void joinGamePositive_White() throws Exception {
        JoinGameRequest req = new JoinGameRequest("WHITE", existingGameID);
        gameService.joinGame(validAuthToken, req);


        GameData updatedGame = dataAccess.getGame(existingGameID);
        assertEquals("testPlayer", updatedGame.whiteUsername(), "White username must be updated to the caller's username.");
        assertNull(updatedGame.blackUsername(), "Black username should remain null.");
    }
    @Test
    void joinGamePositive_Observer() throws Exception {
        JoinGameRequest req = new JoinGameRequest(null, existingGameID);


        gameService.joinGame(validAuthToken, req);

        // check DAO state
        GameData updatedGame = dataAccess.getGame(existingGameID);
        assertNull(updatedGame.whiteUsername(), "White username should remain null for observer.");
        assertNull(updatedGame.blackUsername(), "Black username should remain null for observer.");

        // no exceptions
    }


    @Test
    void joinGameNegative_AlreadyTaken() throws Exception {
        // another player claims the white spot
        dataAccess.createAuth("secondPlayer");
        GameData takenGame = new GameData(existingGameID, "secondPlayer", null, "InitialGame", new chess.ChessGame());
        dataAccess.updateGame(takenGame);

        JoinGameRequest req = new JoinGameRequest("WHITE", existingGameID);

        // 2 players cant be in teh same spot
        assertThrows(AlreadyTakenException.class, () ->
                        gameService.joinGame(validAuthToken, req),
                "Should throw AlreadyTakenException when trying to join a taken spot.");
    }
    @Test
    void joinGameNegative_InvalidGameID() {
        JoinGameRequest req = new JoinGameRequest("WHITE", 9999); // Invalid ID

        assertThrows(BadRequestException.class, () ->
                        gameService.joinGame(validAuthToken, req),
                "Should throw BadRequestException for a non-existent game ID.");
    }


    @Test
    void joinGameNegative_Unauthorized() {
        JoinGameRequest req = new JoinGameRequest("WHITE", existingGameID);

        assertThrows(UnauthorizedException.class, () ->
                        gameService.joinGame("bad-auth", req),
                "Should throw UnauthorizedException for invalid token.");

    }
}