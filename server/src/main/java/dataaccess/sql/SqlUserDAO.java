package dataaccess.sql;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import dataaccess.UserDAO;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class SqlUserDAO implements UserDAO {

    @Override
    public void clear() throws DataAccessException {
        var statement = "DELETE FROM user";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("unable to clear users", ex);
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        var statement = "INSERT INTO user (username, password, email) VALUES (?, ?, ?)";
        var hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, user.username());
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, user.email());
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("unable to create user", ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        var statement = "SELECT username, password, email FROM user WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var preparedStatement = conn.prepareStatement(statement)) {
            preparedStatement.setString(1, username);
            try (var rs = preparedStatement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserData(rs.getString("username"), rs.getString("password"), rs.getString("email"));
            }
        } catch (SQLException ex) {
            throw new DataAccessException("unable to read user", ex);
        }
    }
}