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

    // --- NEW: ID and player tracking ---
    private static int nextId = 1;
    private int id;
    private List<String> players = new ArrayList<>();

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
        this.id = nextId++;
    }

    public int getId() {
        return id;
    }

    public void addPlayer(String token) {
        if (!players.contains(token)) {
            players.add(token);
        }
    }

    public List<String> getPlayers() {
        return players;
    }
    // --- END NEW ---

    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    @Override
    public String toString() {
        return "ChessGame{" +
                "board=" + board +
                ", teamTurn=" + teamTurn +
                ", id=" + id +
                ", players=" + players +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessGame chessGame)) {
            return false;
        }
        return Objects.equals(board, chessGame.board) &&
                teamTurn == chessGame.teamTurn &&
                id == chessGame.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn, id);
    }

    public enum TeamColor {
        WHITE,
        BLACK
    }

    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        if (startPosition == null) return null;
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) return null;

        Collection<ChessMove> pieceMoves = piece.pieceMoves(board, startPosition);
        Collection<ChessMove> legalMoves = new ArrayList<>();

        for (ChessMove move : pieceMoves) {
            ChessBoard copyBoard = deepCopyBoard(board);
            ChessPiece movingPiece = copyBoard.getPiece(move.getStartPosition());
            copyBoard.addPiece(move.getEndPosition(), movingPiece);
            copyBoard.addPiece(move.getStartPosition(), null);

            if (move.getPromotionPiece() != null && movingPiece != null) {
                copyBoard.addPiece(move.getEndPosition(),
                        new ChessPiece(movingPiece.getTeamColor(), move.getPromotionPiece()));
            }

            ChessGame testGame = new ChessGame();
            testGame.setBoard(copyBoard);
            TeamColor movingTeam = piece.getTeamColor();
            if (!testGame.isInCheck(movingTeam)) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (move == null) throw new InvalidMoveException("Move is null");

        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();

        ChessPiece piece = board.getPiece(start);
        if (piece == null) throw new InvalidMoveException("No piece at start position.");

        if (piece.getTeamColor() != teamTurn)
            throw new InvalidMoveException("It is not " + piece.getTeamColor() + "'s turn.");

        Collection<ChessMove> legal = validMoves(start);
        if (legal == null || !legal.contains(move))
            throw new InvalidMoveException("Move is not legal for this piece.");

        ChessPiece movingPiece = board.getPiece(start);
        board.addPiece(end, movingPiece);
        board.addPiece(start, null);

        if (move.getPromotionPiece() != null && movingPiece != null) {
            board.addPiece(end, new ChessPiece(movingPiece.getTeamColor(), move.getPromotionPiece()));
        }

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = null;
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece cp = board.getPiece(p);
                if (cp != null && cp.getTeamColor() == teamColor && cp.getPieceType() == ChessPiece.PieceType.KING) {
                    kingPos = p;
                    break;
                }
            }
            if (kingPos != null) break;
        }
        if (kingPos == null) return false;

        TeamColor opponent = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece cp = board.getPiece(p);
                if (cp != null && cp.getTeamColor() == opponent) {
                    for (ChessMove m : cp.pieceMoves(board, p)) {
                        if (m.getEndPosition().equals(kingPos)) return true;
                    }
                }
            }
        }
        return false;
    }

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

    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) return false;
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece cp = board.getPiece(pos);
                if (cp != null && cp.getTeamColor() == teamColor) {
                    Collection<ChessMove> legal = validMoves(pos);
                    if (legal != null && !legal.isEmpty()) return false;
                }
            }
        }
        return true;
    }

    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) return false;
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece cp = board.getPiece(pos);
                if (cp != null && cp.getTeamColor() == teamColor) {
                    Collection<ChessMove> legal = validMoves(pos);
                    if (legal != null && !legal.isEmpty()) return false;
                }
            }
        }
        return true;
    }

    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    public ChessBoard getBoard() {
        return board;
    }
}
