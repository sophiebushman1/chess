package chess;

import java.util.*;

/**
 * Represents a single chess piece
 */
public class ChessPiece {
    private final ChessGame.TeamColor teamColor;
    private final PieceType pieceType;

    public ChessPiece(ChessGame.TeamColor color, PieceType type) {
        this.teamColor = color;
        this.pieceType = type;
    }

    public enum PieceType { KING, QUEEN, BISHOP, KNIGHT, ROOK, PAWN }

    public ChessGame.TeamColor getTeamColor() { return teamColor; }
    public PieceType getPieceType() { return pieceType; }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition pos) {
        Collection<ChessMove> moves = new ArrayList<>();
        int row = pos.getRow(), col = pos.getColumn();

        switch (pieceType) {
            case PAWN -> {
                int dir = (teamColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
                int oneRow = row + dir;
                // forward 1
                if (isEmpty(board, oneRow, col)) {
                    addPawnMove(moves, pos, new ChessPosition(oneRow, col));
                    // forward 2
                    int startRow = (teamColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
                    if (row == startRow && isEmpty(board, row + 2*dir, col)) {
                        moves.add(new ChessMove(pos, new ChessPosition(row + 2 * dir, col), null));
                    }
                }
                // diagonal captures
                for (int dc : new int[]{-1,1}) {
                    int nr = oneRow, nc = col + dc;
                    if (isEnemy(board, nr, nc)) {
                        addPawnMove(moves, pos, new ChessPosition(nr, nc));
                    }
                }
            }
            case KNIGHT -> addOffsetMoves(board, pos, moves,
                    new int[][]{{2,1},{1,2},{-1,2},{-2,1},{-2,-1},{-1,-2},{1,-2},{2,-1}});
            case KING -> addOffsetMoves(board, pos, moves,
                    new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});
            case BISHOP -> addSlidingMoves(board, pos, moves,
                    new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
            case ROOK -> addSlidingMoves(board, pos, moves,
                    new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
            case QUEEN -> addSlidingMoves(board, pos, moves,
                    new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});
        }
        return moves;
    }

    // ----- Helpers -----
    private void addPawnMove(Collection<ChessMove> moves, ChessPosition from, ChessPosition to) {
        int r = to.getRow();
        if (r == 1 || r == 8) { // promotion
            for (PieceType promo : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}){
                moves.add(new ChessMove(from, to, promo));
            }
        } else {
            moves.add(new ChessMove(from, to, null));
        }
    }

    private void addOffsetMoves(ChessBoard board, ChessPosition from, Collection<ChessMove> moves, int[][] offsets) {
        int r = from.getRow(), c = from.getColumn();
        for (int[] off : offsets) {
            int nr = r + off[0], nc = c + off[1];
            if (isInBounds(nr, nc) && (board.getPiece(new ChessPosition(nr,nc)) == null ||
                    board.getPiece(new ChessPosition(nr,nc)).getTeamColor() != teamColor)) {
                moves.add(new ChessMove(from, new ChessPosition(nr, nc), null));
            }
        }
    }

    private void addSlidingMoves(ChessBoard board, ChessPosition from, Collection<ChessMove> moves, int[][] dirs) {
        int r = from.getRow(), c = from.getColumn();
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (isInBounds(nr, nc)) {
                ChessPosition p = new ChessPosition(nr, nc);
                ChessPiece target = board.getPiece(p);
                if (target == null) {
                    moves.add(new ChessMove(from, p, null));
                }
                else {
                    if (target.getTeamColor() != teamColor) {
                        moves.add(new ChessMove(from, p, null));
                    }
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private boolean isEmpty(ChessBoard b, int r, int c) {
        return isInBounds(r,c) && b.getPiece(new ChessPosition(r,c)) == null;
    }
    private boolean isEnemy(ChessBoard b, int r, int c) {
        if (!isInBounds(r,c)) {
            return false;
        }
        ChessPiece p = b.getPiece(new ChessPosition(r,c));
        return p != null && p.getTeamColor() != teamColor;
    }
    private boolean isInBounds(int r, int c) { return r>=1 && r<=8 && c>=1 && c<=8; }

    // equals/hashCode/toString
    @Override public boolean equals(Object o) {
        return (o instanceof ChessPiece p) && teamColor==p.teamColor && pieceType==p.pieceType;
    }
    @Override public int hashCode() { return Objects.hash(teamColor, pieceType); }
    @Override public String toString() { return teamColor+" "+pieceType; }
}
