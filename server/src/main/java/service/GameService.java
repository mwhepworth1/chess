package service;

import dataaccess.*;

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req) throws UnauthorizedException, BadRequestException, DataAccessException {
        AuthHelper.authenticate(authDAO, authToken);
        if (req == null || req.gameName() == null || req.gameName().isBlank()) {
            throw new BadRequestException();
        }
        int gameID = gameDAO.createGame(req.gameName());
        return new CreateGameResult(gameID);
    }

    public ListGamesResult listGames(String authToken) throws UnauthorizedException, DataAccessException {
        AuthHelper.authenticate(authDAO, authToken);
        return new ListGamesResult(gameDAO.listGames());
    }
}
