package server;

import com.google.gson.Gson;
import service.GameService;
import spark.Request;
import spark.Response;
import spark.Route;

public class JoinGameHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public JoinGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) {
        String token = req.headers("Authorization");

        try {
            var body = gson.fromJson(req.body(), GameService.JoinGameRequest.class);
            gameService.joinGame(token, body);

            res.status(200);
            return gson.toJson(new Object());

        } catch (exception.UnauthorizedException e) {
            res.status(401);
            return gson.toJson(new ErrorMessage("Error: unauthorized"));
        } catch (exception.BadRequestException e) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: bad request"));
        } catch (exception.AlreadyTakenException e) {
            res.status(403);
            return gson.toJson(new ErrorMessage("Error: spot already taken"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    record ErrorMessage(String message) {}
}
