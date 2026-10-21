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
    private static final PieceType[] PROMOTION_TYPES = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT
    };

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

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
        return switch (this.getPieceType()) {
            case BISHOP -> bishopMoves(board, myPosition);
            case ROOK -> rookMoves(board, myPosition);
            case KNIGHT -> knightMoves(board, myPosition);
            case KING -> kingMoves(board, myPosition);
            case QUEEN -> queenMoves(board, myPosition);
            case PAWN -> pawnMoves(board, myPosition);
        };
    }

    private Collection<ChessMove> slidingMoves(ChessBoard board, ChessPosition position, int[][] possibleDirections) {
        Collection<ChessMove> moves = new ArrayList<>();

        for (int[] direction : possibleDirections) {
            int row = position.getRow() + direction[0];
            int col = position.getColumn() + direction[1];

            while (ChessBoard.isOnBoard(row, col)) {
                ChessPosition newPosition = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(newPosition);
                if (canLandOn(target)) {
                    moves.add(new ChessMove(position, newPosition, null));
                }
                if (target != null) {
                    break;
                }
                row += direction[0];
                col += direction[1];
            }
        }
        return moves;
    }

    private Collection<ChessMove> hoppingMoves(ChessBoard board, ChessPosition position, int[][] possibleDirections) {
        Collection<ChessMove> moves = new ArrayList<>();

        for (int[] direction : possibleDirections) {
            int row = position.getRow() + direction[0];
            int col = position.getColumn() + direction[1];
            if (!ChessBoard.isOnBoard(row, col)) {
                continue;
            }

            ChessPosition newPosition = new ChessPosition(row, col);
            if (canLandOn(board.getPiece(newPosition))) {
                moves.add(new ChessMove(position, newPosition, null));
            }
        }
        return moves;
    }

    private boolean canLandOn(ChessPiece target) {
        return target == null || target.getTeamColor() != pieceColor;
    }

    private Collection<ChessMove> bishopMoves(ChessBoard board, ChessPosition position) {
        // bishop always moves in some kind of diagonal line, so make sure it can only do that
        int[][] possibleDirections = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        return slidingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> rookMoves(ChessBoard board, ChessPosition position) {
        // rooks either move up, down, left, or right. Same column for up/down, same row for left/right
        int[][] possibleDirections = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        return slidingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> queenMoves(ChessBoard board, ChessPosition position) {
        // queens move in a straight line horizontally, vertically, or diagonally, so rook moves AND bishop moves.
        int[][] possibleDirections = {
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}, // same as bishop
                {1, 0}, {-1, 0}, {0, 1}, {0, -1} // same as rook
        };
        return slidingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> knightMoves(ChessBoard board, ChessPosition position) {
        // knights move two squares one way, one square perpendicular (never lands next to itself)
        int[][] possibleDirections = {
                {-1, 2}, {1, 2},
                {-2, 1}, {2, 1},

                {-2, -1}, {2, -1},
                {-1, -2}, {1, -2}
        };
        return hoppingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> kingMoves(ChessBoard board, ChessPosition position) {
        // kings move one square in any direction, no more than one square.
        int[][] possibleDirections = {
                {-1, 1}, {0, 1}, {1, 1},
                {-1, 0}, {1, 0},
                {-1, -1}, {0, -1}, {1, -1}
        };
        return hoppingMoves(board, position, possibleDirections);
    }

    private Collection<ChessMove> pawnMoves(ChessBoard board, ChessPosition position) {
        Collection<ChessMove> moves = new ArrayList<>();
        int newRow = position.getRow() + forwardDirection();
        if (!ChessBoard.isOnBoard(newRow, position.getColumn())) {
            return moves;
        }

        addPawnForwardMoves(board, position, moves);
        addPawnCaptures(board, position, moves);
        return moves;
    }

    private void addPawnForwardMoves(ChessBoard board, ChessPosition position, Collection<ChessMove> moves) {
        int startRow = (pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        ChessPosition oneStep = new ChessPosition(position.getRow() + forwardDirection(), position.getColumn());
        if (board.getPiece(oneStep) != null) {
            return;
        }
        addPawnMove(moves, position, oneStep);

        ChessPosition twoStep = new ChessPosition(oneStep.getRow() + forwardDirection(), position.getColumn());
        if (position.getRow() == startRow && board.getPiece(twoStep) == null) {
            addPawnMove(moves, position, twoStep);
        }
    }

    private void addPawnCaptures(ChessBoard board, ChessPosition position, Collection<ChessMove> moves) {
        int newRow = position.getRow() + forwardDirection();
        for (int captureCol : new int[]{position.getColumn() - 1, position.getColumn() + 1}) {
            if (!ChessBoard.isOnBoard(newRow, captureCol)) {
                continue;
            }
            ChessPosition capturePosition = new ChessPosition(newRow, captureCol);
            ChessPiece target = board.getPiece(capturePosition);
            if (target != null && target.getTeamColor() != pieceColor) {
                addPawnMove(moves, position, capturePosition);
            }
        }
    }

    private void addPawnMove(Collection<ChessMove> moves, ChessPosition start, ChessPosition end) {
        int lastRow = (pieceColor == ChessGame.TeamColor.WHITE) ? ChessBoard.BOARD_SIZE : 1;
        if (end.getRow() != lastRow) {
            moves.add(new ChessMove(start, end, null));
            return;
        }
        for (PieceType promotionType : PROMOTION_TYPES) {
            moves.add(new ChessMove(start, end, promotionType));
        }
    }

    private int forwardDirection() {
        return (pieceColor == ChessGame.TeamColor.WHITE) ? 1 : -1;
    }
}
