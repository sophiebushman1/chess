package websocket.messages;

public abstract class ServerMessage {

    public enum ServerMessageType {
        LOAD_GAME,
        NOTIFICATION,
        ERROR
    }

    private final ServerMessageType serverMessageType;

    protected ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    public ServerMessageType getServerMessageType() {
        return serverMessageType;
    }
}
