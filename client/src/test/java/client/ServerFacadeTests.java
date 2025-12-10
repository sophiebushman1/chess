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
        // spin up the server once for all the tests
        server = new Server();

        // run it on a random free port (0 means "pick for me")
        var port = server.run(0);
        System.out.println("Started test HTTP server on port " + port);

        // point the facade at that port so tests hit our test server
        facade = new ServerFacade(port);
    }

    @AfterAll
    public static void stopServer() {
        // shut down the tiny test server once everything's done
        server.stop();
    }

    @BeforeEach
    public void clear() {
        // wiping DB before every test so things are clean
        var result = facade.clear();
        assertNull(result.getMessage(), "clear() should succeed");
    }

    @Test
    @DisplayName("Clear Success")
    public void clearSuccess() {
        // normal clear should not return any error message
        var res = facade.clear();
        assertNull(res.getMessage());
    }

    @Test
    @DisplayName("Clear Fails with Server Down")
    public void clearFails() {
        // make a facade pointing at a dead port
        var badFacade = new ServerFacade(9999);

        // this should obviously fail
        var res = badFacade.clear();
        assertNotNull(res.getMessage());
    }

    @Test
    @DisplayName("Register Success")
    public void registerSuccess() {
        // registering a normal user
        var res = facade.register("alice", "password123", "alice@example.com");

        // expecting all the usual success things
        assertEquals(200, facade.getStatusCode());
        assertNotNull(res.getAuthToken());
        assertEquals("alice", res.getUsername());
    }

    @Test
    @DisplayName("Register Duplicate Username Fails")
    public void registerDuplicateFails() {
        // first register is fine
        facade.register("bob", "pw", "b@example.com");

        // second one with same username should fail
        var res = facade.register("bob", "pw", "b@example.com");

        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("already"));
    }

    @Test
    @DisplayName("Login Success")
    public void loginSuccess() {
        // register a user first
        facade.register("carl", "pw", "c@example.com");

        // now login with correct pw
        var res = facade.login("carl", "pw");

        assertEquals(200, facade.getStatusCode());
        assertNotNull(res.getAuthToken());
    }

    @Test
    @DisplayName("Login Fails with Wrong Password")
    public void loginWrongPassword() {
        // setup user
        facade.register("dana", "pw", "d@example.com");

        // wrong password should get denied
        var res = facade.login("dana", "wrong");

        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    @DisplayName("Create Game Success")
    public void createGameSuccess() {
        // need a valid auth token
        var reg = facade.register("eve", "pw", "e@example.com");

        // create a game with that token
        var create = facade.createGame("Eve’s Game", reg.getAuthToken());

        assertEquals(200, facade.getStatusCode());
        assertNotNull(create.getGameID());
    }

    @Test
    @DisplayName("Create Game Fails without Auth")
    public void createGameFails() {
        // making a game with a garbage token
        var res = facade.createGame("NoAuthGame", "bad-token");

        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    @DisplayName("List Games Success")
    public void listGamesSuccess() {
        // register + create a game so list actually has something to return
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
        // can't list games with a fake token
        var list = facade.listGames("bad-token");

        assertNotEquals(200, facade.getStatusCode());
        assertTrue(list.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    @DisplayName("Join Game Works")
    public void joinGameSuccess() {
        // setup: user + game
        var reg = facade.register("frank", "pw", "f@example.com");
        var create = facade.createGame("Frank’s Game", reg.getAuthToken());

        // try joining as white
        var join = facade.joinGame("WHITE", create.getGameID(), reg.getAuthToken());

        assertEquals(200, facade.getStatusCode());
        assertNull(join.getMessage());
    }

    @Test
    @DisplayName("Join Game Fails with Invalid Game ID")
    public void joinGameInvalidID() {
        // valid user but bogus game ID
        var reg = facade.register("henry", "pw", "h@example.com");

        var join = facade.joinGame("WHITE", 9999, reg.getAuthToken());

        assertNotEquals(200, facade.getStatusCode());
        assertTrue(join.getMessage().toLowerCase().contains("error"));
    }

    @Test
    @DisplayName("Logout Works")
    public void logoutSuccess() {
        // register a user so they have a token to log out with
        var reg = facade.register("gina", "pw", "g@example.com");

        var logout = facade.logout(reg.getAuthToken());

        assertEquals(200, facade.getStatusCode());
        assertNull(logout.getMessage());
    }

    @Test
    @DisplayName("Logout Fails with Invalid Token")
    public void logoutFails() {
        // trying to log out with a fake token
        var res = facade.logout("bad-token");

        assertNotEquals(200, facade.getStatusCode());
        assertTrue(res.getMessage().toLowerCase().contains("unauthorized"));
    }
}
