package dataaccess.sql;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import dataaccess.GameDAO;
import model.GameData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class SqlGameDAO implements GameDAO {
    private static final Gson GSON = new Gson();
    private static final String SELECT_GAMES =
            "SELECT gameID, whiteUsername, blackUsername, gameName, game FROM game";

    @Override
    public void clear() throws DataAccessException {
        var statement = "TRUNCATE TABLE game";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("unable to clear games", ex);
        }
    }

    @Override
    public int createGame(String gameName) throws DataAccessException {
        var statement = "INSERT INTO game (gameName, game) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, gameName);
            preparedStatement.setString(2, GSON.toJson(new ChessGame()));
            preparedStatement.executeUpdate();
            try (var rs = preparedStatement.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new DataAccessException("unable to create game");
                }
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new DataAccessException("unable to create game", ex);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        var statement = SELECT_GAMES + " WHERE gameID = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setInt(1, gameID);
            try (var rs = preparedStatement.executeQuery()) {
                return rs.next() ? readGame(rs) : null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("unable to read game", ex);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        var statement = SELECT_GAMES + " ORDER BY gameID";
        var games = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement);
             var rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                games.add(readGame(rs));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("unable to list games", ex);
        }
        return games;
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        var statement = """
                UPDATE game SET whiteUsername = ?, blackUsername = ?, gameName = ?, game = ?
                WHERE gameID = ?
                """;
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, game.whiteUsername());
            preparedStatement.setString(2, game.blackUsername());
            preparedStatement.setString(3, game.gameName());
            preparedStatement.setString(4, GSON.toJson(game.game()));
            preparedStatement.setInt(5, game.gameID());
            if (preparedStatement.executeUpdate() == 0) {
                throw new DataAccessException("game " + game.gameID() + " does not exist");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("unable to update game", ex);
        }
    }

    private GameData readGame(ResultSet rs) throws SQLException {
        return new GameData(
                rs.getInt("gameID"),
                rs.getString("whiteUsername"),
                rs.getString("blackUsername"),
                rs.getString("gameName"),
                GSON.fromJson(rs.getString("game"), ChessGame.class));
    }
}