package server;

import com.google.gson.Gson;
import service.GameService;
import spark.Request;
import spark.Response;
import spark.Route;

public class CreateGameHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public CreateGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) {
        String token = req.headers("Authorization");

        try {
            var body = gson.fromJson(req.body(), GameService.CreateGameRequest.class);
            var result = gameService.createGame(token, body);

            res.status(200);
            return gson.toJson(result);

        } catch (exception.UnauthorizedException e) {
            res.status(401);
            return gson.toJson(new ErrorMessage("Error: unauthorized"));
        } catch (exception.BadRequestException e) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: bad request"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    record ErrorMessage(String message) {}
}
