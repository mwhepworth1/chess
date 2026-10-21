package service;

import chess.ChessGame;
import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {
    private static final String TOKEN = "valid-token";
    private static final String OTHER_TOKEN = "other-token";

    private GameDAO gameDAO;
    private GameService gameService;

    @BeforeEach
    void setUp() throws DataAccessException {
        gameDAO = new MemoryGameDAO();
        AuthDAO authDAO = new MemoryAuthDAO();
        authDAO.createAuth(new AuthData(TOKEN, "bob"));
        authDAO.createAuth(new AuthData(OTHER_TOKEN, "alice"));
        gameService = new GameService(gameDAO, authDAO);
    }

    @Test
    void createGameSuccess() throws Exception {
        CreateGameResult result = gameService.createGame(TOKEN, new CreateGameRequest("myGame"));

        GameData stored = gameDAO.getGame(result.gameID());
        assertNotNull(stored);
        assertEquals("myGame", stored.gameName());
        assertNull(stored.whiteUsername());
        assertNull(stored.blackUsername());
    }

    @Test
    void createGameUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> gameService.createGame("bad-token", new CreateGameRequest("myGame")));
    }

    @Test
    void createGameMissingName() {
        assertThrows(BadRequestException.class,
                () -> gameService.createGame(TOKEN, new CreateGameRequest(null)));
    }

    @Test
    void listGamesSuccess() throws Exception {
        gameService.createGame(TOKEN, new CreateGameRequest("game1"));
        gameService.createGame(TOKEN, new CreateGameRequest("game2"));

        ListGamesResult result = gameService.listGames(TOKEN);

        assertEquals(2, result.games().size());
    }

    @Test
    void listGamesUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> gameService.listGames("bad-token"));
    }

    @Test
    void joinGameSuccess() throws Exception {
        int gameID = gameService.createGame(TOKEN, new CreateGameRequest("myGame")).gameID();

        gameService.joinGame(TOKEN, new JoinGameRequest(ChessGame.TeamColor.WHITE, gameID));

        GameData stored = gameDAO.getGame(gameID);
        assertEquals("bob", stored.whiteUsername());
        assertNull(stored.blackUsername());
    }

    @Test
    void joinGameColorTaken() throws Exception {
        int gameID = gameService.createGame(TOKEN, new CreateGameRequest("myGame")).gameID();
        gameService.joinGame(OTHER_TOKEN, new JoinGameRequest(ChessGame.TeamColor.BLACK, gameID));

        assertThrows(AlreadyTakenException.class,
                () -> gameService.joinGame(TOKEN, new JoinGameRequest(ChessGame.TeamColor.BLACK, gameID)));
    }

    @Test
    void joinGameWhiteTaken() throws Exception {
        int gameID = gameService.createGame(TOKEN, new CreateGameRequest("myGame")).gameID();
        gameService.joinGame(OTHER_TOKEN, new JoinGameRequest(ChessGame.TeamColor.WHITE, gameID));

        assertThrows(AlreadyTakenException.class,
                () -> gameService.joinGame(TOKEN, new JoinGameRequest(ChessGame.TeamColor.WHITE, gameID)));
    }

    @Test
    void joinGameBadGameID() {
        assertThrows(BadRequestException.class,
                () -> gameService.joinGame(TOKEN, new JoinGameRequest(ChessGame.TeamColor.WHITE, 999)));
    }

    @Test
    void joinGameMissingColor() throws Exception {
        int gameID = gameService.createGame(TOKEN, new CreateGameRequest("myGame")).gameID();

        assertThrows(BadRequestException.class,
                () -> gameService.joinGame(TOKEN, new JoinGameRequest(null, gameID)));
    }

    @Test
    void joinGameUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> gameService.joinGame("bad-token", new JoinGameRequest(ChessGame.TeamColor.WHITE, 1)));
    }
}