package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.*;
import io.javalin.http.Context;
import service.*;

import java.util.Map;

public class Server {

    private final Javalin javalin;

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Register your endpoints and exception handlers here.
        UserDAO userDAO = new MemoryUserDAO();
        GameDAO gameDAO = new MemoryGameDAO();
        AuthDAO authDAO = new MemoryAuthDAO();

        ClearHandler clearHandler = new ClearHandler(new ClearService(userDAO, gameDAO, authDAO));

        javalin.delete("/db", clearHandler::clear);

        javalin.exception(BadRequestException.class, (e, ctx) -> sendError(ctx, 400, e.getMessage()));
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
