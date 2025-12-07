package websocket;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import exception.UnauthorizedException;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {
    private final DataAccess dataAccess;
    private final GameService gameService;
    private final Map<Integer, Map<String, Session>> gameSessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public WebSocketHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
        this.gameService = new GameService(dataAccess);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {}

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        removeSession(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        UserGameCommand cmd = gson.fromJson(message, UserGameCommand.class);

        try {
            switch (cmd.getCommandType()) {
                case CONNECT -> connect(session, cmd);
                case MAKE_MOVE -> sendError(session, "not implemented");
                case LEAVE -> leave(session, cmd);
                case RESIGN -> sendError(session, "not implemented");
            }
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    private void connect(Session session, UserGameCommand cmd) throws IOException, UnauthorizedException, DataAccessException {
        gameService.listGames(cmd.getAuthToken());

        GameData gd = dataAccess.getGame(cmd.getGameID());
        if (gd == null) {
            sendError(session, "game not found");
            return;
        }

        gameSessions.putIfAbsent(cmd.getGameID(), new ConcurrentHashMap<>());
        gameSessions.get(cmd.getGameID()).put(session.getRemoteAddress().toString(), session);

        var msg = new websocket.messages.LoadGameMessage(gd);
        session.getRemote().sendString(gson.toJson(msg));

        broadcast(cmd.getGameID(),
                new websocket.messages.NotificationMessage("a user connected"),
                session);
    }

    private void leave(Session session, UserGameCommand cmd) throws IOException {
        removeSession(session);
        broadcast(cmd.getGameID(),
                new websocket.messages.NotificationMessage("a user left"),
                session);
    }

    private void removeSession(Session session) {
        for (var game : gameSessions.values()) {
            game.values().removeIf(s -> s.equals(session));
        }
    }

    private void broadcast(int gameID, ServerMessage msg, Session except) throws IOException {
        var map = gameSessions.get(gameID);
        if (map == null) return;

        String json = gson.toJson(msg);
        for (Session s : map.values()) {
            if (s != null && s.isOpen() && s != except) {
                s.getRemote().sendString(json);
            }
        }
    }

    private void sendError(Session session, String err) throws IOException {
        var msg = new websocket.messages.ErrorMessage(err);
        session.getRemote().sendString(gson.toJson(msg));
    }
}
