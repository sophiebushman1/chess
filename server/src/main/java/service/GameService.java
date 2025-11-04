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

public class GameService {
    private final DataAccess dataAccess;

    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) {}
    public record CreateGameRequest(String gameName) {}
    public record CreateGameResult(int gameID) {}
    public record ListGamesResult(Collection<GameInfo> games) {}
    public record JoinGameRequest(String playerColor, Integer gameID) {}

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    private AuthData authorize(String authToken) throws UnauthorizedException, DataAccessException {
        if (isEmpty(authToken)) {
            throw new UnauthorizedException("unauthorized");
        }
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return auth;
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {
        authorize(authToken);

        if (isEmpty(req.gameName())) {
            throw new BadRequestException("bad request");
        }

        GameData newGame = dataAccess.createGame(
                new GameData(0, null, null, req.gameName(), new ChessGame())
        );

        return new CreateGameResult(newGame.gameID());
    }

    public ListGamesResult listGames(String authToken)
            throws UnauthorizedException, DataAccessException {
        authorize(authToken);

        Collection<GameInfo> gameInfoList = dataAccess.listGames().stream()
                .map(g -> new GameInfo(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()))
                .collect(Collectors.toList());

        return new ListGamesResult(gameInfoList);
    }

    public void joinGame(String authToken, JoinGameRequest req)
            throws UnauthorizedException, BadRequestException, AlreadyTakenException, DataAccessException {
        String username = authorize(authToken).username();

        if (req.gameID() == null || req.gameID() <= 0) {
            throw new BadRequestException("bad request: missing gameID");
        }

        GameData existingGame = dataAccess.getGame(req.gameID());
        if (existingGame == null) {
            throw new BadRequestException("bad request: game not found");
        }

        String color = req.playerColor();
        if (isEmpty(color)) {
            return;
        }

        updateGameSlot(existingGame, color.toUpperCase(Locale.ROOT), username);
    }

    private void updateGameSlot(GameData game, String color, String username)
            throws BadRequestException, AlreadyTakenException, DataAccessException {
        GameData updated;

        switch (color) {
            case "WHITE" -> {
                if (takenByOther(game.whiteUsername(), username)) {
                    throw new AlreadyTakenException("spot already taken");
                }
                updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            }
            case "BLACK" -> {
                if (takenByOther(game.blackUsername(), username)) {
                    throw new AlreadyTakenException("spot already taken");
                }
                updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            }
            default -> throw new BadRequestException("bad request: invalid player color");
        }

        dataAccess.updateGame(updated);
    }

    private boolean takenByOther(String currentUser, String username) {
        return !isEmpty(currentUser) && !currentUser.equals(username);
    }
}
