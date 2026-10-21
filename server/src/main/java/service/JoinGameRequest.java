package service;

import chess.ChessGame;

public record JoinGameRequest(ChessGame.TeamColor playerColor, Integer gameID) {
}
