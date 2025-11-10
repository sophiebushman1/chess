package client;

import org.junit.jupiter.api.*;
import server.Server;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;
    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0); // start on a random free port
        System.out.println("Started test HTTP server on port " + port);
        facade = new ServerFacade(port);
    }
    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clear() {
        var result = facade.clear();
        assertNull(result.getMessage(), "clear() should succeed");
    }

    // clear tests
    @Test
    @DisplayName("Clear Success")
    public void clearSuccess() {
        var res = facade.clear();
        assertNull(res.getMessage(), "clear should succeed with no error message");
    }

    @Test
    @DisplayName("Clear Fails with Server Down")
    public void clearFails() {
        // simulate by creating a facade with an invalid port
        var badFacade = new ServerFacade(9999);
        var res = badFacade.clear();
        assertNotNull(res.getMessage(), "clear() should fail if server unavailable");
    }

    @Test
    @DisplayName("Register Success")
    public void registerSuccess() {
        var res = facade.register("alice", "password123", "alice@example.com");
        assertEquals(200, facade.getStatusCode());
        assertNotNull(res.getAuthToken());
        assertEquals("alice", res.getUsername());
    }

    @Test
    @DisplayName("Register Duplicate Username Fails")
    public void registerDuplicateFails() {
        facade.register("bob", "pw", "b@example.com");
        var res = facade.register("bob", "pw", "b@example.com");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("already"));
    }

    @Test
    @DisplayName("Login Success")
    public void loginSuccess() {
        facade.register("carl", "pw", "c@example.com");
        var res = facade.login("carl", "pw");
        assertEquals(200, facade.getStatusCode());
        assertNotNull(res.getAuthToken());
    }

    @Test
    @DisplayName("Login Fails with Wrong Password")
    public void loginWrongPassword() {
        facade.register("dana", "pw", "d@example.com");
        var res = facade.login("dana", "wrong");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }

    // createGame tests
    @Test
    @DisplayName("Create Game Success")
    public void createGameSuccess() {
        var reg = facade.register("eve", "pw", "e@example.com");
        var create = facade.createGame("Eve’s Game", reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNotNull(create.getGameID());
    }

    @Test
    @DisplayName("Create Game Fails without Auth")
    public void createGameFails() {
        var res = facade.createGame("NoAuthGame", "bad-token");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }

    // listgames tests
    @Test
    @DisplayName("List Games Success")
    public void listGamesSuccess() {
        var reg = facade.register("ivy", "pw", "i@example.com");
        facade.createGame("Ivy’s Game", reg.getAuthToken());
        var list = facade.listGames(reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNotNull(list.getGames());
        assertTrue(list.getGames().length > 0);
    }

    @Test
    @DisplayName("List Games Fails without Auth")
    public void listGamesFails() {
        var list = facade.listGames("bad-token");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(list.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    @DisplayName("Join Game Works")
    public void joinGameSuccess() {
        var reg = facade.register("frank", "pw", "f@example.com");
        var create = facade.createGame("Frank’s Game", reg.getAuthToken());
        int gameID = create.getGameID();
        var join = facade.joinGame("WHITE", gameID, reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNull(join.getMessage());
    }

    @Test
    @DisplayName("Join Game Fails with Invalid Game ID")
    public void joinGameInvalidID() {
        var reg = facade.register("henry", "pw", "h@example.com");
        var join = facade.joinGame("WHITE", 9999, reg.getAuthToken());
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(join.getMessage().toLowerCase().contains("error"));
    }

    //logout tests
    @Test
    @DisplayName("Logout Works")
    public void logoutSuccess() {
        var reg = facade.register("gina", "pw", "g@example.com");
        var logout = facade.logout(reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNull(logout.getMessage());
    }

    @Test
    @DisplayName("Logout Fails with Invalid Token")
    public void logoutFails() {
        var res = facade.logout("bad-token");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }
}
