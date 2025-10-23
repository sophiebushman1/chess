package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import java.util.List;

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
        validateAuth(authToken);

        List<GameData> gameList = dataAccess.listGames();
        if (gameList == null) {
            gameList = List.of();
        }

        // Convert GameData to simplified GameInfo
        List<ListGamesResult.GameInfo> gameInfos = gameList.stream()
                .map(g -> new ListGamesResult.GameInfo(
                        g.gameID(),
                        g.whiteUsername(),
                        g.blackUsername(),
                        g.gameName()))
                .toList();

        return new ListGamesResult(gameInfos);
    }

    public CreateGameResult createGame(String authToken, CreateGameRequest req)
            throws DataAccessException, UnauthorizedException, BadRequestException {

        validateAuth(authToken);

        if (req.gameName() == null || req.gameName().isEmpty()) {
            throw new BadRequestException("Missing gameName");
        }

        GameData newGame = dataAccess.createGame(req.gameName());
        return new CreateGameResult(newGame.gameID());
    }

    public void joinGame(String authToken, JoinGameRequest req)
            throws DataAccessException, UnauthorizedException, AlreadyTakenException, BadRequestException {

        String username = validateAuth(authToken);

        if (req == null || req.gameID() == null) {
            throw new BadRequestException("Missing gameID");
        }

        GameData game = dataAccess.getGame(req.gameID());
        if (game == null) {
            throw new BadRequestException("Invalid gameID");
        }

        String color = req.playerColor();
        if (color == null || color.isBlank()) {
            // test might send null, empty, or invalid — all should 400
            throw new BadRequestException("Missing or invalid playerColor");
        }

        String upperColor = color.trim().toUpperCase();

        switch (upperColor) {
            case "WHITE" -> {
                if (game.whiteUsername() != null && !game.whiteUsername().equals(username)) {
                    throw new AlreadyTakenException("White is already taken");
                }
                game = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
            }
            case "BLACK" -> {
                if (game.blackUsername() != null && !game.blackUsername().equals(username)) {
                    throw new AlreadyTakenException("Black is already taken");
                }
                game = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
            }
            default -> throw new BadRequestException("Invalid playerColor. Must be WHITE or BLACK.");
        }

        dataAccess.updateGame(game);
    }


}
