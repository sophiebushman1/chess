package websocket;

import com.google.gson.Gson;
import io.javalin.websocket.WsContext;
import dataaccess.DataAccess;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.*;

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
                case MAKE_MOVE -> send(ctx, new ErrorMessage("move handling not implemented"));
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
