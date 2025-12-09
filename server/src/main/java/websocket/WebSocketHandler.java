package websocket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import io.javalin.Javalin;
import io.javalin.websocket.*;
import model.AuthData;
import model.GameData;
import chess.ChessMove;
import chess.ChessPosition;
import chess.ChessGame;
import websocket.messages.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler {

    private static final Gson gson = new Gson();

    // gameID -> session map
    private static final Map<Integer, Map<String, WsContext>> connections = new ConcurrentHashMap<>();

    // used to stop moves after end
    private static final Set<Integer> finishedGames = ConcurrentHashMap.newKeySet();

    private final DataAccess dataAccess;

    public WebSocketHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public static void addWebSocketEndpoints(Javalin app, DataAccess dataAccess) {
        WebSocketHandler handler = new WebSocketHandler(dataAccess);

        app.ws("/ws", ws -> {
            ws.onConnect(handler::onConnect);
            ws.onMessage(ctx -> handler.onMessage(ctx.message(), ctx));
            ws.onClose(handler::onClose);
        });
    }

    private void onConnect(WsConnectContext ctx) {
        // client sends CONNECT later
    }

    private void onClose(WsCloseContext ctx) {
        String sid = ctx.sessionId();
        for (var entry : connections.entrySet()) {
            var sessionMap = entry.getValue();
            if (sessionMap != null && sessionMap.remove(sid) != null) {
                broadcastNotification(entry.getKey(), "a user disconnected");
            }
        }
    }

    private void onMessage(String raw, WsMessageContext ctx) {
        if (raw == null || raw.isBlank()) {
            sendError(ctx, "invalid message");
            return;
        }

        JsonObject root;
        try {
            root = gson.fromJson(raw, JsonObject.class);
        } catch (Exception ex) {
            sendError(ctx, "invalid message");
            return;
        }
        if (root == null) {
            sendError(ctx, "invalid message");
            return;
        }

        String commandType = extractStringField(root, "commandType");
        if (commandType == null) commandType = extractStringField(root, "type");
        if (commandType == null) {
            sendError(ctx, "missing commandType");
            return;
        }

        String authToken = extractStringField(root, "authToken");
        Integer gameID = extractIntField(root, "gameID");

        try {
            switch (commandType.toUpperCase(Locale.ROOT)) {
                case "CONNECT" -> handleConnect(ctx, authToken, gameID, root);
                case "MAKE_MOVE" -> handleMakeMove(ctx, authToken, gameID, root);
                case "LEAVE" -> handleLeave(ctx, authToken, gameID);
                case "RESIGN" -> handleResign(ctx, authToken, gameID);
                default -> sendError(ctx, "unknown command");
            }
        } catch (DataAccessException ex) {
            sendError(ctx, "data access error: " + ex.getMessage());
        }
    }

    private void handleConnect(WsMessageContext ctx, String authToken, Integer gameID, JsonObject raw)
            throws DataAccessException {

        if (authToken == null || gameID == null) {
            sendError(ctx, "missing authToken or gameID");
            return;
        }

        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            sendError(ctx, "invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(gameID);
        if (game == null) {
            sendError(ctx, "game not found");
            return;
        }

        connections.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>())
                .put(ctx.sessionId(), ctx);

        ctx.send(gson.toJson(new LoadGameMessage(game)));

        String username = auth.username();
        String role;
        if (username.equals(game.whiteUsername())) role = "WHITE";
        else if (username.equals(game.blackUsername())) role = "BLACK";
        else role = "OBSERVER";

        String note = username + " joined as " + role.toLowerCase();
        broadcastNotificationToOthers(gameID, note, ctx);
    }

    private void handleMakeMove(WsMessageContext ctx, String authToken, Integer gameID, JsonObject raw)
            throws DataAccessException {

        if (authToken == null || gameID == null) {
            sendError(ctx, "missing authToken or gameID");
            return;
        }

        if (finishedGames.contains(gameID)) {
            sendError(ctx, "game is over");
            return;
        }

        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            sendError(ctx, "invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(gameID);
        if (game == null) {
            sendError(ctx, "game not found");
            return;
        }

        JsonObject moveObj = raw.has("move") && raw.get("move").isJsonObject()
                ? raw.getAsJsonObject("move") : null;
        if (moveObj == null) {
            sendError(ctx, "missing move");
            return;
        }

        ChessMove move;
        try {
            move = parseChessMove(moveObj);
        } catch (Exception ex) {
            sendError(ctx, "invalid move");
            return;
        }

        ChessGame chess = game.game();
        try {
            chess.makeMove(move);
        } catch (Exception ex) {
            sendError(ctx, "error: " + (ex.getMessage() == null ? "invalid move" : ex.getMessage()));
            return;
        }

        GameData updated = new GameData(game.gameID(), game.whiteUsername(), game.blackUsername(),
                game.gameName(), chess);
        dataAccess.updateGame(updated);

        broadcastLoadGame(gameID, updated);

        String note = auth.username() + " moved from " + move.getStartPosition()
                + " to " + move.getEndPosition();
        broadcastNotificationToOthers(gameID, note, ctx);

        if (chess.isInCheck(ChessGame.TeamColor.WHITE)) broadcastNotification(gameID, "white in check");
        if (chess.isInCheck(ChessGame.TeamColor.BLACK)) broadcastNotification(gameID, "black in check");

        if (chess.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                chess.isInCheckmate(ChessGame.TeamColor.BLACK)) {
            broadcastNotification(gameID, "checkmate");
            finishedGames.add(gameID);
        } else if (chess.isInStalemate(ChessGame.TeamColor.WHITE) ||
                chess.isInStalemate(ChessGame.TeamColor.BLACK)) {
            broadcastNotification(gameID, "stalemate");
            finishedGames.add(gameID);
        }
    }

    private void handleLeave(WsMessageContext ctx, String authToken, Integer gameID)
            throws DataAccessException {

        if (authToken == null || gameID == null) {
            sendError(ctx, "missing authToken or gameID");
            return;
        }

        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            sendError(ctx, "invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(gameID);
        if (game == null) {
            sendError(ctx, "game not found");
            return;
        }

        String white = game.whiteUsername();
        String black = game.blackUsername();
        String user = auth.username();

        if (user.equals(white)) white = null;
        if (user.equals(black)) black = null;

        GameData updated = new GameData(game.gameID(), white, black, game.gameName(), game.game());
        dataAccess.updateGame(updated);

        connections.getOrDefault(gameID, Map.of()).remove(ctx.sessionId());
        broadcastNotificationToOthers(gameID, user + " left the game", ctx);
    }

    private void handleResign(WsMessageContext ctx, String authToken, Integer gameID)
            throws DataAccessException {

        if (authToken == null || gameID == null) {
            sendError(ctx, "missing authToken or gameID");
            return;
        }

        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            sendError(ctx, "invalid auth token");
            return;
        }

        GameData game = dataAccess.getGame(gameID);
        if (game == null) {
            sendError(ctx, "game not found");
            return;
        }

        finishedGames.add(gameID);
        broadcastNotification(gameID, auth.username() + " resigned");
    }

    private void broadcastLoadGame(int gameID, GameData game) {
        Map<String, WsContext> sessionMap = connections.getOrDefault(gameID, Map.of());
        String json = gson.toJson(new LoadGameMessage(game));
        for (WsContext c : sessionMap.values()) {
            try { c.send(json); } catch (Exception ignored) {}
        }
    }

    private void broadcastNotificationToOthers(int gameID, String message, WsMessageContext exclude) {
        Map<String, WsContext> sessionMap = connections.getOrDefault(gameID, Map.of());
        String json = gson.toJson(new NotificationMessage(message));
        for (var entry : sessionMap.entrySet()) {
            WsContext c = entry.getValue();
            if (!c.sessionId().equals(exclude.sessionId())) {
                try { c.send(json); } catch (Exception ignored) {}
            }
        }
    }

    private void broadcastNotification(int gameID, String message) {
        Map<String, WsContext> sessionMap = connections.getOrDefault(gameID, Map.of());
        String json = gson.toJson(new NotificationMessage(message));
        for (WsContext c : sessionMap.values()) {
            try { c.send(json); } catch (Exception ignored) {}
        }
    }

    private void sendError(WsMessageContext ctx, String message) {
        ctx.send(gson.toJson(new ErrorMessage(message)));
    }

    private static String extractStringField(JsonObject obj, String name) {
        JsonElement el = obj.get(name);
        if (el == null || el.isJsonNull()) return null;
        try { return el.getAsString(); } catch (Exception ex) { return null; }
    }

    private static Integer extractIntField(JsonObject obj, String name) {
        JsonElement el = obj.get(name);
        if (el == null || el.isJsonNull()) return null;
        try { return el.getAsInt(); } catch (Exception ex) { return null; }
    }

    private static ChessMove parseChessMove(JsonObject moveObj) {
        JsonObject start = moveObj.getAsJsonObject("start");
        JsonObject end = moveObj.getAsJsonObject("end");

        int sr = start.get("row").getAsInt();
        int sc = start.get("col").getAsInt();
        int er = end.get("row").getAsInt();
        int ec = end.get("col").getAsInt();

        return new ChessMove(new ChessPosition(sr, sc), new ChessPosition(er, ec), null);
    }
}
