package server;

import com.google.gson.Gson;
import io.javalin.http.Context;
import service.*;

public class GameHandler {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void createGame(Context ctx) throws Exception {
        CreateGameRequest req = gson.fromJson(ctx.body(), CreateGameRequest.class);
        CreateGameResult res = gameService.createGame(ctx.header("authorization"), req);
        ctx.status(200).result(gson.toJson(res));
    }
    public void listGames(Context ctx) throws Exception {
        ListGamesResult res = gameService.listGames(ctx.header("authorization"));
        ctx.status(200).result(gson.toJson(res));
    }
    public void joinGame(Context ctx) throws Exception {
        JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);
        gameService.joinGame(ctx.header("authorization"), req);
        ctx.status(200).result("{}");
    }
}
