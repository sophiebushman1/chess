package ui;

import client.ServerFacade;
import chess.ChessGame;
import chess.ChessPiece;

import java.util.*;

public class ChessClient {

    private final ServerFacade server;
    private String authToken = null;

    // ANSI COLORS
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";     // white pieces
    private static final String BLUE = "\u001B[34m";    // black pieces

    // Backgrounds
    private static final String BG_LIGHT = "\u001B[47m";   // white
    private static final String BG_DARK = "\u001B[100m";   // dark gray

    public ChessClient(int port) {
        server = new ServerFacade(port);
    }

    public void run() {
        System.out.println("♕ Welcome to the 240 Chess Client!");
        preLoginLoop();
    }

    // ========== PRE-LOGIN LOOP ==========
    private void preLoginLoop() {
        Scanner scan = new Scanner(System.in);

        while (authToken == null) {
            System.out.print("\n[prelogin] > ");
            String cmd = scan.nextLine().trim().toLowerCase();

            switch (cmd) {
                case "help" -> printPreloginHelp();
                case "register" -> handleRegister(scan);
                case "login" -> handleLogin(scan);
                case "quit" -> { System.out.println("Goodbye!"); return; }
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }

        postLoginLoop();
    }

    // ========== POST-LOGIN LOOP ==========
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
                case "play" -> handlePlay(scan);
                case "observe" -> handleObserve(scan);
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }

        preLoginLoop();
    }

    // ========== REGISTER ==========
    private void handleRegister(Scanner scan) {
        System.out.print("username: ");
        String u = scan.nextLine().trim();
        System.out.print("password: ");
        String p = scan.nextLine().trim();
        System.out.print("email: ");
        String e = scan.nextLine().trim();

        var res = server.register(u, p, e);

        if (res.getMessage() != null) {
            System.out.println(res.getMessage());
            return;
        }

        System.out.println("Registered & logged in!");
        authToken = res.getAuthToken();
    }

    // ========== LOGIN ==========
    private void handleLogin(Scanner scan) {
        System.out.print("username: ");
        String u = scan.nextLine().trim();
        System.out.print("password: ");
        String p = scan.nextLine().trim();

        var res = server.login(u, p);

        if (res.getMessage() != null) {
            System.out.println(res.getMessage());
            return;
        }

        System.out.println("Logged in!");
        authToken = res.getAuthToken();
    }

    private void handleLogout() {
        var res = server.logout(authToken);
        if (res.getMessage() != null) {
            System.out.println(res.getMessage());
            return;
        }
        System.out.println("Logged out!");
        authToken = null;
    }

    // ========== CREATE GAME ==========
    private void handleCreateGame(Scanner scan) {
        System.out.print("game name: ");
        String name = scan.nextLine().trim();

        var res = server.createGame(name, authToken);
        if (res.getMessage() != null) {
            System.out.println(res.getMessage());
            return;
        }

        System.out.println("Created game: " + name);
    }

    // ========== LIST GAMES ==========
    private Map<Integer, ServerFacade.GameInfo> indexToGame = new HashMap<>();

    private void handleListGames() {
        var res = server.listGames(authToken);
        if (res.getMessage() != null) {
            System.out.println(res.getMessage());
            return;
        }

        indexToGame.clear();
        System.out.println("\nGames:");

        var games = res.getGames();
        if (games.length == 0) {
            System.out.println("(no games)");
            return;
        }

        int displayIndex = 1;
        for (ServerFacade.GameInfo g : games) {
            String white = g.getWhiteUsername() != null ? g.getWhiteUsername() : "(empty)";
            String black = g.getBlackUsername() != null ? g.getBlackUsername() : "(empty)";

            System.out.printf("%d. %s | WHITE: %s | BLACK: %s%n",
                    displayIndex, g.getGameName(), white, black);

            indexToGame.put(displayIndex, g);
            displayIndex++;
        }
    }

    // ========== PLAY ==========
    private void handlePlay(Scanner scan) {
        handleListGames();
        if (indexToGame.isEmpty()) return;

        System.out.print("game number: ");
        String in = scan.nextLine().trim();

        int choice;
        try { choice = Integer.parseInt(in); }
        catch (Exception e) { System.out.println("Invalid number."); return; }

        if (!indexToGame.containsKey(choice)) {
            System.out.println("No game with that number.");
            return;
        }

        ServerFacade.GameInfo selected = indexToGame.get(choice);
        int realGameID = selected.getGameID();

        System.out.print("color (WHITE/BLACK): ");
        String color = scan.nextLine().trim().toUpperCase();

        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            System.out.println("Invalid color.");
            return;
        }

        var res = server.joinGame(color, realGameID, authToken);
        if (res.getMessage() != null) {
            System.out.println(res.getMessage());
            return;
        }

        System.out.println("Joined game!");

        ChessGame.TeamColor team = color.equals("WHITE")
                ? ChessGame.TeamColor.WHITE
                : ChessGame.TeamColor.BLACK;

        drawBoard(team);
    }

    // ========== OBSERVE ==========
    private void handleObserve(Scanner scan) {
        handleListGames();
        if (indexToGame.isEmpty()) return;

        System.out.print("game number: ");
        String in = scan.nextLine().trim();

        int choice;
        try { choice = Integer.parseInt(in); }
        catch (Exception e) { System.out.println("Invalid number."); return; }

        if (!indexToGame.containsKey(choice)) {
            System.out.println("No game with that number.");
            return;
        }

        ServerFacade.GameInfo selected = indexToGame.get(choice);
        int realGameID = selected.getGameID();

        System.out.println("Observing game!");

        drawBoard(ChessGame.TeamColor.WHITE);
    }

    // ========== BOARD DRAWING ==========
    private void drawBoard(ChessGame.TeamColor perspective) {
        System.out.println("\n-- Chess Board (" + perspective + ") --");

        ChessPiece[][] b = makeStartingBoard();

        if (perspective == ChessGame.TeamColor.BLACK) {
            b = flipBoard(b);
        }

        drawBoardWithColors(b, perspective);
    }

    private ChessPiece[][] makeStartingBoard() {
        ChessPiece[][] board = new ChessPiece[8][8];

        // Black back rank
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

        // Black pawns
        for (int c = 0; c < 8; c++) {
            board[1][c] = new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN);
        }

        // Middle empty
        for (int r = 2; r <= 5; r++) Arrays.fill(board[r], null);

        // White pawns
        for (int c = 0; c < 8; c++) {
            board[6][c] = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        }

        // White back rank
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

        return board;
    }

    private ChessPiece[][] flipBoard(ChessPiece[][] board) {
        ChessPiece[][] flipped = new ChessPiece[8][8];

        // flip rows
        for (int r = 0; r < 8; r++) {
            flipped[7 - r] = Arrays.copyOf(board[r], 8);
        }

        // flip columns
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 4; c++) {
                ChessPiece tmp = flipped[r][c];
                flipped[r][c] = flipped[r][7 - c];
                flipped[r][7 - c] = tmp;
            }
        }

        return flipped;
    }

    // ========== DRAW WITH COLORS ==========
    private void drawBoardWithColors(ChessPiece[][] board, ChessGame.TeamColor perspective) {

        for (int r = 0; r < 8; r++) {

            int rank = (perspective == ChessGame.TeamColor.WHITE) ? (8 - r) : (r + 1);
            System.out.print(rank + "  ");

            for (int c = 0; c < 8; c++) {

                boolean dark = (r + c) % 2 == 1;
                String bg = dark ? BG_DARK : BG_LIGHT;

                ChessPiece p = board[r][c];

                if (p == null) {
                    System.out.print(bg + "  " + RESET);
                } else {
                    boolean isWhitePiece = p.getTeamColor() == ChessGame.TeamColor.WHITE;
                    String color = isWhitePiece ? RED : BLUE;
                    String symbol = "" + getPieceSymbol(p);

                    System.out.print(bg + color + symbol + " " + RESET);
                }
            }

            System.out.println();
        }

        System.out.println();
        System.out.print("   ");

        if (perspective == ChessGame.TeamColor.WHITE) {
            System.out.println("a b c d e f g h");
        } else {
            System.out.println("h g f e d c b a");
        }
    }

    private char getPieceSymbol(ChessPiece p) {
        return switch (p.getPieceType()) {
            case KING -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'K' : 'k';
            case QUEEN -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'Q' : 'q';
            case ROOK -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'R' : 'r';
            case BISHOP -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'B' : 'b';
            case KNIGHT -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'N' : 'n';
            case PAWN -> p.getTeamColor() == ChessGame.TeamColor.WHITE ? 'P' : 'p';
        };
    }

    // HELP MENUS
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
