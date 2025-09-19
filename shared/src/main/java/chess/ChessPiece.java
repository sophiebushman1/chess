package chess;

import java.util.Collection;
import java.util.Objects;


/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private final ChessGame.TeamColor teamColor;
    private final ChessPiece.PieceType pieceType;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.teamColor = pieceColor;
        this.pieceType = type;
    }


    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }
    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {

        return teamColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {

        return pieceType;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        //Return all moves a piece can legally make, ignoring check/checkmate.
        //Going to need helper classes like checkbounds and sliding pieces
        //Must consider the board edges and friendly/enemy pieces.
        Collection<ChessMove> moves = new java.util.ArrayList<>();

        int row = myPosition.getRow();
        int col = myPosition.getColumn();
        //Make the list and get the position

        switch (pieceType) {
            //pawns can go forward 1 or 2 spaces and go diagonal to capture/kill
            case PAWN -> {
                //one step forward
                int direction = (teamColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
                //See what direction the pawn in question should be moving (up if white vs down if black)
                ChessPosition oneStep = new ChessPosition(row + direction, col);
                //moving 1 row in that direction to target row if its on th board and the space if empty
                if (isInBounds(oneStep) && board.getPiece(oneStep) == null) {
                    moves.add(new ChessMove(myPosition, oneStep, null));
                    //since all checks pass we can now use chessmove

                    // 2 steps forward
                    //here was have to check if the pawn is in the front row AND if the two spaces infront of it are on the board and empty, then we can use chesssmove.

                    int startRow = (teamColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
                    ChessPosition twoStep = new ChessPosition(row + 2 * direction, col);
                    if (row == startRow && board.getPiece(twoStep) == null) {
                        moves.add(new ChessMove(myPosition, twoStep, null));
                    }

                }

                // Handling Diagonal captures - has to be inbounds, empty, and an enemy color
                int[] diagCols = {col - 1, col + 1}; //white vs black
                for (int c : diagCols) {
                    ChessPosition diag = new ChessPosition(row + direction, c); //only forward one row
                    if (isInBounds(diag)) {
                        ChessPiece target = board.getPiece(diag);
                        if (target != null && target.getTeamColor() != teamColor) {
                            moves.add(new ChessMove(myPosition, diag, null));
                        }
                    }
                }
            }

            case KNIGHT -> {

            }
            //(row, col)
            case BISHOP -> addSlidingMoves(board, myPosition, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});//all four diagonals
            case ROOK -> addSlidingMoves(board, myPosition, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});//all four directions
            case QUEEN -> addSlidingMoves(board, myPosition, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});//all four directions + four diagonals
            //can move in all 8 directions like the queen but only one step
            case KING -> {

            }
        }
        return moves;
    }
    //Now lets use override to refine the equals function, cause right now if says that positions with the same corrdinates are not equal when they should be.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessPiece that)) return false;
        return teamColor == that.teamColor && pieceType == that.pieceType;
    }
    // whenever you override equals(), you must override hashCode() so they agree.
    @Override
    public int hashCode() {
        return Objects.hash(teamColor, pieceType);
    }
    //tostring function so that we can see positions as more readable
    @Override
    public String toString() {
        return teamColor + " " + pieceType;
    }
}
