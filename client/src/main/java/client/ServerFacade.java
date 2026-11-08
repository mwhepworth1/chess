package client;

import com.google.gson.Gson;
import model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ServerFacade {

    private final String serverUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        serverUrl = "http://localhost:" + port;
    }

    public void clear() throws ResponseException {
        sendRequest("DELETE", "/db", null, null);
    }

    public RegisterResult register(RegisterRequest request) throws ResponseException {
        String json = sendRequest("POST", "/user", request, null);
        return gson.fromJson(json, RegisterResult.class);
    }

    public LoginResult login(LoginRequest request) throws ResponseException {
        String json = sendRequest("POST", "/session", request, null);
        return gson.fromJson(json, LoginResult.class);
    }

    public void logout(String authToken) throws ResponseException {
        sendRequest("DELETE", "/session", null, authToken);
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest request) throws ResponseException {
        String json = sendRequest("POST", "/game", request, authToken);
        return gson.fromJson(json, CreateGameResult.class);
    }

    public ListGamesResult listGames(String authToken) throws ResponseException {
        String json = sendRequest("GET", "/game", null, authToken);
        return gson.fromJson(json, ListGamesResult.class);
    }

    public void joinGame(String authToken, JoinGameRequest request) throws ResponseException {
        sendRequest("PUT", "/game", request, authToken);
    }

    // Sends a request to the server and returns the response body as a JSON string.
    // Throws a ResponseException if the server returns an error or can't be reached.
    private String sendRequest(String method, String path, Object body, String authToken) throws ResponseException {
        HttpRequest.BodyPublisher requestBody;
        if (body == null) {
            requestBody = HttpRequest.BodyPublishers.noBody();
        } else {
            requestBody = HttpRequest.BodyPublishers.ofString(gson.toJson(body));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + path))
                .method(method, requestBody);
        if (authToken != null) {
            builder.header("authorization", authToken);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ResponseException(0, "Unable to reach the server");
        }

        if (response.statusCode() != 200) {
            throw new ResponseException(response.statusCode(), getErrorMessage(response.body()));
        }
        return response.body();
    }

    // The server sends errors as {"message": "Error: ..."}
    private String getErrorMessage(String json) {
        try {
            Map errorBody = gson.fromJson(json, Map.class);
            return (String) errorBody.get("message");
        } catch (Exception e) {
            return "Request failed";
        }
    }
}
