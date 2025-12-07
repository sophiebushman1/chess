package websocket;

import com.google.gson.Gson;
import io.javalin.websocket.WsContext;
import dataaccess.DataAccess;
import model.AuthData;
import model.GameData;
import chess.ChessGame;
import chess.ChessMove;
import websocket.commands.UserGameCommand;
import websocket.commands.MakeMoveCommand;
import websocket.messages.*;
import dataaccess.DataAccessException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

    private final DataAccess dataAccess;
    private final Gson gson = new Gson();
    private final Map<Integer, Set<WsContext>> gameConnections = new ConcurrentHashMap<>();

    public WebSocketHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void register(io.javalin.Javalin app) {
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {});
            ws.onClose(this::removeConnection);
            ws.onMessage(ctx -> handleMessage(ctx, ctx.message()));
        });
    }

    private void handleMessage(WsContext ctx, String raw) {
        UserGameCommand cmd = gson.fromJson(raw, UserGameCommand.class);

        try {
            AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
            if (auth == null) {
                send(ctx, new ErrorMessage("unauthorized"));
                return;
            }

            GameData game = dataAccess.getGame(cmd.getGameID());
            if (game == null) {
                send(ctx, new ErrorMessage("invalid gameID"));
                return;
            }

            switch (cmd.getCommandType()) {
                case CONNECT -> connect(ctx, cmd.getGameID(), game);
                case LEAVE -> leave(ctx, cmd.getGameID());
                case RESIGN -> resign(cmd.getGameID(), auth.username());
                case MAKE_MOVE -> {
                    MakeMoveCommand moveCmd = gson.fromJson(raw, MakeMoveCommand.class);
                    GameData gameData = dataAccess.getGame(moveCmd.getGameID()); // get the current game
                    if (gameData == null) {
                        send(ctx, new ErrorMessage("invalid gameID"));
                        return;
                    }
                    makeMove(ctx, moveCmd, auth.username(), gameData);
                }


            }

        } catch (Exception e) {
            send(ctx, new ErrorMessage("error"));
        }
    }

    private void connect(WsContext ctx, int gameID, GameData game) {
        gameConnections.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(ctx);
        send(ctx, new LoadGameMessage(game));
        broadcast(gameID, new NotificationMessage("a player connected"));
    }

    private void leave(WsContext ctx, int gameID) {
        removeConnection(ctx);
        broadcast(gameID, new NotificationMessage("a player left"));
    }

    private void resign(int gameID, String username) {
        broadcast(gameID, new NotificationMessage(username + " resigned"));
    }

    private void makeMove(WsContext ctx, MakeMoveCommand cmd, String username, GameData gameData) {
        ChessMove move = cmd.toChessMove();
        ChessGame game = gameData.game();

        try {
            // attempt to make the move
            game.makeMove(move);
        } catch (Exception e) {
            send(ctx, new ErrorMessage("invalid move: " + e.getMessage()));
            return;
        }

        // update database with new game state
        GameData updated = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );

        try {
            dataAccess.updateGame(updated);
        } catch (DataAccessException e) {
            send(ctx, new ErrorMessage("internal server error"));
            return;
        }

        // broadcast updated board
        broadcast(gameData.gameID(), new LoadGameMessage(updated));

        // check/checkmate/stalemate notifications
        ChessGame.TeamColor currentTurn = game.getTeamTurn();
        if (game.isInCheckmate(currentTurn)) {
            broadcast(gameData.gameID(), new NotificationMessage(username + " wins by checkmate!"));
        } else if (game.isInStalemate(currentTurn)) {
            broadcast(gameData.gameID(), new NotificationMessage("Game ended in stalemate"));
        } else if (game.isInCheck(currentTurn)) {
            broadcast(gameData.gameID(), new NotificationMessage(currentTurn + " is in check!"));
        } else {
            broadcast(gameData.gameID(), new NotificationMessage(username + " made a move"));
        }
    }



    private void removeConnection(WsContext ctx) {
        gameConnections.values().forEach(set -> set.remove(ctx));
    }

    private void send(WsContext ctx, ServerMessage msg) {
        ctx.send(gson.toJson(msg));
    }

    private void broadcast(int gameID, ServerMessage msg) {
        var set = gameConnections.get(gameID);
        if (set != null) {
            String json = gson.toJson(msg);
            set.forEach(c -> c.send(json));
        }
    }
}
