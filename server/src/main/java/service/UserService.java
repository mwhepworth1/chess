package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    public RegisterResult register(RegisterRequest req) throws BadRequestException, AlreadyTakenException, DataAccessException {
        if (req == null || isBlank(req.username()) || isBlank(req.password()) || isBlank(req.email())) {
            throw new BadRequestException();
        }
        if (userDAO.getUser(req.username()) != null) {
            throw new AlreadyTakenException();
        }
        userDAO.createUser(new UserData(req.username(), req.password(), req.email()));
        String authToken = createAuth(req.username());
        return new RegisterResult(req.username(), authToken);
    }

    private String createAuth(String username) throws DataAccessException {
        String authToken = UUID.randomUUID().toString();
        authDAO.createAuth((new AuthData(authToken, username)));
        return authToken;
    }

    private boolean isBlank(String val) {
        return val == null || val.isBlank();
    }
}
