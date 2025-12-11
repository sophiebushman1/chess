package server;

import com.google.gson.Gson;
import service.UserService;
import spark.Request;
import spark.Response;
import spark.Route;

public class LoginHandler implements Route {

    private final UserService userService;
    private final Gson gson = new Gson();

    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            var loginReq = gson.fromJson(req.body(), UserService.LoginRequest.class);
            var result = userService.login(loginReq);

            res.status(200);
            return gson.toJson(result);

        } catch (exception.BadRequestException e) {
            res.status(400);
            return gson.toJson(new ErrorMessage("Error: bad request"));
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
