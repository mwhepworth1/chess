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
        this.lastMove = null;
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
        if (piece == null) return null;

        ArrayList<ChessMove> legalMoves = new ArrayList<>();
        for (ChessMove move : piece.pieceMoves(activeBoard, startPosition)) {
            ChessBoard copy = activeBoard.createCopy(); // this creates a copy and does not point copy to the same entry of activeBoard in ram!!

            movePiece(copy, move, piece);

            if (!isInCheckAnyBoard(piece.getTeamColor(), copy)) {
                legalMoves.add(move);
            }
        }

        // Castling Moves Logic
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            TeamColor teamColor = piece.getTeamColor();
            int row = (TeamColor.WHITE == teamColor) ? 1 : 8;
            boolean kingMoved = (TeamColor.WHITE == teamColor) ? whiteKingMoved : blackKingMoved;
            ChessPosition kingStartPosition = new ChessPosition(row, 5);

            if (startPosition.equals(kingStartPosition) && !kingMoved && !isInCheck(teamColor)) {
                // kingside
                boolean rightRookMoved = (TeamColor.WHITE == teamColor) ? whiteRightRookMoved : blackRightRookMoved;
                ChessPosition kingsideRook = new ChessPosition(row, 8);
                ChessPosition kingsidePass = new ChessPosition(row, 6);
                ChessPosition kingsideLand = new ChessPosition(row, 7);

                if (isUnmovedRook(kingsideRook, teamColor, rightRookMoved)
                        && isEmpty(kingsidePass) && isEmpty(kingsideLand)
                        && kingSafeAt(teamColor, startPosition, kingsidePass)
                        && kingSafeAt(teamColor, startPosition, kingsideLand)) {
                    legalMoves.add(new ChessMove(startPosition, kingsideLand, null));
                }

                //queenside
                boolean leftRookMoved = (TeamColor.WHITE == teamColor) ? whiteLeftRookMoved : blackLeftRookMoved;
                ChessPosition queensideRook = new ChessPosition(row, 1);
                ChessPosition queensideGap  = new ChessPosition(row, 2);
                ChessPosition queensidePass = new ChessPosition(row, 4);
                ChessPosition queensideLand = new ChessPosition(row, 3);

                if (isUnmovedRook(queensideRook, teamColor, leftRookMoved)
                        && isEmpty(queensideGap) && isEmpty(queensideLand) && isEmpty(queensidePass)
                        && kingSafeAt(teamColor, startPosition, queensidePass)
                        && kingSafeAt(teamColor, startPosition, queensideLand)) {
                    legalMoves.add(new ChessMove(startPosition, queensideLand, null));
                }
            }

        }
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN && lastMove != null) {
            ChessPosition lastMoveStartPosition = lastMove.getStartPosition();
            ChessPosition lastMoveEndPosition = lastMove.getEndPosition();
            ChessPiece lastPiece = activeBoard.getPiece(lastMoveEndPosition);

            boolean enemyPawn = lastPiece != null && lastPiece.getPieceType() == ChessPiece.PieceType.PAWN && lastPiece.getTeamColor() != piece.getTeamColor();
            boolean doubleStep = Math.abs(lastMoveEndPosition.getRow() - lastMoveStartPosition.getRow()) == 2;
            boolean beside = lastMoveEndPosition.getRow() == startPosition.getRow() && Math.abs(lastMoveEndPosition.getColumn() - startPosition.getColumn()) == 1;

            if (enemyPawn && doubleStep && beside) {
                int direction = (piece.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
                ChessPosition landing = new ChessPosition(startPosition.getRow() + direction, lastMoveEndPosition.getColumn());
                ChessMove epMove = new ChessMove(startPosition, landing, null);

                ChessBoard copy = activeBoard.createCopy();
                movePiece(copy, epMove, piece);
                copy.addPiece(lastMoveEndPosition, null); // this would be the pawn we captured
                if (!isInCheckAnyBoard(piece.getTeamColor(), copy)) {
                    legalMoves.add(epMove);
                }
            }
        }

        return legalMoves;
    }
    private boolean isEmpty(ChessPosition position) {
        return activeBoard.getPiece(position) == null;
    }
    private boolean isUnmovedRook (ChessPosition position, TeamColor team, boolean rookMoved) {
        ChessPiece piece = activeBoard.getPiece(position);
        return piece != null && piece.getPieceType() == ChessPiece.PieceType.ROOK && piece.getTeamColor() == team && !rookMoved;
    }
    private boolean kingSafeAt(TeamColor team, ChessPosition from, ChessPosition to) {
        ChessBoard copy  = activeBoard.createCopy();
        movePiece(copy, new ChessMove(from, to, null), activeBoard.getPiece(from));
        return !isInCheckAnyBoard(team, copy);
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

        if (piece == null) throw new InvalidMoveException("No active piece.");
        if (piece.getTeamColor() != activeColor) throw new InvalidMoveException("Targeted piece does not belong to the current player!");
        Collection<ChessMove> moves = validMoves(start);
        if (!moves.contains(move)) throw new InvalidMoveException("Invalid move.");
        ChessPiece.PieceType promotionPiece = move.getPromotionPiece();

        ChessPiece pieceToPlace = piece;
        if (promotionPiece != null) {
            pieceToPlace = new ChessPiece(piece.getTeamColor(), promotionPiece);
        }

        boolean isEnPassant = piece.getPieceType() == ChessPiece.PieceType.PAWN
                && start.getColumn() != move.getEndPosition().getColumn()
                && activeBoard.getPiece(move.getEndPosition()) == null;

        movePiece(activeBoard, move, pieceToPlace);

        if (isEnPassant) {
            activeBoard.addPiece(new ChessPosition(start.getRow(), move.getEndPosition().getColumn()), null);
        }

        int colChange = move.getEndPosition().getColumn() - start.getColumn();
        if (piece.getPieceType() == ChessPiece.PieceType.KING && Math.abs(colChange) == 2) {
            int row = start.getRow();
            if (colChange > 0) {
                ChessPosition rookFrom = new ChessPosition(row, 8);
                ChessPosition rookTo = new ChessPosition(row, 6);
                movePiece(activeBoard, new ChessMove(rookFrom, rookTo, null), activeBoard.getPiece(rookFrom));
            } else {
                ChessPosition rookFrom = new ChessPosition(row, 1);
                ChessPosition rookTo = new ChessPosition(row, 4);
                movePiece(activeBoard, new ChessMove(rookFrom, rookTo, null), activeBoard.getPiece(rookFrom));
            }
        }

        updateCastlingFlags(move);

        // switch turns since one move per turn
        activeColor = (activeColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        this.lastMove = move;
    }

    private void updateCastlingFlags(ChessMove move) {
        for (ChessPosition pos : List.of(move.getStartPosition(), move.getEndPosition())) {
            if (pos.equals(new ChessPosition(1, 5))) whiteKingMoved = true;
            if (pos.equals(new ChessPosition(1, 1))) whiteLeftRookMoved = true;
            if (pos.equals(new ChessPosition(1, 8))) whiteRightRookMoved = true;
            if (pos.equals(new ChessPosition(8, 5))) blackKingMoved = true;
            if (pos.equals(new ChessPosition(8, 1))) blackLeftRookMoved = true;
            if (pos.equals(new ChessPosition(8, 8))) blackRightRookMoved = true;
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
        if (kingPosition == null) return false;
        for (int row = 1; row <= 8; row++){
            for (int col = 1; col <= 8; col++){
                ChessPosition currentPosition = new ChessPosition(row, col);
                ChessPiece target = board.getPiece(currentPosition);
                if ((target != null) && (target.getTeamColor() != teamColor)) {
                    for (ChessMove move : target.pieceMoves(board, currentPosition)) {
                        if (move.getEndPosition().equals(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private ChessPosition findKing(TeamColor team, ChessBoard board) {
        for (int row = 1; row <= 8; row++) {
            for (int col = 1; col <= 8; col++) {
                ChessPiece target = board.getPiece(new ChessPosition(row, col));
                if (target == null) continue;
                boolean isKing = target.getPieceType() == ChessPiece.PieceType.KING;
                if (isKing && target.getTeamColor() != team) continue;
                if (isKing) {
                    return new ChessPosition(row, col);
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
        this.lastMove = null;
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
        for (int row = 1; row <=8;row++) {
            for (int col=1;col<=8;col++) {
                ChessPosition position = new ChessPosition(row, col);
                ChessPiece piece = activeBoard.getPiece(position);
                if ((piece != null) && (piece.getTeamColor() == teamColor)) {
                    if (!validMoves(position).isEmpty()) return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChessGame chessGame = (ChessGame) o;
        return whiteKingMoved == chessGame.whiteKingMoved && blackKingMoved == chessGame.blackKingMoved && whiteLeftRookMoved == chessGame.whiteLeftRookMoved && blackLeftRookMoved == chessGame.blackLeftRookMoved && whiteRightRookMoved == chessGame.whiteRightRookMoved && blackRightRookMoved == chessGame.blackRightRookMoved && Objects.equals(activeBoard, chessGame.activeBoard) && activeColor == chessGame.activeColor && Objects.equals(lastMove, chessGame.lastMove);
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
