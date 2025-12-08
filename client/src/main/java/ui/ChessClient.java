package ui;

import client.ServerFacade;
import chess.*;
import websocket.commands.*;
import websocket.messages.*;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class ChessClient {

    private final ServerFacade server;
    private String authToken = null;

    private final Map<Integer, ServerFacade.GameInfo> gameNumberMap = new HashMap<>();

    private final Gson gson = new Gson();
    private WebSocket ws;
    private ChessGame currentGame = new ChessGame();
    private ChessGame.TeamColor myTeam = ChessGame.TeamColor.WHITE;
    private int currentGameID = -1;

    public ChessClient(int port) {
        server = new ServerFacade(port);
    }

    public void run() {
        System.out.println("♕ Welcome to the 240 Chess Client!");
        preLoginLoop();
    }

    private void preLoginLoop() {
        Scanner scan = new Scanner(System.in);

        while (authToken == null) {
            System.out.print("\n[prelogin] > ");
            String cmd = scan.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "help" -> printPreloginHelp();
                case "register" -> handleRegister(scan);
                case "login" -> handleLogin(scan);
                case "quit" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }

        postLoginLoop();
    }

    private void postLoginLoop() {
        Scanner scan = new Scanner(System.in);

        while (authToken != null) {
            System.out.print("\n[menu] > ");
            String cmd = scan.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "help" -> printPostloginHelp();
                case "logout" -> handleLogout();
                case "creategame" -> handleCreateGame(scan);
                case "listgames" -> handleListGames();
                case "play" -> handleJoin(scan, true);
                case "observe" -> handleObserve(scan);
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }

        preLoginLoop();
    }

    private void handleRegister(Scanner scan) {
        System.out.print("username: ");
        String u = scan.nextLine().trim();

        System.out.print("password: ");
        String p = scan.nextLine().trim();

        System.out.print("email: ");
        String e = scan.nextLine().trim();

        var res = server.register(u, p, e);

        if (res.getMessage() != null) {
            System.out.println("Error: " + res.getMessage());
            return;
        }

        System.out.println("Registered & logged in!");
        authToken = res.getAuthToken();
    }

    private void handleLogin(Scanner scan) {
        System.out.print("username: ");
        String u = scan.nextLine().trim();

        System.out.print("password: ");
        String p = scan.nextLine().trim();

        var res = server.login(u, p);

        if (res.getMessage() != null) {
            System.out.println("Error: " + res.getMessage());
            return;
        }

        System.out.println("Logged in!");
        authToken = res.getAuthToken();
    }

    private void handleLogout() {
        var res = server.logout(authToken);

        if (res.getMessage() != null) {
            System.out.println("Error: " + res.getMessage());
            return;
        }

        System.out.println("Logged out!");
        authToken = null;
    }

    private void handleCreateGame(Scanner scan) {
        System.out.print("game name: ");
        String name = scan.nextLine().trim();

        var res = server.createGame(name, authToken);

        if (res.getMessage() != null) {
            System.out.println("Error: " + res.getMessage());
            return;
        }

        System.out.println("Created game: " + name);
    }

    private void handleListGames() {
        var res = server.listGames(authToken);

        if (res.getMessage() != null) {
            System.out.println("Error: " + res.getMessage());
            return;
        }

        gameNumberMap.clear();

        System.out.println("\nGames:");
        var games = res.getGames();
        for (int i = 0; i < games.length; i++) {
            int displayNum = i + 1;
            ServerFacade.GameInfo g = games[i];
            String whiteName = g.getWhiteUsername() != null ? g.getWhiteUsername() : "(empty)";
            String blackName = g.getBlackUsername() != null ? g.getBlackUsername() : "(empty)";
            System.out.printf("%d. %s | WHITE: %s | BLACK: %s%n", displayNum, g.getGameName(), whiteName, blackName);
            gameNumberMap.put(displayNum, g);
        }

        if (games.length == 0) System.out.println("(no games)");
    }

    private void handleJoin(Scanner scan, boolean playing) {
        System.out.println("\nGames:");
        handleListGames();

        System.out.print("game number: ");
        String input = scan.nextLine().trim();
        int num;

        try {
            num = Integer.parseInt(input);
        } catch (Exception e) {
            System.out.println("Invalid number.");
            return;
        }

        if (!gameNumberMap.containsKey(num)) {
            System.out.println("No game with that number.");
            return;
        }

        ServerFacade.GameInfo g = gameNumberMap.get(num);
        int gameID = g.getGameID();
        currentGameID = gameID;

        if (playing) {
            String color = askColor(scan);
            myTeam = color.equals("WHITE") ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
            var res = server.joinGame(color, gameID, authToken);

            if (res.getMessage() != null) {
                System.out.println("Error: " + res.getMessage());
                return;
            }

            System.out.println("Joined game!");
            connectWebSocket(gameID);
            moveLoop(scan);
        } else {
            System.out.println("Observing game!");
            connectWebSocket(gameID);
            moveLoop(scan);
        }
    }

    private void handleObserve(Scanner scan) {
        handleJoin(scan, false);
    }

    private String askColor(Scanner scan) {
        while (true) {
            System.out.print("color (WHITE/BLACK): ");
            String c = scan.nextLine().trim().toUpperCase();
            if (c.equals("WHITE") || c.equals("BLACK")) return c;
            System.out.println("Invalid color.");
        }
    }

    private void connectWebSocket(int gameID) {
        try {
            ws = HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:8080/ws"), new WSListener())
                    .join();

            UserGameCommand connect = new UserGameCommand(
                    UserGameCommand.CommandType.CONNECT,
                    authToken,
                    gameID
            );

            ws.sendText(gson.toJson(connect), true);

        } catch (Exception e) {
            System.out.println("WebSocket error: " + e.getMessage());
        }
    }

    private class WSListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            String json = data.toString();
            ServerMessage msg = gson.fromJson(json, ServerMessage.class);

            if (msg instanceof LoadGameMessage load) {
                currentGame = load.getGame().game();
                currentGameID = load.getGame().gameID();
                drawBoard(myTeam);
            }

            if (msg instanceof NotificationMessage note) {
                System.out.println("** " + note.getMessage() + " **");
            }

            ws.request(1);
            return null;
        }
    }

    private void moveLoop(Scanner scan) {
        while (true) {
            System.out.print("move (e2e4) or 'quit'> ");
            String s = scan.nextLine().trim();

            if (s.equalsIgnoreCase("quit")) return;
            if (s.length() < 4) {
                System.out.println("Invalid format.");
                continue;
            }

            ChessPosition start = ChessPosition.fromAlgebraic(s.substring(0, 2));
            ChessPosition end = ChessPosition.fromAlgebraic(s.substring(2, 4));

            MakeMoveCommand cmd = new MakeMoveCommand(
                    authToken,
                    currentGameID,
                    start,
                    end,
                    null
            );

            ws.sendText(gson.toJson(cmd), true);
        }
    }

    private void drawBoard(ChessGame.TeamColor perspective) {
        System.out.println("\n-- Chess Board (" + perspective + ") --");

        ChessBoard board = currentGame.getBoard();

        for (int r = 8; r >= 1; r--) {
            System.out.print(r + "  ");
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = board.getPiece(pos);
                if (p == null) {
                    System.out.print(". ");
                } else {
                    System.out.print(getPieceSymbol(p) + " ");
                }
            }
            System.out.println();
        }

        System.out.println("\n   a b c d e f g h");
    }

    private char getPieceSymbol(ChessPiece p) {
        switch (p.getPieceType()) {
            case KING ->   { return p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'K' : 'k'; }
            case QUEEN ->  { return p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'Q' : 'q'; }
            case ROOK ->   { return p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'R' : 'r'; }
            case BISHOP -> { return p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'B' : 'b'; }
            case KNIGHT -> { return p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'N' : 'n'; }
            case PAWN ->   { return p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'P' : 'p'; }
        }
        return '?';
    }

    private void printPreloginHelp() {
        System.out.println("""
                Commands:
                  help        show this menu
                  register    create a user and auto-login
                  login       log into an existing account
                  quit        exit program
                """);
    }

    private void printPostloginHelp() {
        System.out.println("""
                Commands:
                  help        show this menu
                  logout      sign out
                  creategame  make a new game
                  listgames   show all games
                  play        join a game as a player
                  observe     view a game as spectator
                """);
    }

    public static void main(String[] args) {
        new ChessClient(8080).run();
    }
}
