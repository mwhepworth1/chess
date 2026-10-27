package dataaccess;

import dataaccess.sql.SqlUserDAO;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

public class SqlUserDAOTest {
    private SqlUserDAO userDAO;
    private final UserData bob = new UserData("bob", "secret", "bob@mail.com");

    @BeforeEach
    void setUp() throws DataAccessException {
        DatabaseManager.initializeDatabase();
        userDAO = new SqlUserDAO();
        userDAO.clear();
    }

    @Test
    void createUserSuccess() throws DataAccessException {
        userDAO.createUser(bob);

        UserData stored = userDAO.getUser("bob");
        assertEquals("bob", stored.username());
        assertEquals("bob@mail.com", stored.email());
        assertNotEquals("secret", stored.password());
        assertTrue(BCrypt.checkpw("secret", stored.password()));
    }

    @Test
    void createUserDuplicateUsername() throws DataAccessException {
        userDAO.createUser(bob);

        assertThrows(DataAccessException.class,
                () -> userDAO.createUser(new UserData("bob", "other", "other@mail.com")));
        assertTrue(BCrypt.checkpw("secret", userDAO.getUser("bob").password()));
    }

    @Test
    void getUserSuccess() throws DataAccessException {
        userDAO.createUser(bob);

        UserData found = userDAO.getUser("bob");

        assertNotNull(found);
        assertEquals("bob", found.username());
    }

    @Test
    void getUserMissing() throws DataAccessException {
        assertNull(userDAO.getUser("ghost"));
    }

    @Test
    void clearRemovesUsers() throws DataAccessException {
        userDAO.createUser(bob);

        userDAO.clear();

        assertNull(userDAO.getUser("bob"));
    }

    @Test
    void clearEmptyTable() {
        assertDoesNotThrow(() -> userDAO.clear());
    }
}