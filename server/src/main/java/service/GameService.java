package service;

import chess.ChessGame;
import chess.ChessGame.TeamColor;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import service.ServiceExceptions.AlreadyTakenException;
import service.ServiceExceptions.BadRequestException;
import service.ServiceExceptions.UnauthorizedException;

public class GameService {

    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    private AuthData authorize(String authToken) throws UnauthorizedException, DataAccessException {
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Invalid authToken.");
        }
        return auth;
    }

    public ListGamesResult listGames(String authToken) throws UnauthorizedException, DataAccessException {
        authorize(authToken);

        return new ListGamesResult(dataAccess.listGames());
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws BadRequestException, UnauthorizedException, DataAccessException {

        authorize(authToken);

        if (req.gameName() == null || req.gameName().isEmpty()) {
            throw new BadRequestException("Missing gameName.");
        }

        GameData newGameTemplate = new GameData(0, null, null, req.gameName(), null);
        GameData newGame = dataAccess.createGame(newGameTemplate);

        return new CreateGameResult(newGame.gameID());
    }

    public void joinGame(String authToken, JoinGameRequest req)
            throws BadRequestException, UnauthorizedException, AlreadyTakenException, DataAccessException {

        AuthData auth = authorize(authToken);
        String username = auth.username();

        if (req.gameID() == 0) {
            throw new BadRequestException("Missing gameID.");
        }

        GameData game = dataAccess.getGame(req.gameID());
        if (game == null) {
            throw new BadRequestException("Game not found.");
        }

        String playerColor = req.playerColor();
        String whiteUsername = game.whiteUsername();
        String blackUsername = game.blackUsername();

        if (playerColor != null) {
            TeamColor color;
            try {
                color = TeamColor.valueOf(playerColor.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid player color provided.");
            }

            if (color == TeamColor.WHITE) {
                if (whiteUsername != null && !whiteUsername.equals(username)) {
                    throw new AlreadyTakenException("White position already taken.");
                }
                whiteUsername = username;
            } else if (color == TeamColor.BLACK) {
                if (blackUsername != null && !blackUsername.equals(username)) {
                    throw new AlreadyTakenException("Black position already taken.");
                }
                blackUsername = username;
            }
        }

        GameData updatedGame = new GameData(
                game.gameID(),
                whiteUsername,
                blackUsername,
                game.gameName(),
                game.game()
        );
        dataAccess.updateGame(updatedGame);
    }
}