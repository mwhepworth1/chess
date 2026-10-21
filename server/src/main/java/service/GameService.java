package service;

import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {
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

    public void joinGame(String authToken, JoinGameRequest req)
            throws UnauthorizedException, BadRequestException, AlreadyTakenException, DataAccessException {
        AuthData auth = AuthHelper.authenticate(authDAO, authToken);
        if (req == null || req.playerColor() == null || req.gameID() == null) {
            throw new BadRequestException();
        }
        GameData game = gameDAO.getGame(req.gameID());
        if (game == null) {
            throw new BadRequestException();
        }
        gameDAO.updateGame(claimSeat(game, req.playerColor(), auth.username()));
    }

    private GameData claimSeat(GameData game, ChessGame.TeamColor color, String username)
            throws AlreadyTakenException {
        if (color == ChessGame.TeamColor.WHITE) {
            if (game.whiteUsername() != null) {
                throw new AlreadyTakenException();
            }
            return new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
        }
        if (game.blackUsername() != null) {
            throw new AlreadyTakenException();
        }
        return new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
    }
}
