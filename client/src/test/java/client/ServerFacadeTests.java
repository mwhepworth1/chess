package client;

import chess.ChessGame;
import com.sun.net.httpserver.HttpServer;
import model.*;
import org.junit.jupiter.api.*;
import server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearDatabase() throws ResponseException {
        facade.clear();
    }

    // Registers a standard test user and returns their auth token
    private String registerUser() throws ResponseException {
        RegisterResult result = facade.register(new RegisterRequest("player1", "password", "p1@email.com"));
        return result.authToken();
    }


    // ---------- clear ----------

    @Test
    public void clearPositive() throws ResponseException {
        registerUser();
        facade.clear();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.login(new LoginRequest("player1", "password")));
        assertEquals(401, ex.getStatusCode());
    }


    // ---------- server unreachable ----------

    @Test
    public void serverUnreachable() {
        // nothing is listening on port 1, so the request can't connect
        ServerFacade badFacade = new ServerFacade(1);

        ResponseException ex = assertThrows(ResponseException.class, () -> badFacade.clear());
        assertEquals(0, ex.getStatusCode());
    }

    @Test
    public void errorResponseNotJson() throws IOException {
        // a fake server that always answers with a plain-text (non-JSON) 500 error
        HttpServer fakeServer = HttpServer.create(new InetSocketAddress(0), 0);
        fakeServer.createContext("/", exchange -> {
            byte[] body = "Internal Server Error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        fakeServer.start();

        try {
            ServerFacade fakeFacade = new ServerFacade(fakeServer.getAddress().getPort());

            ResponseException ex = assertThrows(ResponseException.class, () -> fakeFacade.clear());
            assertEquals(500, ex.getStatusCode());
            assertEquals("Request failed", ex.getMessage());
        } finally {
            fakeServer.stop(0);
        }
    }


    // ---------- register ----------

    @Test
    public void registerPositive() throws ResponseException {
        RegisterResult result = facade.register(new RegisterRequest("player1", "password", "p1@email.com"));

        assertEquals("player1", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    public void registerNegative() throws ResponseException {
        registerUser();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.register(new RegisterRequest("player1", "other", "other@email.com")));
        assertEquals(403, ex.getStatusCode());
    }


    // ---------- login ----------

    @Test
    public void loginPositive() throws ResponseException {
        registerUser();

        LoginResult result = facade.login(new LoginRequest("player1", "password"));

        assertEquals("player1", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    public void loginNegative() throws ResponseException {
        registerUser();

        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.login(new LoginRequest("player1", "wrongPassword")));
        assertEquals(401, ex.getStatusCode());
    }


    // ---------- logout ----------

    @Test
    public void logoutPositive() throws ResponseException {
        String authToken = registerUser();

        assertDoesNotThrow(() -> facade.logout(authToken));

        // the old token should no longer work
        assertThrows(ResponseException.class, () -> facade.listGames(authToken));
    }

    @Test
    public void logoutNegative() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.logout("fakeToken"));
        assertEquals(401, ex.getStatusCode());
    }


    // ---------- createGame ----------

    @Test
    public void createGamePositive() throws ResponseException {
        String authToken = registerUser();

        CreateGameResult result = facade.createGame(authToken, new CreateGameRequest("myGame"));

        assertTrue(result.gameID() > 0);
    }

    @Test
    public void createGameNegative() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.createGame("fakeToken", new CreateGameRequest("myGame")));
        assertEquals(401, ex.getStatusCode());
    }


    // ---------- listGames ----------

    @Test
    public void listGamesPositive() throws ResponseException {
        String authToken = registerUser();
        facade.createGame(authToken, new CreateGameRequest("game1"));
        facade.createGame(authToken, new CreateGameRequest("game2"));

        ListGamesResult result = facade.listGames(authToken);

        assertEquals(2, result.games().size());
    }

    @Test
    public void listGamesNegative() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.listGames("fakeToken"));
        assertEquals(401, ex.getStatusCode());
    }


    // ---------- joinGame ----------

    @Test
    public void joinGamePositive() throws ResponseException {
        String authToken = registerUser();
        int gameID = facade.createGame(authToken, new CreateGameRequest("myGame")).gameID();

        assertDoesNotThrow(() -> facade.joinGame(authToken, new JoinGameRequest(ChessGame.TeamColor.WHITE, gameID)));

        GameData game = facade.listGames(authToken).games().iterator().next();
        assertEquals("player1", game.whiteUsername());
    }

    @Test
    public void joinGameNegative() throws ResponseException {
        String authToken = registerUser();
        int gameID = facade.createGame(authToken, new CreateGameRequest("myGame")).gameID();
        facade.joinGame(authToken, new JoinGameRequest(ChessGame.TeamColor.WHITE, gameID));

        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.joinGame(authToken, new JoinGameRequest(ChessGame.TeamColor.WHITE, gameID)));
        assertEquals(403, ex.getStatusCode());
    }

}
