package client;

import ui.Repl;

public class ClientMain {
    public static void main(String[] args) {
        ServerFacade facade = new ServerFacade(8080);
        Repl repl = new Repl(facade);
        repl.run();
    }
}
