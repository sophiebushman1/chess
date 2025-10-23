package service;

import java.util.List;

public record ListGamesResult(List<GameInfo> games) {

    public ListGamesResult(List<GameInfo> games) {
        this.games = (games != null) ? games : List.of();
    }

    public record GameInfo(Integer gameID, String whiteUsername, String blackUsername, String gameName) {}
}
