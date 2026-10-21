package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {
    private static final String TOKEN = "valid-token";

    private GameDAO gameDAO;
    private GameService gameService;

    @BeforeEach
    void setUp() throws DataAccessException {
        gameDAO = new MemoryGameDAO();
        AuthDAO authDAO = new MemoryAuthDAO();
        authDAO.createAuth(new AuthData(TOKEN, "bob"));
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
}