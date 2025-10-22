package model;

import chess.ChessGame;

/**
 * Represents a chess game's data.
 * @param gameID A unique ID for the game.
 * @param whiteUsername The username of the player playing white, or null.
 * @param blackUsername The username of the player playing black, or null.
 * @param gameName The name of the game.
 * @param game The ChessGame object representing the current state of the game.
 */
public record GameData(
        int gameID,
        String whiteUsername,
        String blackUsername,
        String gameName,
        ChessGame game) {
}