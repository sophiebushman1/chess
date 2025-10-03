package chess;
import java.util.*;
import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor teamTurn;


    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {

        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {

        this.teamTurn = team;
    }

    @Override
    public String toString() {
        return "ChessGame{" +
                "board=" + board +
                ", teamTurn=" + teamTurn +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessGame chessGame)) {
            return false;
        }
        return Objects.equals(board, chessGame.board) && teamTurn == chessGame.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn);
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null; // no piece at this position
        }

        Collection<ChessMove> pieceMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> legalMoves = new ArrayList<>();

        // For each candidate move: simulate it on a deep copy of the board and
        // check whether the moving piece's team would be in check afterward.
        for (ChessMove move : pieceMoves) {
            ChessBoard copyBoard = deepCopyBoard(board);

            // perform the move on the copy:
            ChessPiece movingPiece = copyBoard.getPiece(move.getStartPosition());
            // place on destination (capture automatically overwrites)
            copyBoard.addPiece(move.getEndPosition(), movingPiece);
            // clear the start square
            copyBoard.addPiece(move.getStartPosition(), null);

            // If there's a promotion, replace the piece at destination with the promoted piece
            if (move.getPromotionPiece() != null && movingPiece != null) {
                copyBoard.addPiece(move.getEndPosition(),
                        new ChessPiece(movingPiece.getTeamColor(), move.getPromotionPiece()));
            }

            // Create a test game that uses the simulated board and ask if that team is in check
            ChessGame testGame = new ChessGame();
            testGame.setBoard(copyBoard);
            // team for the moving piece:
            TeamColor movingTeam = piece.getTeamColor();

            // If the team is NOT in check after the move, it's legal
            if (!testGame.isInCheck(movingTeam)) {
                legalMoves.add(move);
            }
        }

        return legalMoves;

    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {

    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    // Helper: deep-copy a board by cloning every piece into a fresh ChessBoard
    private ChessBoard deepCopyBoard(ChessBoard src) {
        ChessBoard copy = new ChessBoard();
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece cp = src.getPiece(p);
                if (cp != null) {
                    copy.addPiece(p, new ChessPiece(cp.getTeamColor(), cp.getPieceType()));
                }
            }
        }
        return copy;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {

        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {

        return board;
    }
    //Now lets use overide to refine the equals function, cause right now if says that positions with the same corrdinates are not equal when they should be.


}
