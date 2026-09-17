package chess;

import java.util.Arrays;
import java.util.List;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    private ChessPiece[][] squares = new ChessPiece[8][8];
    private ChessPiece.PieceType[] backRank = {ChessPiece.PieceType.ROOK, ChessPiece.PieceType.KNIGHT, ChessPiece.PieceType.BISHOP, ChessPiece.PieceType.QUEEN, ChessPiece.PieceType.KING, ChessPiece.PieceType.BISHOP, ChessPiece.PieceType.KNIGHT, ChessPiece.PieceType.ROOK};
    private int rowLength = 8;
    public ChessBoard() {
        
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
          squares[position.getRow() -1][position.getColumn() -1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return squares[position.getRow() -1][position.getColumn() -1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        // reset board should clear the squares array
        squares = new ChessPiece[8][8];

        for (int row = 1; row <= rowLength; row++) {
            if(List.of(3,4,5,6).contains(row)) continue; // adios empty rows

            for (int col = 1; col <= backRank.length; col++) {
                // row 1 and 2 are always gonna be white, so assume everything else can be black
                ChessGame.TeamColor color = (row == 1 || row == 2) ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
                // back rank row 1 and 8, pawns rows 2 and 7. nothing else.
                ChessPiece.PieceType piece = (row == 1 || row == 8) ? backRank[(col - 1)] : (row == 2 || row == 7) ? ChessPiece.PieceType.PAWN : null;

                this.addPiece(new ChessPosition(row, col), new ChessPiece(color, piece));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChessBoard that = (ChessBoard) o;
        return Arrays.deepEquals(squares, that.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }
}
