package websocket.commands;

import chess.ChessMove;

public class UserGameCommand {

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }

    private CommandType commandType;
    private String authToken;
    private int gameID;
    private ChessMove move;

    // CONNECT / LEAVE / RESIGN
    public UserGameCommand(CommandType commandType, String authToken, int gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
    }

    // MAKE_MOVE
    public UserGameCommand(CommandType commandType, String authToken, int gameID, ChessMove move) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.move = move;
    }

    public CommandType getCommandType() { return commandType; }
    public String getAuthToken() { return authToken; }
    public int getGameID() { return gameID; }
    public ChessMove getMove() { return move; }
}
