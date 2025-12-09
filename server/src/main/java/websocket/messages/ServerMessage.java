package websocket.messages;

/**
 * Base class for server messages. Tests expect a JSON field named "serverMessageType".
 */
public class ServerMessage {
    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessageType serverMessageType;

    public ServerMessage(ServerMessageType t) {
        this.serverMessageType = t;
    }

    public ServerMessage() {}
}
