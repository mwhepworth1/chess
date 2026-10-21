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

    public LoginResult login(LoginRequest req) throws BadRequestException, UnauthorizedException, DataAccessException {
        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            throw new BadRequestException();
        }
        UserData user = userDAO.getUser(req.username());
        if (user == null || !user.password().equals(req.password())) {
            throw new UnauthorizedException();
        }
        String authToken = createAuth(req.username());
        return new LoginResult(req.username(), authToken);
    }

    public void logout(String authToken) throws UnauthorizedException, DataAccessException {
        authenticate(authToken);
        authDAO.deleteAuth(authToken);
    }

    private AuthData authenticate(String authToken) throws UnauthorizedException, DataAccessException {
        AuthData auth = authToken == null ? null : authDAO.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException();
        }
        return auth;
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
