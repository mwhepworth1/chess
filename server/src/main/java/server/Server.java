package server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dataaccess.*;
import dataaccess.sql.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.*;

import java.util.Map;

public class Server {

    private final Javalin javalin;

    public Server() {
        try {
            DatabaseManager.initializeDatabase();
        } catch (DataAccessException ex) {
            throw new RuntimeException("Unable to initialize database", ex);
        }

        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Register your endpoints and exception handlers here.
        UserDAO userDAO = new SqlUserDAO();
        GameDAO gameDAO = new SqlGameDAO();
        AuthDAO authDAO = new SqlAuthDAO();

        ClearHandler clearHandler = new ClearHandler(new ClearService(userDAO, gameDAO, authDAO));
        UserHandler userHandler = new UserHandler(new UserService(userDAO, authDAO));
        GameHandler gameHandler = new GameHandler(new GameService(gameDAO, authDAO));

        javalin.delete("/db", clearHandler::clear);
        javalin.post("/user", userHandler::register);
        javalin.post("/session", userHandler::login);
        javalin.delete("/session", userHandler::logout);
        javalin.post("/game", gameHandler::createGame);
        javalin.get("/game", gameHandler::listGames);
        javalin.put("/game", gameHandler::joinGame);

        javalin.exception(BadRequestException.class, (e, ctx) -> sendError(ctx, 400, e.getMessage()));
        javalin.exception(JsonSyntaxException.class, (e, ctx) -> sendError(ctx, 400, "Error: bad request"));
        javalin.exception(UnauthorizedException.class, (e, ctx) -> sendError(ctx, 401, e.getMessage()));
        javalin.exception(AlreadyTakenException.class, (e, ctx) -> sendError(ctx, 403, e.getMessage()));
        javalin.exception(Exception.class, (e, ctx) -> sendError(ctx, 500, "Error: " + e.getMessage()));
    }

    private static void sendError(Context ctx, int status, String message) {
        ctx.status(status).result(new Gson().toJson(Map.of("message", message)));
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
