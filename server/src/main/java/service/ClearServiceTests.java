package service;

import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTests {

    private ClearService clearService;
    private UserService userService;
    private DataAccess dao;

    @BeforeEach
    public void setup() throws Exception {
        dao = new MemoryDAO();
        clearService = new ClearService(dao);
        userService = new UserService(dao);
    }

    @Test
    public void testClearRemovesAllData() throws Exception {
        var reg = new UserService.RegisterRequest("clear", "me", "x@x.com");
        userService.register(reg);
        clearService.clearApplication();
        assertNull(dao.getUser("clear"));
    }

    @Test
    public void testClearEmptyDB() {
        assertDoesNotThrow(() -> clearService.clearApplication());
    }
}
