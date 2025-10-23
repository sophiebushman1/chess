package server;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import web.ErrorResponse;
import service.UserService;
import service.AuthResult;
import service.LoginRequest;
import service.RegisterRequest;
import service.ServiceExceptions.*;
import dataaccess.MemoryDAO;
import chess.*;

public class Server {

    private Javalin app;
    private final UserService userService;

    // --- No-arg constructor for Main ---
    public Server() {
        MemoryDAO dao = new MemoryDAO();
        this.userService = new UserService(dao);
    }

    // --- Existing constructor ---
    public Server(UserService userService) {
        this.userService = userService;
    }

    public int run(int port) {
        Gson gson = new Gson();
        app = Javalin.create(config -> {
            config.jsonMapper(new GsonJavalinJsonMapper(gson));
            config.staticFiles.add(staticFiles -> staticFiles.directory = "/web");
        });

        // Global Exception Handler
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            e.printStackTrace();
            ctx.json(new ErrorResponse("Error: Internal server error: " + e.getMessage()));
        });

        app.before(ctx -> ctx.header("Access-Control-Allow-Origin", "*"));

        // --- Existing endpoints ---
        app.post("/user", this::handleRegister);
        app.post("/session", this::handleLogin);
        app.delete("/session", this::handleLogout);
        app.delete("/db", this::handleClear);

        // --- New /game endpoint ---
        app.post("/game", this::handleCreateGame);

        app.start(port);
        return app.port();
    }

    public void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
    }

    private void handleRegister(Context ctx) {
        try {
            RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
            if (req.username() == null || req.password() == null || req.email() == null ||
                    req.username().isEmpty() || req.password().isEmpty() || req.email().isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Error: Missing username, password, or email."));
                return;
            }

            AuthResult result = userService.register(req);
            ctx.status(200);
            ctx.json(result);

        } catch (BadRequestException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("Error: " + e.getMessage()));
        } catch (AlreadyTakenException e) {
            ctx.status(403);
            ctx.json(new ErrorResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("Error: Internal server error: " + e.getMessage()));
        }
    }

    private void handleLogin(Context ctx) {
        try {
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            if (req.username() == null || req.password() == null ||
                    req.username().isEmpty() || req.password().isEmpty()) {
                ctx.status(400);
                ctx.json(new ErrorResponse("Error: Missing username or password."));
                return;
            }

            AuthResult result = userService.login(req);
            ctx.status(200);
            ctx.json(result);

        } catch (BadRequestException e) {
            ctx.status(400);
            ctx.json(new ErrorResponse("Error: " + e.getMessage()));
        } catch (UnauthorizedException e) {
            ctx.status(401);
            ctx.json(new ErrorResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("Error: Internal server error: " + e.getMessage()));
        }
    }

    private void handleLogout(Context ctx) {
        String authToken = ctx.header("authorization");
        if (authToken == null || authToken.isEmpty()) {
            ctx.status(401);
            ctx.json(new ErrorResponse("Error: Missing or invalid auth token."));
            return;
        }

        try {
            userService.logout(authToken);
            ctx.status(200);
        } catch (UnauthorizedException e) {
            ctx.status(401);
            ctx.json(new ErrorResponse("Error: " + e.getMessage()));
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("Error: Internal server error: " + e.getMessage()));
        }
    }

    private void handleClear(Context ctx) {
        try {
            userService.clear(); // calls DAO clear
            ctx.status(200);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("Error: Internal server error: " + e.getMessage()));
        }
    }

    // --- New /game handler ---
    private void handleCreateGame(Context ctx) {
        try {
            // TODO: replace this with actual game creation logic if needed
            ChessGame newGame = new ChessGame(); // placeholder class
            ctx.status(201);
            ctx.json(newGame);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new ErrorResponse("Error: " + e.getMessage()));
        }
    }
}
