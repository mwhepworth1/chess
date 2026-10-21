package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private static final int KING_START_COL = 5;
    private static final int LEFT_ROOK_COL = 1;
    private static final int RIGHT_ROOK_COL = 8;

    private ChessBoard activeBoard = new ChessBoard();
    private TeamColor activeColor;

    // Castling Fields
    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteLeftRookMoved;
    private boolean blackLeftRookMoved;
    private boolean whiteRightRookMoved;
    private boolean blackRightRookMoved;

    // En Passant
    private ChessMove lastMove;

    public ChessGame() {
        activeBoard.resetBoard();
        activeColor = TeamColor.WHITE;
        resetCastling();
    }

    private void resetCastling() {
        this.whiteKingMoved = false;
        this.blackKingMoved = false;
        this.whiteLeftRookMoved = false;
        this.blackLeftRookMoved = false;
        this.whiteRightRookMoved = false;
        this.blackRightRookMoved = false;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return activeColor;
    }

    /**
     * Sets which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        activeColor = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets all valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = activeBoard.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        Collection<ChessMove> legalMoves = new ArrayList<>();
        for (ChessMove move : piece.pieceMoves(activeBoard, startPosition)) {
            if (leavesKingSafe(move, piece)) {
                legalMoves.add(move);
            }
        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            legalMoves.addAll(castlingMoves(startPosition, piece.getTeamColor()));
        }
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            ChessMove enPassant = enPassantMove(startPosition, piece);
            if (enPassant != null) {
                legalMoves.add(enPassant);
            }
        }
        return legalMoves;
    }

    private boolean leavesKingSafe(ChessMove move, ChessPiece piece) {
        ChessBoard copy = activeBoard.createCopy();
        movePiece(copy, move, piece);
        return !isInCheckAnyBoard(piece.getTeamColor(), copy);
    }

    private Collection<ChessMove> castlingMoves(ChessPosition kingPosition, TeamColor team) {
        Collection<ChessMove> moves = new ArrayList<>();
        boolean kingMoved = (team == TeamColor.WHITE) ? whiteKingMoved : blackKingMoved;
        ChessPosition kingStart = new ChessPosition(homeRow(team), KING_START_COL);
        if (!kingPosition.equals(kingStart) || kingMoved || isInCheck(team)) {
            return moves;
        }

        boolean leftRookMoved = (team == TeamColor.WHITE) ? whiteLeftRookMoved : blackLeftRookMoved;
        boolean rightRookMoved = (team == TeamColor.WHITE) ? whiteRightRookMoved : blackRightRookMoved;
        addCastleIfAllowed(moves, kingPosition, team, LEFT_ROOK_COL, leftRookMoved);
        addCastleIfAllowed(moves, kingPosition, team, RIGHT_ROOK_COL, rightRookMoved);
        return moves;
    }

    private void addCastleIfAllowed(Collection<ChessMove> moves, ChessPosition kingPosition, TeamColor team,
                                    int rookCol, boolean rookMoved) {
        int row = kingPosition.getRow();
        int step = (rookCol > KING_START_COL) ? 1 : -1;
        ChessPosition pass = new ChessPosition(row, KING_START_COL + step);
        ChessPosition land = new ChessPosition(row, KING_START_COL + 2 * step);

        if (isUnmovedRook(new ChessPosition(row, rookCol), team, rookMoved)
                && isPathClear(row, rookCol)
                && leavesKingSafe(new ChessMove(kingPosition, pass, null), activeBoard.getPiece(kingPosition))
                && leavesKingSafe(new ChessMove(kingPosition, land, null), activeBoard.getPiece(kingPosition))) {
            moves.add(new ChessMove(kingPosition, land, null));
        }
    }

    private boolean isPathClear(int row, int rookCol) {
        int fromCol = Math.min(KING_START_COL, rookCol) + 1;
        int toCol = Math.max(KING_START_COL, rookCol) - 1;
        for (int col = fromCol; col <= toCol; col++) {
            if (activeBoard.getPiece(new ChessPosition(row, col)) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean isUnmovedRook(ChessPosition position, TeamColor team, boolean rookMoved) {
        ChessPiece piece = activeBoard.getPiece(position);
        return !rookMoved
                && piece != null
                && piece.getPieceType() == ChessPiece.PieceType.ROOK
                && piece.getTeamColor() == team;
    }

    private ChessMove enPassantMove(ChessPosition start, ChessPiece pawn) {
        if (!canCaptureEnPassant(start, pawn)) {
            return null;
        }
        ChessPosition capturedPosition = lastMove.getEndPosition();
        ChessPosition landing = new ChessPosition(start.getRow() + forwardDirection(pawn.getTeamColor()),
                capturedPosition.getColumn());
        ChessMove move = new ChessMove(start, landing, null);

        ChessBoard copy = activeBoard.createCopy();
        movePiece(copy, move, pawn);
        copy.addPiece(capturedPosition, null);
        return isInCheckAnyBoard(pawn.getTeamColor(), copy) ? null : move;
    }

    private boolean canCaptureEnPassant(ChessPosition start, ChessPiece pawn) {
        if (lastMove == null) {
            return false;
        }
        ChessPosition lastStart = lastMove.getStartPosition();
        ChessPosition lastEnd = lastMove.getEndPosition();
        ChessPiece lastPiece = activeBoard.getPiece(lastEnd);

        boolean enemyPawn = lastPiece != null
                && lastPiece.getPieceType() == ChessPiece.PieceType.PAWN
                && lastPiece.getTeamColor() != pawn.getTeamColor();
        boolean doubleStep = Math.abs(lastEnd.getRow() - lastStart.getRow()) == 2;
        boolean beside = lastEnd.getRow() == start.getRow()
                && Math.abs(lastEnd.getColumn() - start.getColumn()) == 1;
        return enemyPawn && doubleStep && beside;
    }

    /**
     * Makes a move in the chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        ChessPiece piece = activeBoard.getPiece(start);
        validateMove(move, piece);

        boolean isEnPassant = piece.getPieceType() == ChessPiece.PieceType.PAWN
                && start.getColumn() != move.getEndPosition().getColumn()
                && activeBoard.getPiece(move.getEndPosition()) == null;

        movePiece(activeBoard, move, pieceAfterMove(piece, move));
        if (isEnPassant) {
            activeBoard.addPiece(new ChessPosition(start.getRow(), move.getEndPosition().getColumn()), null);
        }
        moveRookIfCastling(move, piece);
        updateCastlingFlags(move);

        activeColor = (activeColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        lastMove = move;
    }

    private void validateMove(ChessMove move, ChessPiece piece) throws InvalidMoveException {
        if (piece == null) {
            throw new InvalidMoveException("No active piece.");
        }
        if (piece.getTeamColor() != activeColor) {
            throw new InvalidMoveException("Targeted piece does not belong to the current player!");
        }
        if (!validMoves(move.getStartPosition()).contains(move)) {
            throw new InvalidMoveException("Invalid move.");
        }
    }

    private ChessPiece pieceAfterMove(ChessPiece piece, ChessMove move) {
        if (move.getPromotionPiece() == null) {
            return piece;
        }
        return new ChessPiece(piece.getTeamColor(), move.getPromotionPiece());
    }

    private void moveRookIfCastling(ChessMove move, ChessPiece piece) {
        int colChange = move.getEndPosition().getColumn() - move.getStartPosition().getColumn();
        if (piece.getPieceType() != ChessPiece.PieceType.KING || Math.abs(colChange) != 2) {
            return;
        }
        int row = move.getStartPosition().getRow();
        int rookFromCol = (colChange > 0) ? RIGHT_ROOK_COL : LEFT_ROOK_COL;
        int rookToCol = KING_START_COL + Integer.signum(colChange);

        ChessPosition rookFrom = new ChessPosition(row, rookFromCol);
        ChessPosition rookTo = new ChessPosition(row, rookToCol);
        movePiece(activeBoard, new ChessMove(rookFrom, rookTo, null), activeBoard.getPiece(rookFrom));
    }

    private void updateCastlingFlags(ChessMove move) {
        for (ChessPosition pos : List.of(move.getStartPosition(), move.getEndPosition())) {
            if (pos.equals(new ChessPosition(1, KING_START_COL))) {
                whiteKingMoved = true;
            }
            if (pos.equals(new ChessPosition(1, LEFT_ROOK_COL))) {
                whiteLeftRookMoved = true;
            }
            if (pos.equals(new ChessPosition(1, RIGHT_ROOK_COL))) {
                whiteRightRookMoved = true;
            }
            if (pos.equals(new ChessPosition(ChessBoard.BOARD_SIZE, KING_START_COL))) {
                blackKingMoved = true;
            }
            if (pos.equals(new ChessPosition(ChessBoard.BOARD_SIZE, LEFT_ROOK_COL))) {
                blackLeftRookMoved = true;
            }
            if (pos.equals(new ChessPosition(ChessBoard.BOARD_SIZE, RIGHT_ROOK_COL))) {
                blackRightRookMoved = true;
            }
        }
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        return isInCheckAnyBoard(teamColor, activeBoard);
    }

    private boolean isInCheckAnyBoard(TeamColor teamColor, ChessBoard board) {
        ChessPosition kingPosition = findKing(teamColor, board);
        if (kingPosition == null) {
            return false;
        }
        for (int row = 1; row <= ChessBoard.BOARD_SIZE; row++) {
            for (int col = 1; col <= ChessBoard.BOARD_SIZE; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece attacker = board.getPiece(position);
                if (attacker != null && attacker.getTeamColor() != teamColor
                        && canReach(board, position, attacker, kingPosition)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canReach(ChessBoard board, ChessPosition from, ChessPiece piece, ChessPosition target) {
        for (ChessMove move : piece.pieceMoves(board, from)) {
            if (move.getEndPosition().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private ChessPosition findKing(TeamColor team, ChessBoard board) {
        for (int row = 1; row <= ChessBoard.BOARD_SIZE; row++) {
            for (int col = 1; col <= ChessBoard.BOARD_SIZE; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = board.getPiece(position);
                if (piece != null && piece.getPieceType() == ChessPiece.PieceType.KING
                        && piece.getTeamColor() == team) {
                    return position;
                }
            }
        }
        return null;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !hasAnyValidMoves(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !hasAnyValidMoves(teamColor);
    }

    /**
     * Sets this game's chessboard to a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        activeBoard = board;
        resetCastling();
        lastMove = null;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return activeBoard;
    }

    private void movePiece(ChessBoard board, ChessMove move, ChessPiece piece) {
        board.addPiece(move.getEndPosition(), piece);
        board.addPiece(move.getStartPosition(), null);
    }

    private boolean hasAnyValidMoves(TeamColor teamColor) {
        for (int row = 1; row <= ChessBoard.BOARD_SIZE; row++) {
            for (int col = 1; col <= ChessBoard.BOARD_SIZE; col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = activeBoard.getPiece(position);
                if (piece != null && piece.getTeamColor() == teamColor && !validMoves(position).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int homeRow(TeamColor team) {
        return (team == TeamColor.WHITE) ? 1 : ChessBoard.BOARD_SIZE;
    }

    private static int forwardDirection(TeamColor team) {
        return (team == TeamColor.WHITE) ? 1 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChessGame chessGame = (ChessGame) o;
        return whiteKingMoved == chessGame.whiteKingMoved
                && blackKingMoved == chessGame.blackKingMoved
                && whiteLeftRookMoved == chessGame.whiteLeftRookMoved
                && blackLeftRookMoved == chessGame.blackLeftRookMoved
                && whiteRightRookMoved == chessGame.whiteRightRookMoved
                && blackRightRookMoved == chessGame.blackRightRookMoved
                && Objects.equals(activeBoard, chessGame.activeBoard)
                && activeColor == chessGame.activeColor
                && Objects.equals(lastMove, chessGame.lastMove);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(activeBoard);
        result = 31 * result + Objects.hashCode(activeColor);
        result = 31 * result + Boolean.hashCode(whiteKingMoved);
        result = 31 * result + Boolean.hashCode(blackKingMoved);
        result = 31 * result + Boolean.hashCode(whiteLeftRookMoved);
        result = 31 * result + Boolean.hashCode(blackLeftRookMoved);
        result = 31 * result + Boolean.hashCode(whiteRightRookMoved);
        result = 31 * result + Boolean.hashCode(blackRightRookMoved);
        result = 31 * result + Objects.hashCode(lastMove);
        return result;
    }
}
