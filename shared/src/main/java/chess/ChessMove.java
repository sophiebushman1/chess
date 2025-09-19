package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        throw new RuntimeException("Not implemented");
    }
    //Now lets use overide to refine the equals function, cause right now if says that positions with the same corrdinates are not equal when they should be.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessPosition that = (ChessPosition) o;
        return row == that.row && column == that.column;
    }
    // whenever you override equals(), you must override hashCode() so they agree.
    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }
    //tostring function so that we can see positions as more readable
    @Override
    public String toString() {
        return "(" + row + "," + column + ")";
    }
}
