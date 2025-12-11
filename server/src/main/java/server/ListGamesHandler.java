package server;

import com.google.gson.Gson;
import service.GameService;
import spark.Request;
import spark.Response;
import spark.Route;

public class ListGamesHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public ListGamesHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) {
        String token = req.headers("Authorization");

        try {
            var result = gameService.listGames(token);
            res.status(200);
            return gson.toJson(result);

        } catch (exception.UnauthorizedException e) {
            res.status(401);
            return gson.toJson(new ErrorMessage("Error: unauthorized"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    record ErrorMessage(String message) {}
}
