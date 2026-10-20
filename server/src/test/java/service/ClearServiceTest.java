package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClearServiceTest {
    private UserDAO userDAO;
    private GameDAO gameDAO;
    private AuthDAO authDAO;
    private ClearService clearService;

    @BeforeEach
    void setUp() {
        userDAO = new MemoryUserDAO();
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        clearService = new ClearService(userDAO, gameDAO, authDAO);
    }

    @Test
    void clearRemovesAllData() throws DataAccessException {
        userDAO.createUser(new UserData("bob", "pass", "bob@mail.com"));
        authDAO.createAuth(new AuthData("token123", "bob"));
        int gameID = gameDAO.createGame("game1");

        clearService.clear();

        assertNull(userDAO.getUser("bob"));
        assertNull(authDAO.getAuth("token123"));
        assertNull(gameDAO.getGame(gameID));
        assertTrue(gameDAO.listGames().isEmpty());
    }
}
