
package websocket.messages;

import model.GameData;

import java.util.Objects;
public class ServerMessage {
    ServerMessageType serverMessageType;
    GameData game;
    String errorMessage;
    String message;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    public ServerMessage(ServerMessageType type, GameData game) {
        this.serverMessageType = type;
        this.game = game;
    }

    public ServerMessage(ServerMessageType type, String message) {
        this.serverMessageType = type;
        this.message = message;
    }

    public ServerMessage(ServerMessageType type, String errorMessage, boolean isError) {
        this.serverMessageType = type;
        if (isError) {
            this.errorMessage = errorMessage;  // For error, use errorMessage
        } else {
            this.message = errorMessage;  // For notifications, use message
        }
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public String getMessage() {
        return message;
    }

    public GameData getGame() {
        return this.game;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage that)) {
            return false;
        }
        return getServerMessageType() == that.getServerMessageType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType());
    }

    // subclasses
    public static class LoadGameMessage extends ServerMessage {
        public LoadGameMessage(GameData gameData) {
            super(ServerMessageType.LOAD_GAME, gameData);
        }

        public GameData getGame() {
            return this.game;
        }
    }


    public static class ErrorMessage extends ServerMessage {
        public ErrorMessage(String errorMessage) {
            super(ServerMessageType.ERROR, errorMessage, true);
        }

        public String getErrorMessage() {
            return this.errorMessage; // grab error message
        }
    }


    public static class NotificationMessage extends ServerMessage {
        public NotificationMessage(String message) {
            super(ServerMessageType.NOTIFICATION, message);
        }

        public String getNotificationMessage() {
            return this.message;
        }
    }
}
