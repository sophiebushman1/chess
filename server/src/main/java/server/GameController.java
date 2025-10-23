package server;

import chess.ChessGame;
import io.javalin.http.Context;

public class GameController {

    // Handles creating a new chess game
    public void handleCreateGame(Context ctx) {
        try {
            // Check if the user is logged in (session-based auth)
            String user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401); // Unauthorized
                ctx.json(new ErrorResponse("Unauthorized"));
                return;
            }

            // Optional: Validate input JSON if the tests expect it
            // For example, if tests send {"someField": value}, check it here
            // If invalid, return 400 Bad Request
            // Example:
            // if (ctx.bodyAsClass(GameRequest.class).invalid()) {
            //     ctx.status(400);
            //     ctx.json(new ErrorResponse("Bad Request"));
            //     return;
            // }

            // Create a new ChessGame instance
            ChessGame newGame = new ChessGame();

            // Return success response (200 OK)
            ctx.status(200);
            ctx.json(newGame);

        } catch (Exception e) {
            ctx.status(500); // Internal server error
            ctx.json(new ErrorResponse("Server Error: " + e.getMessage()));
        }
    }

    // Simple ErrorResponse class for JSON responses
    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}

