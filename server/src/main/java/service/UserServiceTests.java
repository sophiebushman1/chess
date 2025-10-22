package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDAO;
import model.UserData;
import model.AuthData;
import model.UserData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class UserServiceTests {

    private DataAccess dataAccess;
    private UserService userService;

    @BeforeEach
    public void setup() {
        dataAccess = new MemoryDAO();
        userService = new UserService(dataAccess);
    }

    //reg tests
    @Test
    void registerPositive() throws Exception {
        RegisterRequest req = new RegisterRequest("newuser", "pass", "new@mail.com");
        AuthResult result = userService.register(req);


        assertEquals("newuser", result.username(), "Result username should match request.");
        assertNotNull(result.authToken(), "Auth token must be generated.");

        assertNotNull(dataAccess.getUser("newuser"), "User must be stored in the DAO.");
        assertNotNull(dataAccess.getAuth(result.authToken()), "Auth token must be stored in the DAO.");
    }


    @Test
    void registerNegative_AlreadyTaken() throws Exception {
        // 1rst is ok
        userService.register(new RegisterRequest("taken", "pass1", "a@a.com"));

        // 2nd registration not allowed
        RegisterRequest failureReq = new RegisterRequest("taken", "pass2", "b@b.com");

        assertThrows(AlreadyTakenException.class, () ->
                        userService.register(failureReq),
                "Should throw AlreadyTakenException for duplicate username.");

    }


    @Test
    void registerNegative_BadRequest() {
        RegisterRequest failureReq = new RegisterRequest("user", null, "email"); // Missing password

        assertThrows(BadRequestException.class, () ->
                        userService.register(failureReq),
                "Should throw BadRequestException for missing password.");
    }

    //login tests

    @Test
    void loginPositive() throws Exception {
        // setup user
        dataAccess.insertUser(new UserData("loginuser", "correctpass", "l@l.com"));

        LoginRequest req = new LoginRequest("loginuser", "correctpass");
        AuthResult result = userService.login(req);


        // correct?
        assertEquals("loginuser", result.username(), "Result username should match request.");
        assertNotNull(result.authToken(), "Auth token must be generated.");

        // was auth stored?
        assertNotNull(dataAccess.getAuth(result.authToken()), "New Auth token must be stored in the DAO.");

    }


    @Test
    void loginNegative_Unauthorized() throws Exception {
        // Setup: register a user
        dataAccess.insertUser(new UserData("badpassuser", "correct", "b@p.com"));

        LoginRequest failureReq = new LoginRequest("badpassuser", "wrong"); // Wrong password

        assertThrows(UnauthorizedException.class, () ->
                        userService.login(failureReq),
                "Should throw UnauthorizedException for incorrect password.");

    }



    //logout tests
    @Test
    void logoutPositive() throws Exception {
        // new auth token
        AuthData auth = dataAccess.createAuth("logoutuser");
        String token = auth.authToken();

        // logout
        userService.logout(token);

        // was token deleted?
        assertNull(dataAccess.getAuth(token), "Auth token should be deleted from the DAO.");
    }

    //401 negative logout test
    @Test
    void logoutNegative_Unauthorized() {
        // if the token nonexistent was never created


        assertThrows(UnauthorizedException.class, () ->
                        userService.logout("nonexistent"),
                "Should throw UnauthorizedException for an invalid token.");

    }
}