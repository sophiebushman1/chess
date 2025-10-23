package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDAO;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import service.*;

import java.util.Map;

public class Server {

    private final Javalin javalin;
    private final UserService userService;
    private final GameService gameService;
    private final ClearService clearService;
    private final Gson gson = new Gson();

    public Server() {
        // ✅ Configure Javalin to use Jackson for JSON (fixes mapper warnings)
        javalin = Javalin.create(config -> {
            config.staticFiles.add("/web");
            config.jsonMapper(new JavalinJackson());
        });

        // DAO and Services
        DataAccess dataAccess = new MemoryDAO();
        userService = new UserService(dataAccess);
        gameService = new GameService(dataAccess);
        clearService = new ClearService(dataAccess);

        // Register endpoints
        javalin.delete("/db", this::handleClearApplication);
        javalin.post("/user", this::handleRegister);
        javalin.post("/session", this::handleLogin);
        javalin.delete("/session", this::handleLogout);
        javalin.get("/game", this::handleListGames);
        javalin.post("/game", this::handleCreateGame);
        javalin.put("/game", this::handleJoinGame);

        // Global exception handler
        javalin.exception(Exception.class, this::handleException);
    }

    // --- Handlers ---

    private void handleClearApplication(Context ctx) throws DataAccessException {
        clearService.clear();
        ctx.status(HttpStatus.OK).result("{}");
    }

    private void handleRegister(Context ctx) throws DataAccessException, BadRequestException, AlreadyTakenException {
        RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
        AuthResult result = userService.register(req);
        ctx.status(HttpStatus.OK).json(result);
    }

    private void handleLogin(Context ctx) throws DataAccessException, BadRequestException, UnauthorizedException {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
        AuthResult result = userService.login(req);
        ctx.status(HttpStatus.OK).json(result);
    }

    private void handleLogout(Context ctx) throws DataAccessException, UnauthorizedException {
        String authToken = ctx.header("Authorization");
        userService.logout(authToken);
        ctx.status(HttpStatus.OK).result("{}");
    }

    private void handleListGames(Context ctx) throws DataAccessException, UnauthorizedException {
        String authToken = ctx.header("Authorization");
        ListGamesResult result = gameService.listGames(authToken);
        ctx.status(HttpStatus.OK).json(result);
    }

    private void handleCreateGame(Context ctx) throws DataAccessException, UnauthorizedException, BadRequestException {
        String authToken = ctx.header("Authorization");
        CreateGameRequest req = ctx.bodyAsClass(CreateGameRequest.class);
        CreateGameResult result = gameService.createGame(authToken, req);
        ctx.status(HttpStatus.OK).json(result);
    }

    private void handleJoinGame(Context ctx) throws DataAccessException, UnauthorizedException, BadRequestException, AlreadyTakenException {
        String authToken = ctx.header("Authorization");
        JoinGameRequest req = ctx.bodyAsClass(JoinGameRequest.class);
        gameService.joinGame(authToken, req);
        ctx.status(HttpStatus.OK).result("{}");
    }

    private void handleException(Exception e, Context ctx) {
        int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();

        if (e instanceof BadRequestException || e instanceof com.google.gson.JsonSyntaxException) {
            status = HttpStatus.BAD_REQUEST.getCode();
        } else if (e instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED.getCode();
        } else if (e instanceof AlreadyTakenException) {
            status = HttpStatus.FORBIDDEN.getCode();
        } else if (e instanceof DataAccessException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();
        }

        String message = e.getMessage() != null ? e.getMessage() : "Error: Server error";
        ctx.status(status).json(Map.of("message", message));
    }

    // --- Run and stop methods ---
    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }

    public static class Main {
        public static void main(String[] args) {
            Server server = new Server();
            server.run(8080);

            System.out.println("♕ 240 Chess Server");
        }
    }
}
