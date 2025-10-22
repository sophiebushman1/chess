package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import java.util.Objects;

public class GameService {
    private final DataAccess dataAccess;

    public GameService(DataAccess dataAccess) {
        this.dataAccess = dataAccess;

    }

    /** Validates the authToken and returns the associated username. */
    private String validateAuth(String authToken) throws DataAccessException, UnauthorizedException {
        if (authToken == null) {
            throw new UnauthorizedException("Authorization header missing");
        }
        AuthData auth = dataAccess.getAuth(authToken);
        if (auth == null) {
            throw new UnauthorizedException("Invalid authToken");
        }
        return auth.username();
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException, UnauthorizedException {
        validateAuth(authToken); // valid?
        return new ListGamesResult(dataAccess.listGames()); // retrieve & return
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws DataAccessException, UnauthorizedException, BadRequestException {

        validateAuth(authToken);


        if (req.gameName() == null || req.gameName().isEmpty()) {
            throw new BadRequestException("Missing gameName");
        }

        GameData newGame = dataAccess.createGame(req.gameName()); // 2. Create game

        return new CreateGameResult(newGame.gameID());
    }


    public void joinGame(String authToken, JoinGameRequest req)
            throws DataAccessException, UnauthorizedException, AlreadyTakenException, BadRequestException {

        String username = validateAuth(authToken);

        // valid input?
        if (req.gameID() == null) {

            throw new BadRequestException("Missing gameID");
        }
        // csn be null (observers)

        // grbs game
        GameData game = dataAccess.getGame(req.gameID());
        if (game == null) {
            throw new BadRequestException("Invalid gameID");
        }
        // check color and update
        if (req.playerColor() != null) {
            String upperColor = req.playerColor().toUpperCase();

            if ("WHITE".equals(upperColor)) {
                if (game.whiteUsername() != null && !game.whiteUsername().equals(username)) {
                    throw new AlreadyTakenException("White is already taken");
                }
                game = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            }
            else if ("BLACK".equals(upperColor)) {
                if (game.blackUsername() != null && !game.blackUsername().equals(username)) {
                    throw new AlreadyTakenException("Black is already taken");
                }
                game = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            }
            else {
                throw new BadRequestException("Invalid playerColor. Must be WHITE or BLACK.");
            }
        }
        // playcolor null? player is observer

        // update DAO
        dataAccess.updateGame(game);
    }

}