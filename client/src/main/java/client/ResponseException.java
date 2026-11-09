package client;

// Thrown by ServerFacade when a request fails.
// statusCode is the HTTP status from the server, or 0 if the server couldn't be reached.
public class ResponseException extends Exception {

    private final int statusCode;

    public ResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
