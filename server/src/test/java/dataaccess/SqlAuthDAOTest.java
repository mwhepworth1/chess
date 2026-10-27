package dataaccess;

import dataaccess.sql.SqlAuthDAO;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlAuthDAOTest {
    private SqlAuthDAO authDAO;
    private final AuthData bobAuth = new AuthData("token-bob", "bob");
    private final AuthData aliceAuth = new AuthData("token-alice", "alice");

    @BeforeEach
    void setUp() throws DataAccessException {
        DatabaseManager.initializeDatabase();
        authDAO = new SqlAuthDAO();
        authDAO.clear();
    }

    @Test
    void createAuthSuccess() throws DataAccessException {
        authDAO.createAuth(bobAuth);

        assertEquals(bobAuth, authDAO.getAuth("token-bob"));
    }

    @Test
    void createAuthDuplicateToken() throws DataAccessException {
        authDAO.createAuth(bobAuth);

        assertThrows(DataAccessException.class,
                () -> authDAO.createAuth(new AuthData("token-bob", "eve")));
        assertEquals("bob", authDAO.getAuth("token-bob").username());
    }

    @Test
    void getAuthSuccess() throws DataAccessException {
        authDAO.createAuth(bobAuth);
        authDAO.createAuth(aliceAuth);

        assertEquals("alice", authDAO.getAuth("token-alice").username());
    }

    @Test
    void getAuthMissing() throws DataAccessException {
        assertNull(authDAO.getAuth("no-such-token"));
    }

    @Test
    void deleteAuthRemovesOnlyThatToken() throws DataAccessException {
        authDAO.createAuth(bobAuth);
        authDAO.createAuth(aliceAuth);

        authDAO.deleteAuth("token-bob");

        assertNull(authDAO.getAuth("token-bob"));
        assertNotNull(authDAO.getAuth("token-alice"));
    }

    @Test
    void deleteAuthMissingToken() throws DataAccessException {
        authDAO.createAuth(bobAuth);

        assertDoesNotThrow(() -> authDAO.deleteAuth("no-such-token"));
        assertNotNull(authDAO.getAuth("token-bob"));
    }

    @Test
    void clearRemovesAllTokens() throws DataAccessException {
        authDAO.createAuth(bobAuth);
        authDAO.createAuth(aliceAuth);

        authDAO.clear();

        assertNull(authDAO.getAuth("token-bob"));
        assertNull(authDAO.getAuth("token-alice"));
    }

    @Test
    void clearEmptyTable() {
        assertDoesNotThrow(() -> authDAO.clear());
    }
}