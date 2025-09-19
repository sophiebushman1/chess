package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {
    //Setup variables
    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPosition = Objects.requireNonNull(startPosition, "start position is null");
        this.endPosition = Objects.requireNonNull(endPosition, "end position is null");
        this.promotionPiece = promotionPiece;
        // The promotionalpeice can be null but the start and end can't be so check
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {

        return startPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {

        return endPosition;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {

        return promotionPiece;
    }
    //Now lets use overide to refine the equals function, cause right now if says that positions with the same corrdinates are not equal when they should be.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessMove chessMove)) return false;
        return Objects.equals(startPosition, chessMove.startPosition) &&
                Objects.equals(endPosition, chessMove.endPosition) &&
                promotionPiece == chessMove.promotionPiece;
    }
    // whenever you override equals(), you must override hashCode() so they agree.
    @Override
    public int hashCode() {
        return Objects.hash(startPosition, endPosition, promotionPiece);
    }
    //tostring function so that we can see positions as more readable
    @Override
    public String toString() {
        return startPosition + " -> " + endPosition + (promotionPiece != null ? " promote to " + promotionPiece : "");
    }
}
