package dataaccess;

import chess.ChessGame;
import dataaccess.sql.SqlAuthDAO;
import dataaccess.sql.SqlGameDAO;
import dataaccess.sql.SqlUserDAO;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SqlDAOFailureTest {
    private final SqlUserDAO userDAO = new SqlUserDAO();
    private final SqlAuthDAO authDAO = new SqlAuthDAO();
    private final SqlGameDAO gameDAO = new SqlGameDAO();

    @BeforeEach
    void dropTables() throws DataAccessException, SQLException {
        DatabaseManager.initializeDatabase();
        try (var conn = DatabaseManager.getConnection();
             var statement = conn.createStatement()) {
            statement.executeUpdate("DROP TABLE user");
            statement.executeUpdate("DROP TABLE auth");
            statement.executeUpdate("DROP TABLE game");
        }
    }

    @AfterEach
    void restoreTables() throws DataAccessException {
        DatabaseManager.initializeDatabase();
    }

    @Test
    void userDAOThrowsWhenTableMissing() {
        assertThrows(DataAccessException.class, () -> userDAO.clear());
        assertThrows(DataAccessException.class, () -> userDAO.createUser(new UserData("bob", "pw", "b@mail.com")));
        assertThrows(DataAccessException.class, () -> userDAO.getUser("bob"));
    }

    @Test
    void authDAOThrowsWhenTableMissing() {
        assertThrows(DataAccessException.class, () -> authDAO.clear());
        assertThrows(DataAccessException.class, () -> authDAO.createAuth(new AuthData("token", "bob")));
        assertThrows(DataAccessException.class, () -> authDAO.getAuth("token"));
        assertThrows(DataAccessException.class, () -> authDAO.deleteAuth("token"));
    }

    @Test
    void gameDAOThrowsWhenTableMissing() {
        assertThrows(DataAccessException.class, () -> gameDAO.clear());
        assertThrows(DataAccessException.class, () -> gameDAO.createGame("game"));
        assertThrows(DataAccessException.class, () -> gameDAO.getGame(1));
        assertThrows(DataAccessException.class, () -> gameDAO.listGames());
        assertThrows(DataAccessException.class,
                () -> gameDAO.updateGame(new GameData(1, null, null, "game", new ChessGame())));
    }
}