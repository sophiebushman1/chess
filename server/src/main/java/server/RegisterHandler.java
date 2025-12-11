package server;

import com.google.gson.Gson;
import service.UserService;
import spark.Request;
import spark.Response;
import spark.Route;

public class RegisterHandler implements Route {

    private final UserService userService;
    private final Gson gson = new Gson();

    public RegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            var body = gson.fromJson(req.body(), UserService.RegisterRequest.class);
            var result = userService.register(body);

            res.status(200);
            return gson.toJson(result);

        } catch (exception.BadRequestException e) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: bad request"));
        } catch (exception.AlreadyTakenException e) {
            res.status(403);
            return gson.toJson(new ErrorMessage("Error: already taken"));
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    record ErrorMessage(String message) {}
}
