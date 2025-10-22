package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDAO;
import io.javalin.Javalin;

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
        javalin = Javalin.create(config -> config.staticFiles.add("/web"));

        // DAO and Services
        DataAccess dataAccess = new MemoryDAO(); // use MemoryDAO
        userService = new UserService(dataAccess);

        gameService = new GameService(dataAccess);
        clearService = new ClearService(dataAccess);

        // register endpoints
        javalin.delete("/db", this::handleClearApplication);
        javalin.post("/user", this::handleRegister);
        javalin.post("/session", this::handleLogin);
        javalin.delete("/session", this::handleLogout);
        javalin.get("/game", this::handleListGames);
        javalin.post("/game", this::handleCreateGame);
        javalin.put("/game", this::handleJoinGame);

        // global exception handler
        javalin.exception(Exception.class, this::handleException);
    }

    // Handlers

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
        // default er status
        int status = HttpStatus.INTERNAL_SERVER_ERROR.getCode();

        // HTTP status based on the exception type
        if (e instanceof BadRequestException || e instanceof com.google.gson.JsonSyntaxException) {
            status = HttpStatus.BAD_REQUEST.getCode(); // 400
        } else if (e instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED.getCode(); // 401
        } else if (e instanceof AlreadyTakenException) {

            status = HttpStatus.FORBIDDEN.getCode(); // 403
        } else if (e instanceof DataAccessException) {
            status = HttpStatus.INTERNAL_SERVER_ERROR.getCode(); // 500
        } else {

            status = HttpStatus.INTERNAL_SERVER_ERROR.getCode(); // 500
        }

        // error message
        String message = e.getMessage() != null ? e.getMessage() : "Error: Server error";

        ctx.status(status).json(Map.of("message", message));
    }


    //run and stop

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}