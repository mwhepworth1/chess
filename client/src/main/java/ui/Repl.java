package ui;

import client.ResponseException;
import client.ServerFacade;
import model.*;

import java.util.Scanner;

import static ui.EscapeSequences.*;

public class Repl {

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);

    // authToken is null when logged out
    private String authToken = null;
    private String username = null;
    private boolean running = true;

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


    // ---------- Postlogin (filled in during Chunk 4) ----------

    private void handlePostloginCommand(String[] words) {
        String command = words[0].toLowerCase();

        switch (command) {
            case "help" -> System.out.println("  quit - exit the program\n  help - show this list");
            case "quit" -> running = false;
            default -> printError("Unknown command. Type help to see what you can do.");
        }
    }


    // ---------- Helpers ----------

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
