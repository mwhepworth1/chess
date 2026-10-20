package service;

public class BadRequestException extends Exception {
    public BadRequestException() {
        super("Error: bad request");
    }
}
