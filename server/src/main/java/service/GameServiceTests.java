package service;

import dataaccess.*;
import exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTests {

    private GameService gameService;
    private UserService userService;
    private DataAccess dao;
    private String token;

    @BeforeEach
    public void setup() throws Exception {
        dao = new MemoryDAO();
        dao.clear();
        userService = new UserService(dao);
        gameService = new GameService(dao);
        var reg = new UserService.RegisterRequest("bob", "pw", "b@x.com");
        var result = userService.register(reg);
        token = result.authToken();
    }

    @Test
    public void testCreateGameSuccess() throws Exception {
        var req = new GameService.CreateGameRequest("CoolGame");
        var res = gameService.createGame(token, req);
        assertTrue(res.gameID() > 0);
    }

    @Test
    public void testCreateGameBadRequest() {
        var req = new GameService.CreateGameRequest("");
        assertThrows(BadRequestException.class, () -> gameService.createGame(token, req));
    }

    @Test
    public void testListGamesEmpty() throws Exception {
        var list = gameService.listGames(token);
        assertNotNull(list);
        assertTrue(list.games().isEmpty());
    }

    @Test
    public void testJoinGameAsWhite() throws Exception {
        var create = gameService.createGame(token, new GameService.CreateGameRequest("Joinable"));
        var join = new GameService.JoinGameRequest("WHITE", create.gameID());
        assertDoesNotThrow(() -> gameService.joinGame(token, join));
    }

    @Test
    public void testJoinGameInvalidColor() throws Exception {
        var create = gameService.createGame(token, new GameService.CreateGameRequest("BadColor"));
        var join = new GameService.JoinGameRequest("PURPLE", create.gameID());
        assertThrows(BadRequestException.class, () -> gameService.joinGame(token, join));
    }

    @Test
    public void testJoinGameAlreadyTaken() throws Exception {
        var create = gameService.createGame(token, new GameService.CreateGameRequest("TakenGame"));
        var join1 = new GameService.JoinGameRequest("WHITE", create.gameID());
        gameService.joinGame(token, join1);

        var reg2 = new UserService.RegisterRequest("sam", "pw", "s@x.com");
        var auth2 = userService.register(reg2);
        var join2 = new GameService.JoinGameRequest("WHITE", create.gameID());
        assertThrows(AlreadyTakenException.class, () -> gameService.joinGame(auth2.authToken(), join2));
    }

}
