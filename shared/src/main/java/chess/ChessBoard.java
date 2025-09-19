package chess;

import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private final ChessPiece[][] board;
    //creating the board



    public ChessBoard(){
        board = new ChessPiece[8][8];
    //Set it to be an 8x8 board
        
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board[position.getRow() - 1][position.getColumn() - 1] = piece;
        //Java arrays are 0–7 so we need to subtract 1 to get board[7] which becomes our 'piece'
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return board[position.getRow() - 1][position.getColumn() - 1];

    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {

        // Phase 1: not yet
        throw new RuntimeException("Not implemented");
    }
    //Now lets use overide to refine the equals function, cause right now if says that positions with the same corrdinates are not equal when they should be.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessBoard that)) return false;
        return java.util.Arrays.deepEquals(board, that.board);


    }
    // whenever you override equals(), you must override hashCode() so they agree.
    @Override
    public int hashCode() {
        return Objects.hash(java.util.Arrays.deepHashCode(board));
    }
    //tostring function so that we can see positions as more readable
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 8; row >= 1; row--) {
            for (int col = 1; col <= 8; col++) {
                ChessPiece piece = getPiece(new ChessPosition(row, col));
                sb.append(piece == null ? "." : piece.getPieceType().toString().charAt(0));
                sb.append(" ");
                // First letter of piece is printed with a space and period for readability
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    //This to string loops through the whole board position by position top to bottom to build the current board with stringbuilder
}
