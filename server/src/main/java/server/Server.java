package server;

import spark.Spark;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class Server {

    private final WebSocketServer socketServer;

    public Server() {
        this.socketServer = new WebSocketServer();
    }

    public int run(int port) {
        Spark.port(port);
        Spark.staticFiles.location("web");

        Spark.webSocket("/ws", socketServer);
        System.out.println("websocket active on /ws");

        addRoutes();
        Spark.init();

        initDB();

        Spark.awaitInitialization();
        return Spark.port();
    }

    private void addRoutes() {
        Spark.post("/user", new RegisterHandler());
        Spark.post("/session", new LoginHandler());
        Spark.delete("/session", new LogoutHandler());

        Spark.get("/game", new ListGamesHandler());
        Spark.post("/game", new CreateGameHandler());
        Spark.put("/game", new JoinGameHandler());

        Spark.delete("/db", new ClearHandler());
    }

    private void initDB() {
        try {
            DatabaseManager.createDatabase();
        } catch (DataAccessException e) {
            System.err.println("db init failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
