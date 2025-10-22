package service;
public class BadRequestException extends Exception {
    public BadRequestException(String message) {
        super("Error: bad request");
    }
}