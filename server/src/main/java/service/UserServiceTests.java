package service;
import dataaccess.*;
import exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTests {

    private UserService userService;
    private DataAccess dao;

    @BeforeEach
    public void setup() throws DataAccessException {
        dao = new MemoryDAO();
        dao.clear();
        userService = new UserService(dao);
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        var req = new UserService.RegisterRequest("sophia", "pass123", "email@x.com");
        var result = userService.register(req);
        assertNotNull(result);
        assertEquals("sophia", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    public void testRegisterAlreadyTaken() throws Exception {
        var req = new UserService.RegisterRequest("sam", "pw", "x@x.com");
        userService.register(req);
        assertThrows(AlreadyTakenException.class, () -> userService.register(req));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        var reg = new UserService.RegisterRequest("user1", "abc", "u@e.com");
        userService.register(reg);
        var login = new UserService.LoginRequest("user1", "abc");
        var result = userService.login(login);
        assertNotNull(result.authToken());
    }

    @Test
    public void testLoginBadPassword() throws Exception {
        var reg = new UserService.RegisterRequest("bad", "ok", "x@x.com");
        userService.register(reg);
        var login = new UserService.LoginRequest("bad", "wrong");
        assertThrows(UnauthorizedException.class, () -> userService.login(login));
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        var reg = new UserService.RegisterRequest("sue", "123", "a@a.com");
        var res = userService.register(reg);
        assertDoesNotThrow(() -> userService.logout(res.authToken()));
    }


    @Test
    public void testLogoutUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> userService.logout("fakeToken"));
    }
}
