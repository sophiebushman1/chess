package websocket.commands;

import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

public class MakeMoveCommand extends UserGameCommand {

    private final ChessPosition start;
    private final ChessPosition end;
    private final ChessPiece.PieceType promotion;

    public MakeMoveCommand(String authToken, Integer gameID,
                           ChessPosition start, ChessPosition end,
                           ChessPiece.PieceType promotion) {
        super(CommandType.MAKE_MOVE, authToken, gameID);
        this.start = start;
        this.end = end;
        this.promotion = promotion;
    }

    public ChessMove toChessMove() {
        return new ChessMove(start, end, promotion);
    }
}
