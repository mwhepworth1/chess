package dataaccess;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import dataaccess.sql.SqlGameDAO;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class SqlGameDAOTest {
    private SqlGameDAO gameDAO;

    @BeforeEach
    void setUp() throws DataAccessException {
        DatabaseManager.initializeDatabase();
        gameDAO = new SqlGameDAO();
        gameDAO.clear();
    }

    @Test
    void createGameSuccess() throws DataAccessException {
        int gameID = gameDAO.createGame("first");

        GameData stored = gameDAO.getGame(gameID);
        assertEquals("first", stored.gameName());
        assertNull(stored.whiteUsername());
        assertNull(stored.blackUsername());
        assertEquals(new ChessGame(), stored.game());
    }

    @Test
    void createGameAssignsDistinctIDs() throws DataAccessException {
        int first = gameDAO.createGame("first");
        int second = gameDAO.createGame("second");

        assertNotEquals(first, second);
    }

    @Test
    void getGameMissing() throws DataAccessException {
        assertNull(gameDAO.getGame(999));
    }

    @Test
    void listGamesReturnsAllGames() throws DataAccessException {
        gameDAO.createGame("first");
        gameDAO.createGame("second");

        Collection<GameData> games = gameDAO.listGames();

        assertEquals(2, games.size());
    }

    @Test
    void listGamesEmpty() throws DataAccessException {
        assertTrue(gameDAO.listGames().isEmpty());
    }

    @Test
    void updateGameSavesSeatsAndBoardState() throws Exception {
        int gameID = gameDAO.createGame("first");
        ChessGame played = gameDAO.getGame(gameID).game();
        played.makeMove(new ChessMove(new ChessPosition(2, 5), new ChessPosition(4, 5), null));

        gameDAO.updateGame(new GameData(gameID, "bob", "alice", "first", played));

        GameData updated = gameDAO.getGame(gameID);
        assertEquals("bob", updated.whiteUsername());
        assertEquals("alice", updated.blackUsername());
        assertEquals(played, updated.game());
        assertEquals(ChessGame.TeamColor.BLACK, updated.game().getTeamTurn());
    }

    @Test
    void updateGameLeavesOtherGamesAlone() throws DataAccessException {
        int first = gameDAO.createGame("first");
        int second = gameDAO.createGame("second");

        gameDAO.updateGame(new GameData(first, "bob", null, "first", new ChessGame()));

        assertNull(gameDAO.getGame(second).whiteUsername());
    }

    @Test
    void updateGameMissing() {
        GameData missing = new GameData(999, null, null, "ghost", new ChessGame());

        assertThrows(DataAccessException.class, () -> gameDAO.updateGame(missing));
    }

    @Test
    void clearRemovesGames() throws DataAccessException {
        gameDAO.createGame("first");

        gameDAO.clear();

        assertTrue(gameDAO.listGames().isEmpty());
    }

    @Test
    void clearEmptyTable() {
        assertDoesNotThrow(() -> gameDAO.clear());
    }
}