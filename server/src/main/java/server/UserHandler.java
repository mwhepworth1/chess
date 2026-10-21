package server;

import com.google.gson.Gson;
import io.javalin.http.Context;
import service.*;

public class UserHandler {
    private final UserService userService;
    private final Gson gson = new Gson();

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public void register(Context ctx) throws Exception {
        RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);
        RegisterResult result = userService.register(req);
        ctx.status(200).result(gson.toJson(result));
    }

    public void login(Context ctx) throws Exception {
        LoginRequest req = gson.fromJson(ctx.body(), LoginRequest.class);
        LoginResult res = userService.login(req);
        ctx.status(200).result(gson.toJson(res));
    }

    public void logout(Context ctx) throws Exception {
        userService.logout(ctx.header("authorization"));
        ctx.status(200).result("{}");
    }
}