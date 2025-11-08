package client;

import org.junit.jupiter.api.*;
import server.Server;
import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
        System.out.println("Stopped server");
    }

    @BeforeEach
    void clearDB() {
        var res = facade.clear();
        assertNull(res.getMessage(), "clear() should succeed");
    }

    @Test
    @DisplayName("Register Success")
    public void registerSuccess() {
        var res = facade.register("alice", "password123", "alice@example.com");
        assertEquals(200, facade.getStatusCode());
        assertNotNull(res.getAuthToken(), "Auth token should be returned");
        assertEquals("alice", res.getUsername());
    }

    @Test
    @DisplayName("Register Duplicate Username Fails")
    public void registerDuplicateFails() {
        facade.register("bob", "pw", "b@example.com");
        var res = facade.register("bob", "pw", "b@example.com");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().contains("already") || res.getMessage().contains("exists"));
    }

    @Test
    @DisplayName("Login Success")
    public void loginSuccess() {
        facade.register("carl", "pw", "c@example.com");
        var res = facade.login("carl", "pw");
        assertEquals(200, facade.getStatusCode());
        assertNotNull(res.getAuthToken());
        assertEquals("carl", res.getUsername());
    }

    @Test
    @DisplayName("Login Fails with Wrong Password")
    public void loginWrongPassword() {
        facade.register("dana", "pw", "d@example.com");
        var res = facade.login("dana", "wrong");
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    @DisplayName("Create Game and List It")
    public void createAndListGame() {
        var reg = facade.register("eve", "pw", "e@example.com");
        var create = facade.createGame("Eve’s Game", reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNotNull(create.getGameID());

        var list = facade.listGames(reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNotNull(list.getGames());
        assertTrue(list.getGames().length >= 1);
        assertEquals("Eve’s Game", list.getGames()[0].getGameName());
    }

    @Test
    @DisplayName("Join Game Works")
    public void joinGameSuccess() {
        var reg = facade.register("frank", "pw", "f@example.com");
        var create = facade.createGame("Frank’s Game", reg.getAuthToken());
        int gameID = create.getGameID();
        var join = facade.joinGame("WHITE", gameID, reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNull(join.getMessage(), "joinGame should succeed with no message");
    }
    @Test
    @DisplayName("Join Game Fails with Invalid Game ID")
    public void joinGameInvalidID() {
        var reg = facade.register("henry", "pw", "h@example.com");
        var join = facade.joinGame("WHITE", 9999, reg.getAuthToken()); // 9999 should not exist
        assertNotEquals(200, facade.getStatusCode());
        assertTrue(join.getMessage().toLowerCase().contains("error") || join.getMessage().toLowerCase().contains("invalid"));
    }


    @Test
    @DisplayName("Logout Works")
    public void logoutSuccess() {
        var reg = facade.register("gina", "pw", "g@example.com");
        var logout = facade.logout(reg.getAuthToken());
        assertEquals(200, facade.getStatusCode());
        assertNull(logout.getMessage());
    }
}
