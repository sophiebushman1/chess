package server;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.json.JsonMapper;
import dataaccess.*;
import model.*;
import exception.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

public class Server {
    private final Gson gson = new Gson();
    private Javalin app;
    private final DataAccess dataAccess;

    public Server() {
        try {
            this.dataAccess = new SQLDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException("Failed to initialize SQLDataAccess", e);
        }
    }

    public int run(int port) {
        app = Javalin.create(config -> {
            config.jsonMapper(new GsonJsonMapper(gson));
            config.staticFiles.add(staticFiles -> staticFiles.directory = "/web");
        });

        app.exception(ResponseException.class, (e, ctx) -> {
            ctx.status(e.getStatusCode());
            ctx.json(new ErrorResponse(e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            e.printStackTrace();
            ctx.json(new ErrorResponse("Error: Internal server error"));
        });

        app.before(ctx -> ctx.header("Access-Control-Allow-Origin", "*"));

        app.post("/user", this::registerUser);
        app.post("/session", this::loginUser);
        app.delete("/session", this::logoutUser);
        app.post("/game", this::createGame);
        app.get("/game", this::listGames);
        app.put("/game", this::joinGame);
        app.delete("/db", this::clearDB);

        app.start(port);
        return app.port();
    }

    public void stop() {
        if (app != null) app.stop();
    }

    private void registerUser(Context ctx) throws ResponseException {
        RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
        if (req.username() == null || req.password() == null || req.email() == null ||
                req.username().isEmpty() || req.password().isEmpty() || req.email().isEmpty()) {
            throw new BadRequestException("Error: bad request");
        }

        try {
            if (dataAccess.getUser(req.username()) != null) {
                throw new AlreadyTakenException();
            }

            dataAccess.createUser(new UserData(req.username(), req.password(), req.email()));
            String token = UUID.randomUUID().toString();
            dataAccess.createAuth(new AuthData(token, req.username()));
            ctx.status(200);
            ctx.json(new AuthResult(token, req.username()));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void loginUser(Context ctx) throws ResponseException {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);

        // validate request before any DB calls
        if (req.username() == null || req.password() == null ||
                req.username().isEmpty() || req.password().isEmpty()) {
            throw new BadRequestException("Error: bad request");
        }

        try {
            UserData user = dataAccess.getUser(req.username());
            // compare provided password with hashed password in DB using BCrypt
            if (user == null || !BCrypt.checkpw(req.password(), user.password())) {
                throw new UnauthorizedException();
            }

            String token = UUID.randomUUID().toString();
            dataAccess.createAuth(new AuthData(token, req.username()));
            ctx.status(200);
            ctx.json(new AuthResult(token, req.username()));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void logoutUser(Context ctx) throws ResponseException {
        String token = ctx.header("authorization");
        if (token == null || token.isEmpty()) throw new UnauthorizedException();

        try {
            AuthData auth = dataAccess.getAuth(token);
            if (auth == null) throw new UnauthorizedException();
            dataAccess.deleteAuth(token);
            ctx.status(200);
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void createGame(Context ctx) throws ResponseException {
        try {
            String token = ctx.header("authorization");
            if (token == null || dataAccess.getAuth(token) == null) throw new UnauthorizedException();

            CreateGameRequest req = ctx.bodyAsClass(CreateGameRequest.class);
            if (req.gameName() == null || req.gameName().isEmpty()) throw new BadRequestException("Error: bad request");

            GameData created = dataAccess.createGame(new GameData(0, null, null, req.gameName(), new chess.ChessGame()));
            ctx.status(200);
            ctx.json(new CreateGameResult(created.gameID()));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }
    private void listGames(Context ctx) throws ResponseException {
        String token = ctx.header("authorization");
        try {
            if (token == null || dataAccess.getAuth(token) == null) throw new UnauthorizedException();

            var games = dataAccess.listGames().stream()
                    .map(g -> new GameInfo(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()))
                    .toArray(GameInfo[]::new);

            ctx.status(200);
            ctx.json(new ListGamesResult(games));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void joinGame(Context ctx) throws ResponseException {
        String token = ctx.header("authorization");
        if (token == null) throw new UnauthorizedException();

        try {
            if (dataAccess.getAuth(token) == null) throw new UnauthorizedException();
            JoinGameRequest req = ctx.bodyAsClass(JoinGameRequest.class);
            // treat null/empty playerColor as bad request
            if (req.playerColor() == null || req.playerColor().isEmpty()) {
                throw new BadRequestException("Error: bad request");
            }

            GameData game = dataAccess.getGame(req.gameID());
            if (game == null) throw new BadRequestException("Error: bad request");

            String username = dataAccess.getAuth(token).username();
            GameData updated;

            if (req.playerColor().equalsIgnoreCase("WHITE")) {
                if (game.whiteUsername() != null) throw new AlreadyTakenException();
                updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            } else if (req.playerColor().equalsIgnoreCase("BLACK")) {
                if (game.blackUsername() != null) throw new AlreadyTakenException();
                updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            } else {
                throw new BadRequestException("Error: bad request");
            }

            dataAccess.updateGame(updated);
            ctx.status(200);
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void clearDB(Context ctx) {
        try {
            dataAccess.clear();
            ctx.status(200);
        } catch (DataAccessException e) {
            ctx.status(500).json(new ErrorResponse("Error: " + e.getMessage()));
        }
    }


    public record RegisterRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResult(String authToken, String username) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) {}
    public record ListGamesResult(GameInfo[] games) {}
    public record JoinGameRequest(String playerColor, int gameID) {}
    public record ErrorResponse(String message) {}

    private static class GsonJsonMapper implements JsonMapper {
        private final Gson gson;
        public GsonJsonMapper(Gson gson) { this.gson = gson; }

        public <T> T fromJsonString(String json, Type type) { return gson.fromJson(json, type); }
        public String toJsonString(Object obj, Type type) { return gson.toJson(obj, type); }

        public <T> T fromJsonStream(InputStream json, Type type) {
            try (InputStreamReader reader = new InputStreamReader(json)) {
                return gson.fromJson(reader, type);
            } catch (IOException e) {
                throw new InternalServerErrorResponse("Invalid JSON");
            }
        }

        public void toJsonStream(Object obj, Type type, OutputStream stream) {
            try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {
                gson.toJson(obj, type, writer);
            } catch (IOException e) {
                throw new InternalServerErrorResponse("Error writing JSON");
            }
        }
    }
}
