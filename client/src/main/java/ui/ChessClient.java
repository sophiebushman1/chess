package ui;

import client.ServerFacade;
import chess.ChessGame;
import chess.ChessPiece;
import java.util.*;

public class ChessClient {

    private final ServerFacade server;
    private String authToken = null;

    // keeps mapping displayedNumber to the real gameID
    private final Map<Integer, ServerFacade.GameInfo> gameNumberMap = new HashMap<>();

    public ChessClient(int port) {
        server = new ServerFacade(port);
    }

    public void run() {
        System.out.println("♕ Welcome to the 240 Chess Client!");
        preLoginLoop();
    }

    // pre login
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

    // post login
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
        authToken = null; // return to prelogin
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

        if (playing) {
            String color = askColor(scan);
            var res = server.joinGame(color, gameID, authToken);

            if (res.getMessage() != null) {
                System.out.println("Error: " + res.getMessage());
                return;
            }

            System.out.println("Joined game!");
            drawBoard(ChessGame.TeamColor.WHITE);
        } else {
            // observe (don't join)
            System.out.println("Observing game!");
            drawBoard(ChessGame.TeamColor.WHITE);
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

    //board looks
    private void drawBoard(ChessGame.TeamColor perspective) {
        System.out.println("\n-- Chess Board (" + perspective + ") --");

        // empty board
        ChessPiece[][] board = new ChessPiece[8][8];

        // white  perspective
        // Row 0 = top black major
        // Row 1 = black pawns
        board[0] = new ChessPiece[]{
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.ROOK),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KNIGHT),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.BISHOP),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.QUEEN),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KING),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.BISHOP),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.KNIGHT),
                new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.ROOK)
        };
        Arrays.fill(board[1], new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN));

        // empty middle rows
        for (int r = 2; r <= 5; r++) {
            Arrays.fill(board[r], null);
        }

        // Row 6 = white pawns
        // Row 7 = whit major
        Arrays.fill(board[6], new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));
        board[7] = new ChessPiece[]{
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.ROOK),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KNIGHT),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.BISHOP),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.QUEEN),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KING),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.BISHOP),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.KNIGHT),
                new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.ROOK)
        };

        // Draw rows 8→1
        //white perspective = a1 on bottom left corner
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + "  ");
            for (int c = 0; c < 8; c++) {
                ChessPiece p = board[r][c];
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


    // HELP TEXT

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
