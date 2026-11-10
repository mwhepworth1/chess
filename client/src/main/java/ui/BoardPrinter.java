package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import static ui.EscapeSequences.*;

public class BoardPrinter {

    // Change these to restyle the board
    private static final String LIGHT_SQUARE = SET_BG_COLOR_LIGHT_GREY;
    private static final String DARK_SQUARE = SET_BG_COLOR_DARK_GREEN;
    private static final String BORDER = SET_BG_COLOR_DARK_GREY;
    private static final String BORDER_TEXT = SET_TEXT_COLOR_WHITE;
    private static final String WHITE_PIECE_TEXT = SET_TEXT_COLOR_WHITE;
    private static final String BLACK_PIECE_TEXT = SET_TEXT_COLOR_BLACK;

    private static final String[] COLUMN_LETTERS = {"a", "b", "c", "d", "e", "f", "g", "h"};

    // Draws the board. WHITE puts a1 in the bottom left, BLACK puts a1 in the top right.
    public static void drawBoard(ChessBoard board, ChessGame.TeamColor perspective) {
        boolean whiteView = (perspective == ChessGame.TeamColor.WHITE);

        System.out.println();
        drawColumnLetters(whiteView);

        for (int i = 0; i < 8; i++) {
            int row;
            if (whiteView) {
                row = 8 - i;   // 8, 7, ... 1
            } else {
                row = 1 + i;   // 1, 2, ... 8
            }
            drawRow(board, row, whiteView);
        }

        drawColumnLetters(whiteView);
    }

    private static void drawColumnLetters(boolean whiteView) {
        System.out.print(BORDER + BORDER_TEXT + EMPTY);

        for (int i = 0; i < 8; i++) {
            int col;
            if (whiteView) {
                col = 1 + i;   // a ... h
            } else {
                col = 8 - i;   // h ... a
            }
            System.out.print(" " + COLUMN_LETTERS[col - 1] + " ");
        }

        System.out.print(EMPTY);
        endLine();
    }

    private static void drawRow(ChessBoard board, int row, boolean whiteView) {
        // row number on the left
        System.out.print(BORDER + BORDER_TEXT + " " + row + " ");

        for (int i = 0; i < 8; i++) {
            int col;
            if (whiteView) {
                col = 1 + i;
            } else {
                col = 8 - i;
            }

            // a square is light when row + col is odd (so a1 is dark, h1 and a8 are light)
            if ((row + col) % 2 == 1) {
                System.out.print(LIGHT_SQUARE);
            } else {
                System.out.print(DARK_SQUARE);
            }

            ChessPiece piece = board.getPiece(new ChessPosition(row, col));
            System.out.print(getPieceSymbol(piece));
        }

        // row number on the right
        System.out.print(BORDER + BORDER_TEXT + " " + row + " ");
        endLine();
    }

    private static String getPieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        }

        if (piece.getTeamColor() == ChessGame.TeamColor.WHITE) {
            String symbol = switch (piece.getPieceType()) {
                case KING -> WHITE_KING;
                case QUEEN -> WHITE_QUEEN;
                case BISHOP -> WHITE_BISHOP;
                case KNIGHT -> WHITE_KNIGHT;
                case ROOK -> WHITE_ROOK;
                case PAWN -> WHITE_PAWN;
            };
            return WHITE_PIECE_TEXT + symbol;
        } else {
            String symbol = switch (piece.getPieceType()) {
                case KING -> BLACK_KING;
                case QUEEN -> BLACK_QUEEN;
                case BISHOP -> BLACK_BISHOP;
                case KNIGHT -> BLACK_KNIGHT;
                case ROOK -> BLACK_ROOK;
                case PAWN -> BLACK_PAWN;
            };
            return BLACK_PIECE_TEXT + symbol;
        }
    }

    // Reset colors before the newline so the background doesn't bleed across the terminal
    private static void endLine() {
        System.out.println(RESET_BG_COLOR + RESET_TEXT_COLOR);
    }
}
