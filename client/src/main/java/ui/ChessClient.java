package ui;

import client.ServerFacade;
import service.CreateGameRequest;
import service.JoinGameRequest;
import model.UserData;

public class ChessClient {
    private final ServerFacade server;
    private String authToken;

    public ChessClient(int port) {
        server = new ServerFacade(port);
    }

    public void run() {
        clear();

        // Register user
        var reg = server.register("sophia", "password123", "sophia@example.com");
        System.out.println("Register status: " + server.getStatusCode());
        System.out.println("Register message: " + reg.getMessage());
        authToken = reg.getAuthToken();

        // Login
        var login = server.login("sophia", "password123");
        System.out.println("Login status: " + server.getStatusCode());
        System.out.println("Login message: " + login.getMessage());
        authToken = login.getAuthToken();

        // Create game
        var game = server.createGame("My Cool Game", authToken);
        System.out.println("Create game status: " + server.getStatusCode());
        System.out.println("Create game message: " + game.getMessage());
        System.out.println("New Game ID: " + game.getGameID());

        // List games
        var list = server.listGames(authToken);
        System.out.println("List games status: " + server.getStatusCode());
        System.out.println("List games message: " + list.getMessage());
        for (var g : list.getGames()) {
            System.out.println("Game: " + g.getGameName() + " (ID " + g.getGameID() + ")");
        }

        // Join game
        if (list.getGames().length > 0) {
            int id = list.getGames()[0].getGameID();
            var join = server.joinGame("WHITE", id, authToken);
            System.out.println("Join game status: " + server.getStatusCode());
            System.out.println("Join game message: " + join.getMessage());
        }

        // Logout
        var logout = server.logout(authToken);
        System.out.println("Logout status: " + server.getStatusCode());
        System.out.println("Logout message: " + logout.getMessage());
    }

    private void clear() {
        var res = server.clear();
        System.out.println("Clear DB status: " + server.getStatusCode());
        System.out.println("Clear DB message: " + res.getMessage());
    }

    public static void main(String[] args) {
        var client = new ChessClient(8080);
        client.run();
    }
}
