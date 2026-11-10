package ui;

import chess.ChessBoard;
import chess.ChessGame;
import client.ResponseException;
import client.ServerFacade;
import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static ui.EscapeSequences.*;

public class Repl {

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);

    // authToken is null when logged out
    private String authToken = null;
    private String username = null;
    private boolean running = true;

    // games from the most recent "list", so "play 2" can find the real game ID
    private List<GameData> lastListedGames = null;

    public Repl(ServerFacade facade) {
        this.facade = facade;
    }

    public void run() {
        System.out.println("♕ Welcome to 240 Chess. Type help to get started.");

        while (running) {
            printPrompt();
            if (!scanner.hasNextLine()) {
                break; // input was closed (e.g. Ctrl+D)
            }
            String line = scanner.nextLine();
            String[] words = line.trim().split("\\s+");

            if (words[0].isEmpty()) {
                continue;
            }

            try {
                if (authToken == null) {
                    handlePreloginCommand(words);
                } else {
                    handlePostloginCommand(words);
                }
            } catch (Exception e) {
                printError("Something went wrong. Please try again.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void printPrompt() {
        if (authToken == null) {
            System.out.print("\n[LOGGED_OUT] >>> ");
        } else {
            System.out.print("\n[" + username + "] >>> ");
        }
    }


    // ---------- Prelogin ----------

    private void handlePreloginCommand(String[] words) {
        String command = words[0].toLowerCase();

        switch (command) {
            case "help" -> printPreloginHelp();
            case "quit" -> running = false;
            case "register" -> register(words);
            case "login" -> login(words);
            case "logout", "create", "list", "play", "observe" -> printError("Please log in first.");
            default -> printError("Unknown command. Type help to see what you can do.");
        }
    }

    private void printPreloginHelp() {
        System.out.println("  register <USERNAME> <PASSWORD> <EMAIL> - create an account");
        System.out.println("  login <USERNAME> <PASSWORD> - log in to play chess");
        System.out.println("  quit - exit the program");
        System.out.println("  help - show this list");
    }

    private void register(String[] words) {
        if (words.length != 4) {
            printError("Usage: register <USERNAME> <PASSWORD> <EMAIL>");
            return;
        }

        try {
            RegisterResult result = facade.register(new RegisterRequest(words[1], words[2], words[3]));
            authToken = result.authToken();
            username = result.username();
            System.out.println("Registered and logged in as " + username + ".");
        } catch (ResponseException e) {
            if (e.getStatusCode() == 403) {
                printError("That username is already taken.");
            } else {
                printError(friendlyMessage(e));
            }
        }
    }

    private void login(String[] words) {
        if (words.length != 3) {
            printError("Usage: login <USERNAME> <PASSWORD>");
            return;
        }

        try {
            LoginResult result = facade.login(new LoginRequest(words[1], words[2]));
            authToken = result.authToken();
            username = result.username();
            System.out.println("Logged in as " + username + ".");
        } catch (ResponseException e) {
            if (e.getStatusCode() == 401) {
                printError("Incorrect username or password.");
            } else {
                printError(friendlyMessage(e));
            }
        }
    }


    // ---------- Postlogin ----------

    private void handlePostloginCommand(String[] words) {
        String command = words[0].toLowerCase();

        switch (command) {
            case "help" -> printPostloginHelp();
            case "logout" -> logout();
            case "create" -> createGame(words);
            case "list" -> listGames();
            case "play" -> playGame(words);
            case "observe" -> observeGame(words);
            case "quit" -> quitWhileLoggedIn();
            default -> printError("Unknown command. Type help to see what you can do.");
        }
    }

    private void printPostloginHelp() {
        System.out.println("  create <NAME> - create a new game");
        System.out.println("  list - list all games");
        System.out.println("  play <NUMBER> <WHITE|BLACK> - join a game");
        System.out.println("  observe <NUMBER> - watch a game");
        System.out.println("  logout - log out");
        System.out.println("  quit - exit the program");
        System.out.println("  help - show this list");
    }

    private void logout() {
        try {
            facade.logout(authToken);
            System.out.println("Logged out.");
        } catch (ResponseException e) {
            if (e.getStatusCode() == 401) {
                System.out.println("Logged out.");  // session was already gone on the server
            } else {
                printError(friendlyMessage(e));
            }
        }
        // clear local state either way so the user isn't stuck logged in
        clearSession();
    }

    // Log out quietly on the way out so the session doesn't linger on the server
    private void quitWhileLoggedIn() {
        try {
            facade.logout(authToken);
        } catch (ResponseException e) {
            // nothing useful to tell the user while quitting
        }
        clearSession();
        running = false;
    }

    private void createGame(String[] words) {
        if (words.length < 2) {
            printError("Usage: create <NAME>");
            return;
        }

        // allow names with spaces, e.g. "create my cool game"
        String gameName = words[1];
        for (int i = 2; i < words.length; i++) {
            gameName = gameName + " " + words[i];
        }

        try {
            facade.createGame(authToken, new CreateGameRequest(gameName));
            System.out.println("Created game \"" + gameName + "\". Use list to see it.");
        } catch (ResponseException e) {
            handleServerError(e);
        }
    }

    private void listGames() {
        try {
            ListGamesResult result = facade.listGames(authToken);
            lastListedGames = new ArrayList<>(result.games());
        } catch (ResponseException e) {
            handleServerError(e);
            return;
        }

        if (lastListedGames.isEmpty()) {
            System.out.println("No games yet. Use create <NAME> to make one.");
            return;
        }

        // pad names so the WHITE/BLACK columns line up
        int nameWidth = 0;
        for (GameData game : lastListedGames) {
            nameWidth = Math.max(nameWidth, game.gameName().length());
        }

        for (int i = 0; i < lastListedGames.size(); i++) {
            GameData game = lastListedGames.get(i);
            String number = String.format("%3d. ", i + 1);
            String name = String.format("%-" + nameWidth + "s", game.gameName());
            System.out.println(number + name
                    + "   WHITE: " + String.format("%-12s", playerName(game.whiteUsername()))
                    + " BLACK: " + playerName(game.blackUsername()));
        }
    }

    private void playGame(String[] words) {
        if (words.length != 3) {
            printError("Usage: play <NUMBER> <WHITE|BLACK>");
            return;
        }

        GameData game = findListedGame(words[1]);
        if (game == null) {
            return; // findListedGame already printed the problem
        }

        ChessGame.TeamColor color;
        if (words[2].equalsIgnoreCase("white")) {
            color = ChessGame.TeamColor.WHITE;
        } else if (words[2].equalsIgnoreCase("black")) {
            color = ChessGame.TeamColor.BLACK;
        } else {
            printError("Color must be WHITE or BLACK.");
            return;
        }

        // already sitting in that seat? just show the board again
        String currentPlayer;
        if (color == ChessGame.TeamColor.WHITE) {
            currentPlayer = game.whiteUsername();
        } else {
            currentPlayer = game.blackUsername();
        }
        if (username.equals(currentPlayer)) {
            System.out.println("Rejoining \"" + game.gameName() + "\" as " + color + ".");
            drawBoard(game, color);
            return;
        }

        try {
            facade.joinGame(authToken, new JoinGameRequest(color, game.gameID()));
            System.out.println("Joined \"" + game.gameName() + "\" as " + color + ".");
            rememberSeat(game, color);
            drawBoard(game, color);
        } catch (ResponseException e) {
            if (e.getStatusCode() == 403) {
                printError("That color is already taken in this game.");
            } else if (e.getStatusCode() == 400) {
                printError("That game no longer exists. Run list to refresh.");
            } else {
                handleServerError(e);
            }
        }
    }

    private void observeGame(String[] words) {
        if (words.length != 2) {
            printError("Usage: observe <NUMBER>");
            return;
        }

        GameData game = findListedGame(words[1]);
        if (game == null) {
            return;
        }

        System.out.println("Observing \"" + game.gameName() + "\".");
        drawBoard(game, ChessGame.TeamColor.WHITE);
    }

    // Turns the number the user typed into a game from the last list.
    // Prints a message and returns null if it can't.
    private GameData findListedGame(String numberText) {
        if (lastListedGames == null) {
            printError("Run list first to see game numbers.");
            return null;
        }

        int number;
        try {
            number = Integer.parseInt(numberText);
        } catch (NumberFormatException e) {
            printError("\"" + numberText + "\" is not a game number.");
            return null;
        }

        if (number < 1 || number > lastListedGames.size()) {
            printError("No game with that number. Run list to see the games.");
            return null;
        }
        return lastListedGames.get(number - 1);
    }

    // Update our saved list so it shows we're now in that seat
    private void rememberSeat(GameData game, ChessGame.TeamColor color) {
        GameData updated;
        if (color == ChessGame.TeamColor.WHITE) {
            updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
        } else {
            updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
        }
        int index = lastListedGames.indexOf(game);
        lastListedGames.set(index, updated);
    }

    private String playerName(String name) {
        if (name == null) {
            return "(open)";
        }
        return name;
    }

    private void drawBoard(GameData game, ChessGame.TeamColor perspective) {
        ChessBoard board;
        if (game.game() != null) {
            board = game.game().getBoard();
        } else {
            board = new ChessBoard();
            board.resetBoard();
        }
        BoardPrinter.drawBoard(board, perspective);
    }


    // ---------- Helpers ----------

    // For errors while logged in. A 401 means our session is gone
    // (e.g. the server restarted), so drop back to the logged-out menu.
    private void handleServerError(ResponseException e) {
        if (e.getStatusCode() == 401) {
            clearSession();
            printError("Your session has expired. Please log in again.");
        } else {
            printError(friendlyMessage(e));
        }
    }

    private void clearSession() {
        authToken = null;
        username = null;
        lastListedGames = null;
    }

    // General messages for errors that aren't specific to one command
    private String friendlyMessage(ResponseException e) {
        int code = e.getStatusCode();
        if (code == 0) {
            return "Can't reach the server. Is it running?";
        } else if (code == 400) {
            return "Missing or invalid information.";
        } else if (code == 401) {
            return "You need to log in again.";
        } else if (code == 403) {
            return "That's already taken.";
        } else {
            return "Something went wrong. Please try again.";
        }
    }

    private void printError(String message) {
        System.out.println(SET_TEXT_COLOR_RED + message + RESET_TEXT_COLOR);
    }
}
