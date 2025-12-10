package websocket.messages;

public class ErrorMessage extends ServerMessage {
    // tests expect the field to be called errorMessage
    public String errorMessage;

    public ErrorMessage(String msg) {
        super(ServerMessageType.ERROR);
        this.errorMessage = msg;
    }
}
