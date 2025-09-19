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
                int oneRow = row + direction;
                if (isInBounds(oneRow, col) && board.getPiece(new ChessPosition(oneRow, col)) == null) {
                    ChessPosition oneStep = new ChessPosition(oneRow, col);
                    moves.add(new ChessMove(myPosition, oneStep, null));

                    // 2 steps forward
                    int startRow = (teamColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
                    int twoRow = row + 2 * direction;
                    if (row == startRow && isInBounds(twoRow, col) && board.getPiece(new ChessPosition(twoRow, col)) == null) {
                        ChessPosition twoStep = new ChessPosition(twoRow, col);
                        moves.add(new ChessMove(myPosition, twoStep, null));
                    }
                }

                // Handling Diagonal captures - has to be inbounds, empty, and an enemy color
                int[] diagCols = {col - 1, col + 1}; //white vs black
                for (int c : diagCols) {
                    int newRow = row + direction;
                    if (isInBounds(newRow, c)) {
                        ChessPosition diag = new ChessPosition(newRow, c); //only forward one row
                        ChessPiece target = board.getPiece(diag);
                        if (target != null && target.getTeamColor() != teamColor) {
                            moves.add(new ChessMove(myPosition, diag, null));
                        }
                    }
                }
            }

            case KNIGHT -> {
                int[][] offsets = {{2,1},{1,2},{-1,2},{-2,1},{-2,-1},{-1,-2},{1,-2},{2,-1}};
                for (int[] off : offsets) {
                    int newRow = row + off[0];
                    int newCol = col + off[1];
                    if (isInBounds(newRow, newCol)) {
                        ChessPosition pos = new ChessPosition(newRow, newCol);
                        ChessPiece target = board.getPiece(pos);
                        if (target == null || target.getTeamColor() != teamColor) {
                            moves.add(new ChessMove(myPosition, pos, null));
                        }
                    }
                }
            }
            //(row, col)
            case BISHOP -> addSlidingMoves(board, myPosition, moves, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});//all four diagonals
            case ROOK -> addSlidingMoves(board, myPosition, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});//all four directions
            case QUEEN -> addSlidingMoves(board, myPosition, moves, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});//all four directions + four diagonals
            //can move in all 8 directions like the queen but only one step
            case KING -> {
                int[][] offsets = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
                for (int[] off : offsets) {
                    int newRow = row + off[0];
                    int newCol = col + off[1];
                    if (isInBounds(newRow, newCol)) {
                        ChessPosition pos = new ChessPosition(newRow, newCol);
                        ChessPiece target = board.getPiece(pos);
                        if (target == null || target.getTeamColor() != teamColor) {
                            moves.add(new ChessMove(myPosition, pos, null));
                        }
                    }
                }
            }
        }


        return moves;
    }
    // Helper: sliding pieces like Rook, Bishop, Queen
    private void addSlidingMoves(ChessBoard board, ChessPosition start, Collection<ChessMove> moves, int[][] directions) {
        int row = start.getRow();
        int col = start.getColumn();
        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (isInBounds(r, c)) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece target = board.getPiece(pos);
                if (target == null) {
                    moves.add(new ChessMove(start, pos, null));
                } else {
                    if (target.getTeamColor() != teamColor) {
                        moves.add(new ChessMove(start, pos, null));
                    }
                    break; // cannot jump over pieces
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }

    // Helper: check bounds
    private boolean isInBounds(ChessPosition pos) {
        return pos.getRow() >= 1 && pos.getRow() <= 8 && pos.getColumn() >= 1 && pos.getColumn() <= 8;
    }
    private boolean isInBounds(int row, int col) {
        return row >= 1 && row <= 8 && col >= 1 && col <= 8;
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
