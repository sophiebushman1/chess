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
            DatabaseManager.createDatabase();
            this.dataAccess = new SQLDataAccess();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing database", e);
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
        if (app != null) {
            app.stop();
        }
    }

    private void registerUser(Context ctx) throws ResponseException {
        RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
        requireNonEmpty(req.username(), req.password(), req.email());
        try {
            if (dataAccess.getUser(req.username()) != null) {
                throw new AlreadyTakenException("Error: already taken");
            }
            dataAccess.createUser(new UserData(req.username(), req.password(), req.email()));
            String token = createAuthToken(req.username());
            ctx.status(200).json(new AuthResult(token, req.username()));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void loginUser(Context ctx) throws ResponseException {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
        requireNonEmpty(req.username(), req.password());
        try {
            UserData user = dataAccess.getUser(req.username());
            if (user == null || !BCrypt.checkpw(req.password(), user.password())) {
                throw new UnauthorizedException("Error: unauthorized");
            }
            String token = createAuthToken(req.username());
            ctx.status(200).json(new AuthResult(token, req.username()));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void logoutUser(Context ctx) throws ResponseException {
        String token = requireAuth(ctx);
        try {
            if (dataAccess.getAuth(token) == null) {
                throw new UnauthorizedException("Error: unauthorized");
            }
            dataAccess.deleteAuth(token);
            ctx.status(200);
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void createGame(Context ctx) throws ResponseException {
        String token = requireAuth(ctx);
        requireValidAuth(token);

        CreateGameRequest req = ctx.bodyAsClass(CreateGameRequest.class);
        requireNonEmpty(req.gameName());

        try {
            GameData created = dataAccess.createGame(
                    new GameData(0, null, null, req.gameName(), new chess.ChessGame())
            );
            ctx.status(200).json(new CreateGameResult(created.gameID()));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void listGames(Context ctx) throws ResponseException {
        String token = requireAuth(ctx);
        requireValidAuth(token);

        try {
            var games = dataAccess.listGames().stream()
                    .map(g -> new GameInfo(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()))
                    .toArray(GameInfo[]::new);
            ctx.status(200).json(new ListGamesResult(games));
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void joinGame(Context ctx) throws ResponseException {
        String token = requireAuth(ctx);
        requireValidAuth(token);

        JoinGameRequest req = ctx.bodyAsClass(JoinGameRequest.class);
        requireNonEmpty(req.playerColor());

        try {
            GameData game = dataAccess.getGame(req.gameID());
            if (game == null) { throw new BadRequestException("Error: bad request"); }

            String username = dataAccess.getAuth(token).username();
            GameData updated = switch (req.playerColor().toUpperCase()) {
                case "WHITE" -> {
                    if (game.whiteUsername() != null) { throw new AlreadyTakenException("Error: already taken"); }
                    yield new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
                }
                case "BLACK" -> {
                    if (game.blackUsername() != null) { throw new AlreadyTakenException("Error: already taken"); }
                    yield new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
                }
                default -> throw new BadRequestException("Error: bad request");
            };
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

    private String createAuthToken(String username) throws DataAccessException {
        String token = UUID.randomUUID().toString();
        dataAccess.createAuth(new AuthData(token, username));
        return token;
    }

    private String requireAuth(Context ctx) throws ResponseException {
        String token = ctx.header("authorization");
        if (token == null || token.isEmpty()) { throw new UnauthorizedException("Error: unauthorized"); }
        return token;
    }

    private void requireValidAuth(String token) throws ResponseException {
        try {
            if (dataAccess.getAuth(token) == null) { throw new UnauthorizedException("Error: unauthorized"); }
        } catch (DataAccessException e) {
            throw new ResponseException(500, "Error: " + e.getMessage());
        }
    }

    private void requireNonEmpty(String... fields) throws ResponseException {
        for (String f : fields) {
            if (f == null || f.isEmpty()) {
                throw new BadRequestException("Error: bad request");
            }
        }
    }

    // Records
    public record RegisterRequest(String username, String password, String email) {}
    public record LoginRequest(String username, String password) {}
    public record AuthResult(String authToken, String username) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) {}
    public record ListGamesResult(GameInfo[] games) {}
    public record JoinGameRequest(String playerColor, int gameID) {}
    public record ErrorResponse(String message) {}

    // JSON Mapper
    private static class GsonJsonMapper implements JsonMapper {
        private final Gson gson;

        public GsonJsonMapper(Gson gson) {
            this.gson = gson;
        }

        @Override
        public <T> T fromJsonString(String json, Type type) {
            return gson.fromJson(json, type);
        }

        @Override
        public String toJsonString(Object obj, Type type) {
            return gson.toJson(obj, type);
        }

        @Override
        public <T> T fromJsonStream(InputStream json, Type type) {
            try (InputStreamReader reader = new InputStreamReader(json)) {
                return gson.fromJson(reader, type);
            } catch (IOException e) {
                throw new InternalServerErrorResponse("Invalid JSON");
            }
        }

    }


}
