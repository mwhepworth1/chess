package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import model.AuthData;

final class AuthHelper {
    private AuthHelper() {
    }

    static AuthData authenticate(AuthDAO authDAO, String authToken)
            throws UnauthorizedException, DataAccessException {
        AuthData auth = authToken == null ? null : authDAO.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException();
        }
        return auth;
    }
}