package exception;
public class AlreadyTakenException extends ResponseException {


    public AlreadyTakenException(String message) {
        super(403, "Error: " + message);
    }

    public AlreadyTakenException() {
        this("already taken");
    }

}