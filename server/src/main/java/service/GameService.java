package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import exception.AlreadyTakenException;
import exception.BadRequestException;
import exception.UnauthorizedException;
import chess.ChessGame;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service class responsible for managing chess games: creation, listing, and joining.
 * This service handles business logic and delegates data persistence to the DataAccess layer.
 */
public class GameService {
    private final DataAccess dataAccess;

    // --- Inner Records for Request/Response ---
    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record ListGamesResult(Collection<GameInfo> games) {}
    public record JoinGameRequest(String playerColor, Integer gameID) {}
    // ------------------------------------------

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * Private helper method to authorize a user based on the provided token.
     * Throws UnauthorizedException (HTTP 401) if the token is invalid or missing.
     */
    private AuthData authorize(String authToken) throws UnauthorizedException, DataAccessException {
        if (authToken == null || authToken.isEmpty()) {
            throw new UnauthorizedException("unauthorized");
        }
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return auth;
    }

    /**
     * Creates a new game and returns its ID.
     */
    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {

        authorize(authToken);

        // 1. Validate Request (400 Bad Request)
        if (req.gameName() == null || req.gameName().isEmpty()) {
            throw new BadRequestException("bad request");
        }

        // 2. Create Game (ChessGame is initialized to the starting position)
        GameData gameTemplate = new GameData(0, null, null, req.gameName(), new ChessGame());
        GameData newGame = dataAccess.createGame(gameTemplate);

        return new CreateGameResult(newGame.gameID());
    }

    /**
     * Lists all available games.
     */
    public ListGamesResult listGames(String authToken)
            throws UnauthorizedException, DataAccessException {

        authorize(authToken);

        Collection<GameData> games = dataAccess.listGames();

        // Convert GameData models to the simplified GameInfo records
        Collection<GameInfo> gameInfoList = games.stream()
                .map(game -> new GameInfo(
                        game.gameID(),
                        game.whiteUsername(),
                        game.blackUsername(),
                        game.gameName()))
                .collect(Collectors.toList());

        return new ListGamesResult(gameInfoList);
    }

    /**
     * Allows a user to join a game as a player (WHITE/BLACK) or an observer.
     */
    public void joinGame(String authToken, JoinGameRequest req)
            throws UnauthorizedException, BadRequestException, AlreadyTakenException, DataAccessException {

        AuthData auth = authorize(authToken);
        String username = auth.username();

        // 1. Validate Request (400 Bad Request if ID is missing or invalid)
        if (req.gameID() == null || req.gameID() <= 0) {
            throw new BadRequestException("bad request: missing gameID");
        }

        // 2. Get Game (400 Bad Request if game not found)
        GameData existingGame = dataAccess.getGame(req.gameID());

        if (existingGame == null) {
            throw new BadRequestException("bad request: game not found");
        }

        String playerColor = req.playerColor();

        // 3. Handle Joining Logic
        if (playerColor == null || playerColor.isEmpty()) {
            // Joining as an observer, no update to GameData needed.
            return;
        }

        // Normalize color input
        String normalizedColor = playerColor.toUpperCase(Locale.ROOT);
        GameData updatedGame;

        if ("WHITE".equals(normalizedColor)) {
            // Check for Already Taken (403 Forbidden)
            if (existingGame.whiteUsername() != null && !existingGame.whiteUsername().isEmpty() && !username.equals(existingGame.whiteUsername())) {
                throw new AlreadyTakenException("spot already taken");
            }
            // Update the game with the new White player
            updatedGame = new GameData(
                    existingGame.gameID(),
                    username,
                    existingGame.blackUsername(),
                    existingGame.gameName(),
                    existingGame.game()
            );
        } else if ("BLACK".equals(normalizedColor)) {
            // Check for Already Taken (403 Forbidden)
            if (existingGame.blackUsername() != null && !existingGame.blackUsername().isEmpty() && !username.equals(existingGame.blackUsername())) {
                throw new AlreadyTakenException("spot already taken");
            }
            // Update the game with the new Black player
            updatedGame = new GameData(
                    existingGame.gameID(),
                    existingGame.whiteUsername(),
                    username,
                    existingGame.gameName(),
                    existingGame.game()
            );
        } else {
            // Invalid color provided (e.g., "RED") (400 Bad Request)
            throw new BadRequestException("bad request: invalid player color");
        }

        // 4. Update Game to save the new player assignment
        // This is where the persistence must happen. If this is failing, your updateGame
        // implementation in DataAccess is the next place to look!
        dataAccess.updateGame(updatedGame);
    }
}
