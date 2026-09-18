package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    private ChessGame.TeamColor pieceColor;
    private PieceType type;
    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
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
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(pieceColor);
        result = 31 * result + Objects.hashCode(type);
        return result;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch(this.getPieceType()) {
            case BISHOP -> bishopMoves(board, myPosition);
            case ROOK -> rookMoves(board, myPosition);
            case KNIGHT -> knightMoves(board, myPosition);
            case KING -> kingMoves(board, myPosition);
            case QUEEN -> queenMoves(board, myPosition);
            case PAWN -> pawnMoves(board, myPosition);
        };

    }

    private Collection<ChessMove> slidingMoves(ChessBoard board, ChessPosition position, int[][] possibleDirections) {
        ArrayList<ChessMove> moves = new ArrayList<ChessMove>();

        for(int[] direction : possibleDirections) { //basically the java equiv of forEach(){} in js
            int row = position.getRow();
            int col = position.getColumn();

            while (true) {
                row += direction[0];
                col += direction[1];

                // did we somehow invent more spaces on the board that totally exist???
                if (row < 1 || col < 1 || row > 8 || col > 8) {
                    break;
                }

                // is the new space empty?
                ChessPosition newPosition = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(newPosition);
                if (target == null) {
                    moves.add(new ChessMove(position, newPosition, null)); //promotionPiece is always nothing for bishops
                } else {
                    if (target.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(position, newPosition, null));
                    }
                    break;
                }
            }
        }
        return moves;
    }

    private Collection<ChessMove> hoppingMoves(ChessBoard board, ChessPosition position, int[][] possibleDirections) {
        ArrayList<ChessMove> moves = new ArrayList<ChessMove>();

        for (int[] direction : possibleDirections) {
            int row = position.getRow() + direction[0];
            int col = position.getColumn() + direction[1];

            if (row < 1 || col < 1 || row > 8 || col > 8){
                continue;
            }

            ChessPosition newPosition = new ChessPosition(row, col);
            ChessPiece target = board.getPiece(newPosition);
            if (target == null) {
                moves.add(new ChessMove(position, newPosition, null));
            } else {
                if (target.getTeamColor() != this.getTeamColor()) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
            }
        }

        return moves;
    }

    private Collection<ChessMove> bishopMoves(ChessBoard board, ChessPosition position) {
        // bishop always moves in some kind of diagonal line, so make sure it can only do that
        int[][] possibleDirections = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
        return slidingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> rookMoves(ChessBoard board, ChessPosition position) {
        // rooks either move up, down, left, or right. Same column for up/down, same row for left/right
        int[][] possibleDirections = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        return slidingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> queenMoves(ChessBoard board, ChessPosition position) {
        // queens move in a straight line horizontally, vertically, or diagonally, so rook moves AND bishop moves.
        int[][] possibleDirections = {
                {1,1}, {1,-1}, {-1,1}, {-1,-1}, // same as bishop
                {1,0}, {-1,0}, {0,1}, {0,-1} // same as rook
        };
        return slidingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> knightMoves(ChessBoard board, ChessPosition position) {
        // knights move two squares one way, one square perpendicular (never lands next to itself)
        int[][] possibleDirections = {
                {-1,2},     {1,2},
                {-2,1},            {2,1},

                {-2,-1},           {2,-1},
                {-1,-2},     {1,-2}
        };
        return hoppingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> kingMoves(ChessBoard board, ChessPosition position) {
        // kings move one square in any direction, no more than one square.
        int[][] possibleDirections = {
                {-1,1},{0,1},{1,1},
                {-1,0},      {1,0},
                {-1,-1},{0,-1},{1,-1}
        };
        return hoppingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> pawnMoves(ChessBoard board, ChessPosition position) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        int direction = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int startRow = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int row = position.getRow();
        int col = position.getColumn();
        int newRow = row + direction;

        if ((1 <= newRow) && (newRow <= 8)) {
            // forward moves
            if ((board.getPiece(new ChessPosition(newRow, col)) == null)) {
                addPawnMove(moves, position, new ChessPosition(newRow, col));

                int doubleRow = newRow + direction;
                if (row == startRow && board.getPiece(new ChessPosition(doubleRow, col)) == null) {
                    addPawnMove(moves, position, new ChessPosition(doubleRow, col));
                }
            }

            // captures
            int leftCaptureCol = col - 1;
            int rightCaptureCol = col + 1;

            if (leftCaptureCol >= 1) {
                ChessPiece target = board.getPiece(new ChessPosition(newRow, leftCaptureCol));
                if (target != null && target.getTeamColor() != this.getTeamColor()) {
                    addPawnMove(moves, position, new ChessPosition(newRow, leftCaptureCol));
                }
            }

            if (rightCaptureCol <= 8) {
                ChessPiece target = board.getPiece(new ChessPosition(newRow, rightCaptureCol));
                if (target != null && target.getTeamColor() != this.getTeamColor()) {
                    addPawnMove(moves, position, new ChessPosition(newRow, rightCaptureCol));
                }
            }
        }

        return moves;
    }

    private void addPawnMove(Collection<ChessMove> moves, ChessPosition start, ChessPosition end) {
        int lastRow = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? 8 : 1;

        if (end.getRow() == lastRow) {
            moves.add(new ChessMove(start, end, PieceType.QUEEN));
            moves.add(new ChessMove(start, end, PieceType.ROOK));
            moves.add(new ChessMove(start, end, PieceType.BISHOP));
            moves.add(new ChessMove(start, end, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(start, end, null));
        }
    }
}
