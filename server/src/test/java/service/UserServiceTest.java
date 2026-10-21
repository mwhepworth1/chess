package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryUserDAO;
import dataaccess.UserDAO;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserDAO userDAO;
    private AuthDAO authDAO;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    void registerSuccess() throws Exception {
        RegisterResult result = userService.register(new RegisterRequest("bob", "pass", "bob@mail.com"));

        assertEquals("bob", result.username());
        assertNotNull(result.authToken());
        assertNotNull(userDAO.getUser("bob"));
        assertEquals("bob", authDAO.getAuth(result.authToken()).username());
    }

    @Test
    void registerDuplicateUsername() throws Exception {
        userService.register(new RegisterRequest("bob", "pass", "bob@mail.com"));

        assertThrows(AlreadyTakenException.class,
                () -> userService.register(new RegisterRequest("bob", "other", "other@mail.com")));
    }

    @Test
    void registerMissingPassword() {
        assertThrows(BadRequestException.class,
                () -> userService.register(new RegisterRequest("bob", null, "bob@mail.com")));
    }

    @Test
    void registerFailureDoesNotCreateUser() throws DataAccessException {
        assertThrows(BadRequestException.class,
                () -> userService.register(new RegisterRequest("bob", "", "bob@mail.com")));

        assertNull(userDAO.getUser("bob"));
    }
}
