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
        if (authToken == null || authToken.isEmpty()) {
            throw new UnauthorizedException("unauthorized");
        }
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return auth;
    }


    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws UnauthorizedException, BadRequestException, DataAccessException {

        authorize(authToken);

        // Validate Request 400
        if (req.gameName() == null || req.gameName().isEmpty()) {
            throw new BadRequestException("bad request");
        }

        // make game
        GameData gameTemplate = new GameData(0, null, null, req.gameName(), new ChessGame());
        GameData newGame = dataAccess.createGame(gameTemplate);

        return new CreateGameResult(newGame.gameID());
    }


    public ListGamesResult listGames(String authToken)
            throws UnauthorizedException, DataAccessException {

        authorize(authToken);

        Collection<GameData> games = dataAccess.listGames();


        Collection<GameInfo> gameInfoList = games.stream()
                .map(game -> new GameInfo(
                        game.gameID(),
                        game.whiteUsername(),
                        game.blackUsername(),
                        game.gameName()))
                .collect(Collectors.toList());

        return new ListGamesResult(gameInfoList);
    }


    public void joinGame(String authToken, JoinGameRequest req)
            throws UnauthorizedException, BadRequestException, AlreadyTakenException, DataAccessException {

        AuthData auth = authorize(authToken);
        String username = auth.username();


        if (req.gameID() == null || req.gameID() <= 0) {
            throw new BadRequestException("bad request: missing gameID");
        }

        //get game
        GameData existingGame = dataAccess.getGame(req.gameID());

        if (existingGame == null) {
            throw new BadRequestException("bad request: game not found");
        }

        String playerColor = req.playerColor();


        if (playerColor == null || playerColor.isEmpty()) {
            // if joining as an observer
            return;
        }


        String normalizedColor = playerColor.toUpperCase(Locale.ROOT);
        GameData updatedGame;

        if ("WHITE".equals(normalizedColor)) {
            if (existingGame.whiteUsername() != null && !existingGame.whiteUsername().isEmpty() && !username.equals(existingGame.whiteUsername())) {
                throw new AlreadyTakenException("spot already taken");
            }
            updatedGame = new GameData(
                    existingGame.gameID(),
                    username,
                    existingGame.blackUsername(),
                    existingGame.gameName(),
                    existingGame.game()
            );
        } else if ("BLACK".equals(normalizedColor)) {
            if (existingGame.blackUsername() != null && !existingGame.blackUsername().isEmpty() && !username.equals(existingGame.blackUsername())) {
                throw new AlreadyTakenException("spot already taken");
            }
            updatedGame = new GameData(
                    existingGame.gameID(),
                    existingGame.whiteUsername(),
                    username,
                    existingGame.gameName(),
                    existingGame.game()
            );
        } else {
            throw new BadRequestException("bad request: invalid player color");
        }
        dataAccess.updateGame(updatedGame);
    }
}
